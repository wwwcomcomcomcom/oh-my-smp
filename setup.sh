#!/usr/bin/env bash
# Portable one-shot setup for the full 4-process test stack:
#   auth-server, Minestom lobby, Velocity proxy, and a Paper "content" server
#   running SmpAuth (content-lib) + the oh-my-smp plugin.
#
# This script writes everything into the directory you RUN it from, so you can
# spin up an isolated environment anywhere:
#
#   mkdir ~/smp-test && cd ~/smp-test
#   /path/to/repo/setup.sh          # builds jars, downloads Velocity + Paper, writes configs
#   ./start-all.sh                  # launch auth + lobby + velocity + paper (each in a tmux session)
#   ./console.sh {auth|lobby|velocity|paper}   # attach to a server's console (detach: Ctrl-B, D)
#   ./stop-all.sh                   # stop them
#
# After changing code, run /path/to/repo/update.sh from the same directory to
# rebuild and swap in just the jars (configs/db/worlds untouched).
#
# Requires tmux (each process runs in its own detached session, so start-all.sh
# returns immediately but you can still attach and type console commands).
#
# Ports default to velocity=25565 lobby=25566 paper=25567 auth=8080; override with
# --velocity-port / --lobby-port / --paper-port / --auth-port (see --help).
#
# Steps: 1. build jars  2. download Velocity + Paper  3. write configs
#        4. copy plugin jars  5. generate + patch Paper's velocity forwarding
#        6. emit start/stop scripts
set -euo pipefail

# ---------------------------------------------------------------- paths
# PROJECT_ROOT = where this script lives (the repo root, next to gradlew).
# RUN_DIR      = the directory you invoked the script from (current working dir).
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$PWD"
JARS_DIR="$RUN_DIR/jars"
LOG_DIR="$RUN_DIR/logs"

log() { printf "\033[1;36m[setup]\033[0m %s\n" "$*"; }

# ---------------------------------------------------------------- ports (overridable via CLI flags)
VELOCITY_PORT=25565   # players connect here
LOBBY_PORT=25566      # Minestom lobby (Velocity server "lobby")
PAPER_PORT=25567      # Paper content server (Velocity server "content")
AUTH_PORT=8080        # auth web server

usage() {
  cat <<USAGE
Usage: $0 [options]

Optional port overrides (defaults shown):
  --velocity-port PORT   Velocity proxy port, what players connect to (default: $VELOCITY_PORT)
  --lobby-port PORT      Minestom lobby port (default: $LOBBY_PORT)
  --paper-port PORT      Paper content server port (default: $PAPER_PORT)
  --auth-port PORT       Auth web server port (default: $AUTH_PORT)
  -h, --help             Show this help and exit
USAGE
}

is_valid_port() { [[ "$1" =~ ^[0-9]+$ ]] && [ "$1" -ge 1 ] && [ "$1" -le 65535 ]; }

while [ $# -gt 0 ]; do
  case "$1" in
    --velocity-port) VELOCITY_PORT="$2"; shift 2 ;;
    --velocity-port=*) VELOCITY_PORT="${1#*=}"; shift ;;
    --lobby-port) LOBBY_PORT="$2"; shift 2 ;;
    --lobby-port=*) LOBBY_PORT="${1#*=}"; shift ;;
    --paper-port) PAPER_PORT="$2"; shift 2 ;;
    --paper-port=*) PAPER_PORT="${1#*=}"; shift ;;
    --auth-port) AUTH_PORT="$2"; shift 2 ;;
    --auth-port=*) AUTH_PORT="${1#*=}"; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown option: $1" >&2; usage >&2; exit 1 ;;
  esac
done

for name in VELOCITY_PORT LOBBY_PORT PAPER_PORT AUTH_PORT; do
  is_valid_port "${!name}" || { echo "Invalid port for $name: ${!name}" >&2; usage >&2; exit 1; }
done

if [ "$RUN_DIR" = "$PROJECT_ROOT" ]; then
  echo "Refusing to run inside the repo root (it would litter the source tree)." >&2
  echo "Make a scratch dir and run from there, e.g.:  mkdir ~/smp-test && cd ~/smp-test && $0" >&2
  exit 1
fi

# ---------------------------------------------------------------- Java 25
JAVA25="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java"
if [ -x "$JAVA25" ]; then JAVA="$JAVA25"; else JAVA="java"; fi

# ---------------------------------------------------------------- versions
PAPER_VERSION="26.2"                 # Paper server (Minecraft 26.2)
VELOCITY_VERSION="3.5.0-SNAPSHOT"    # first line that speaks the Minecraft 26.2 protocol

