#!/usr/bin/env python3
from pathlib import Path

def fix(path: Path) -> None:
    if not path.exists():
        return
    s = path.read_text()
    import_line = "import androidx.savedstate.setViewTreeSavedStateRegistryOwner\n"
    if import_line not in s:
        anchor = "import androidx.savedstate.SavedStateRegistryOwner\n"
        if anchor in s:
            s = s.replace(anchor, anchor + import_line, 1)
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
            AnimationMode.ALWAYS_ON -> true
            AnimationMode.TEMPORARY -> !userPresentSincePlugged || locked
        }'''
    new_mode = '''        val shouldShow = enabled && charging && when (animationMode) {
            // Always On: stay visible for the entire charging session.
            AnimationMode.ALWAYS_ON -> true
            // Temporary: show until the user unlocks once this session.
            AnimationMode.TEMPORARY -> !userPresentSincePlugged || locked
        }'''
    if old_mode in s:
        s = s.replace(old_mode, new_mode, 1)
    old_monitor = "\n".join([
        '            ACTION_PLUGGED_IN, ACTION_MONITOR, null -> evaluateAnimationState("SERVICE_START", false)',
        '        }',
        '        return START_NOT_STICKY',
        '    }',
        '',
        '    private fun evaluateAnimationState(reason: String, haptic: Boolean) {',
    ])
    new_monitor = "\n".join([
        '            ACTION_PLUGGED_IN, null -> evaluateAnimationState("SERVICE_START", false)',
        '            ACTION_MONITOR -> {',
        '                evaluateAnimationState("SERVICE_MONITOR", false)',
        '                if (!isCurrentlyCharging()) {',
        '                    DiagnosticLog.add(this, "Monitor request found no active charging; stopping watcher")',
        '                    stopSelf(startId)',
        '                    return START_NOT_STICKY',
        '                }',
        '            }',
        '        }',
        '        return START_NOT_STICKY',
        '    }',
        '',
        '    private fun isCurrentlyCharging(): Boolean {',
        '        val battery = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))',
        '        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1',
        '        val plugged = battery?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0',
        '        return (status == BatteryManager.BATTERY_STATUS_CHARGING ||',
        '            status == BatteryManager.BATTERY_STATUS_FULL) && plugged != 0',
        '    }',
        '',
        '    private fun evaluateAnimationState(reason: String, haptic: Boolean) {',
    ])
    if old_monitor in s:
        s = s.replace(old_monitor, new_monitor, 1)
    path.write_text(s)

# Fix patch source and extracted service if present
fix(Path("../.github/patches/ChargingService.kt"))
fix(Path("app/src/main/java/com/chargeanim/pro/service/ChargingService.kt"))
print("ChargingService lifecycle fixes applied")
