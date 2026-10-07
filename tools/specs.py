#!/usr/bin/env python3
"""Writes every Pride Furniture model spec to models-src/spec/<name>.json.
Blockbench (window.PF.buildFrom) turns each spec into the real model + painted texture.
Conventions: front faces NORTH (z=0); y may go to 32 for 2-tall pieces, z to 32 for 2-long ones."""
import json, os

OUT = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), 'models-src/spec')
SPECS = {}

def C(n, f, t, m, **kw):
    d = {'n': n, 'f': [round(v, 3) for v in f], 't': [round(v, 3) for v in t], 'm': m}
    d.update(kw)
    return d

def spec(name, mats, cubes, camH=None):
    s = {'name': name, 'mats': mats, 'cubes': cubes}
    if camH: s['camH'] = camH
    SPECS[name] = s

def knob(n, x, y, z0=0.3, m='handle', s=0.8):
    return [C(n, [x - s / 2, y - s / 2, z0], [x + s / 2, y + s / 2, z0 + 0.7], m)]

def bar_v(n, x, y0, y1, m='handle'):   # vertical bar handle with standoffs, on a front at z~0.8
    return [C(n, [x - 0.35, y0, 0], [x + 0.35, y1, 0.55], m),
            C(n + '_a', [x - 0.3, y1 - 0.5, 0.45], [x + 0.3, y1, 0.9], m),
            C(n + '_b', [x - 0.3, y0, 0.45], [x + 0.3, y0 + 0.5, 0.9], m)]

def bar_h(n, x0, x1, y, m='handle'):
    return [C(n, [x0, y - 0.35, 0], [x1, y + 0.35, 0.55], m),
            C(n + '_a', [x0, y - 0.3, 0.45], [x0 + 0.5, y + 0.3, 0.9], m),
            C(n + '_b', [x1 - 0.5, y - 0.3, 0.45], [x1, y + 0.3, 0.9], m)]

def panel_door(n, x0, x1, y0, y1, m='door', trim='trim'):
    """shaker door: slab + 4 raised rails around a recessed centre"""
    return [C(n, [x0, y0, 0.9], [x1, y1, 1.6], m),
            C(n + '_rt', [x0, y1 - 1.2, 0.6], [x1, y1, 0.9], trim),
            C(n + '_rb', [x0, y0, 0.6], [x1, y0 + 1.2, 0.9], trim),
            C(n + '_rl', [x0, y0 + 1.2, 0.6], [x0 + 1.2, y1 - 1.2, 0.9], trim),
            C(n + '_rr', [x1 - 1.2, y0 + 1.2, 0.6], [x1, y1 - 1.2, 0.9], trim)]

# ------------------------------------------------------------------ finishes
FINISH = {
    'white':  ({'c': '#eceae4', 's': 'paint'}, {'c': '#f6f4ef', 's': 'paint'}),
    'oak':    ({'c': '#b9874f', 's': 'wood'},  {'c': '#c9965c', 's': 'wood'}),
    'walnut': ({'c': '#5a3b26', 's': 'wood'},  {'c': '#6b4730', 's': 'wood'}),
    'sage':   ({'c': '#9fb39a', 's': 'paint'}, {'c': '#adc1a8', 's': 'paint'}),
    'navy':   ({'c': '#2f3f5e', 's': 'paint'}, {'c': '#3a4b6d', 's': 'paint'}),
}
HW = {'handle': {'c': '#b9a26a', 's': 'metal'}, 'dark': {'c': '#2a2a2c', 's': 'dark'}, 'inside': {'c': '#3c3630', 's': 'paint'}}

COUNTERS = {
    'granite': {'c': '#6e6a66', 's': 'granite', 'c2': '#2b2826', 'c3': '#d8d2c8'},
    'marble':  {'c': '#eeedeb', 's': 'marble'},
    'butcher': {'c': '#c08a52', 's': 'wood'},
    'quartz':  {'c': '#f2f0ec', 's': 'granite', 'c2': '#d9d6d0', 'c3': '#ffffff'},
    'black':   {'c': '#26272a', 's': 'granite', 'c2': '#111214', 'c3': '#5a5c60'},
    'concrete': {'c': '#9c9a96', 's': 'granite', 'c2': '#86837f', 'c3': '#b2b0ac'},
}

