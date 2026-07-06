#!/usr/bin/env bash
# smp.sh — single CLI for the full 4-process stack:
#   auth-server, Minestom lobby, Velocity proxy, and a Paper "content" server
#   running SmpAuth (content-lib) + the oh-my-smp plugin.
#
# Everything is driven by a profile file (profiles/<name>.env, Spring-Boot style:
# local, production, …). The profile is the single source of truth — every
# generated config file is re-rendered from it on each `start`, so you never
# hand-edit the per-server config.properties/velocity.toml files.
#
# The stack lives in the directory you RUN this from (a scratch dir, never the
# repo root):
#
#   mkdir ~/smp-test && cd ~/smp-test
#   /path/to/repo/smp.sh setup              # local profile: build, download, render, first-boot Paper
#   /path/to/repo/smp.sh start              # render configs + launch all four (tmux sessions)
#   /path/to/repo/smp.sh console velocity   # attach to a server console (detach: Ctrl-B, D)
#   /path/to/repo/smp.sh stop
#
# After code changes:  smp.sh update [--restart]   (swaps jars only)
# After profile edits: smp.sh restart              (configs re-render on start)
#
# Requires tmux (each process runs in its own detached session, so `start`
# returns immediately but you can still attach and type console commands).
set -euo pipefail

# ---------------------------------------------------------------- paths
# PROJECT_ROOT = where this script lives (the repo root, next to gradlew).
# RUN_DIR      = the directory you invoked the script from (current working dir).
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$PWD"
JARS_DIR="$RUN_DIR/jars"
LOG_DIR="$RUN_DIR/logs"
PROFILES_DIR="$PROJECT_ROOT/profiles"
MARKER="$RUN_DIR/.smp-profile"
TMUX_PREFIX="smp"
SERVERS=(auth lobby paper velocity)   # start order; stop iterates in reverse

log()  { printf "\033[1;36m[smp]\033[0m %s\n" "$*"; }
warn() { printf "\033[1;33m[smp]\033[0m %s\n" "$*" >&2; }
die()  { printf "\033[1;31m[smp]\033[0m %s\n" "$*" >&2; exit 1; }

usage() {
  cat <<USAGE
Usage: $0 <command> [args]

Commands (run from your scratch directory, not the repo root):
  setup   [--profile NAME]   provision the stack here: build jars, download
                             Velocity+Paper, render configs, first-boot Paper.
                             Records the profile in .smp-profile (default: local).
  update  [--restart]        rebuild + swap in only the project jars
                             (configs/db/worlds untouched)
  start   [name...]          re-render all configs from the profile, then launch
                             (default: all of ${SERVERS[*]})
  stop    [name...]          stop servers gracefully (default: all, reverse order)
  restart [name...]          stop + start
  console <name>             attach to a server's tmux console (detach: Ctrl-B, D)
  status                     active profile + per-server state
  logs <name> [-f]           show (or follow) a server's log
  render                     re-render configs from the profile without starting
  help                       this text

Profile selection: --profile NAME > \$SMP_PROFILE > .smp-profile marker.
Profiles live in $PROFILES_DIR/<NAME>.env — edit those, never the rendered files.
USAGE
}