# ---------------------------------------------------------------- secrets (defaults, overridable)
FORWARDING_SECRET="smp-test-forwarding-secret"   # Velocity <-> lobby/paper modern forwarding
SHARED_SECRET="smp-test-shared-secret"           # auth-server <-> lobby <-> velocity plugin
DATAGSM_CLIENT_ID="test-client-id"
DATAGSM_CLIENT_SECRET="test-client-secret"
PUBLIC_BASE_URL="http://127.0.0.1:${AUTH_PORT}"
# Drop a secrets.env next to where you run this to override the above.
[ -f "$RUN_DIR/secrets.env" ] && source "$RUN_DIR/secrets.env"

# Built artifact paths are resolved *after* the build (step 1) — all modules inherit
# version=1.0-SNAPSHOT from gradle.properties, so filenames carry the version.

mkdir -p "$JARS_DIR" "$RUN_DIR"/auth "$RUN_DIR"/lobby \
         "$RUN_DIR"/velocity/plugins/smp-auth \
         "$RUN_DIR"/paper/plugins "$RUN_DIR"/paper/config \
         "$LOG_DIR"

# ---------------------------------------------------------------- 1. build
log "Building project jars (shadow)…"
( cd "$PROJECT_ROOT" && ./gradlew --console=plain \
    :auth-server:shadowJar :lobby-server:shadowJar :velocity-plugin:shadowJar \
    :content-lib:shadowJar :smp-server:shadowJar )

# Resolve built jars (versioned filenames). The auth/lobby fat jars use Shadow's "-all"
# classifier; the Paper/Velocity plugins are the shaded classifier-less jar (not "-thin").
AUTH_JAR="$(ls "$PROJECT_ROOT"/auth-server/build/libs/auth-server-*-all.jar | head -1)"
LOBBY_JAR="$(ls "$PROJECT_ROOT"/lobby-server/build/libs/lobby-server-*-all.jar | head -1)"
VELOCITY_PLUGIN_JAR="$(ls "$PROJECT_ROOT"/velocity-plugin/build/libs/velocity-plugin-*.jar | grep -v -- '-thin' | head -1)"
SMPAUTH_JAR="$(ls "$PROJECT_ROOT"/content-lib/build/libs/content-lib-*.jar | grep -v -- '-thin' | head -1)"
OHMYSMP_JAR="$(ls "$PROJECT_ROOT"/smp-server/build/libs/oh-my-smp-*-all.jar | head -1)"

# ---------------------------------------------------------------- 2. download Velocity + Paper
fill_url() { # project version -> latest build download URL (PaperMC v3 "fill" API)
  curl -fsSL "https://fill.papermc.io/v3/projects/$1/versions/$2/builds" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['downloads']['server:default']['url'])"
}
download() { # url dest
  if [ -s "$2" ]; then log "exists: $(basename "$2")"; else log "downloading $(basename "$2") …"; curl -fsSL -o "$2" "$1"; fi
}
log "Resolving Paper ${PAPER_VERSION} + Velocity ${VELOCITY_VERSION} (v3 API)…"
download "$(fill_url paper "$PAPER_VERSION")"       "$JARS_DIR/paper.jar"
download "$(fill_url velocity "$VELOCITY_VERSION")" "$JARS_DIR/velocity.jar"

# ---------------------------------------------------------------- 3. configs
log "Writing auth-server config…"
cat > "$RUN_DIR/auth/config.properties" <<EOF
server.port=${AUTH_PORT}
server.publicBaseUrl=${PUBLIC_BASE_URL}
oauth.redirectUri=${PUBLIC_BASE_URL}/callback
datagsm.clientId=${DATAGSM_CLIENT_ID}
datagsm.clientSecret=${DATAGSM_CLIENT_SECRET}
datagsm.scope=self:read
security.sharedSecret=${SHARED_SECRET}
db.path=smp-auth.db
key.ttlSeconds=300
key.length=8
EOF

log "Writing lobby config…"
cat > "$RUN_DIR/lobby/config.properties" <<EOF
host=0.0.0.0
port=${LOBBY_PORT}
velocitySecret=${FORWARDING_SECRET}
authServerBaseUrl=http://127.0.0.1:${AUTH_PORT}
authLoginUrl=${PUBLIC_BASE_URL}/login
sharedSecret=${SHARED_SECRET}
EOF

log "Writing Velocity config (lobby + content)…"
cat > "$RUN_DIR/velocity/velocity.toml" <<EOF
config-version = "2.7"
bind = "0.0.0.0:${VELOCITY_PORT}"
motd = "<aqua>SMP Test Network"
show-max-players = 50
# Offline so you can join locally without a premium account; modern forwarding still
# carries the (offline) profile to backends. Set true for production (real Mojang auth).
online-mode = false
force-key-authentication = false
prevent-client-proxy-connections = false
player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"
announce-forge = false
kick-existing-players = false
ping-passthrough = "DISABLED"

[servers]
lobby = "127.0.0.1:${LOBBY_PORT}"
content = "127.0.0.1:${PAPER_PORT}"
try = ["lobby"]