def kitchen():
    for fin, (body, door) in FINISH.items():
        mats = dict(HW, body=body, door=door, trim=door)
        # base cabinet: 2 shaker doors
        cubes = [C('carcass', [0, 1, 1.6], [16, 14.5, 16], 'body'), C('toe', [0.5, 0, 2.6], [15.5, 1, 15.5], 'dark')]
        cubes += panel_door('door_l', 0.2, 7.9, 1.3, 14.3) + panel_door('door_r', 8.1, 15.8, 1.3, 14.3)
        cubes += knob('k_l', 6.9, 11.5) + knob('k_r', 9.1, 11.5)
        spec(f'cabinet_{fin}', mats, cubes, 8)
        # drawer cabinet: 3 drawers
        cubes = [C('carcass', [0, 1, 1.6], [16, 14.5, 16], 'body'), C('toe', [0.5, 0, 2.6], [15.5, 1, 15.5], 'dark')]
        for i, (y0, y1) in enumerate([(1.3, 5.6), (5.8, 10.1), (10.3, 14.3)]):
            cubes += panel_door(f'drawer{i}', 0.2, 15.8, y0, y1)
            cubes += bar_h(f'pull{i}', 5.5, 10.5, (y0 + y1) / 2 + 0.6)
        spec(f'drawers_{fin}', mats, cubes, 8)
        # sink cabinet: doors + false drawer front (basin/faucet live in the counter_sink model)
        cubes = [C('carcass', [0, 1, 1.6], [16, 14.5, 16], 'body'), C('toe', [0.5, 0, 2.6], [15.5, 1, 15.5], 'dark')]
        cubes += panel_door('false_front', 0.2, 15.8, 10.6, 14.3) + panel_door('door_l', 0.2, 7.9, 1.3, 10.4) + panel_door('door_r', 8.1, 15.8, 1.3, 10.4)
        cubes += knob('k_l', 6.9, 8.2) + knob('k_r', 9.1, 8.2)
        spec(f'sink_cabinet_{fin}', mats, cubes, 8)
        # upper wall cabinet (shallow, hangs on the wall behind it)
        cubes = [C('carcass', [0, 2, 8.6], [16, 16, 16], 'body'), C('bottom_trim', [0, 1.6, 8.2], [16, 2, 16], 'trim')]
        cubes += [C('door_l', [0.2, 2.2, 7.9], [7.9, 15.8, 8.6], 'door'), C('door_r', [8.1, 2.2, 7.9], [15.8, 15.8, 8.6], 'door')]
        cubes += [C('frame_l', [0.6, 2.6, 7.6], [7.5, 3.6, 7.9], 'trim'), C('frame_r', [8.5, 2.6, 7.6], [15.4, 3.6, 7.9], 'trim')]
        cubes += [C('k_l', [6.6, 3.2, 7.2], [7.4, 4.0, 7.9], 'handle'), C('k_r', [8.6, 3.2, 7.2], [9.4, 4.0, 7.9], 'handle')]
        spec(f'wall_cabinet_{fin}', mats, cubes, 9)
        # glass-door wall cabinet
        mg = dict(mats, glass={'c': '#bfe3ef', 's': 'glass', 'a': 0.45}, plate={'c': '#f4f4f2', 's': 'porcelain'})
        cubes = [C('carcass', [0, 2, 8.6], [16, 16, 16], 'body'), C('bottom_trim', [0, 1.6, 8.2], [16, 2, 16], 'trim'),
                 C('shelf', [0.6, 8.6, 9], [15.4, 9, 15.6], 'body'), C('plate1', [2, 9, 13], [6, 13, 13.5], 'plate'), C('plate2', [9, 9, 13], [13, 13, 13.5], 'plate'),
                 C('cup1', [3, 2.4, 11], [5, 4.6, 13], 'plate'), C('cup2', [10, 2.4, 11], [12, 4.6, 13], 'plate')]
        for side, (x0, x1) in (('l', (0.2, 7.9)), ('r', (8.1, 15.8))):
            cubes += [C(f'f{side}_t', [x0, 14.6, 7.9], [x1, 15.8, 8.6], 'door'), C(f'f{side}_b', [x0, 2.2, 7.9], [x1, 3.4, 8.6], 'door'),
                      C(f'f{side}_l', [x0, 3.4, 7.9], [x0 + 1.2, 14.6, 8.6], 'door'), C(f'f{side}_r', [x1 - 1.2, 3.4, 7.9], [x1, 14.6, 8.6], 'door'),
                      C(f'g{side}', [x0 + 1.2, 3.4, 8.2], [x1 - 1.2, 14.6, 8.3], 'glass')]
        cubes += [C('k_l', [6.6, 3.6, 7.2], [7.4, 4.4, 7.9], 'handle'), C('k_r', [8.6, 3.6, 7.2], [9.4, 4.4, 7.9], 'handle')]
        spec(f'glass_cabinet_{fin}', mg, cubes, 9)
        # pantry: 2 tall
        cubes = [C('carcass', [0, 1, 1.6], [16, 32, 16], 'body'), C('toe', [0.5, 0, 2.6], [15.5, 1, 15.5], 'dark')]
        cubes += panel_door('door_lo_l', 0.2, 7.9, 1.3, 15.8) + panel_door('door_lo_r', 8.1, 15.8, 1.3, 15.8)
        cubes += panel_door('door_hi_l', 0.2, 7.9, 16.2, 31.8) + panel_door('door_hi_r', 8.1, 15.8, 16.2, 31.8)
        cubes += bar_v('h_l', 6.9, 12, 20) + bar_v('h_r', 9.1, 12, 20)
        spec(f'pantry_{fin}', mats, cubes, 15)

    # countertops: overlay slab on a cabinet (y 14.5-16, 0.5 overhang at the front)
    for cn, cm in COUNTERS.items():
        spec(f'counter_{cn}', {'top': cm, 'edge': cm}, [C('slab', [0, 14.5, 0], [16, 16, 16], 'top')], 14)
        steel = {'c': '#c3c8ce', 's': 'metal'}
        sink = [C('front', [0, 14.5, 0], [16, 16, 3], 'top'), C('back', [0, 14.5, 12], [16, 16, 16], 'top'),
                C('left', [0, 14.5, 3], [2.5, 16, 12], 'top'), C('right', [13.5, 14.5, 3], [16, 16, 12], 'top'),
                C('basin_floor', [2.5, 10.5, 3], [13.5, 11, 12], 'steel'),
                C('basin_n', [2.5, 11, 3], [13.5, 15.9, 3.3], 'steel'), C('basin_s', [2.5, 11, 11.7], [13.5, 15.9, 12], 'steel'),
                C('basin_w', [2.5, 11, 3.3], [2.8, 15.9, 11.7], 'steel'), C('basin_e', [13.2, 11, 3.3], [13.5, 15.9, 11.7], 'steel'),
                C('drain', [7.4, 10.9, 7.1], [8.6, 11.1, 8.3], 'dark'),
                C('faucet_base', [7.2, 16, 12.6], [8.8, 16.6, 14.2], 'chrome'), C('faucet_neck', [7.6, 16.6, 13], [8.4, 22, 13.8], 'chrome'),
                C('faucet_arch', [7.6, 21.2, 9.5], [8.4, 22, 13.8], 'chrome'), C('faucet_spout', [7.6, 19.8, 9.5], [8.4, 21.2, 10.3], 'chrome'),
                C('lever', [8.4, 17.5, 13.1], [10.5, 18.1, 13.7], 'chrome')]
        spec(f'counter_{cn}_sink', {'top': cm, 'steel': steel, 'chrome': {'c': '#e2e6ea', 's': 'metal'}, 'dark': {'c': '#202022', 's': 'dark'}}, sink, 14)

    steel = {'c': '#c9ced4', 's': 'metal'}
    door = {'c': '#d9dde2', 's': 'metal'}
    base = {'steel': steel, 'door': door, 'dark': {'c': '#24272b', 's': 'dark'}, 'handle': {'c': '#a3aab2', 's': 'metal'},
            'seal': {'c': '#4b4f55', 's': 'paint'}, 'glow': {'c': '#7fd6ff', 's': 'glow'}}

    # --- fridges (smart French-door is hand-made: fridge_smart.json)
    white = {'c': '#f1f1ee', 's': 'paint'}
    wd = {'c': '#f7f7f4', 's': 'paint'}
    # cheap top-freezer, white
    m = {'body': white, 'door': wd, 'dark': base['dark'], 'handle': {'c': '#d8d8d4', 's': 'paint'}, 'seal': base['seal']}
    cubes = [C('body', [1, 1, 2], [15, 30, 16], 'body'), C('kick', [1.5, 0, 2.5], [14.5, 1, 15.5], 'dark'),
             C('door_low', [1, 1, 1], [15, 20.4, 2.05], 'door'), C('door_up', [1, 21, 1], [15, 30, 2.05], 'door'), C('gap', [1.1, 20.4, 1.5], [14.9, 21, 1.95], 'seal')]
    cubes += bar_v('h_low', 2.6, 14, 19.4) + bar_v('h_up', 2.6, 21.8, 25.5)
    spec('fridge_basic', m, cubes, 15)
    # bottom freezer, stainless
    cubes = [C('body', [1, 1, 2], [15, 31.5, 16], 'steel'), C('kick', [1.5, 0, 2.5], [14.5, 1, 15.5], 'dark'),
             C('drawer', [1, 1, 1], [15, 11.4, 2.05], 'door'), C('door', [1, 12, 1], [15, 31.5, 2.05], 'door'), C('gap', [1.1, 11.4, 1.5], [14.9, 12, 1.95], 'seal')]
    cubes += bar_h('h_drawer', 4, 12, 9.6) + bar_v('h_door', 13.3, 14, 27)
    spec('fridge_bottom_freezer', base, cubes, 15)
    # side by side
    cubes = [C('body', [0.5, 1, 2], [15.5, 31.5, 16], 'steel'), C('kick', [1, 0, 2.5], [15, 1, 15.5], 'dark'),
             C('door_l', [0.5, 1, 1], [7.95, 31.5, 2.05], 'door'), C('door_r', [8.05, 1, 1], [15.5, 31.5, 2.05], 'door'),
             C('split', [7.95, 1, 1.4], [8.05, 31.5, 1.95], 'seal'), C('disp', [10, 17, 0.5], [14, 25, 0.95], 'dark'),
             C('spout', [11.6, 23, 0.3], [12.4, 23.8, 0.6], 'handle'), C('led', [10.3, 24.4, 0.4], [11.3, 24.7, 0.55], 'glow')]
    cubes += bar_v('h_l', 7.0, 8, 28) + bar_v('h_r', 9.0, 8, 28)
    spec('fridge_side_by_side', base, cubes, 15)
    # retro 50s, mint with chrome lever handle and rounded-looking top
    m = {'body': {'c': '#a8dcc6', 's': 'paint'}, 'door': {'c': '#b4e3cf', 's': 'paint'}, 'chrome': {'c': '#e6eaee', 's': 'metal'},
         'dark': base['dark'], 'seal': {'c': '#7fb8a0', 's': 'paint'}, 'badge': {'c': '#e8c35a', 's': 'metal'}}
    cubes = [C('body', [1, 1.5, 2], [15, 28.5, 16], 'body'), C('top_round', [1.5, 28.5, 2.5], [14.5, 29.5, 15.5], 'body'),
             C('top_round2', [2.5, 29.5, 3.5], [13.5, 30, 14.5], 'body'), C('foot_l', [1.5, 0, 2.5], [3.5, 1.5, 4.5], 'chrome'),
             C('foot_r', [12.5, 0, 2.5], [14.5, 1.5, 4.5], 'chrome'), C('foot_bl', [1.5, 0, 13.5], [3.5, 1.5, 15.5], 'chrome'),
             C('foot_br', [12.5, 0, 13.5], [14.5, 1.5, 15.5], 'chrome'), C('kick', [1.5, 1.5, 1.5], [14.5, 3.5, 2.1], 'chrome'),
             C('door', [1.5, 3.5, 1], [14.5, 27.5, 2.05], 'door'), C('door_round', [2.5, 27.5, 1.2], [13.5, 28.2, 2.05], 'door'),
             C('lever', [11.5, 17, -0.2], [13.8, 19.5, 0.9], 'chrome'), C('lever_base', [11.8, 16.6, 0.8], [13.5, 19.9, 1.05], 'chrome'),
             C('badge', [5.5, 24, 0.7], [10.5, 25.2, 1.05], 'badge'), C('stripe', [1.5, 9, 0.85], [14.5, 9.4, 1.05], 'chrome')]
    spec('fridge_retro', m, cubes, 15)
    # mini fridge (1 tall)
    m = {'body': {'c': '#2d2f33', 's': 'dark'}, 'door': {'c': '#35383d', 's': 'dark'}, 'handle': base['handle'], 'seal': base['seal'], 'dark': base['dark']}
    cubes = [C('body', [2, 0.5, 3], [14, 14, 15], 'body'), C('feet', [2.5, 0, 3.5], [13.5, 0.5, 14.5], 'dark'),
             C('door', [2, 0.5, 2.2], [14, 14, 3.1], 'door'), C('top', [2, 14, 3], [14, 14.4, 15], 'seal')]
    cubes += [C('handle', [12, 3, 1.5], [12.8, 11.5, 2.2], 'handle')]
    spec('mini_fridge', m, cubes, 7)
    # wine fridge (glass door, under-counter height)
    m = {'body': {'c': '#1f2023', 's': 'dark'}, 'frame': steel, 'glass': {'c': '#34404a', 's': 'glass', 'a': 0.55},
         'wine': {'c': '#5b1424', 's': 'glass'}, 'rack': {'c': '#7a5a3a', 's': 'wood'}, 'glow': {'c': '#ffd9a0', 's': 'glow'}}
    cubes = [C('body', [0.5, 0, 1.6], [15.5, 14.5, 16], 'body'), C('frame_t', [0.5, 12.8, 0.8], [15.5, 14.3, 1.7], 'frame'),
             C('frame_b', [0.5, 0.5, 0.8], [15.5, 2, 1.7], 'frame'), C('frame_l', [0.5, 2, 0.8], [2, 12.8, 1.7], 'frame'),
             C('frame_r', [14, 2, 0.8], [15.5, 12.8, 1.7], 'frame'), C('glass', [2, 2, 1.1], [14, 12.8, 1.2], 'glass'),
             C('light', [2.5, 12.2, 2], [13.5, 12.6, 3], 'glow')]
    for i, y in enumerate((3, 6.3, 9.6)):
        cubes += [C(f'rack{i}', [2, y - 0.4, 2], [14, y, 15], 'rack')]
        for j, x in enumerate((3.2, 6.2, 9.2, 12.2)):
            cubes += [C(f'bottle{i}{j}', [x - 0.8, y, 2.4], [x + 0.8, y + 1.6, 12], 'wine')]
    spec('wine_fridge', m, cubes, 7)
    # commercial glass-door beverage fridge, 2 tall
    m = {'body': {'c': '#e9ebee', 's': 'paint'}, 'frame': {'c': '#2b2e33', 's': 'dark'}, 'glass': {'c': '#cfe9f2', 's': 'glass', 'a': 0.35},
         'shelf': {'c': '#b9bec4', 's': 'metal'}, 'can_r': {'c': '#d63a3a', 's': 'metal'}, 'can_b': {'c': '#3a6ad6', 's': 'metal'},
         'can_g': {'c': '#39b26a', 's': 'metal'}, 'sign': {'c': '#ef7fbf', 's': 'glow'}, 'inside': {'c': '#f5f7f9', 's': 'paint'}}
    cubes = [C('back', [0.5, 0, 15], [15.5, 32, 16], 'inside'), C('side_l', [0.5, 0, 1.6], [1.5, 32, 15], 'body'),
             C('side_r', [14.5, 0, 1.6], [15.5, 32, 15], 'body'), C('base', [1.5, 0, 1.6], [14.5, 2.5, 15], 'frame'),
             C('top', [1.5, 28, 1.6], [14.5, 32, 15], 'body'), C('sign', [2, 28.8, 1.3], [14, 31.2, 1.6], 'sign'),
             C('frame_t', [0.5, 27, 0.6], [15.5, 28, 1.6], 'frame'), C('frame_b', [0.5, 2.5, 0.6], [15.5, 3.5, 1.6], 'frame'),
             C('frame_l', [0.5, 3.5, 0.6], [1.5, 27, 1.6], 'frame'), C('frame_r', [14.5, 3.5, 0.6], [15.5, 27, 1.6], 'frame'),
             C('glass', [1.5, 3.5, 1.0], [14.5, 27, 1.1], 'glass'), C('handle', [13, 8, 0], [13.7, 22, 0.6], 'frame')]
    cans = ['can_r', 'can_b', 'can_g']
    for i, y in enumerate((3.5, 9.5, 15.5, 21.5)):
        cubes += [C(f'shelf{i}', [1.5, y, 2], [14.5, y + 0.4, 15], 'shelf')]
        for j in range(5):
            x = 2.4 + j * 2.4
            cubes += [C(f'can{i}{j}', [x, y + 0.4, 3], [x + 1.8, y + 3.6, 13], cans[(i + j) % 3])]
    spec('fridge_commercial', m, cubes, 15)

    # --- dishwasher (under counter, takes a countertop)
    m = dict(base)
    cubes = [C('body', [0.3, 1, 1.6], [15.7, 14.5, 16], 'steel'), C('toe', [0.5, 0, 2.6], [15.5, 1, 15.5], 'dark'),
             C('door', [0.3, 1.2, 0.8], [15.7, 12.4, 1.7], 'door'), C('control', [0.3, 12.6, 0.8], [15.7, 14.4, 1.7], 'dark'),
             C('led1', [10, 13.2, 0.7], [11, 13.7, 0.85], 'glow'), C('led2', [11.6, 13.2, 0.7], [12.6, 13.7, 0.85], 'glow'),
             C('btn', [13.2, 13.1, 0.6], [14.4, 13.8, 0.85], 'handle')]
    cubes += bar_h('handle', 3, 13, 11.2)
    spec('dishwasher', m, cubes, 8)
    # --- trash compactor
    m = dict(base)
    cubes = [C('body', [0.3, 1, 1.6], [15.7, 14.5, 16], 'steel'), C('toe', [0.5, 0, 2.6], [15.5, 1, 15.5], 'dark'),
             C('drawer', [0.3, 1.2, 0.8], [15.7, 11.6, 1.7], 'door'), C('top_panel', [0.3, 11.8, 0.8], [15.7, 14.4, 1.7], 'door'),
             C('key', [2, 12.6, 0.5], [3.2, 13.6, 0.85], 'dark'), C('led', [4, 12.9, 0.7], [5, 13.3, 0.85], 'glow')]
    cubes += bar_h('handle', 4, 12, 10.2)
    spec('trash_compactor', m, cubes, 8)
    # --- stove / range with oven (own cooktop) + backguard knobs
    m = dict(base, glass={'c': '#15171a', 's': 'dark'}, ring={'c': '#3a3d42', 's': 'paint'}, window={'c': '#2a2f36', 's': 'glass'})
    cubes = [C('body', [0.3, 1, 1.6], [15.7, 15, 16], 'steel'), C('toe', [0.5, 0, 2.6], [15.5, 1, 15.5], 'dark'),
             C('cooktop', [0.3, 15, 1.2], [15.7, 16, 15], 'glass'), C('backguard', [0.3, 15, 15], [15.7, 19, 16], 'steel'),
             C('oven_door', [0.6, 3, 0.8], [15.4, 12.8, 1.7], 'door'), C('window', [3, 5.5, 0.7], [13, 10.5, 0.85], 'window'),
             C('drawer', [0.6, 1.2, 0.8], [15.4, 2.8, 1.7], 'door'), C('panel', [0.6, 13, 0.8], [15.4, 15, 1.7], 'dark'),
             C('clock', [6.8, 13.5, 0.6], [9.2, 14.5, 0.85], 'glow')]
    cubes += bar_h('handle', 2.5, 13.5, 12)
    for i, (x, z) in enumerate(((4.5, 4.5), (11.5, 4.5), (4.5, 11), (11.5, 11))):
        r = 2.4 if i % 3 == 0 else 1.8
        cubes += [C(f'burner{i}', [x - r, 16, z - r], [x + r, 16.1, z + r], 'ring')]
    for i, x in enumerate((2.2, 4.4, 11.6, 13.8)):
        cubes += [C(f'knob{i}', [x - 0.5, 13.6, 0.3], [x + 0.5, 14.6, 0.8], 'handle')]
    spec('stove', m, cubes, 9)
    # --- range hood (chimney, on the wall above the stove)
    cubes = [C('canopy', [0.5, 2, 6], [15.5, 4, 16], 'steel'), C('canopy_top', [2.5, 4, 8.5], [13.5, 6, 16], 'steel'),
             C('chimney', [5, 6, 11], [11, 16, 16], 'steel'), C('lip', [0.5, 1.6, 6], [15.5, 2, 6.6], 'dark'),
             C('light_l', [3, 1.9, 9], [5, 2, 11], 'glow'), C('light_r', [11, 1.9, 9], [13, 2, 11], 'glow'),
             C('btns', [6, 2.3, 5.8], [10, 3.3, 6.1], 'dark')]
    spec('range_hood', dict(base), cubes, 8)
    # --- microwave (sits on a counter)
    m = dict(base, window={'c': '#1a1e23', 's': 'glass'})
    cubes = [C('body', [2, 0, 4], [14, 7, 13], 'steel'), C('door', [2, 0.3, 3.4], [10.6, 6.7, 4.05], 'dark'),
             C('window', [2.6, 1, 3.3], [9.8, 6, 3.45], 'window'), C('panel', [10.7, 0.3, 3.4], [14, 6.7, 4.05], 'door'),
             C('display', [11.2, 5.2, 3.3], [13.5, 6.1, 3.45], 'glow'), C('pad', [11.3, 1.6, 3.3], [13.4, 4.6, 3.45], 'seal'),
             C('open_btn', [11.6, 0.6, 3.3], [13.1, 1.3, 3.45], 'handle'), C('feet', [2.5, -0.1, 4.5], [13.5, 0, 12.5], 'dark')]
    spec('microwave', m, cubes, 4)
    # --- small counter appliances
    red = {'c': '#d64545', 's': 'paint'}
    spec('toaster', {'body': red, 'chrome': {'c': '#e2e6ea', 's': 'metal'}, 'dark': base['dark']},
         [C('body', [4, 0, 5], [12, 5, 11], 'body'), C('slot1', [5, 5, 6.3], [11, 5.05, 7.3], 'dark'), C('slot2', [5, 5, 8.7], [11, 5.05, 9.7], 'dark'),
          C('lever', [3.4, 2.5, 7.5], [4, 3.2, 8.5], 'chrome'), C('base', [4.3, -0.1, 5.3], [11.7, 0, 10.7], 'dark'), C('knob', [3.6, 1, 6], [4, 1.6, 6.6], 'chrome')], 3)
    spec('coffee_maker', {'body': {'c': '#1f2023', 's': 'dark'}, 'steel': steel, 'pot': {'c': '#7a5232', 's': 'glass', 'a': 0.7},
                          'glow': {'c': '#ff9a3c', 's': 'glow'}},
         [C('base', [4, 0, 4], [12, 1, 12], 'body'), C('tower', [4, 1, 9], [12, 11, 12], 'body'), C('head', [4, 9, 4], [12, 11, 9], 'body'),
          C('pot', [5.5, 1, 5], [10.5, 6, 8.5], 'pot'), C('pot_lid', [5.7, 6, 5.2], [10.3, 6.6, 8.3], 'body'), C('pot_handle', [10.5, 2, 6.2], [11.3, 5.5, 7.2], 'body'),
          C('plate', [5, 1, 4.6], [11, 1.1, 9], 'steel'), C('led', [5, 10, 3.9], [6, 10.5, 4], 'glow')], 5)
    spec('blender', {'base': {'c': '#e9e9e6', 's': 'paint'}, 'jar': {'c': '#d9f0f7', 's': 'glass', 'a': 0.5}, 'lid': {'c': '#2a2a2c', 's': 'dark'},
                     'smoothie': {'c': '#ef6f9a', 's': 'paint'}, 'btn': {'c': '#7fd6ff', 's': 'glow'}},
         [C('base', [5, 0, 5], [11, 3, 11], 'base'), C('btn', [6.5, 1, 4.9], [9.5, 2, 5], 'btn'), C('jar', [5.6, 3, 5.6], [10.4, 11, 10.4], 'jar'),
          C('fill', [6, 3.2, 6], [10, 7, 10], 'smoothie'), C('lid', [5.4, 11, 5.4], [10.6, 12, 10.6], 'lid'), C('handle', [10.4, 5, 7.4], [11.4, 10, 8.6], 'jar')], 5)
    spec('kettle', {'body': {'c': '#e0e4e8', 's': 'metal'}, 'dark': base['dark'], 'glow': {'c': '#7fd6ff', 's': 'glow'}},
         [C('base', [5, 0, 5], [11, 0.8, 11], 'dark'), C('body', [5.5, 0.8, 5.5], [10.5, 7, 10.5], 'body'), C('lid', [6.5, 7, 6.5], [9.5, 7.6, 9.5], 'dark'),
          C('spout', [7.3, 4.5, 4], [8.7, 6.2, 5.5], 'body'), C('handle', [7.3, 3, 10.5], [8.7, 7.8, 12], 'dark'), C('led', [7.5, 1.2, 5.4], [8.5, 1.8, 5.5], 'glow')], 4)
    spec('knife_block', {'wood': {'c': '#8a5a34', 's': 'wood'}, 'dark': base['dark'], 'blade': {'c': '#e2e6ea', 's': 'metal'}},
         [C('block', [5.5, 0, 6], [10.5, 6, 11], 'wood', r=['x', -22.5, [8, 0, 8]])] + [C(f'h{i}', [6 + i * 1.1, 5, 6.5 + i * 0.4], [6.8 + i * 1.1, 9, 7.3 + i * 0.4], 'dark') for i in range(4)], 4)
    spec('fruit_bowl', {'bowl': {'c': '#eae6dc', 's': 'porcelain'}, 'apple': {'c': '#d33a3a', 's': 'paint'}, 'orange': {'c': '#f29a2e', 's': 'paint'},
                        'banana': {'c': '#f2d64b', 's': 'paint'}, 'grape': {'c': '#7a3fa0', 's': 'paint'}},
         [C('bowl', [4, 0, 4], [12, 2.5, 12], 'bowl'), C('apple', [5, 2, 5], [8, 5, 8], 'apple'), C('orange', [8.4, 2, 7.6], [11.4, 5, 10.6], 'orange'),
          C('banana', [4.8, 2.4, 9.2], [10, 3.6, 10.6], 'banana', r=['y', 22.5, [8, 3, 10]]), C('grapes', [6.6, 2.4, 8.4], [8.4, 4.6, 10.2], 'grape')], 3)
    spec('paper_towel', {'wood': {'c': '#8a5a34', 's': 'wood'}, 'paper': {'c': '#fbfbf8', 's': 'fabric'}},
         [C('base', [5, 0, 5], [11, 0.6, 11], 'wood'), C('rod', [7.6, 0.6, 7.6], [8.4, 9, 8.4], 'wood'), C('roll', [5.5, 0.8, 5.5], [10.5, 8, 10.5], 'paper')], 4)
    spec('spice_rack', {'wood': {'c': '#9a6a40', 's': 'wood'}, 'j1': {'c': '#c0392b', 's': 'glass'}, 'j2': {'c': '#e67e22', 's': 'glass'},
                        'j3': {'c': '#27ae60', 's': 'glass'}, 'j4': {'c': '#8e6e53', 's': 'glass'}, 'lid': {'c': '#2a2a2c', 's': 'dark'}},
         [C('back', [1, 0, 14], [15, 9, 16], 'wood'), C('shelf1', [1, 0, 10], [15, 0.6, 16], 'wood'), C('shelf2', [1, 4.5, 10], [15, 5.1, 16], 'wood'),
          C('rail1', [1, 2, 10], [15, 2.4, 10.4], 'wood'), C('rail2', [1, 6.5, 10], [15, 6.9, 10.4], 'wood')] +
         [C(f'jar{r}{i}', [1.8 + i * 2.7, 0.6 + r * 4.5, 11], [3.8 + i * 2.7, 3.6 + r * 4.5, 13], f'j{(i + r) % 4 + 1}') for r in range(2) for i in range(5)] +
         [C(f'lid{r}{i}', [1.9 + i * 2.7, 3.6 + r * 4.5, 11.1], [3.7 + i * 2.7, 4.1 + r * 4.5, 12.9], 'lid') for r in range(2) for i in range(5)], 4)
    spec('trash_can', {'steel': steel, 'dark': base['dark'], 'pedal': base['handle']},
         [C('can', [4, 0, 4], [12, 11, 12], 'steel'), C('lid', [3.8, 11, 3.8], [12.2, 12, 12.2], 'steel'), C('band', [3.9, 0, 3.9], [12.1, 1, 12.1], 'dark'),
          C('pedal', [6, 0, 2.8], [10, 0.8, 4], 'pedal')], 6)
    spec('bar_stool', {'seat': {'c': '#3a3a3e', 's': 'fabric'}, 'chrome': {'c': '#e2e6ea', 's': 'metal'}},
         [C('seat', [3, 12, 3], [13, 14, 13], 'seat'), C('post', [7.2, 2, 7.2], [8.8, 12, 8.8], 'chrome'), C('base', [3.5, 0, 3.5], [12.5, 0.8, 12.5], 'chrome'),
          C('foot_ring', [4.5, 6, 4.5], [11.5, 6.5, 11.5], 'chrome'), C('ring_hole', [5.2, 5.95, 5.2], [10.8, 6.55, 10.8], 'seat')], 7)


