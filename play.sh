#!/usr/bin/env bash
#
# Builds the game and starts it outside Gradle, so the terminal shows nothing but the game itself and
# Ctrl-C/Ctrl-D reach the REPL rather than the build. Arguments are passed straight through:
#
#   ./play.sh
#   ./play.sh --balance 500
#
set -eu

cd "$(dirname "$0")"

./gradlew installDist -q

exec build/install/slots/bin/slots "$@"