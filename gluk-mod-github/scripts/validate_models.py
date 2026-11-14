import json, glob, sys
bad = 0
for f in glob.glob('src/main/resources/assets/*/models/**/*.json', recursive=True):
    m = json.load(open(f, encoding='utf-8'))
    for i, e in enumerate(m.get('elements', [])):
        for k in ('from', 'to'):
            for v in e[k]:
                if v < -16 or v > 32:
                    print('ERROR %s: element %d, %s = %s is outside -16..32' % (f, i, k, v)); bad += 1
        for fn, fc in e.get('faces', {}).items():
            if 'uv' in fc and any(x < 0 or x > 16 for x in fc['uv']):
                print('ERROR %s: element %d, face %s, uv %s is outside 0..16' % (f, i, fn, fc['uv'])); bad += 1
print('Models with errors:', bad)
sys.exit(1 if bad else 0)