def bathroom():
    por = {'c': '#f4f4f1', 's': 'porcelain'}
    chrome = {'c': '#e2e6ea', 's': 'metal'}
    dark = {'c': '#2a2a2c', 's': 'dark'}
    water = {'c': '#6fb7e0', 's': 'glass', 'a': 0.7}
    spec('toilet', {'por': por, 'chrome': chrome, 'seat': {'c': '#fbfbfa', 's': 'porcelain'}, 'water': water, 'dark': dark},
         [C('base', [5, 0, 5], [11, 6, 12], 'por'), C('bowl', [3.5, 6, 2], [12.5, 9.5, 12], 'por'), C('water', [5, 9.2, 3.5], [11, 9.4, 10.5], 'water'),
          C('seat', [3.3, 9.5, 1.8], [12.7, 10.2, 12], 'seat'), C('lid', [3.3, 10.2, 11.3], [12.7, 19.5, 12], 'seat'),
          C('tank', [2.5, 9.5, 12], [13.5, 19, 16], 'por'), C('tank_lid', [2.2, 19, 11.8], [13.8, 20, 16], 'por'),
          C('flush', [10.8, 16.8, 11.4], [12.4, 17.6, 12], 'chrome')], 10)
    for fin in ('white', 'oak', 'walnut', 'sage', 'navy'):
        body, door = FINISH[fin]
        mats = dict(HW, body=body, door=door, trim=door, por=por, chrome=chrome, top={'c': '#eeedeb', 's': 'marble'}, mirror={'c': '#cfe3ea', 's': 'glass'})
        cubes = [C('carcass', [0, 1, 2.6], [16, 13.5, 16], 'body'), C('toe', [0.5, 0, 3.6], [15.5, 1, 15.5], 'dark'),
                 C('top_front', [0, 13.5, 1.2], [16, 15, 4], 'top'), C('top_back', [0, 13.5, 12], [16, 15, 16], 'top'),
                 C('top_l', [0, 13.5, 4], [3, 15, 12], 'top'), C('top_r', [13, 13.5, 4], [16, 15, 12], 'top'),
                 C('basin', [3, 10.5, 4], [13, 11, 12], 'por'), C('basin_n', [3, 11, 4], [13, 14.9, 4.3], 'por'), C('basin_s', [3, 11, 11.7], [13, 14.9, 12], 'por'),
                 C('basin_w', [3, 11, 4.3], [3.3, 14.9, 11.7], 'por'), C('basin_e', [12.7, 11, 4.3], [13, 14.9, 11.7], 'por'),
                 C('faucet', [7.4, 15, 12.6], [8.6, 18.5, 13.8], 'chrome'), C('spout', [7.5, 17.6, 10.2], [8.5, 18.5, 12.6], 'chrome'),
                 C('lever', [7.6, 18.5, 12.8], [8.4, 19.6, 13.6], 'chrome')]
        cubes += [c for c in panel_door('door_l', 0.2, 7.9, 1.3, 13.3) + panel_door('door_r', 8.1, 15.8, 1.3, 13.3)]
        for c in cubes:
            if c['n'].startswith('door'):
                c['f'][2] += 1; c['t'][2] += 1
        cubes += knob('k_l', 6.9, 10.5, 1.3) + knob('k_r', 9.1, 10.5, 1.3)
        spec(f'vanity_{fin}', mats, cubes, 9)
    # bathtub: 2 long (z 0..32), front faces north = the long side faces the player
    spec('bathtub', {'por': por, 'chrome': chrome, 'water': water, 'feet': {'c': '#c9a24a', 's': 'metal'}},
         [C('floor', [1, 1.5, 1], [15, 2.5, 31], 'por'), C('wall_n', [1, 2.5, 1], [15, 11, 2], 'por'), C('wall_s', [1, 2.5, 30], [15, 11, 31], 'por'),
          C('wall_w', [1, 2.5, 2], [2, 11, 30], 'por'), C('wall_e', [14, 2.5, 2], [15, 11, 30], 'por'),
          C('rim', [0.6, 11, 0.6], [15.4, 11.8, 31.4], 'por'), C('rim_hole', [2, 11, 2], [14, 11.85, 30], 'water', hide=['north', 'south', 'east', 'west', 'down']),
          C('foot1', [1.5, 0, 2], [3.5, 1.5, 4], 'feet'), C('foot2', [12.5, 0, 2], [14.5, 1.5, 4], 'feet'),
          C('foot3', [1.5, 0, 28], [3.5, 1.5, 30], 'feet'), C('foot4', [12.5, 0, 28], [14.5, 1.5, 30], 'feet'),
          C('faucet', [7.2, 11.8, 29.5], [8.8, 14.5, 30.8], 'chrome'), C('spout', [7.4, 13.6, 27], [8.6, 14.5, 29.5], 'chrome'),
          C('tap_l', [5.4, 11.8, 29.8], [6.4, 12.8, 30.6], 'chrome'), C('tap_r', [9.6, 11.8, 29.8], [10.6, 12.8, 30.6], 'chrome')], 8)
    spec('bathtub_water', {'water': water}, [C('water', [2, 9.5, 2], [14, 10, 30], 'water')], 8)
    # shower stall: 2 tall, glass front door
    spec('shower', {'tile': {'c': '#eef2f4', 's': 'tile', 'c2': '#b9c3ca'}, 'tray': por, 'chrome': chrome, 'glass': {'c': '#d4eef6', 's': 'glass', 'a': 0.3}, 'dark': dark},
         [C('tray', [0, 0, 0], [16, 1, 16], 'tray'), C('drain', [7.3, 1, 7.3], [8.7, 1.05, 8.7], 'dark'),
          C('wall_back', [0, 1, 15], [16, 32, 16], 'tile'), C('wall_l', [0, 1, 1], [1, 32, 15], 'tile'), C('wall_r', [15, 1, 1], [16, 32, 15], 'tile'),
          C('glass_door', [1, 1.5, 0.4], [15, 30, 0.6], 'glass'), C('frame_t', [0.5, 30, 0], [15.5, 31, 1], 'chrome'),
          C('frame_l', [0.5, 1, 0], [1.3, 30, 1], 'chrome'), C('frame_r', [14.7, 1, 0], [15.5, 30, 1], 'chrome'),
          C('door_handle', [12.5, 13, -0.4], [13.1, 19, 0.4], 'chrome'),
          C('pipe', [7.6, 18, 14.4], [8.4, 27, 15], 'chrome'), C('arm', [7.6, 26.4, 11], [8.4, 27, 15], 'chrome'),
          C('head', [6.5, 25.6, 9.5], [9.5, 26.4, 12.5], 'chrome'), C('valve', [7, 15, 14.3], [9, 17, 15], 'chrome'),
          C('shelf', [11, 18, 13.5], [14.5, 18.4, 15], 'chrome')], 16)
    spec('mirror', {'frame': {'c': '#c9a24a', 's': 'metal'}, 'mirror': {'c': '#d4e6ee', 's': 'glass'}},
         [C('frame', [2, 1, 15], [14, 15, 16], 'frame'), C('glass', [3, 2, 14.7], [13, 14, 15], 'mirror')], 8)
    spec('medicine_cabinet', {'body': {'c': '#f2f2ef', 's': 'paint'}, 'mirror': {'c': '#d4e6ee', 's': 'glass'}, 'chrome': chrome},
         [C('box', [2, 2, 12], [14, 15, 16], 'body'), C('door', [2.2, 2.2, 11.4], [13.8, 14.8, 12], 'body'), C('glass', [3, 3, 11.2], [13, 14, 11.4], 'mirror'),
          C('pull', [12.6, 7.5, 10.8], [13.2, 9.5, 11.4], 'chrome')], 8)
    spec('towel_rack', {'chrome': chrome, 'towel': {'c': '#f4a6c8', 's': 'fabric'}, 'towel2': {'c': '#a6d4f4', 's': 'fabric'}},
         [C('bar', [1, 11, 13], [15, 11.6, 13.6], 'chrome'), C('post_l', [1, 10.6, 13.6], [2, 12, 16], 'chrome'), C('post_r', [14, 10.6, 13.6], [15, 12, 16], 'chrome'),
          C('towel', [3, 3, 12.6], [8.5, 11.8, 14], 'towel'), C('towel2', [9, 4.5, 12.6], [13, 11.8, 14], 'towel2')], 8)
    spec('toilet_paper', {'chrome': chrome, 'paper': {'c': '#fbfbf8', 's': 'fabric'}},
         [C('plate', [6, 7, 15.4], [10, 9, 16], 'chrome'), C('arm', [6.5, 7.6, 12], [7.1, 8.4, 15.4], 'chrome'), C('arm2', [8.9, 7.6, 12], [9.5, 8.4, 15.4], 'chrome'),
          C('roll', [5.5, 5.5, 11.5], [10.5, 10.5, 15], 'paper')], 8)
    spec('bath_mat', {'mat': {'c': '#9fd3c9', 's': 'fabric'}, 'edge': {'c': '#7fbfb3', 's': 'fabric'}},
         [C('mat', [2, 0, 4], [14, 0.5, 12], 'mat'), C('fringe_l', [1.4, 0, 4.5], [2, 0.3, 11.5], 'edge'), C('fringe_r', [14, 0, 4.5], [14.6, 0.3, 11.5], 'edge')], 2)
    spec('hamper', {'wicker': {'c': '#c8a46e', 's': 'wood'}, 'lid': {'c': '#b08a54', 's': 'wood'}, 'cloth': {'c': '#e9e2d6', 's': 'fabric'}},
         [C('basket', [3.5, 0, 3.5], [12.5, 12, 12.5], 'wicker'), C('liner', [3.3, 10.5, 3.3], [12.7, 12.5, 12.7], 'cloth'), C('lid', [3.2, 12.5, 3.2], [12.8, 13.3, 12.8], 'lid'),
          C('knob', [7.4, 13.3, 7.4], [8.6, 14, 8.6], 'lid')], 6)
    white = {'c': '#f2f3f4', 's': 'paint'}
    spec('washing_machine', {'body': white, 'door': {'c': '#c9ced4', 's': 'metal'}, 'glass': {'c': '#3b5a73', 's': 'glass'}, 'panel': {'c': '#dfe2e5', 's': 'paint'},
                             'dark': dark, 'glow': {'c': '#7fd6ff', 's': 'glow'}},
         [C('body', [1, 0, 2], [15, 15, 16], 'body'), C('panel', [1, 12, 1.4], [15, 15, 2], 'panel'), C('knob', [11, 12.8, 0.8], [13, 14.6, 1.4], 'door'),
          C('display', [6, 13.2, 1.2], [10, 14.2, 1.4], 'glow'), C('drawer', [2, 12.6, 1.2], [5, 14.4, 1.4], 'panel'),
          C('ring', [3.5, 2.5, 1.2], [12.5, 11.5, 2], 'door'), C('glass', [4.5, 3.5, 1.1], [11.5, 10.5, 1.2], 'glass'), C('feet', [1.5, -0.1, 2.5], [14.5, 0, 15.5], 'dark')], 8)
    spec('dryer', {'body': white, 'door': {'c': '#c9ced4', 's': 'metal'}, 'glass': {'c': '#5a6773', 's': 'glass'}, 'panel': {'c': '#dfe2e5', 's': 'paint'},
                   'dark': dark, 'glow': {'c': '#ffb36a', 's': 'glow'}},
         [C('body', [1, 0, 2], [15, 15, 16], 'body'), C('panel', [1, 12, 1.4], [15, 15, 2], 'panel'), C('knob', [11, 12.8, 0.8], [13, 14.6, 1.4], 'door'),
          C('display', [6, 13.2, 1.2], [10, 14.2, 1.4], 'glow'), C('ring', [3.5, 2.5, 1.2], [12.5, 11.5, 2], 'door'), C('glass', [4.5, 3.5, 1.1], [11.5, 10.5, 1.2], 'glass'),
          C('vent', [3, 0.8, 1.6], [13, 1.6, 2.05], 'dark'), C('feet', [1.5, -0.1, 2.5], [14.5, 0, 15.5], 'dark')], 8)
    spec('bath_scale', {'body': {'c': '#e9eef2', 's': 'glass'}, 'display': {'c': '#7fd6ff', 's': 'glow'}, 'dark': dark},
         [C('plate', [3, 0, 3], [13, 0.8, 13], 'body'), C('screen', [6.5, 0.8, 3.6], [9.5, 0.85, 5.2], 'display'), C('feet', [3.5, -0.1, 3.5], [12.5, 0, 12.5], 'dark')], 2)


