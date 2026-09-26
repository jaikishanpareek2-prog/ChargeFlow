#!/usr/bin/env python3
from pathlib import Path

p = Path("app/src/main/java/com/chargeanim/pro/data/PreferencesRepository.kt")
s = p.read_text()

# Keep AnimationMode as a compatibility enum for the lifecycle patch, but
# persist the existing autoHide preference. Do not introduce a second
# animation-mode DataStore key.
if "enum class AnimationMode" not in s:
    s = s.replace(
        "enum class MediaType",
        "enum class AnimationMode { TEMPORARY, ALWAYS_ON }\n\nenum class MediaType",
        1,
    )

if "val animationMode:" not in s:
    marker = "    val enabled: Flow<Boolean>"
    pos = s.find(marker)
    if pos >= 0:
        line_end = s.find("\n", pos)
        insert = (
            "\n    val animationMode: Flow<AnimationMode> = context.dataStore.data.map { prefs ->\n"
            "        if (prefs[Keys.AUTO_HIDE] == true) AnimationMode.TEMPORARY else AnimationMode.ALWAYS_ON\n"
            "    }\n"
        )
        s = s[:line_end + 1] + insert + s[line_end + 1:]

if "suspend fun setAnimationMode" not in s:
    marker = "    suspend fun setEnabled"
    pos = s.find(marker)
    if pos >= 0:
        brace = s.find("\n    }", pos)
        insert_at = brace + len("\n    }")
        method = (
            "\n\n    suspend fun setAnimationMode(mode: AnimationMode) {\n"
            "        setAutoHide(mode == AnimationMode.TEMPORARY)\n"
            "    }"
        )
        s = s[:insert_at] + method + s[insert_at:]

p.write_text(s)
print("AnimationMode compatibility patched to existing autoHide preference")
