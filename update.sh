#!/usr/bin/env bash
# Rebuild the project jars and swap them into an existing test environment
# provisioned by setup.sh — nothing else. Configs, secrets, the auth SQLite db,
# and Paper worlds are left untouched.
#
# Run it from the same scratch directory you ran setup.sh in:
#
#   cd ~/smp-test
#   /path/to/repo/update.sh             # build + copy jars, then restart yourself
#   /path/to/repo/update.sh --restart   # also ./stop-all.sh && ./start-all.sh
#
# Note: running servers keep their already-loaded classes; new jars only take
# effect after a restart.
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$PWD"
JARS_DIR="$RUN_DIR/jars"

log() { printf "\033[1;36m[update]\033[0m %s\n" "$*"; }

RESTART=false
case "${1:-}" in
  --restart) RESTART=true ;;
  -h|--help) echo "Usage: $0 [--restart]"; exit 0 ;;
  "") ;;
  *) echo "Unknown option: $1" >&2; echo "Usage: $0 [--restart]" >&2; exit 1 ;;
esac

if [ "$RUN_DIR" = "$PROJECT_ROOT" ]; then
  echo "Run this from your scratch directory (where you ran setup.sh), not the repo root." >&2
  exit 1
fi
if [ ! -f "$RUN_DIR/start-all.sh" ] || [ ! -d "$JARS_DIR" ]; then
  echo "This directory doesn't look like a setup.sh environment (missing start-all.sh / jars/)." >&2
  echo "Run /path/to/repo/setup.sh here first." >&2
  exit 1
fi

# ---------------------------------------------------------------- 1. build
log "Building project jars (shadow)…"
( cd "$PROJECT_ROOT" && ./gradlew --console=plain \
    :auth-server:shadowJar :lobby-server:shadowJar :velocity-plugin:shadowJar \
    :content-lib:shadowJar :smp-server:shadowJar )

AUTH_JAR="$(ls "$PROJECT_ROOT"/auth-server/build/libs/auth-server-*-all.jar | head -1)"
LOBBY_JAR="$(ls "$PROJECT_ROOT"/lobby-server/build/libs/lobby-server-*-all.jar | head -1)"
VELOCITY_PLUGIN_JAR="$(ls "$PROJECT_ROOT"/velocity-plugin/build/libs/velocity-plugin-*.jar | grep -v -- '-thin' | head -1)"
SMPAUTH_JAR="$(ls "$PROJECT_ROOT"/content-lib/build/libs/content-lib-*.jar | grep -v -- '-thin' | head -1)"
OHMYSMP_JAR="$(ls "$PROJECT_ROOT"/smp-server/build/libs/oh-my-smp-*-all.jar | head -1)"

# ---------------------------------------------------------------- 2. swap jars only
log "Updating jars (configs/db/worlds untouched)…"
cp -f "$AUTH_JAR"            "$JARS_DIR/auth-server-all.jar"
cp -f "$LOBBY_JAR"           "$JARS_DIR/lobby-server-all.jar"
cp -f "$VELOCITY_PLUGIN_JAR" "$RUN_DIR/velocity/plugins/velocity-plugin.jar"
cp -f "$SMPAUTH_JAR"         "$RUN_DIR/paper/plugins/SmpAuth.jar"
cp -f "$OHMYSMP_JAR"         "$RUN_DIR/paper/plugins/oh-my-smp.jar"

# ---------------------------------------------------------------- 3. restart (optional)
if [ "$RESTART" = true ]; then
  log "Restarting the stack…"
  "$RUN_DIR/stop-all.sh"
  "$RUN_DIR/start-all.sh"
else
  log "Done. Restart to pick up the new jars:  ./stop-all.sh && ./start-all.sh"
fi