def foods():
    spec('toast', {'bread': {'c': '#d9a35a', 's': 'paint'}, 'crust': {'c': '#8a5426', 's': 'paint'}, 'butter': {'c': '#f6e27a', 's': 'paint'}},
         [C('slice', [4, 0, 7], [12, 9, 8.5], 'bread'), C('crust_t', [4.5, 9, 7], [11.5, 10.2, 8.5], 'crust'), C('crust_l', [3.4, 0, 7], [4, 9.4, 8.5], 'crust'),
          C('crust_r', [12, 0, 7], [12.6, 9.4, 8.5], 'crust'), C('butter', [6, 3, 6.7], [10, 6, 7], 'butter')], 4)
    spec('coffee', {'mug': {'c': '#ef7fbf', 's': 'porcelain'}, 'coffee': {'c': '#4a2a16', 's': 'paint'}, 'foam': {'c': '#e6d2b4', 's': 'paint'}},
         [C('mug', [5, 0, 5], [11, 8, 11], 'mug'), C('coffee', [5.5, 7.6, 5.5], [10.5, 7.8, 10.5], 'coffee'), C('foam', [6.5, 7.8, 6.5], [9.5, 7.9, 9.5], 'foam'),
          C('handle_t', [11, 5.5, 7.4], [13, 6.5, 8.6], 'mug'), C('handle_b', [11, 1.5, 7.4], [13, 2.5, 8.6], 'mug'), C('handle_o', [12.6, 2.5, 7.4], [13.4, 5.5, 8.6], 'mug')], 4)
    spec('smoothie', {'cup': {'c': '#f4c4d8', 's': 'glass', 'a': 0.8}, 'lid': {'c': '#f2f2ef', 's': 'paint'}, 'straw': {'c': '#7fd6ff', 's': 'paint'}},
         [C('cup', [5.5, 0, 5.5], [10.5, 10, 10.5], 'cup'), C('lid', [5.2, 10, 5.2], [10.8, 11, 10.8], 'lid'), C('straw', [7.5, 11, 7.5], [8.5, 15, 8.5], 'straw')], 5)
    spec('tea', {'cup': {'c': '#f4f4f1', 's': 'porcelain'}, 'tea': {'c': '#b5763a', 's': 'paint'}, 'saucer': {'c': '#a6d4f4', 's': 'porcelain'}},
         [C('saucer', [3, 0, 3], [13, 0.6, 13], 'saucer'), C('cup', [5, 0.6, 5], [11, 5.6, 11], 'cup'), C('tea', [5.5, 5.2, 5.5], [10.5, 5.4, 10.5], 'tea'),
          C('handle', [11, 2, 7.4], [12.6, 4.6, 8.6], 'cup')], 3)
    spec('trash_cube', {'trash': {'c': '#6b6a5c', 's': 'granite', 'c2': '#3e3d33', 'c3': '#9a9884'}, 'band': {'c': '#2a2a2c', 's': 'dark'}},
         [C('cube', [3, 0, 3], [13, 10, 13], 'trash'), C('band1', [2.8, 3, 2.8], [13.2, 3.6, 13.2], 'band'), C('band2', [2.8, 6.6, 2.8], [13.2, 7.2, 13.2], 'band')], 5)


