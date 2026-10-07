#!/usr/bin/env python3
"""The one table of Pride Furniture blocks. Writes blockstates, item models, lang, recipes and Defs.java.
Run after tools/unbundle.py (it reads the block models to work out each block's hitbox)."""
import json, os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
A = ROOT + '/src/main/resources/assets/pridefurniture'
JAVA = ROOT + '/src/main/java/com/dogpound/furniture/Defs.java'

FINISHES = ['white', 'oak', 'walnut', 'sage', 'navy']
COUNTERS = ['granite', 'marble', 'butcher', 'quartz', 'black', 'concrete']
FIN_NAME = {'white': 'White Shaker', 'oak': 'Oak', 'walnut': 'Walnut', 'sage': 'Sage Green', 'navy': 'Navy'}

# (id, display name, kind, flags, inventory size, tab)
# flags: c = takes a countertop, t = 2 tall, l = 2 long, g = see-through glass (translucent), s = sink counter variant, n = no collision
B = []
for f in FINISHES:
    n = FIN_NAME[f]
    B += [(f'cabinet_{f}', f'{n} Base Cabinet', 'STORAGE', 'c', 18),
          (f'drawers_{f}', f'{n} Drawer Cabinet', 'STORAGE', 'c', 27),
          (f'sink_cabinet_{f}', f'{n} Sink Cabinet', 'SINK', 'cs', 9),
          (f'wall_cabinet_{f}', f'{n} Wall Cabinet', 'STORAGE', '', 18),
          (f'glass_cabinet_{f}', f'{n} Glass Wall Cabinet', 'STORAGE', 'g', 18),
          (f'pantry_{f}', f'{n} Pantry', 'STORAGE', 't', 54),
          (f'vanity_{f}', f'{n} Bathroom Vanity', 'VANITY', '', 9)]
B += [('fridge_smart', 'Smart Fridge (Family Screen)', 'FRIDGE', 't', 54),
      ('fridge_side_by_side', 'Side-by-Side Fridge', 'FRIDGE', 't', 54),
      ('fridge_bottom_freezer', 'Bottom-Freezer Fridge', 'FRIDGE', 't', 45),
      ('fridge_basic', 'Basic Fridge', 'FRIDGE', 't', 27),
      ('fridge_retro', 'Retro 50s Fridge', 'FRIDGE', 't', 36),
      ('fridge_commercial', 'Glass-Door Drinks Fridge', 'FRIDGE', 'tg', 45),
      ('mini_fridge', 'Mini Fridge', 'FRIDGE', '', 9),
      ('wine_fridge', 'Wine Fridge', 'FRIDGE', 'cg', 18),
      ('dishwasher', 'Dishwasher', 'DISHWASHER', 'c', 9),
      ('trash_compactor', 'Trash Compactor', 'COMPACTOR', 'c', 9),
      ('stove', 'Stove & Oven', 'STOVE', '', 8),
      ('range_hood', 'Range Hood', 'HOOD', 'n', 0),
      ('microwave', 'Microwave', 'MICROWAVE', '', 2),
      ('toaster', 'Toaster', 'TOASTER', '', 2),
      ('coffee_maker', 'Coffee Maker', 'COFFEE', 'g', 3),
      ('blender', 'Blender', 'BLENDER', 'g', 2),
      ('kettle', 'Electric Kettle', 'KETTLE', '', 2),
      ('knife_block', 'Knife Block', 'DECOR', '', 0),
      ('fruit_bowl', 'Fruit Bowl', 'FRUIT', '', 0),
      ('paper_towel', 'Paper Towel Holder', 'DECOR', '', 0),
      ('spice_rack', 'Spice Rack', 'DECOR', 'g', 0),
      ('trash_can', 'Trash Can', 'TRASH', '', 0),
      ('bar_stool', 'Bar Stool', 'SEAT', '', 0),
      ('toilet', 'Toilet', 'TOILET', '', 0),
      ('bathtub', 'Bathtub', 'TUB', 'lg', 0),
      ('shower', 'Shower', 'SHOWER', 'tg', 0),
      ('mirror', 'Bathroom Mirror', 'MIRROR', 'n', 0),
      ('medicine_cabinet', 'Medicine Cabinet', 'STORAGE', '', 9),
      ('towel_rack', 'Towel Rack', 'TOWEL', 'n', 0),
      ('toilet_paper', 'Toilet Paper Holder', 'DECOR', 'n', 0),
      ('bath_mat', 'Bath Mat', 'DECOR', 'n', 0),
      ('hamper', 'Laundry Hamper', 'STORAGE', '', 18),
      ('washing_machine', 'Washing Machine', 'WASHER', 'g', 2),
      ('dryer', 'Dryer', 'DRYER', 'g', 2),
      ('bath_scale', 'Bathroom Scale', 'SCALE', 'n', 0),
      ('furniture_workbench', 'Furniture Workbench', 'WORKBENCH', '', 0)]
