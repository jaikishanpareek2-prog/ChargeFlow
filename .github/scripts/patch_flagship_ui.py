from pathlib import Path

def wrap_set_content(path: Path):
    s = path.read_text()
    if "ChargeFlowTheme" in s:
        return False
    pos = s.find("setContent {")
    if pos < 0:
        return False
    open_brace = s.find("{", pos)
    depth = 0
    end = None
    for i in range(open_brace, len(s)):
        ch = s[i]
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                end = i
                break
    if end is None:
        return False
    s = s[:open_brace + 1] + "\n        ChargeFlowTheme {" + s[open_brace + 1:end] + "\n        }" + s[end:]
    if "import com.chargeanim.pro.ui.theme.ChargeFlowTheme" not in s:
        lines=s.splitlines()
        idx=next((i for i,x in enumerate(lines) if x.startswith("import ")),len(lines))
        lines.insert(idx,"import com.chargeanim.pro.ui.theme.ChargeFlowTheme")
        s="\n".join(lines)+"\n"
    path.write_text(s)
    return True

matches=[]
for p in Path("app/src/main/java").rglob("MainActivity.kt"):
    if wrap_set_content(p):
        matches.append(str(p))
if not matches:
    raise SystemExit("No MainActivity.kt setContent block found or already themed")
print("ChargeFlowTheme applied:", ", ".join(matches))