def workshop():
    wood = {'c': '#a87545', 's': 'wood'}
    top = {'c': '#c9935a', 's': 'wood'}
    steel = {'c': '#b9bec4', 's': 'metal'}
    peg = {'c': '#d9c3a0', 's': 'paint'}
    red = {'c': '#d64545', 's': 'paint'}
    spec('furniture_workbench', {'wood': wood, 'top': top, 'steel': steel, 'peg': peg, 'red': red, 'dark': {'c': '#2a2a2c', 's': 'dark'},
                                 'pink': {'c': '#ef7fbf', 's': 'paint'}},
         [C('top', [0, 12, 1], [16, 14, 15], 'top'), C('leg1', [1, 0, 2], [3, 12, 4], 'wood'), C('leg2', [13, 0, 2], [15, 12, 4], 'wood'),
          C('leg3', [1, 0, 12], [3, 12, 14], 'wood'), C('leg4', [13, 0, 12], [15, 12, 14], 'wood'), C('shelf', [1, 3, 2], [15, 4, 14], 'wood'),
          C('board', [0, 14, 14.5], [16, 24, 16], 'peg'), C('board_trim', [0, 23.5, 14], [16, 24.2, 16], 'wood'),
          C('saw_blade', [2, 17, 14], [8, 19.5, 14.4], 'steel'), C('saw_handle', [8, 16.8, 14], [10, 19.8, 14.4], 'red'),
          C('hammer_head', [11, 21, 13.8], [14, 22.2, 14.4], 'dark'), C('hammer_handle', [12.1, 16, 14], [12.9, 21, 14.4], 'wood'),
          C('square', [3, 20, 14.1], [7, 20.6, 14.4], 'steel'), C('square2', [3, 20, 14.1], [3.6, 23, 14.4], 'steel'),
          C('vise', [12, 12.5, 0], [15, 15, 2.5], 'steel'), C('vise_screw', [13.1, 13.2, -1.2], [13.9, 14, 0], 'dark'),
          C('plank', [2, 14, 4], [10, 14.6, 8], 'top'), C('pencil', [10.5, 14, 6], [14, 14.4, 6.4], 'pink'),
          C('tape', [4, 14, 9.5], [6, 15.2, 11.5], 'red')], 10)


