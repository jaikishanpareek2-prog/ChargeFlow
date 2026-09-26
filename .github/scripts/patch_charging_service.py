#!/usr/bin/env python3
from pathlib import Path

def fix(path: Path) -> None:
    if not path.exists():
        return
    s = path.read_text()
    old_catch = '''        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "Overlay addView failed: BadTokenException", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: BadTokenException: ${e.message}")
            destroyOverlayOwner()
            stopSelf()
        } catch (e: Exception) {
            Log.e(TAG, "Overlay addView failed: ${e::class.simpleName}: ${e.message}", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: ${e::class.simpleName}: ${e.message}")
            destroyOverlayOwner()
            stopSelf()
        }'''
    new_catch = '''        } catch (e: WindowManager.BadTokenException) {
            Log.e(TAG, "Overlay addView failed: BadTokenException", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: BadTokenException: ${e.message}")
            destroyOverlayOwner()
            // Keep the permanent watcher alive; the next relevant event will retry.
        } catch (e: Exception) {
            Log.e(TAG, "Overlay addView failed: ${e::class.simpleName}: ${e.message}", e)
            DiagnosticLog.add(this, "Overlay addView FAILED: ${e::class.simpleName}: ${e.message}")
            destroyOverlayOwner()
            // Keep the permanent watcher alive; the next relevant event will retry.
        }'''
    if old_catch in s:
        s = s.replace(old_catch, new_catch, 1)
    old_mode = '''        val shouldShow = enabled && charging && when (animationMode) {
            AnimationMode.ALWAYS_ON -> locked
            AnimationMode.TEMPORARY -> !userPresentSincePlugged || locked
        }'''
    new_mode = '''        val shouldShow = enabled && charging && when (animationMode) {
            // Always On: remain visible for the whole charging session.
            AnimationMode.ALWAYS_ON -> true
            // Temporary: show from plug until the user unlocks once this session.
            AnimationMode.TEMPORARY -> !userPresentSincePlugged || locked
        }'''
    if old_mode in s:
        s = s.replace(old_mode, new_mode, 1)
    path.write_text(s)

# Fix patch source and extracted service if present
fix(Path("../.github/patches/ChargingService.kt"))
fix(Path("app/src/main/java/com/chargeanim/pro/service/ChargingService.kt"))
print("ChargingService lifecycle fixes applied")