[forced-hosts]

[advanced]
compression-threshold = 256
login-ratelimit = 0

[query]
enabled = false
EOF
printf '%s' "$FORWARDING_SECRET" > "$RUN_DIR/velocity/forwarding.secret"

log "Writing Velocity plugin (smp-auth) config…"
cat > "$RUN_DIR/velocity/plugins/smp-auth/config.properties" <<EOF
authServerBaseUrl=http://127.0.0.1:${AUTH_PORT}
sharedSecret=${SHARED_SECRET}
lobbyServerName=lobby
gatedServers=
EOF

log "Writing Paper eula + server.properties…"
echo "eula=true" > "$RUN_DIR/paper/eula.txt"
cat > "$RUN_DIR/paper/server.properties" <<EOF
online-mode=false
server-port=${PAPER_PORT}
server-ip=127.0.0.1
motd=SMP Content (test)
level-type=minecraft\\:flat
level-name=world
spawn-protection=0
max-players=20
view-distance=6
simulation-distance=6
network-compression-threshold=256
enable-command-block=false
allow-nether=false
EOF

# ---------------------------------------------------------------- 4. project jars
# Copied to stable (version-less) names so start scripts and update.sh never
# depend on the repo's current version string.
log "Copying project jars…"
cp -f "$AUTH_JAR"            "$JARS_DIR/auth-server-all.jar"
cp -f "$LOBBY_JAR"           "$JARS_DIR/lobby-server-all.jar"
cp -f "$VELOCITY_PLUGIN_JAR" "$RUN_DIR/velocity/plugins/velocity-plugin.jar"
cp -f "$SMPAUTH_JAR"         "$RUN_DIR/paper/plugins/SmpAuth.jar"
cp -f "$OHMYSMP_JAR"         "$RUN_DIR/paper/plugins/oh-my-smp.jar"

# ---------------------------------------------------------------- 5. Paper velocity forwarding
PG="$RUN_DIR/paper/config/paper-global.yml"
if grep -q "enabled: true" "$PG" 2>/dev/null && grep -q "$FORWARDING_SECRET" "$PG" 2>/dev/null; then
  log "Paper velocity forwarding already configured."
else
  log "Generating Paper config (first boot, ~30-60s)…"
  ( cd "$RUN_DIR/paper" && exec "$JAVA" -Xms512M -Xmx1G -jar "$JARS_DIR/paper.jar" nogui ) > "$LOG_DIR/paper-firstboot.log" 2>&1 &
  for i in $(seq 1 120); do
    grep -q 'Done (' "$LOG_DIR/paper-firstboot.log" 2>/dev/null && break
    pgrep -f "$JARS_DIR/paper.jar" >/dev/null 2>&1 || break
    sleep 1
  done
  # Kill the real java process by jar path (not just the subshell parent), then wait for the world lock to release.
  pkill -TERM -f "$JARS_DIR/paper.jar" 2>/dev/null || true
  for i in $(seq 1 20); do pgrep -f "$JARS_DIR/paper.jar" >/dev/null 2>&1 || break; sleep 1; done
  pkill -9 -f "$JARS_DIR/paper.jar" 2>/dev/null || true
  sleep 1

  log "Patching $PG (enable velocity modern forwarding)…"
  PG="$PG" SECRET="$FORWARDING_SECRET" python3 - <<'PY'
import os, re
path, secret = os.environ['PG'], os.environ['SECRET']
lines = open(path).read().split('\n')
out, in_vel, vel_indent = [], False, 2
for line in lines:
    stripped = line.lstrip(' '); indent = len(line) - len(stripped)
    if indent == 2 and stripped.startswith('velocity:'):
        in_vel = True; out.append(line); continue
    if in_vel:
        if stripped and indent <= vel_indent:
            in_vel = False
        elif re.match(r'enabled:', stripped):
            out.append(' '*indent + 'enabled: true'); continue
        elif re.match(r'secret:', stripped):
            out.append(' '*indent + f"secret: '{secret}'"); continue
    out.append(line)
open(path, 'w').write('\n'.join(out))
print("patched")
PY
fi

# ---------------------------------------------------------------- 6. start/stop scripts
log "Emitting start/stop scripts…"

cat > "$RUN_DIR/start-auth.sh" <<EOF
#!/usr/bin/env bash
cd "\$(dirname "\${BASH_SOURCE[0]}")/auth"
exec "$JAVA" -jar "$JARS_DIR/auth-server-all.jar"
EOF

cat > "$RUN_DIR/start-lobby.sh" <<EOF
#!/usr/bin/env bash
cd "\$(dirname "\${BASH_SOURCE[0]}")/lobby"
exec "$JAVA" -jar "$JARS_DIR/lobby-server-all.jar"
EOF