# ---------------------------------------------------------------- profile
# smp.sh owns a complete set of defaults; a profile only overrides what differs.
profile_defaults() {
  # ports
  SMP_VELOCITY_PORT=25565   # players connect here
  SMP_LOBBY_PORT=25566      # Minestom lobby (Velocity server "lobby")
  SMP_PAPER_PORT=25567      # Paper content server (Velocity server "content")
  SMP_AUTH_PORT=8080        # auth web server
  # secrets
  SMP_FORWARDING_SECRET="smp-test-forwarding-secret"   # Velocity <-> lobby/paper modern forwarding
  SMP_SHARED_SECRET="smp-test-shared-secret"           # auth-server <-> lobby <-> velocity plugin
  # DataGSM OAuth
  SMP_DATAGSM_CLIENT_ID="test-client-id"
  SMP_DATAGSM_CLIENT_SECRET="test-client-secret"
  SMP_DATAGSM_SCOPE="self:read"
  SMP_PUBLIC_BASE_URL=""    # empty => http://127.0.0.1:${SMP_AUTH_PORT}
  # velocity-plugin gating
  SMP_GATED_SERVERS=""      # comma-separated; empty = every non-lobby server is gated
  SMP_CONTENT_SERVER_NAME="content"
  # network behavior
  SMP_ONLINE_MODE=false
  SMP_MOTD="<aqua>SMP Test Network"
  SMP_MAX_PLAYERS=20
  SMP_LEVEL_TYPE='minecraft\:flat'
  # runtime / versions
  SMP_JAVA=""               # empty => brew openjdk@25 if present, else "java" on PATH
  SMP_PAPER_VERSION="26.2"
  SMP_VELOCITY_VERSION="3.5.0-SNAPSHOT"
  SMP_PAPER_HEAP="-Xms1G -Xmx2G"
  SMP_VELOCITY_HEAP="-Xms256M -Xmx512M -XX:+UseG1GC"
}

resolve_profile() { # flag > env > marker; leaves PROFILE possibly empty
  PROFILE="$PROFILE_FLAG"
  [ -n "$PROFILE" ] || PROFILE="${SMP_PROFILE:-}"
  if [ -z "$PROFILE" ] && [ -f "$MARKER" ]; then
    PROFILE="$(head -n1 "$MARKER" | tr -d '[:space:]')"
  fi
}

load_profile() {
  [ -n "$PROFILE" ] || resolve_profile
  [ -n "$PROFILE" ] || die "No profile selected. Pass --profile NAME (or run 'smp.sh setup' here first)."
  local file="$PROFILES_DIR/$PROFILE.env"
  [ -f "$file" ] || die "Profile not found: $file (available: $(ls "$PROFILES_DIR" 2>/dev/null | sed 's/\.env.*//' | paste -sd' ' -))"
  profile_defaults
  # shellcheck disable=SC1090
  source "$file"
  # derived values
  [ -n "$SMP_PUBLIC_BASE_URL" ] || SMP_PUBLIC_BASE_URL="http://127.0.0.1:${SMP_AUTH_PORT}"
  if [ -z "$SMP_JAVA" ]; then
    local brew_java="/opt/homebrew/opt/openjdk@25/libexec/openjdk.jdk/Contents/Home/bin/java"
    if [ -x "$brew_java" ]; then SMP_JAVA="$brew_java"; else SMP_JAVA="java"; fi
  fi
}

is_valid_port() { [[ "$1" =~ ^[0-9]+$ ]] && [ "$1" -ge 1 ] && [ "$1" -le 65535 ]; }

validate_profile() {
  local name val
  for name in SMP_VELOCITY_PORT SMP_LOBBY_PORT SMP_PAPER_PORT SMP_AUTH_PORT; do
    is_valid_port "${!name}" || die "Invalid port for $name in profiles/$PROFILE.env: ${!name}"
  done
  # An empty/placeholder forwarding secret silently drops the lobby into
  # standalone (no-proxy) mode, so fail loudly here instead.
  for name in SMP_FORWARDING_SECRET SMP_SHARED_SECRET SMP_DATAGSM_CLIENT_ID SMP_DATAGSM_CLIENT_SECRET; do
    val="${!name}"
    [ -n "$val" ] || die "$name is empty — set it in profiles/$PROFILE.env"
  done
  for name in SMP_FORWARDING_SECRET SMP_SHARED_SECRET SMP_DATAGSM_CLIENT_ID SMP_DATAGSM_CLIENT_SECRET SMP_PUBLIC_BASE_URL; do
    case "${!name}" in
      CHANGE_ME*|REPLACE_ME*) die "$name still has a placeholder value in profiles/$PROFILE.env" ;;
    esac
  done
}

# ---------------------------------------------------------------- guards
require_not_repo_root() {
  if [ "$RUN_DIR" = "$PROJECT_ROOT" ]; then
    die "Refusing to run inside the repo root (it would litter the source tree).
Make a scratch dir and run from there, e.g.:  mkdir ~/smp-test && cd ~/smp-test && $0 setup"
  fi
}

