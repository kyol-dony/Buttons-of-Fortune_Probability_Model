#!/bin/sh
# Development-only formatter. Use JDK 25; application source remains Java 17 compatible.
set -eu
mode="${1:---check}"
case "$mode" in
  --check) flags="--dry-run --set-exit-if-changed" ;;
  --write) flags="--replace" ;;
  *) echo "Usage: sh scripts/format.sh [--check|--write]" >&2; exit 2 ;;
esac
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
jar="$root/.tools/google-java-format-1.37.0.jar"
expected="834b2a0c38cb774953322a84b5ca3f2f40dd3156650b3cd44d3b744345962f7a"
if [ ! -f "$jar" ]; then
  mkdir -p "$root/.tools"
  curl --fail --location --silent --show-error \
    "https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/1.37.0/google-java-format-1.37.0-all-deps.jar" \
    --output "$jar"
fi
if command -v sha256sum >/dev/null 2>&1; then
  actual=$(sha256sum "$jar" | cut -d ' ' -f 1)
else
  actual=$(shasum -a 256 "$jar" | cut -d ' ' -f 1)
fi
if [ "$actual" != "$expected" ]; then
  echo "Formatter checksum mismatch. Remove $jar and retry." >&2
  exit 1
fi
cd "$root"
find src -name '*.java' -type f -exec java -jar "$jar" $flags {} +

