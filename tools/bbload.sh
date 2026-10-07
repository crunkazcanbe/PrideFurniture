#!/bin/bash
# (Re)load the PF helper into Blockbench and make sure the spec server is up.
cd "$(dirname "$0")/.."
ss -tln | grep -q ':47911 ' || (cd models-src/spec && setsid nohup python3 -m http.server 47911 --bind 127.0.0.1 >/dev/null 2>&1 < /dev/null &)
python3 -c "import json,sys;print(json.dumps({'action':'execute_script','params':{'code':open('models-src/pf-helper.js').read()}}))" \
 | curl -s -m 30 -X POST http://127.0.0.1:8787/command -H 'Content-Type: application/json' -d @-; echo