require_env_dir() {
  if [ ! -f "$MARKER" ] || [ ! -d "$JARS_DIR" ]; then
    die "This directory doesn't look like a provisioned environment (missing .smp-profile / jars/). Run '$0 setup' here first."
  fi
}

require_tmux() {
  command -v tmux >/dev/null 2>&1 || die "tmux is required for consoled server management (brew install tmux)."
}

# fill_url + patch_paper_global drive setup/render through python3; on a minimal
# Linux box it may be absent, so fail with a clear message instead of a stack trace.
require_python3() {
  command -v python3 >/dev/null 2>&1 || die "python3 is required (config download + paper-global patch). Install it (e.g. apt install python3)."
}

# The whole stack targets Java 25; a stray 17/21 as PATH `java` would build/boot
# and then die with a class-version error, so verify SMP_JAVA up front.
check_java() {
  local v
  v="$("$SMP_JAVA" -version 2>&1 | head -1)" || die "Cannot run '$SMP_JAVA' — set SMP_JAVA in profiles/$PROFILE.env to your Java 25 binary."
  case "$v" in
    *'version "25'*) : ;;
    *) die "SMP_JAVA ($SMP_JAVA) is not Java 25: $v — set SMP_JAVA in profiles/$PROFILE.env (find it via: readlink -f \$(which java))." ;;
  esac
}

