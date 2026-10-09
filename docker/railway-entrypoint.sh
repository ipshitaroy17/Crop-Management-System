#!/bin/sh
set -eu

port="${PORT:-8080}"
case "$port" in
    ''|*[!0-9]*) echo "PORT must be a numeric TCP port" >&2; exit 1 ;;
esac
if [ "$port" -lt 1 ] || [ "$port" -gt 65535 ]; then
    echo "PORT must be between 1 and 65535" >&2
    exit 1
fi

sed -i "s/port=\"8080\"/port=\"${port}\"/" "$CATALINA_HOME/conf/server.xml"
exec "$CATALINA_HOME/bin/catalina.sh" run