def stairs():
    """spiral stair: half a turn per block (4 treads, 4px apart) so a stack leaves 2 blocks of headroom.
    The block above is turned 180 degrees automatically, continuing the spiral."""
    kinds = {'oak': ({'c': '#c9965c', 's': 'wood'}, {'c': '#8a6038', 's': 'wood'}),
             'walnut': ({'c': '#6b4730', 's': 'wood'}, {'c': '#3d2a1c', 's': 'wood'}),
             'iron': ({'c': '#3a3d42', 's': 'dark'}, {'c': '#2a2c30', 's': 'dark'}),
             'white': ({'c': '#f2f0ec', 's': 'paint'}, {'c': '#c9a24a', 's': 'metal'})}
    for k, (tread, pole) in kinds.items():
        cubes = [C('pole', [7, 0, 7], [9, 16, 9], 'pole')]
        spokes = {0: ([4.3, 0.2], [11.7, 8]), 2: ([8, 4.3], [15.8, 11.7])}       # N and E, axis aligned
        for i in range(4):
            y = i * 4
            if i % 2 == 0:
                (x0, z0), (x1, z1) = spokes[i]
                cubes.append(C(f'tread{i}', [x0, y, z0], [x1, y + 1, z1], 'tread'))
            else:      # previous spoke turned 45 degrees further round
                (x0, z0), (x1, z1) = ([5.5, -0.8], [10.5, 8]) if i == 1 else ([8, 5.5], [16.8, 10.5])
                cubes.append(C(f'tread{i}', [x0, y, z0], [x1, y + 1, z1], 'tread', r=['y', -45, [8, y, 8]]))
        # handrail posts at the outer ends + a rail segment
        for i, (px, pz) in enumerate(((8, 1.0), (13.2, 2.8), (15.0, 8), (13.2, 13.2))):
            cubes.append(C(f'post{i}', [px - 0.4, i * 4 + 1, pz - 0.4], [px + 0.4, i * 4 + 12, pz + 0.4], 'pole'))
        spec(f'spiral_stairs_{k}', {'tread': tread, 'pole': pole}, cubes, 8)

