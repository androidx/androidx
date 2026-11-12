#!/usr/bin/env bash
# Prints the JVM argument that loads HostAllowlistAgent.java, compiling it into $OUT_DIR if changed.
set -e
cd "$(dirname "$0")"
jar="$OUT_DIR/host-allowlist-$(cat ./* | cksum | cut -d' ' -f1).jar"
if [ ! -f "$jar" ]; then
  # Not mktemp: when gradlew runs this right after --clean, TMPDIR doesn't exist yet
  classes="$jar.$$.classes"
  mkdir -p "$classes/META-INF/services"
  echo HostAllowlistAgent > "$classes/META-INF/services/java.net.spi.InetAddressResolverProvider"
  # Host tests also load it (AndroidXImplPlugin), and some projects run them on JDK 21
  "$JAVA_HOME/bin/javac" --release 21 -d "$classes" HostAllowlistAgent.java
  "$JAVA_HOME/bin/jar" cfm "$jar.$$" <(echo "Premain-Class: HostAllowlistAgent") -C "$classes" .
  mv "$jar.$$" "$jar"
  rm -rf "$classes"
fi
echo "-javaagent:$jar"