# ---------------------------------------------------------------- build & artifacts
build_jars() {
  check_java
  log "Building project jars (shadow)…"
  # SMP_JAVA also drives the Gradle toolchain: running gradlew ON the Java 25 JDK
  # makes it the toolchain candidate, so no OS-specific path is hardcoded for the
  # build. A bare "java" on PATH falls back to gradle.properties' pin (mac dev).
  local -a genv=()
  case "$SMP_JAVA" in
    /*/bin/java) genv=(JAVA_HOME="${SMP_JAVA%/bin/java}") ;;
  esac
  ( cd "$PROJECT_ROOT" && env ${genv[@]+"${genv[@]}"} ./gradlew --console=plain \
      :auth-server:shadowJar :lobby-server:shadowJar :velocity-plugin:shadowJar \
      :content-lib:shadowJar :smp-server:shadowJar )
}

# Resolve built jars (versioned filenames). The auth/lobby fat jars use Shadow's "-all"
# classifier; the Paper/Velocity plugins are the shaded classifier-less jar (not "-thin").
# Copied to stable (version-less) names so nothing depends on the repo's version string.
install_jars() {
  local auth_jar lobby_jar velocity_plugin_jar smpauth_jar ohmysmp_jar
  auth_jar="$(ls "$PROJECT_ROOT"/auth-server/build/libs/auth-server-*-all.jar | head -1)"
  lobby_jar="$(ls "$PROJECT_ROOT"/lobby-server/build/libs/lobby-server-*-all.jar | head -1)"
  velocity_plugin_jar="$(ls "$PROJECT_ROOT"/velocity-plugin/build/libs/velocity-plugin-*.jar | grep -v -- '-thin' | head -1)"
  smpauth_jar="$(ls "$PROJECT_ROOT"/content-lib/build/libs/content-lib-*.jar | grep -v -- '-thin' | head -1)"
  ohmysmp_jar="$(ls "$PROJECT_ROOT"/smp-server/build/libs/oh-my-smp-*-all.jar | head -1)"

  log "Installing project jars…"
  cp -f "$auth_jar"            "$JARS_DIR/auth-server-all.jar"
  cp -f "$lobby_jar"           "$JARS_DIR/lobby-server-all.jar"
  cp -f "$velocity_plugin_jar" "$RUN_DIR/velocity/plugins/velocity-plugin.jar"
  cp -f "$smpauth_jar"         "$RUN_DIR/paper/plugins/SmpAuth.jar"
  cp -f "$ohmysmp_jar"         "$RUN_DIR/paper/plugins/oh-my-smp.jar"
}

fill_url() { # project version -> latest build download URL (PaperMC v3 "fill" API)
  curl -fsSL "https://fill.papermc.io/v3/projects/$1/versions/$2/builds" \
    | python3 -c "import sys,json; print(json.load(sys.stdin)[0]['downloads']['server:default']['url'])"
}
download() { # url dest
  if [ -s "$2" ]; then log "exists: $(basename "$2")"; else log "downloading $(basename "$2") …"; curl -fsSL -o "$2" "$1"; fi
}
download_servers() {
  log "Resolving Paper ${SMP_PAPER_VERSION} + Velocity ${SMP_VELOCITY_VERSION} (v3 API)…"
  download "$(fill_url paper "$SMP_PAPER_VERSION")"       "$JARS_DIR/paper.jar"
  download "$(fill_url velocity "$SMP_VELOCITY_VERSION")" "$JARS_DIR/velocity.jar"
}

# ---------------------------------------------------------------- config rendering
gen_header() { printf '# DO NOT EDIT — generated by smp.sh from profiles/%s.env; edit the profile and rerun start/render.\n' "$PROFILE"; }

render_auth_config() {
  cat > "$RUN_DIR/auth/config.properties" <<EOF
$(gen_header)
server.port=${SMP_AUTH_PORT}
server.publicBaseUrl=${SMP_PUBLIC_BASE_URL}
oauth.redirectUri=${SMP_PUBLIC_BASE_URL}/callback
datagsm.clientId=${SMP_DATAGSM_CLIENT_ID}
datagsm.clientSecret=${SMP_DATAGSM_CLIENT_SECRET}
datagsm.scope=${SMP_DATAGSM_SCOPE}
security.sharedSecret=${SMP_SHARED_SECRET}
db.path=smp-auth.db
key.ttlSeconds=300
key.length=8
EOF
}

render_lobby_config() {
  cat > "$RUN_DIR/lobby/config.properties" <<EOF
$(gen_header)
host=0.0.0.0
port=${SMP_LOBBY_PORT}
velocitySecret=${SMP_FORWARDING_SECRET}
authServerBaseUrl=http://127.0.0.1:${SMP_AUTH_PORT}
authLoginUrl=${SMP_PUBLIC_BASE_URL}/login
sharedSecret=${SMP_SHARED_SECRET}
EOF
}

render_velocity_toml() {
  cat > "$RUN_DIR/velocity/velocity.toml" <<EOF
$(gen_header)
config-version = "2.8"
bind = "0.0.0.0:${SMP_VELOCITY_PORT}"
motd = "${SMP_MOTD}"
show-max-players = ${SMP_MAX_PLAYERS}
# Offline mode lets you join locally without a premium account; modern forwarding
# still carries the (offline) profile to backends. Production uses true (real Mojang auth).
online-mode = ${SMP_ONLINE_MODE}
force-key-authentication = false
prevent-client-proxy-connections = false
player-info-forwarding-mode = "modern"
forwarding-secret-file = "forwarding.secret"
announce-forge = false
kick-existing-players = false
ping-passthrough = "DISABLED"
forced-hosts = {}

[servers]
lobby = "127.0.0.1:${SMP_LOBBY_PORT}"
content = "127.0.0.1:${SMP_PAPER_PORT}"
try = ["lobby"]

[advanced]
compression-threshold = 256
login-ratelimit = 0

[query]
enabled = false
EOF
  # raw single line — Velocity reads the whole file as the secret, so no header/newline
  printf '%s' "$SMP_FORWARDING_SECRET" > "$RUN_DIR/velocity/forwarding.secret"
}

render_smpauth_config() {
  cat > "$RUN_DIR/velocity/plugins/smp-auth/config.properties" <<EOF
$(gen_header)
authServerBaseUrl=http://127.0.0.1:${SMP_AUTH_PORT}
sharedSecret=${SMP_SHARED_SECRET}
lobbyServerName=lobby
contentServerName=${SMP_CONTENT_SERVER_NAME}
gatedServers=${SMP_GATED_SERVERS}
EOF
}

render_paper_config() {
  echo "eula=true" > "$RUN_DIR/paper/eula.txt"
  cat > "$RUN_DIR/paper/server.properties" <<EOF
$(gen_header)
online-mode=${SMP_ONLINE_MODE}
server-port=${SMP_PAPER_PORT}
server-ip=127.0.0.1
motd=SMP Content
level-type=${SMP_LEVEL_TYPE}
level-name=world
spawn-protection=0
max-players=${SMP_MAX_PLAYERS}
view-distance=6
simulation-distance=6
network-compression-threshold=256
enable-command-block=false
allow-nether=false
EOF
}

# Idempotently rewrite the velocity: block of an existing paper-global.yml.
patch_paper_global() {
  local pg="$RUN_DIR/paper/config/paper-global.yml"
  [ -f "$pg" ] || return 0
  PG="$pg" SECRET="$SMP_FORWARDING_SECRET" python3 - <<'PY'
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
PY
}

render_all() {
  mkdir -p "$RUN_DIR"/auth "$RUN_DIR"/lobby \
           "$RUN_DIR"/velocity/plugins/smp-auth \
           "$RUN_DIR"/paper/plugins "$RUN_DIR"/paper/config
  log "Rendering configs from profiles/$PROFILE.env…"
  render_auth_config
  render_lobby_config
  render_velocity_toml
  render_smpauth_config
  render_paper_config
  patch_paper_global
}

# setup-only: Paper writes paper-global.yml on first boot; boot it once, kill it,
# then patch velocity forwarding into the generated file.
bootstrap_paper_global() {
  local pg="$RUN_DIR/paper/config/paper-global.yml"
  if grep -q "enabled: true" "$pg" 2>/dev/null && grep -q "$SMP_FORWARDING_SECRET" "$pg" 2>/dev/null; then
    log "Paper velocity forwarding already configured."
    return 0
  fi
  if [ ! -f "$pg" ]; then
    log "Generating Paper config (first boot, ~30-60s)…"
    ( cd "$RUN_DIR/paper" && exec "$SMP_JAVA" -Xms512M -Xmx1G -jar "$JARS_DIR/paper.jar" nogui ) > "$LOG_DIR/paper-firstboot.log" 2>&1 &
    for _ in $(seq 1 120); do
      grep -q 'Done (' "$LOG_DIR/paper-firstboot.log" 2>/dev/null && break
      pgrep -f "$JARS_DIR/paper.jar" >/dev/null 2>&1 || break
      sleep 1
    done
    # Kill the real java process by jar path (not just the subshell parent), then wait for the world lock to release.
    pkill -TERM -f "$JARS_DIR/paper.jar" 2>/dev/null || true
    for _ in $(seq 1 20); do pgrep -f "$JARS_DIR/paper.jar" >/dev/null 2>&1 || break; sleep 1; done
    pkill -9 -f "$JARS_DIR/paper.jar" 2>/dev/null || true
    sleep 1
  fi
  log "Patching paper-global.yml (enable velocity modern forwarding)…"
  patch_paper_global
}

# ---------------------------------------------------------------- process management
session_name() { echo "${TMUX_PREFIX}-$1"; }
is_running()   { tmux has-session -t "$(session_name "$1")" 2>/dev/null; }

check_server_name() {
  [[ "$1" =~ ^(auth|lobby|velocity|paper)$ ]] || die "Unknown server: $1 (expected: ${SERVERS[*]})"
}

start_one() {
  local name="$1" session dir cmd
  session="$(session_name "$name")"
  if is_running "$name"; then log "$name already running"; return 0; fi
  case "$name" in
    auth)     dir=auth;     cmd="exec '$SMP_JAVA' -jar '$JARS_DIR/auth-server-all.jar'" ;;
    lobby)    dir=lobby;    cmd="exec '$SMP_JAVA' -jar '$JARS_DIR/lobby-server-all.jar'" ;;
    velocity) dir=velocity; cmd="exec '$SMP_JAVA' $SMP_VELOCITY_HEAP -jar '$JARS_DIR/velocity.jar'" ;;
    paper)    dir=paper;    cmd="exec '$SMP_JAVA' $SMP_PAPER_HEAP -jar '$JARS_DIR/paper.jar' nogui" ;;
  esac
  # Run the java process as the pane's own command (not typed into a persistent
  # shell) so the pane/session exits the instant the process does — otherwise
  # stop_one can never observe a graceful shutdown and always falls back to a kill.
  tmux new-session -d -s "$session" -c "$RUN_DIR/$dir" "$cmd"
  tmux pipe-pane -o -t "$session" "cat >> '$LOG_DIR/${name}.log'"
  log "started $name -> $0 console $name  (logs/${name}.log)"
}

stop_one() { # name [console-command]
  local name="$1" cmd="${2:-}" session
  session="$(session_name "$name")"
  tmux has-session -t "$session" 2>/dev/null || return 0
  if [ -n "$cmd" ]; then
    tmux send-keys -t "$session" "$cmd" C-m
  else
    tmux send-keys -t "$session" C-c
  fi
  for _ in $(seq 1 20); do
    tmux has-session -t "$session" 2>/dev/null || { log "stopped $name"; return 0; }
    sleep 1
  done
  tmux kill-session -t "$session" 2>/dev/null || true
  log "stopped $name (forced)"
}

# Belt-and-suspenders: kill any stragglers by absolute jar path, so only
# processes of THIS run dir match (never another environment's servers).
kill_stragglers() {
  pkill -f "$JARS_DIR/auth-server-all.jar" 2>/dev/null || true
  pkill -f "$JARS_DIR/lobby-server-all.jar" 2>/dev/null || true
  pkill -f "$JARS_DIR/velocity.jar" 2>/dev/null || true
  pkill -f "$JARS_DIR/paper.jar" 2>/dev/null || true
}

# ---------------------------------------------------------------- commands
cmd_setup() {
  require_not_repo_root
  resolve_profile
  [ -n "$PROFILE" ] || PROFILE=local
  load_profile
  validate_profile
  require_python3
  printf '%s\n' "$PROFILE" > "$MARKER"
  log "Provisioning with profile '$PROFILE' into $RUN_DIR"

  mkdir -p "$JARS_DIR" "$LOG_DIR"
  build_jars
  download_servers
  render_all
  install_jars
  bootstrap_paper_global
  log "Done. '$0 start' launches the stack, '$0 console <name>' attaches, '$0 stop' stops."
}

cmd_update() {
  require_not_repo_root
  require_env_dir
  load_profile   # build_jars/check_java need SMP_JAVA resolved from the profile
  local restart=false
  case "${1:-}" in
    --restart) restart=true ;;
    "") ;;
    *) die "Unknown option for update: $1 (usage: $0 update [--restart])" ;;
  esac
  build_jars
  log "Updating jars (configs/db/worlds untouched)…"
  install_jars
  if [ "$restart" = true ]; then
    log "Restarting the stack…"
    cmd_stop
    cmd_start
  else
    log "Done. Restart to pick up the new jars:  $0 restart"
  fi
}

cmd_start() {
  require_not_repo_root
  require_env_dir
  require_tmux
  load_profile
  validate_profile
  check_java
  require_python3   # render_all -> patch_paper_global uses python3
  mkdir -p "$LOG_DIR"
  render_all

  local requested=("$@") name selected=()
  for name in "${requested[@]:-}"; do [ -n "$name" ] && check_server_name "$name"; done
  for name in "${SERVERS[@]}"; do   # canonical start order regardless of arg order
    if [ ${#requested[@]} -eq 0 ] || [[ " ${requested[*]} " == *" $name "* ]]; then
      selected+=("$name")
    fi
  done

  local i
  for i in "${!selected[@]}"; do
    name="${selected[$i]}"
    start_one "$name"
    if [ "$i" -lt $(( ${#selected[@]} - 1 )) ]; then
      # let the auth API come up before backends/proxy query it
      if [ "$name" = auth ]; then sleep 3; else sleep 2; fi
    fi
  done

  if [ ${#selected[@]} -eq ${#SERVERS[@]} ]; then
    cat <<MSG

All four processes launched (profile: $PROFILE), each in its own tmux session.
  • Connect a Minecraft 26.2 client to  127.0.0.1:${SMP_VELOCITY_PORT}
  • In the lobby:  /login  → open the URL → DataGSM → /verify <key>
  • Then hop to the content server:  /server content   (oh-my-smp gameplay)

Console access:   $0 console {auth|lobby|velocity|paper}   (detach: Ctrl-B then D)
Tail logs:        $0 logs <name> -f
Stop all:         $0 stop
MSG
  fi
}

cmd_stop() {
  require_not_repo_root
  require_tmux
  local requested=("$@") name
  for name in "${requested[@]:-}"; do [ -n "$name" ] && check_server_name "$name"; done
  local idx
  # Reverse of start order; graceful console commands where the server has one.
  for (( idx=${#SERVERS[@]}-1; idx>=0; idx-- )); do
    name="${SERVERS[$idx]}"
    if [ ${#requested[@]} -eq 0 ] || [[ " ${requested[*]} " == *" $name "* ]]; then
      case "$name" in
        velocity) stop_one velocity shutdown ;;
        paper)    stop_one paper stop ;;
        *)        stop_one "$name" ;;
      esac
    fi
  done
  if [ ${#requested[@]} -eq 0 ]; then
    kill_stragglers
    log "all stopped."
  fi
}

cmd_restart() {
  cmd_stop "$@"
  cmd_start "$@"
}

cmd_console() {
  require_tmux
  [ $# -eq 1 ] || die "Usage: $0 console {auth|lobby|velocity|paper}"
  check_server_name "$1"
  is_running "$1" || die "$1 is not running (start it with '$0 start $1')."
  exec tmux attach -t "$(session_name "$1")"
}

cmd_status() {
  require_not_repo_root
  require_env_dir
  load_profile
  log "profile: $PROFILE ($PROFILES_DIR/$PROFILE.env)"
  local name port state
  for name in "${SERVERS[@]}"; do
    case "$name" in
      auth)     port=$SMP_AUTH_PORT ;;
      lobby)    port=$SMP_LOBBY_PORT ;;
      paper)    port=$SMP_PAPER_PORT ;;
      velocity) port=$SMP_VELOCITY_PORT ;;
    esac
    if is_running "$name"; then state="\033[1;32mrunning\033[0m"; else state="\033[1;31mstopped\033[0m"; fi
    printf "  %-8s %b   port %s\n" "$name" "$state" "$port"
  done
}

cmd_logs() {
  [ $# -ge 1 ] || die "Usage: $0 logs <name> [-f]"
  check_server_name "$1"
  local file="$LOG_DIR/$1.log"
  [ -f "$file" ] || die "No log yet: $file"
  if [ "${2:-}" = "-f" ]; then exec tail -f "$file"; else exec tail -n 100 "$file"; fi
}

cmd_render() {
  require_not_repo_root
  load_profile
  validate_profile
  render_all
  log "Rendered: auth/config.properties lobby/config.properties velocity/velocity.toml velocity/forwarding.secret velocity/plugins/smp-auth/config.properties paper/server.properties (+ paper-global.yml patch if present)"
}

# ---------------------------------------------------------------- main
PROFILE_FLAG=""
PROFILE=""
ARGS=()
while [ $# -gt 0 ]; do
  case "$1" in
    --profile)   [ $# -ge 2 ] || die "--profile needs a value"; PROFILE_FLAG="$2"; shift 2 ;;
    --profile=*) PROFILE_FLAG="${1#*=}"; shift ;;
    -h|--help)   usage; exit 0 ;;
    *)           ARGS+=("$1"); shift ;;
  esac
done
set -- ${ARGS[@]+"${ARGS[@]}"}

CMD="${1:-help}"
[ $# -gt 0 ] && shift

case "$CMD" in
  setup)   cmd_setup "$@" ;;
  update)  cmd_update "$@" ;;
  start)   cmd_start "$@" ;;
  stop)    cmd_stop "$@" ;;
  restart) cmd_restart "$@" ;;
  console) cmd_console "$@" ;;
  status)  cmd_status "$@" ;;
  logs)    cmd_logs "$@" ;;
  render)  cmd_render "$@" ;;
  help|"") usage ;;
  *)       usage >&2; die "Unknown command: $CMD" ;;
esac