cat > "$RUN_DIR/start-velocity.sh" <<EOF
#!/usr/bin/env bash
cd "\$(dirname "\${BASH_SOURCE[0]}")/velocity"
exec "$JAVA" -Xms256M -Xmx512M -XX:+UseG1GC -jar "$JARS_DIR/velocity.jar"
EOF

cat > "$RUN_DIR/start-paper.sh" <<EOF
#!/usr/bin/env bash
cd "\$(dirname "\${BASH_SOURCE[0]}")/paper"
exec "$JAVA" -Xms1G -Xmx2G -jar "$JARS_DIR/paper.jar" nogui
EOF

cat > "$RUN_DIR/start-all.sh" <<'EOF'
#!/usr/bin/env bash
# Launch auth + lobby + velocity + paper, each in its own tmux session so you
# can attach and use the server console. Output is also mirrored to logs/.
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
log() { printf "\033[1;32m[start]\033[0m %s\n" "$*"; }
PREFIX="smp"

if ! command -v tmux >/dev/null 2>&1; then
  echo "tmux is required for consoled server management (brew install tmux)." >&2; exit 1
fi
if [ ! -f "$DIR/jars/velocity.jar" ] || [ ! -f "$DIR/jars/paper.jar" ]; then
  echo "velocity.jar / paper.jar missing — run ./setup.sh first." >&2; exit 1
fi

start() { # name script
  local name="$1" script="$2" session="${PREFIX}-${1}"
  tmux new-session -d -s "$session" -c "$DIR"
  tmux pipe-pane -o -t "$session" "cat >> '$DIR/logs/${name}.log'"
  tmux send-keys -t "$session" "bash '$DIR/${script}'" C-m
  log "started $name -> tmux attach -t $session  (logs/${name}.log)"
}

start auth     start-auth.sh
sleep 3                        # let the auth API come up before backends/proxy query it
start lobby    start-lobby.sh
sleep 2
start paper    start-paper.sh
sleep 2
start velocity start-velocity.sh

cat <<MSG

All four processes launched, each in its own tmux session.
  • Connect a Minecraft 26.2 client to  127.0.0.1:25565
  • In the lobby:  /login  → open the URL → DataGSM → /verify <key>
  • Then hop to the content server:  /server content   (oh-my-smp gameplay)

Console access:   ./console.sh {auth|lobby|velocity|paper}   (detach: Ctrl-B then D)
Tail logs:        tail -f logs/{auth,lobby,velocity,paper}.log
Stop all:         ./stop-all.sh
MSG
EOF

cat > "$RUN_DIR/console.sh" <<'EOF'
#!/usr/bin/env bash
# Attach to a running server's console (interactive — type commands directly).
# Detach without stopping the server: Ctrl-B then D.
if [ $# -ne 1 ] || [[ ! "$1" =~ ^(auth|lobby|velocity|paper)$ ]]; then
  echo "Usage: $0 {auth|lobby|velocity|paper}" >&2
  exit 1
fi
exec tmux attach -t "smp-$1"
EOF

cat > "$RUN_DIR/stop-all.sh" <<'EOF'
#!/usr/bin/env bash
DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PREFIX="smp"

stop() { # name [console-command]
  local name="$1" cmd="${2:-}" session="${PREFIX}-${1}"
  tmux has-session -t "$session" 2>/dev/null || return 0
  if [ -n "$cmd" ]; then
    tmux send-keys -t "$session" "$cmd" C-m
  else
    tmux send-keys -t "$session" C-c
  fi
  for i in $(seq 1 20); do
    tmux has-session -t "$session" 2>/dev/null || { printf "\033[1;33m[stop]\033[0m %s\n" "$name"; return 0; }
    sleep 1
  done
  tmux kill-session -t "$session" 2>/dev/null || true
  printf "\033[1;33m[stop]\033[0m %s (forced)\n" "$name"
}

# Reverse of start order; graceful console commands where the server has one.
stop velocity shutdown
stop paper stop
stop lobby
stop auth

# Belt-and-suspenders: kill any stragglers by jar name.
pkill -f 'auth-server-all.jar' 2>/dev/null || true
pkill -f 'lobby-server-all.jar' 2>/dev/null || true
pkill -f 'jars/velocity.jar' 2>/dev/null || true
pkill -f 'jars/paper.jar' 2>/dev/null || true
echo "stopped."
EOF

chmod +x "$RUN_DIR"/start-all.sh "$RUN_DIR"/stop-all.sh "$RUN_DIR"/console.sh \
         "$RUN_DIR"/start-auth.sh "$RUN_DIR"/start-lobby.sh \
         "$RUN_DIR"/start-velocity.sh "$RUN_DIR"/start-paper.sh

log "Done. Run ./start-all.sh to launch auth+lobby+velocity+paper, ./console.sh <name> for a console, ./stop-all.sh to stop."
