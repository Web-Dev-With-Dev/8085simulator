import sys, re
sys.stdout.reconfigure(encoding='utf-8')

with open(r'D:\Aura Studio\8085simulator\src\Help.java', 'r', encoding='utf-8') as f:
    content = f.read()

for idx in range(1, 6):
    marker = f'jTextArea{idx}.setText('
    start = content.find(marker)
    if start == -1:
        continue
    # find opening quote
    qstart = content.find('"', start)
    # walk to find matching closing );
    depth = 0
    pos = qstart
    in_str = False
    while pos < len(content):
        c = content[pos]
        if c == '"' and (pos == 0 or content[pos-1] != '\\'):
            in_str = not in_str
        if not in_str:
            if c == '(':
                depth += 1
            elif c == ')':
                depth -= 1
                if depth == 0:
                    break
        pos += 1
    raw = content[start:pos+1]
    # Extract string value - just print first 600 chars
    print(f'\n=== AREA {idx} ===')
    print(raw[:600])
