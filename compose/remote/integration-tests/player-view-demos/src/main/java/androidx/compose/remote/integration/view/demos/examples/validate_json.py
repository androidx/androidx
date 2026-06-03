import sys, re, json
content = open('RcJsonTicker.kt').read()
match = re.search(r'val json = """(.*?)"""', content, re.DOTALL)
if not match:
    print('JSON string not found')
    sys.exit(1)
js_str = match.group(1)
# Simulate Kotlin interpolations
js_str = js_str.replace("${'$'}", "$")
js_str = js_str.replace("${stockDataJson}", "[0.0]")
js_str = js_str.replace("${refreshStr}", "M0,0")
js_str = re.sub(r'\${Rc\.TextFromFloat[^}]*}', '0', js_str)

# Clean up indent if needed (simplified trimIndent)
lines = js_str.split('\n')
if lines and not lines[0].strip(): lines = lines[1:]
if lines:
    indent = len(lines[0]) - len(lines[0].lstrip())
    lines = [line[indent:] if len(line) >= indent else line for line in lines]
js_str = '\n'.join(lines)

try:
    json.loads(js_str)
    print('JSON is valid')
except json.JSONDecodeError as e:
    print(f'Error: {e}')
    lines = js_str.split('\n')
    # Print context
    start = max(0, e.lineno - 10)
    end = min(len(lines), e.lineno + 10)
    for i in range(start, end):
        prefix = '>>>' if i == e.lineno - 1 else '   '
        print(f'{prefix}{i+1:4}: {lines[i]}')
