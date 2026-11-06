#!/usr/bin/env bash
# Prints the JVM argument that loads HostAllowlistAgent.java, compiling it into $OUT_DIR if changed.
set -e
cd "$(dirname "$0")"
jar="$OUT_DIR/host-allowlist-$(cksum HostAllowlistAgent.java | cut -d' ' -f1).jar"
if [ ! -f "$jar" ]; then
  classes="$(mktemp -d)"
  mkdir -p "$classes/META-INF/services"
  echo HostAllowlistAgent > "$classes/META-INF/services/java.net.spi.InetAddressResolverProvider"
  "$JAVA_HOME/bin/javac" -d "$classes" HostAllowlistAgent.java
  "$JAVA_HOME/bin/jar" cfm "$jar.$$" <(echo "Premain-Class: HostAllowlistAgent") -C "$classes" .
  mv "$jar.$$" "$jar"
fi
echo "-javaagent:$jar"
