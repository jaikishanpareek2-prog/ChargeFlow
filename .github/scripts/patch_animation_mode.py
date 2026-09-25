#!/usr/bin/env python3
from pathlib import Path
p = Path("app/src/main/java/com/chargeanim/pro/data/PreferencesRepository.kt")
s = p.read_text()
if "enum class AnimationMode" not in s:
    s = s.replace(
        "enum class MediaType",
        "enum class AnimationMode { TEMPORARY, ALWAYS_ON }\n\nenum class MediaType",
        1,
    )
if 'booleanPreferencesKey("enabled")' in s and 'stringPreferencesKey("animation_mode")' not in s:
    s = s.replace(
        'val ENABLED = booleanPreferencesKey("enabled")',
        'val ENABLED = booleanPreferencesKey("enabled")\n        val ANIMATION_MODE = stringPreferencesKey("animation_mode")',
        1,
    )
if "val animationMode:" not in s:
    marker = "    val enabled: Flow<Boolean>"
    pos = s.find(marker)
    if pos >= 0:
        line_end = s.find("\n", pos)
        insert = (
            "\n    val animationMode: Flow<AnimationMode> = context.dataStore.data.map { prefs ->\n"
            "        runCatching { AnimationMode.valueOf(prefs[Keys.ANIMATION_MODE] ?: AnimationMode.TEMPORARY.name) }"
            ".getOrDefault(AnimationMode.TEMPORARY)\n"
            "    }\n"
        )
        s = s[: line_end + 1] + insert + s[line_end + 1 :]
if "suspend fun setAnimationMode" not in s:
    marker = "    suspend fun setEnabled"
    pos = s.find(marker)
    if pos >= 0:
        brace = s.find("\n    }", pos)
        insert_at = brace + len("\n    }")
        method = (
            "\n\n    suspend fun setAnimationMode(mode: AnimationMode) {\n"
            "        context.dataStore.edit { it[Keys.ANIMATION_MODE] = mode.name }\n"
            "    }"
        )
        s = s[:insert_at] + method + s[insert_at:]
p.write_text(s)
print("AnimationMode preference patched")
