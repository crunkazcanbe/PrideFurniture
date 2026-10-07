#!/bin/bash
# Build specs in Blockbench (via the MCP bridge HTTP port) and export bundles. Usage: bbbuild.sh [name...]
cd "$(dirname "$0")/.."
BB=http://127.0.0.1:8787/command
names=("$@"); [ ${#names[@]} -eq 0 ] && names=($(ls models-src/spec | sed 's/\.json$//'))
for n in "${names[@]}"; do
  r=$(curl -s -m 60 -X POST $BB -H 'Content-Type: application/json' -d "{\"action\":\"execute_script\",\"params\":{\"code\":\"return PF.buildFrom('$n')\"}}")
  e=$(curl -s -m 60 -X POST $BB -H 'Content-Type: application/json' -d "{\"action\":\"export_project\",\"params\":{\"path\":\"$PWD/models-src/out/$n.bundle.json\"}}")
  echo "$n: $(echo "$r" | head -c 90) | $(echo "$e" | grep -o '"exported":true' || echo "$e" | head -c 120)"
  sleep ${BB_DELAY:-1.2}
done