COLS = (('pink', 'Pink'), ('gray', 'Gray'), ('navy', 'Navy'), ('sage', 'Sage'))
B += [(f'sofa_{c}', f'{n} Sofa', 'SEAT', '', 0) for c, n in COLS] + [(f'armchair_{c}', f'{n} Armchair', 'SEAT', '', 0) for c, n in COLS]
B += [('coffee_table', 'Glass Coffee Table', 'DECOR', 'g', 0), ('side_table', 'Side Table', 'STORAGE', '', 9),
      ('tv_stand', 'TV Stand', 'STORAGE', '', 18), ('tv', 'Flat-Screen TV', 'SCREEN', 'n', 0), ('bookshelf', 'Bookshelf', 'BOOKS', 't', 27),
      ('floor_lamp', 'Floor Lamp', 'LAMP', 't', 0), ('table_lamp', 'Table Lamp', 'LAMP', '', 0), ('rug_pride', 'Pride Rug', 'DECOR', 'n', 0),
      ('fireplace', 'Fireplace', 'FIREPLACE', '', 0), ('plant_monstera', 'Monstera Plant', 'DECOR', '', 0), ('wall_clock', 'Wall Clock', 'CLOCK', 'n', 0),
      ('desk_globe', 'Desk Globe', 'GLOBE', '', 0), ('computer_desk', 'Computer Desk', 'STORAGE', '', 18), ('computer', 'Gaming PC', 'SCREEN', '', 0),
      ('office_chair', 'Office Chair', 'SEAT', '', 0), ('fish_tank', 'Fish Tank', 'TANK', 'g', 0)]
B += [(f'spiral_stairs_{k}', f'{n} Spiral Staircase', 'STAIRS', '', 0) for k, n in
      (('oak', 'Oak'), ('walnut', 'Walnut'), ('iron', 'Wrought Iron'), ('white', 'White & Gold'))]

ITEMS = [(f'countertop_{c}', f'{c.title()} Countertop'.replace('Butcher', 'Butcher Block')) for c in COUNTERS]
FOODS = [('toast', 'Toast'), ('coffee', 'Mug of Coffee'), ('smoothie', 'Berry Smoothie'), ('tea', 'Cup of Tea'), ('trash_cube', 'Trash Cube')]

DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
           "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
           "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
           "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}


def disp(tall, long):
    d = json.loads(json.dumps(DISPLAY))
    if tall:
        d['gui'] = {"rotation": [30, 225, 0], "translation": [0, -4, 0], "scale": [0.36, 0.36, 0.36]}
        for k in ('thirdperson_righthand', 'firstperson_righthand', 'firstperson_lefthand'): d[k]['scale'] = [0.22] * 3
    if long:
        d['gui'] = {"rotation": [30, 225, 0], "translation": [2, 0, 2], "scale": [0.36, 0.36, 0.36]}
    return d


def bounds(name):
    m = json.load(open(f'{A}/models/block/{name}.json'))
    lo = [99, 99, 99]; hi = [-99, -99, -99]
    for e in m['elements']:
        for i in range(3):
            lo[i] = min(lo[i], e['from'][i], e['to'][i]); hi[i] = max(hi[i], e['from'][i], e['to'][i])
    return lo, hi


