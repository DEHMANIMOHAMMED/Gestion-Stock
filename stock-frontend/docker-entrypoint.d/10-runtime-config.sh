#!/bin/sh
set -eu

cat > /usr/share/nginx/html/assets/runtime-config.js <<EOF
window.stockPilotConfig = {
  googleClientId: "${GOOGLE_CLIENT_ID:-}"
};
EOF