def living():
    wood = {'c': '#8a5a34', 's': 'wood'}
    oak = {'c': '#c9965c', 's': 'wood'}
    dark = {'c': '#222326', 's': 'dark'}
    steel = {'c': '#c9ced4', 's': 'metal'}
    gold = {'c': '#c9a24a', 's': 'metal'}
    # sofas: 1 block wide pieces in 4 fabrics (place several side by side)
    for col, (fab, fab2) in {'pink': ('#e98bb8', '#f2a6c9'), 'gray': ('#8a8d93', '#9da0a6'), 'navy': ('#34456a', '#40527a'),
                              'sage': ('#8fa88a', '#a1b99c')}.items():
        m = {'fab': {'c': fab, 's': 'fabric'}, 'cush': {'c': fab2, 's': 'fabric'}, 'leg': wood}
        spec(f'sofa_{col}', m, [C('base', [0, 2, 1], [16, 6, 15], 'fab'), C('seat', [0.3, 6, 1.3], [15.7, 8.5, 11], 'cush'),
             C('back', [0, 6, 11], [16, 15, 15], 'fab'), C('back_cush', [0.5, 8.5, 9.5], [15.5, 14, 11.2], 'cush'),
             C('leg1', [1, 0, 2], [2.5, 2, 3.5], 'leg'), C('leg2', [13.5, 0, 2], [15, 2, 3.5], 'leg'),
             C('leg3', [1, 0, 12.5], [2.5, 2, 14], 'leg'), C('leg4', [13.5, 0, 12.5], [15, 2, 14], 'leg')], 7)
        spec(f'armchair_{col}', m, [C('base', [1, 2, 1], [15, 6, 15], 'fab'), C('seat', [3, 6, 1.3], [13, 8.5, 11], 'cush'),
             C('back', [1, 6, 11], [15, 16, 15], 'fab'), C('arm_l', [1, 6, 1], [3, 11, 11], 'fab'), C('arm_r', [13, 6, 1], [15, 11, 11], 'fab'),
             C('leg1', [1.5, 0, 1.5], [3, 2, 3], 'leg'), C('leg2', [13, 0, 1.5], [14.5, 2, 3], 'leg'),
             C('leg3', [1.5, 0, 13], [3, 2, 14.5], 'leg'), C('leg4', [13, 0, 13], [14.5, 2, 14.5], 'leg')], 7)
    spec('coffee_table', {'top': {'c': '#d4e9f0', 's': 'glass', 'a': 0.55}, 'frame': oak, 'shelf': oak},
         [C('top', [1, 6, 2], [15, 6.6, 14], 'top'), C('rim', [1, 5.4, 2], [15, 6, 14], 'frame'),
          C('leg1', [1.5, 0, 2.5], [3, 5.4, 4], 'frame'), C('leg2', [13, 0, 2.5], [14.5, 5.4, 4], 'frame'),
          C('leg3', [1.5, 0, 12], [3, 5.4, 13.5], 'frame'), C('leg4', [13, 0, 12], [14.5, 5.4, 13.5], 'frame'), C('shelf', [3, 1.5, 4], [13, 2, 12], 'shelf')], 4)
    spec('side_table', {'wood': oak, 'knob': gold},
         [C('top', [3, 9, 3], [13, 10, 13], 'wood'), C('box', [3.5, 5, 3.5], [12.5, 9, 12.5], 'wood'), C('drawer', [4, 5.5, 3.2], [12, 8.5, 3.6], 'wood'),
          C('knob', [7.5, 6.6, 2.7], [8.5, 7.4, 3.2], 'knob'), C('leg1', [3.5, 0, 3.5], [4.5, 5, 4.5], 'wood'), C('leg2', [11.5, 0, 3.5], [12.5, 5, 4.5], 'wood'),
          C('leg3', [3.5, 0, 11.5], [4.5, 5, 12.5], 'wood'), C('leg4', [11.5, 0, 11.5], [12.5, 5, 12.5], 'wood')], 5)
    spec('tv_stand', {'wood': wood, 'dark': dark, 'knob': gold},
         [C('body', [0, 1, 4], [16, 7, 15], 'wood'), C('door_l', [0.3, 1.5, 3.6], [7.9, 6.5, 4], 'wood'), C('door_r', [8.1, 1.5, 3.6], [15.7, 6.5, 4], 'wood'),
          C('k1', [7, 3.5, 3.2], [7.6, 4.5, 3.6], 'knob'), C('k2', [8.4, 3.5, 3.2], [9, 4.5, 3.6], 'knob'), C('feet', [1, 0, 5], [15, 1, 14], 'dark')], 4)
    spec('tv', {'frame': dark, 'screen': {'c': '#0c0f13', 's': 'dark'}, 'stand': {'c': '#3a3d42', 's': 'metal'}, 'led': {'c': '#ff5a5a', 's': 'glow'}},
         [C('panel', [-4, 2, 9], [20, 15.5, 10], 'frame'), C('screen', [-3.5, 2.5, 8.85], [19.5, 15, 9.05], 'screen'),
          C('neck', [7, 1, 9.8], [9, 2.5, 10.8], 'stand'), C('base', [4, 0, 8], [12, 1, 12], 'stand'), C('led', [7.6, 2.1, 8.8], [8.4, 2.4, 9.0], 'led')], 8)
    spec('bookshelf', {'wood': wood, 'b1': {'c': '#c0392b', 's': 'paint'}, 'b2': {'c': '#2e86c1', 's': 'paint'}, 'b3': {'c': '#27ae60', 's': 'paint'},
                       'b4': {'c': '#f1c40f', 's': 'paint'}, 'b5': {'c': '#8e44ad', 's': 'paint'}, 'b6': {'c': '#ef7fbf', 's': 'paint'}},
         [C('back', [0, 0, 14], [16, 32, 16], 'wood'), C('side_l', [0, 0, 2], [1, 32, 14], 'wood'), C('side_r', [15, 0, 2], [16, 32, 14], 'wood'),
          C('top', [0, 31, 2], [16, 32, 14], 'wood')] +
         [C(f'shelf{i}', [1, y, 2], [15, y + 1, 14], 'wood') for i, y in enumerate((0, 8, 16, 24))] +
         [C(f'book{i}{j}', [1.3 + j * 2.25, y + 1, 4 + (j % 3) * 0.5], [3.1 + j * 2.25, y + 1 + 5 + (j * 7 + i) % 3, 13.5], f'b{(i * 3 + j) % 6 + 1}')
          for i, y in enumerate((0, 8, 16, 24)) for j in range(6)], 15)
    spec('floor_lamp', {'pole': gold, 'shade': {'c': '#f6ead2', 's': 'fabric'}, 'glow': {'c': '#ffe7a8', 's': 'glow'}, 'base': dark},
         [C('base', [5, 0, 5], [11, 1, 11], 'base'), C('pole', [7.5, 1, 7.5], [8.5, 22, 8.5], 'pole'), C('bulb', [6.8, 21, 6.8], [9.2, 23.5, 9.2], 'glow'),
          C('shade', [4.5, 21, 4.5], [11.5, 27, 11.5], 'shade', hide=['up', 'down'])], 13)
    spec('table_lamp', {'base': {'c': '#7fb8a0', 's': 'porcelain'}, 'shade': {'c': '#f6ead2', 's': 'fabric'}, 'glow': {'c': '#ffe7a8', 's': 'glow'}, 'pole': gold},
         [C('base', [6, 0, 6], [10, 4, 10], 'base'), C('pole', [7.6, 4, 7.6], [8.4, 7, 8.4], 'pole'), C('bulb', [7, 6.5, 7], [9, 8.5, 9], 'glow'),
          C('shade', [5, 6.5, 5], [11, 11, 11], 'shade', hide=['up', 'down'])], 5)
    spec('rug_pride', {'r': {'c': '#e40303', 's': 'fabric'}, 'o': {'c': '#ff8c00', 's': 'fabric'}, 'y': {'c': '#ffed00', 's': 'fabric'},
                       'g': {'c': '#008026', 's': 'fabric'}, 'b': {'c': '#004dff', 's': 'fabric'}, 'v': {'c': '#750787', 's': 'fabric'}},
         [C(f'stripe{i}', [0, 0, i * 16 / 6], [16, 0.5, (i + 1) * 16 / 6], m) for i, m in enumerate('roygbv')], 2)
    spec('fireplace', {'brick': {'c': '#9b4a3a', 's': 'granite', 'c2': '#6e3226', 'c3': '#c27a62'}, 'mantel': wood, 'fire': {'c': '#ff9a3c', 's': 'glow'},
                       'fire2': {'c': '#ffd36a', 's': 'glow'}, 'soot': dark, 'log': {'c': '#5a3b26', 's': 'wood'}},
         [C('left', [0, 0, 6], [3, 13, 16], 'brick'), C('right', [13, 0, 6], [16, 13, 16], 'brick'), C('top', [0, 10, 6], [16, 13, 16], 'brick'),
          C('back', [3, 0, 14], [13, 10, 16], 'soot'), C('hearth', [-1, 0, 3], [17, 1, 6], 'brick'), C('mantel', [-1, 13, 4], [17, 14.5, 16], 'mantel'),
          C('log1', [4, 1, 9], [12, 2.5, 10.5], 'log'), C('log2', [5, 1, 11], [11, 2.5, 12.5], 'log', r=['y', 22.5, [8, 1, 11]]),
          C('flame1', [5, 2.5, 9.5], [8, 7, 11], 'fire'), C('flame2', [8, 2.5, 10], [11, 6, 11.5], 'fire'), C('flame3', [6.5, 2.5, 10.5], [9.5, 8.5, 11.2], 'fire2')], 7)
    spec('plant_monstera', {'pot': {'c': '#e8e2d6', 's': 'porcelain'}, 'soil': {'c': '#3d2a1c', 's': 'paint'}, 'leaf': {'c': '#3f8f4a', 's': 'paint'},
                            'leaf2': {'c': '#57a861', 's': 'paint'}, 'stem': {'c': '#2f6b38', 's': 'paint'}},
         [C('pot', [4.5, 0, 4.5], [11.5, 6, 11.5], 'pot'), C('soil', [5, 5.6, 5], [11, 5.9, 11], 'soil'), C('stem', [7.6, 6, 7.6], [8.4, 12, 8.4], 'stem'),
          C('leaf1', [2, 12, 4], [8, 12.4, 10], 'leaf', r=['z', 22.5, [8, 12, 7]]), C('leaf2', [8, 13, 6], [14, 13.4, 12], 'leaf2', r=['z', -22.5, [8, 13, 9]]),
          C('leaf3', [5, 15, 2], [11, 15.4, 8], 'leaf', r=['x', -22.5, [8, 15, 8]]), C('leaf4', [5, 11, 9], [11, 11.4, 15], 'leaf2', r=['x', 22.5, [8, 11, 9]])], 7)
    spec('wall_clock', {'face': {'c': '#fbfbf8', 's': 'porcelain'}, 'rim': gold, 'hand': dark},
         [C('rim', [3, 3, 15], [13, 13, 16], 'rim'), C('face', [4, 4, 14.7], [12, 12, 15], 'face'), C('hour', [7.6, 7.6, 14.5], [8.4, 10.5, 14.7], 'hand'),
          C('minute', [7.6, 5, 14.5], [8.4, 8.4, 14.7], 'hand', r=['z', 45, [8, 8, 14.6]])], 8)
    spec('desk_globe', {'ocean': {'c': '#2f7fc1', 's': 'paint'}, 'land': {'c': '#4fae5a', 's': 'paint'}, 'stand': wood, 'arc': gold},
         [C('base', [5, 0, 5], [11, 1, 11], 'stand'), C('post', [7.6, 1, 7.6], [8.4, 3, 8.4], 'stand'),
          C('globe', [5, 3.5, 5], [11, 9.5, 11], 'ocean'), C('land1', [5.6, 6, 4.8], [8.5, 8.5, 5.2], 'land'), C('land2', [8, 4.5, 10.8], [10.5, 7, 11.2], 'land'),
          C('land3', [10.8, 5, 6], [11.2, 8, 9], 'land'), C('arc', [7.7, 3, 4.2], [8.3, 10.3, 4.6], 'arc', r=['x', 22.5, [8, 6.5, 8]])], 4)
    spec('computer_desk', {'top': oak, 'leg': dark, 'drawer': wood, 'knob': steel},
         [C('top', [-4, 12, 2], [20, 13, 15], 'top'), C('leg_l', [-3.5, 0, 3], [-2.5, 12, 14], 'leg'), C('drawers', [12, 0, 3], [19.5, 12, 14.5], 'drawer'),
          C('d1', [12.4, 7, 2.6], [19.1, 11.5, 3], 'drawer'), C('d2', [12.4, 1, 2.6], [19.1, 6.5, 3], 'drawer'),
          C('k1', [15.3, 9, 2.2], [16.3, 9.5, 2.6], 'knob'), C('k2', [15.3, 3.5, 2.2], [16.3, 4, 2.6], 'knob')], 8)
    spec('computer', {'case': {'c': '#1c1d21', 's': 'dark'}, 'rgb': {'c': '#ef7fbf', 's': 'glow'}, 'rgb2': {'c': '#7fd6ff', 's': 'glow'},
                      'screen': {'c': '#0c0f13', 's': 'dark'}, 'frame': {'c': '#2a2c30', 's': 'dark'}, 'keys': {'c': '#3a3d42', 's': 'paint'}},
         [C('monitor', [1, 4, 10], [15, 13, 11], 'frame'), C('screen', [1.5, 4.5, 9.85], [14.5, 12.5, 10.05], 'screen'), C('neck', [7.3, 1, 10.6], [8.7, 4, 11.6], 'frame'),
          C('foot', [5, 0, 9], [11, 0.6, 13], 'frame'), C('keyboard', [2.5, 0, 3], [12.5, 0.6, 7], 'keys'), C('mouse', [13.5, 0, 4], [15, 0.8, 6], 'keys'),
          C('rgb_strip', [1.5, 3.8, 10.2], [14.5, 4.1, 10.6], 'rgb'), C('mouse_led', [13.9, 0.8, 4.4], [14.6, 0.9, 5], 'rgb2')], 6)
    spec('office_chair', {'seat': {'c': '#2a2c30', 's': 'fabric'}, 'accent': {'c': '#ef7fbf', 's': 'paint'}, 'metal': steel},
         [C('seat', [3, 8, 3], [13, 10, 13], 'seat'), C('back', [3.5, 10, 11.5], [12.5, 22, 13.5], 'seat'), C('stripe', [7, 12, 11.3], [9, 21, 11.6], 'accent'),
          C('post', [7.3, 2, 7.3], [8.7, 8, 8.7], 'metal'), C('star1', [2.5, 1, 7.3], [13.5, 2, 8.7], 'metal'), C('star2', [7.3, 1, 2.5], [8.7, 2, 13.5], 'metal'),
          C('w1', [2, 0, 7], [3.5, 1, 9], 'seat'), C('w2', [12.5, 0, 7], [14, 1, 9], 'seat'), C('w3', [7, 0, 2], [9, 1, 3.5], 'seat'), C('w4', [7, 0, 12.5], [9, 1, 14], 'seat')], 10)
    spec('fish_tank', {'glass': {'c': '#cfeaf2', 's': 'glass', 'a': 0.3}, 'water': {'c': '#3f9ad8', 's': 'glass', 'a': 0.45}, 'frame': dark,
                       'sand': {'c': '#e2cf97', 's': 'granite', 'c2': '#c9b37a', 'c3': '#f4e6b8'}, 'weed': {'c': '#3fa04f', 's': 'paint'},
                       'fish1': {'c': '#ff8c00', 's': 'paint'}, 'fish2': {'c': '#ef7fbf', 's': 'paint'}, 'fish3': {'c': '#ffed00', 's': 'paint'}, 'light': {'c': '#bfe8ff', 's': 'glow'}},
         [C('base', [0, 0, 2], [16, 1, 14], 'frame'), C('lid', [0, 13, 2], [16, 14, 14], 'frame'), C('light', [2, 12.9, 5], [14, 13, 11], 'light'),
          C('front', [0.5, 1, 2.2], [15.5, 13, 2.4], 'glass'), C('back', [0.5, 1, 13.6], [15.5, 13, 13.8], 'glass'),
          C('left', [0.2, 1, 2.2], [0.4, 13, 13.8], 'glass'), C('right', [15.6, 1, 2.2], [15.8, 13, 13.8], 'glass'),
          C('sand', [0.5, 1, 2.5], [15.5, 2.2, 13.5], 'sand'), C('water', [0.6, 2.2, 2.6], [15.4, 11.5, 13.4], 'water'),
          C('weed1', [3, 2.2, 10], [4, 9, 11], 'weed'), C('weed2', [12, 2.2, 9], [13, 7.5, 10], 'weed'), C('rock', [9, 2.2, 10], [12, 4, 12.5], 'sand'),
          C('fish1', [4, 6, 6], [7, 7.6, 6.8], 'fish1'), C('fish2', [9, 8.5, 7.5], [11.5, 9.8, 8.2], 'fish2'), C('fish3', [6, 4, 9], [8, 5.2, 9.6], 'fish3')], 7)


kitchen()
bathroom()
foods()
workshop()
stairs()
living()
os.makedirs(OUT, exist_ok=True)
for n, s in SPECS.items():
    if n == 'fridge_smart':
        continue
    json.dump(s, open(os.path.join(OUT, n + '.json'), 'w'), separators=(',', ':'))
print(len(SPECS), 'specs')