def w(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    json.dump(obj, open(path, 'w'), indent=1)


lang = ['itemGroup.pridefurniture=Pride Furniture', 'tile.pridefurniture.filler.name=Furniture']
rot = {'north': 0, 'east': 90, 'south': 180, 'west': 270}
java_rows = []
for bid, name, kind, flags, inv in B:
    tall, long, counter = 't' in flags, 'l' in flags, 'c' in flags
    lo, hi = bounds(bid)
    box = [max(0, lo[0]), max(0, lo[1]), max(0, lo[2]), min(16, hi[0]), min(16, hi[1]), min(16, hi[2])]
    if box[4] - box[1] < 1: box[4] = box[1] + 1
    # blockstate: multipart so countertops / water / "on" overlays stack on the base model
    parts = []
    for fac, y in rot.items():
        parts.append({"when": {"facing": fac}, "apply": {"model": f"pridefurniture:{bid}", "y": y}})
    if counter:
        for c in COUNTERS:
            cm = f'counter_{c}_sink' if 's' in flags else f'counter_{c}'
            for fac, y in rot.items():
                parts.append({"when": {"facing": fac, "counter": c}, "apply": {"model": f"pridefurniture:{cm}", "y": y}})
    if kind == 'TUB':
        for fac, y in rot.items():
            parts.append({"when": {"facing": fac, "on": "true"}, "apply": {"model": "pridefurniture:bathtub_water", "y": y}})
    w(f'{A}/blockstates/{bid}.json', {"multipart": parts})
    w(f'{A}/models/item/{bid}.json', {"parent": f"pridefurniture:block/{bid}", "display": disp(tall, long)})
    lang.append(f'tile.pridefurniture.{bid}.name={name}')
    java_rows.append(f'        d("{bid}", Kind.{kind}, "{flags}", {inv}, {", ".join(str(round(v / 16, 4)) + "D" for v in box)});')

w(f'{A}/blockstates/filler.json', {"variants": {"normal": {"model": "pridefurniture:empty"}}})
w(f'{A}/models/block/empty.json', {"textures": {"particle": "minecraft:blocks/iron_block"}, "elements": []})
for iid, name in ITEMS:
    c = iid.split('_', 1)[1]
    w(f'{A}/models/item/{iid}.json', {"parent": f"pridefurniture:block/counter_{c}", "display": {**DISPLAY, "gui": {"rotation": [30, 225, 0], "translation": [0, -5, 0], "scale": [0.625] * 3}}})
    lang.append(f'item.pridefurniture.{iid}.name={name}')
for iid, name in FOODS:
    w(f'{A}/models/item/{iid}.json', {"parent": f"pridefurniture:block/{iid}", "display": DISPLAY})
    lang.append(f'item.pridefurniture.{iid}.name={name}')
open(f'{A}/lang/en_us.lang', 'w').write('\n'.join(lang) + '\n')

java = '''package com.dogpound.furniture;

import java.util.ArrayList;
import java.util.List;

/** GENERATED by tools/gen_assets.py — every furniture block: id, what it does, flags, slots, hitbox (0..1). */
public final class Defs {
    private Defs() {}

    public enum Kind { STORAGE, FRIDGE, SINK, VANITY, DISHWASHER, COMPACTOR, STOVE, HOOD, MICROWAVE, TOASTER, COFFEE, BLENDER, KETTLE,
        DECOR, FRUIT, TRASH, SEAT, TOILET, TUB, SHOWER, MIRROR, TOWEL, WASHER, DRYER, SCALE, WORKBENCH, STAIRS, SCREEN, BOOKS, LAMP, FIREPLACE, CLOCK, GLOBE, TANK }

    public static final class Def {
        public final String id, flags; public final Kind kind; public final int slots; public final double[] box;
        Def(String id, Kind kind, String flags, int slots, double... box) { this.id = id; this.kind = kind; this.flags = flags; this.slots = slots; this.box = box; }
        public boolean has(char f) { return flags.indexOf(f) >= 0; }
    }

    public static final List<Def> ALL = new ArrayList<>();
    private static void d(String id, Kind kind, String flags, int slots, double... box) { ALL.add(new Def(id, kind, flags, slots, box)); }

    public static final String[] COUNTERS = {%s};

    static {
%s
    }
}
''' % (', '.join(f'"{c}"' for c in COUNTERS), '\n'.join(java_rows))
os.makedirs(os.path.dirname(JAVA), exist_ok=True)
open(JAVA, 'w').write(java)
print(len(B), 'blocks', len(ITEMS) + len(FOODS), 'items')

# ------------------------------------------------------------------ recipes
R = A + '/recipes'
os.makedirs(R, exist_ok=True)
for old in os.listdir(R): os.remove(os.path.join(R, old))
I = lambda item, data=None: dict({'item': item}, **({'data': data} if data is not None else {}))
P = {'type': 'forge:ore_dict', 'ore': 'plankWood'}
COMMON = {'I': I('minecraft:iron_ingot'), 'C': I('minecraft:chest'), 'G': I('minecraft:glass_pane'), 'R': I('minecraft:redstone'),
          'S': I('minecraft:snow'), 'Q': I('minecraft:quartz_block'), 'B': I('minecraft:bucket'), 'F': I('minecraft:furnace'),
          'K': I('minecraft:cauldron'), 'W': I('minecraft:wool'), 'N': I('minecraft:gold_nugget'), 'T': I('minecraft:stick'),
          'O': I('minecraft:glass_bottle'), 'X': I('minecraft:piston'), 'L': I('minecraft:glowstone_dust'), 'P': P, 'D': None}
FIN_DYE = {'white': I('minecraft:dye', 15), 'oak': I('minecraft:log', 0), 'walnut': I('minecraft:dye', 3), 'sage': I('minecraft:dye', 2), 'navy': I('minecraft:dye', 4)}

CATALOG = []

def category(out):
    if out.startswith('countertop_'): return 'Countertops'
    if 'fridge' in out: return 'Fridges'
    if 'stairs' in out: return 'Stairs'
    if any(k in out for k in ('sofa', 'armchair', 'coffee_table', 'side_table', 'tv', 'bookshelf', 'lamp', 'rug', 'fireplace', 'plant', 'clock')): return 'Living Room'
    if any(k in out for k in ('globe', 'computer', 'office', 'fish_tank')): return 'Office & Hobby'
    if any(k in out for k in ('cabinet', 'drawers', 'pantry')) and 'medicine' not in out: return 'Cabinets'
    if any(k in out for k in ('vanity', 'toilet', 'bath', 'shower', 'mirror', 'medicine', 'towel', 'hamper', 'washing', 'dryer')): return 'Bathroom'
    if any(k in out for k in ('stove', 'dishwasher', 'compactor', 'hood', 'microwave', 'toaster', 'coffee', 'blender', 'kettle', 'trash_can')): return 'Appliances'
    return 'Kitchen Decor'

def add(out, ings, count):
    merged = []
    for ing in ings:
        for m in merged:
            if {k: v for k, v in m.items() if k != 'count'} == ing: m['count'] += 1; break
        else: merged.append(dict(ing, count=1))
    CATALOG.append({'id': out, 'count': count, 'category': category(out), 'ingredients': merged})

def shaped(out, pattern, count=1, extra=None):
    add(out, [(extra or {}).get(ch, COMMON.get(ch)) for row in pattern for ch in row if ch != ' '], count)

def shapeless(out, items, count=1):
    add(out, items, count)

for f in FINISHES:
    e = {'D': FIN_DYE[f]}
    shaped(f'cabinet_{f}', ['PPP', 'PDP', 'PCP'], 1, e)
    shaped(f'drawers_{f}', ['PCP', 'PDP', 'PCP'], 1, e)
    shaped(f'sink_cabinet_{f}', ['PKP', 'PDP', 'PCP'], 1, e)
    shaped(f'wall_cabinet_{f}', ['PCP', 'PDP'], 1, e)
    shaped(f'glass_cabinet_{f}', ['PGP', 'PDP'], 1, e)
    shaped(f'pantry_{f}', ['PCP', 'PCP', 'PDP'], 1, e)
    shaped(f'vanity_{f}', ['QKQ', 'PDP', 'PCP'], 1, e)
shaped('fridge_basic', ['III', 'ISI', 'ICI'])
shaped('fridge_bottom_freezer', ['ICI', 'ISI', 'III'])
shaped('fridge_side_by_side', ['ISI', 'ICI', 'ISI'])
shaped('fridge_smart', ['IGI', 'ISI', 'IRI'])
shaped('fridge_retro', ['IDI', 'ISI', 'ICI'], 1, {'D': I('minecraft:dye', 6)})
shaped('fridge_commercial', ['IGI', 'GSG', 'ICI'])
shaped('mini_fridge', ['II', 'SC'])
shaped('wine_fridge', ['IGI', 'ISI', 'IPI'])
shaped('dishwasher', ['III', 'IBI', 'IRI'])
shaped('trash_compactor', ['III', 'IXI', 'IRI'])
shaped('stove', ['III', 'IFI', 'IRI'])
shaped('range_hood', ['III', 'ILI'])
shaped('microwave', ['III', 'GFR'])
shaped('toaster', ['IRI', 'IFI'])
shaped('coffee_maker', ['IDI', 'IOI'], 1, {'D': I('minecraft:dye', 3)})
shaped('blender', ['GGG', 'GIG', 'IRI'])
shaped('kettle', ['IOI', 'IRI'])
shapeless('knife_block', [P, I('minecraft:iron_sword')])
shapeless('fruit_bowl', [I('minecraft:bowl'), I('minecraft:apple'), I('minecraft:apple')])
shapeless('paper_towel', [I('minecraft:paper'), I('minecraft:paper'), I('minecraft:stick')])
shapeless('spice_rack', [P, I('minecraft:glass_bottle'), I('minecraft:glass_bottle')])
shaped('trash_can', ['I I', 'I I', 'III'])
shaped('bar_stool', ['WWW', ' I ', ' I '])
shaped('toilet', ['QQ ', 'QBQ', ' Q '])
shaped('bathtub', ['QBQ', 'QQQ'])
shaped('shower', ['GIG', 'GBG', 'GQG'])
shaped('mirror', ['NNN', 'NGN', 'NNN'])
shaped('medicine_cabinet', ['QGQ', 'QCQ'])
shaped('towel_rack', ['III', 'WWW'])
shapeless('toilet_paper', [I('minecraft:paper'), I('minecraft:paper'), I('minecraft:paper'), I('minecraft:iron_nugget')])
shapeless('bath_mat', [I('minecraft:carpet'), I('minecraft:carpet')])
shaped('hamper', ['T T', 'TCT', 'TTT'])
shaped('washing_machine', ['III', 'GBG', 'IRI'])
shaped('dryer', ['III', 'GFG', 'IRI'])
shaped('bath_scale', ['GRG', 'III'])
shaped('spiral_stairs_oak', ['P  ', 'PT ', 'PPP'], 2)
shaped('spiral_stairs_walnut', ['P  ', 'PT ', 'PPD'], 2, {'D': I('minecraft:dye', 3)})
shaped('spiral_stairs_iron', ['I  ', 'IT ', 'III'], 2)
shaped('spiral_stairs_white', ['Q  ', 'QN ', 'QQQ'], 2)
for c, d in (('pink', 9), ('gray', 7), ('navy', 11), ('sage', 13)):
    shaped(f'sofa_{c}', ['W  ', 'WWW', 'P P'], 1, {'W': I('minecraft:wool', d)})
    shaped(f'armchair_{c}', ['W W', 'WWW', 'P P'], 1, {'W': I('minecraft:wool', d)})
shaped('coffee_table', ['GGG', 'P P'])
shaped('side_table', ['PPP', 'PCP', 'P P'])
shaped('tv_stand', ['PPP', 'PCP'])
shaped('tv', ['GGG', 'GRG', ' I '])
shaped('bookshelf', ['PPP', 'BBB', 'PPP'], 1, {'B': I('minecraft:book')})
shaped('floor_lamp', [' L ', ' I ', ' I '], 1, {'L': I('minecraft:glowstone')})
shaped('table_lamp', ['WLW', ' I '], 1, {'L': I('minecraft:glowstone_dust')})
shapeless('rug_pride', [I('minecraft:carpet', 14), I('minecraft:carpet', 1), I('minecraft:carpet', 4), I('minecraft:carpet', 13), I('minecraft:carpet', 11), I('minecraft:carpet', 10)], 2)
shaped('fireplace', ['BBB', 'B B', 'BNB'], 1, {'B': I('minecraft:brick_block'), 'N': I('minecraft:netherrack')})
shapeless('plant_monstera', [I('minecraft:flower_pot'), I('minecraft:leaves'), I('minecraft:vine')])
shaped('wall_clock', [' N ', 'NRN', ' N '], 1, {'N': I('minecraft:gold_ingot')})
shaped('desk_globe', [' D ', 'DND', ' P '], 1, {'D': I('minecraft:dye', 4), 'N': I('minecraft:compass')})
shaped('computer_desk', ['PPP', 'I C'])
shaped('computer', ['GGG', 'GRG', 'IDI'], 1, {'D': I('minecraft:diamond')})
shaped('office_chair', ['WW ', 'WWW', 'III'], 1, {'W': I('minecraft:wool', 15)})
shaped('fish_tank', ['GGG', 'GFG', 'III'], 1, {'F': I('minecraft:fish')})
CT = {'granite': I('minecraft:stone', 1), 'marble': I('minecraft:stone', 3), 'butcher': P, 'quartz': I('minecraft:quartz_block'),
      'black': I('minecraft:obsidian'), 'concrete': I('minecraft:concrete', 8)}
for c, m in CT.items():
    shaped(f'countertop_{c}', ['MMM', ' N '], 3, {'M': m, 'N': I('minecraft:iron_nugget')})
w(f'{A}/workbench.json', CATALOG)
w(f'{R}/furniture_workbench.json', {'type': 'minecraft:crafting_shaped', 'pattern': ['PPP', 'TCT', 'IXI'],
    'key': {'P': P, 'T': I('minecraft:stick'), 'C': I('minecraft:crafting_table'), 'I': I('minecraft:iron_ingot'), 'X': I('minecraft:iron_axe')},
    'result': {'item': 'pridefurniture:furniture_workbench'}})
print(len(CATALOG), 'workbench recipes')

tips = {'storage': 'Storage.', 'fridge': 'Keeps your food. The freezer turns water buckets into ice.',
        'sink': 'Right-click with a bucket or bottle for water, or empty-handed to wash. Sneak to open the cupboard.',
        'vanity': 'A sink with a cupboard: water for buckets and bottles, washes dye out of leather.',
        'dishwasher': 'Fills empty bottles with water and washes dye out of leather armor.',
        'compactor': 'Crushes whatever you put in. Every 64 items becomes a Trash Cube.',
        'stove': 'Cooks any raw food, 4 at a time. No fuel needed.', 'hood': 'Right-click to turn the light on and off.',
        'microwave': 'Heats food fast. Ding!', 'toaster': 'Bread → Toast.', 'coffee': 'Cocoa beans + a water bottle → coffee (Speed + Haste).',
        'blender': 'Fruit or veggies → smoothie.', 'kettle': 'Water bottle → tea.', 'fruit': 'Right-click for a piece of fruit every 5 minutes.',
        'trash': 'Right-click with anything in your hand to throw it away.', 'seat': 'Right-click to sit.',
        'toilet': 'Sit, then right-click again to go. Sneak to stand up and flush.',
        'tub': 'Right-click to fill, again to get in. Heals and cleans you.', 'shower': 'Right-click to turn on, then stand in it to get clean.',
        'mirror': 'How do you look?', 'towel': 'Dry off after a shower.', 'washer': 'Washes dye out of leather, wool and carpet.',
        'dryer': 'Dries wet sponges.', 'screen': 'Right-click: power. Sneak + right-click: next GIF. Add your own GIFs in config/pridefurniture/screens.',
        'books': 'Holds books. Powers enchanting tables like a bookshelf.', 'lamp': 'Right-click to switch it on and off.',
        'fireplace': 'Cozy light and warmth. Stand near it to get warm.', 'clock': 'Right-click to check the time.',
        'globe': 'Give it a spin.', 'tank': 'Fish! They swim around in there.', 'workbench': 'Where all Pride furniture is made.', 'stairs': 'Walk round and up. Stack them: each one turns to continue the spiral.', 'scale': 'Step on it… well, right-click it.'}
with open(f'{A}/lang/en_us.lang', 'a') as fh:
    for k, v in tips.items(): fh.write(f'tip.pridefurniture.{k}={v}\n')
