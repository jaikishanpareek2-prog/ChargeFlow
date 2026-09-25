#!/usr/bin/env python3
from pathlib import Path
p = Path("app/src/main/java/com/chargeanim/pro/ui/theme/ChargeFlowThemes.kt")
s = p.read_text()
old_enum = '    NATURE("Nature"), MINIMAL("Minimal"), ANIME("Anime"), ABSTRACT("Abstract")\n}'
new_enum = (
    '    NATURE("Nature"), MINIMAL("Minimal"), ANIME("Anime"), ABSTRACT("Abstract"),\n'
    '    MIDNIGHT_GARDEN("Midnight Garden"), CELESTIAL_SPARKLE("Celestial Sparkle"),\n'
    '    ENCHANTED_FOREST("Enchanted Forest"), OCEAN_ABYSS("Ocean Abyss")\n'
    '}'
)
if old_enum in s and "MIDNIGHT_GARDEN" not in s:
    s = s.replace(old_enum, new_enum, 1)
old_catalog = (
    '    ThemeId.ABSTRACT to ThemeStyle(Color(0xFFFF6FD8), Color(0xFF6FE0FF), RendererKind.RIBBON_WAVE)\n'
    ')'
)
new_catalog = (
    '    ThemeId.ABSTRACT to ThemeStyle(Color(0xFFFF6FD8), Color(0xFF6FE0FF), RendererKind.RIBBON_WAVE),\n'
    '\n'
    '    // Soft Vibes presets reuse the existing lightweight renderers with dedicated palettes.\n'
    '    ThemeId.MIDNIGHT_GARDEN to ThemeStyle(\n'
    '        Color(0xFFB86BFF), Color(0xFFFF77C8), RendererKind.PARTICLE_FIELD, ParticleDrift.DRIFT\n'
    '    ),\n'
    '    ThemeId.CELESTIAL_SPARKLE to ThemeStyle(\n'
    '        Color(0xFF9B8CFF), Color(0xFF65D9FF), RendererKind.PARTICLE_FIELD, ParticleDrift.RADIAL\n'
    '    ),\n'
    '    ThemeId.ENCHANTED_FOREST to ThemeStyle(\n'
    '        Color(0xFF64E89A), Color(0xFFB8FF70), RendererKind.PARTICLE_FIELD, ParticleDrift.DRIFT\n'
    '    ),\n'
    '    ThemeId.OCEAN_ABYSS to ThemeStyle(\n'
    '        Color(0xFF28C8FF), Color(0xFF63FFE0), RendererKind.LIQUID_RIPPLE\n'
    '    )\n'
    ')'
)
if old_catalog in s and "ThemeId.MIDNIGHT_GARDEN to" not in s:
    s = s.replace(old_catalog, new_catalog, 1)
s = s.replace("import androidx.compose.runtime.setValue\n", "").replace("import androidx.compose.runtime.getValue\n", "")
p.write_text(s)
print("Vibes themes patched")
