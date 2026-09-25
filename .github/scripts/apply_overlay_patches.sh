#!/usr/bin/env bash
set -euo pipefail
# Run from ChargeAnimPro working directory
cp ../.github/patches/ChargingService.kt app/src/main/java/com/chargeanim/pro/service/ChargingService.kt
cp ../.github/patches/ChargingReceiver.kt app/src/main/java/com/chargeanim/pro/service/ChargingReceiver.kt
cp ../.github/patches/EnergyRingVisual.kt app/src/main/java/com/chargeanim/pro/ui/theme/EnergyRingVisual.kt
cp ../.github/patches/MediaColorAdapter.kt app/src/main/java/com/chargeanim/pro/media/MediaColorAdapter.kt
cp ../.github/patches/VibesThemeManager.kt app/src/main/java/com/chargeanim/pro/ui/theme/VibesThemeManager.kt
cp ../.github/patches/VibesTab.kt app/src/main/java/com/chargeanim/pro/ui/main/VibesTab.kt
cp ../.github/patches/FlowingWaveView.kt app/src/main/java/com/chargeanim/pro/ui/overlay/FlowingWaveView.kt
mkdir -p app/src/main/java/com/chargeanim/pro/settings
cp ../.github/patches/ChargeFlowSettings.kt app/src/main/java/com/chargeanim/pro/settings/ChargeFlowSettings.kt
mkdir -p app/src/main/java/com/chargeanim/pro/history
cp ../.github/patches/ChargingHistoryStore.kt app/src/main/java/com/chargeanim/pro/history/ChargingHistoryStore.kt
mkdir -p app/src/main/java/com/chargeanim/pro/alert
cp ../.github/patches/ChargingAlertManager.kt app/src/main/java/com/chargeanim/pro/alert/ChargingAlertManager.kt
cp ../.github/patches/ChargingOverlayScreen.kt app/src/main/java/com/chargeanim/pro/ui/overlay/ChargingOverlayScreen.kt
mkdir -p app/src/main/java/com/chargeanim/pro/diagnostics
cp ../.github/patches/DiagnosticLog.kt app/src/main/java/com/chargeanim/pro/diagnostics/DiagnosticLog.kt
cp ../.github/patches/MainTabDashboard.kt app/src/main/java/com/chargeanim/pro/ui/main/MainTabDashboard.kt
mkdir -p app/src/main/java/com/chargeanim/pro/telemetry
cp ../.github/patches/ChargingMetrics.kt app/src/main/java/com/chargeanim/pro/telemetry/ChargingMetrics.kt
cp ../.github/patches/ChargingMetricsProvider.kt app/src/main/java/com/chargeanim/pro/telemetry/ChargingMetricsProvider.kt
python3 ../.github/scripts/patch_charging_service.py
python3 <<'PY'
from pathlib import Path
s = Path("app/src/main/AndroidManifest.xml").read_text()
for permission in [
    '    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />',
    '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />',
    '    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />',
    '    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />',
    '    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />']:
    if permission not in s:
        s = s.replace('<application', permission + "\n    <application", 1)
if 'android:name=".service.ChargingService"' not in s:
    s = s.replace('</application>', '    <service android:name=".service.ChargingService" android:enabled="true" android:exported="false" android:foregroundServiceType="specialUse" />\n    </application>')
if 'android:name=".service.ChargingReceiver"' not in s:
    receiver = (
        '    <receiver android:name=".service.ChargingReceiver" android:enabled="true" android:exported="false">\n'
        '              <intent-filter>\n'
        '                  <action android:name="android.intent.action.ACTION_POWER_CONNECTED" />\n'
        '                  <action android:name="android.intent.action.ACTION_POWER_DISCONNECTED" />\n'
        '                  <action android:name="android.intent.action.BOOT_COMPLETED" />\n'
        '                  <action android:name="android.intent.action.LOCKED_BOOT_COMPLETED" />\n'
        '                  <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />\n'
        '              </intent-filter>\n'
        '          </receiver>\n'
    )
    s = s.replace('</application>', receiver + '    </application>')
import re
s = re.sub(r'\s*<receiver[^>]*PowerConnectionReceiver[^>]*>.*?</receiver>', '', s, flags=re.S)
Path("app/src/main/AndroidManifest.xml").write_text(s)
print("Overlay patches applied")
PY
