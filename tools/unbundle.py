#!/usr/bin/env python3
"""models-src/out/*.bundle.json (Blockbench export) -> assets/pridefurniture/{models/block,textures/block}."""
import json, base64, glob, os, sys
root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = os.path.join(root, 'src/main/resources/assets/pridefurniture')
os.makedirs(A + '/models/block', exist_ok=True); os.makedirs(A + '/textures/block', exist_ok=True)
for f in sorted(glob.glob(root + '/models-src/out/*.bundle.json')):
    b = json.load(open(f)); name = b['name']
    m = b['model']; m = json.loads(m) if isinstance(m, str) else m
    m.pop('format_version', None); m['credit'] = 'Pride Furniture - made in Blockbench'
    tex = 'pridefurniture:block/' + name
    m['textures'] = {'0': tex, 'particle': tex}
    for e in m['elements']:
        e.pop('name', None)
    json.dump(m, open(f'{A}/models/block/{name}.json', 'w'), separators=(',', ':'))
    open(f'{A}/textures/block/{name}.png', 'wb').write(base64.b64decode(b['png'].split(',', 1)[1]))
    print(name, len(m['elements']), 'elements')
