from pathlib import Path

p = Path("app/src/main/java/com/chargeanim/pro/ui/main/MainActivity.kt")
if not p.exists():
    raise SystemExit("MainActivity.kt not found")

s = p.read_text()
if "import com.chargeanim.pro.ui.theme.ChargeFlowTheme" not in s:
    marker = "package com.chargeanim.pro.ui.main\n"
    if marker in s:
        s = s.replace(marker, marker + "\nimport com.chargeanim.pro.ui.theme.ChargeFlowTheme\n", 1)

# Wrap the existing Activity Compose root without changing its service, receiver,
# telemetry, renderer, or charging lifecycle code.
needle = "setContent {"
pos = s.find(needle)
if pos < 0:
    raise SystemExit("setContent { not found")
after = pos + len(needle)
if s[after:after + 40].lstrip().startswith("ChargeFlowTheme {"):
    p.write_text(s)
    raise SystemExit(0)

# Find the matching closing brace of setContent's lambda.
depth = 1
i = after
in_string = False
escape = False
while i < len(s) and depth:
    ch = s[i]
    if in_string:
        if escape:
            escape = False
        elif ch == "\\":
            escape = True
        elif ch == '"':
            in_string = False
    else:
        if ch == '"':
            in_string = True
        elif ch == '{':
            depth += 1
        elif ch == '}':
            depth -= 1
    i += 1

if depth != 0:
    raise SystemExit("Could not match setContent braces")

body = s[after:i-1]
s = s[:after] + "\n            ChargeFlowTheme {" + body + "\n            }\n        " + s[i-1:]
p.write_text(s)
