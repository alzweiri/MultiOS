package com.galaxy.m21.desktop.hardware

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.Settings
import android.util.Log
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Hardware SysFS & Magisk Superuser (`su`) Bridge for MultiOS.
 *
 * - Controls CPU frequency tables & scaling governors.
 * - Hardware display brightness & backlight scaling.
 * - Wi-Fi & Bluetooth radio control.
 * - Extracts real OS task snapshot images (/data/system_ce/0/snapshots/taskId.jpg) for Recents!
 * - Auto-grants `BIND_APPWIDGET` & `NotificationListenerService` permissions.
 * - Bypasses stock crDroid 16 Lock Screen (`locksettings set-disabled true` / `wm dismiss-keyguard`)!
 */
object SysFsBridge {
    private const val TAG = "MultiOsSysFs"

    private val POLICY_DIRS = (0..7).map { "/sys/devices/system/cpu/cpufreq/policy$it" }
    private val LITTLE_FREQ_PATHS = listOf(
        "/sys/devices/system/cpu/cpufreq/policy0/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpufreq/policy0/cpuinfo_cur_freq"
    )
    private val BIG_FREQ_PATHS = listOf(
        "/sys/devices/system/cpu/cpufreq/policy6/scaling_cur_freq",
        "/sys/devices/system/cpu/cpufreq/policy4/scaling_cur_freq",
        "/sys/devices/system/cpu/cpu4/cpufreq/scaling_cur_freq",
        "/sys/devices/system/cpu/cpufreq/policy4/cpuinfo_cur_freq"
    )
    private val THERMAL_PATHS = listOf(
        "/sys/class/thermal/thermal_zone0/temp",
        "/sys/class/thermal/thermal_zone1/temp",
        "/sys/class/thermal/thermal_zone2/temp",
        "/sys/devices/virtual/thermal/thermal_zone0/temp"
    )
    private val BACKLIGHT_DIRS = listOf(
        "/sys/class/backlight/panel",
        "/sys/class/backlight/panel0-backlight",
        "/sys/class/leds/lcd-backlight"
    )

    @Volatile
    var hasMagiskRoot: Boolean = false
        private set

    private var cachedPolicyFreqTables: Map<String, List<Long>>? = null
    private val unreadablePaths = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()
    private var prevIdleTime = 0L
    private var prevTotalTime = 0L

    /**
     * Triggers Magisk Superuser grant prompt (`su -c id`) and verifies UID 0.
     */
    fun requestMagiskRoot(): Boolean {
        return try {
            val process = ProcessBuilder("su", "-c", "id")
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(8000, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                false
            } else {
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val granted = output.contains("uid=0")
                hasMagiskRoot = granted
                if (granted && cachedPolicyFreqTables == null) {
                    cachedPolicyFreqTables = discoverCpuFrequencyTables()
                }
                granted
            }
        } catch (e: Exception) {
            Log.w(TAG, "Magisk su binary not present: ${e.message}")
            hasMagiskRoot = false
            false
        }
    }

    private fun discoverCpuFrequencyTables(): Map<String, List<Long>> {
        val result = mutableMapOf<String, List<Long>>()
        for (dirPath in POLICY_DIRS) {
            val dir = File(dirPath)
            if (!dir.exists()) continue

            val availFile = File(dir, "scaling_available_frequencies")
            val freqs = runCatching {
                if (availFile.exists() && availFile.canRead()) {
                    availFile.readText()
                        .trim()
                        .split("\\s+".toRegex())
                        .mapNotNull { it.toLongOrNull() }
                        .sorted()
                } else {
                    emptyList()
                }
            }.getOrDefault(emptyList())

            if (freqs.isNotEmpty()) {
                result[dirPath] = freqs
            } else {
                val minF = runCatching { File(dir, "cpuinfo_min_freq").readText().trim().toLongOrNull() }.getOrNull()
                val maxF = runCatching { File(dir, "cpuinfo_max_freq").readText().trim().toLongOrNull() }.getOrNull()
                if (minF != null && maxF != null && maxF >= minF) {
                    val steps = (0..8).map { i -> minF + ((maxF - minF) * i) / 8L }
                    result[dirPath] = steps
                }
            }
        }

        if (result.isEmpty() && hasMagiskRoot) {
            val out = executeRootCommandWithOutput(
                "for p in /sys/devices/system/cpu/cpufreq/policy*; do " +
                    "if [ -f \"\$p/scaling_available_frequencies\" ]; then echo \"\$p:\$(cat \$p/scaling_available_frequencies)\"; " +
                    "elif [ -f \"\$p/cpuinfo_max_freq\" ]; then echo \"\$p:\$(cat \$p/cpuinfo_min_freq) \$(cat \$p/cpuinfo_max_freq)\"; fi; done"
            )
            out.lines().forEach { line ->
                val parts = line.split(":")
                if (parts.size == 2) {
                    val nums = parts[1].trim().split("\\s+".toRegex()).mapNotNull { it.toLongOrNull() }.sorted()
                    if (nums.size == 2) {
                        val minF = nums[0]
                        val maxF = nums[1]
                        result[parts[0].trim()] = (0..8).map { i -> minF + ((maxF - minF) * i) / 8L }
                    } else if (nums.size > 2) {
                        result[parts[0].trim()] = nums
                    }
                }
            }
        }
        return result
    }

    fun applyCpuSpeedNormalized(normalized: Float): Pair<Float, String> {
        val clamped = normalized.coerceIn(0f, 1f)
        val governor = when {
            clamped < 0.25f -> "powersave"
            clamped > 0.82f -> "performance"
            else -> "schedutil"
        }

        if (!hasMagiskRoot) {
            requestMagiskRoot()
        }

        val tables = cachedPolicyFreqTables ?: discoverCpuFrequencyTables().also {
            if (it.isNotEmpty()) cachedPolicyFreqTables = it
        }

        val shellCmds = mutableListOf<String>()
        var highestSelectedKhz = 0L

        if (tables.isNotEmpty()) {
            for ((policyPath, freqs) in tables) {
                val idx = (clamped * (freqs.size - 1)).roundToInt().coerceIn(0, freqs.size - 1)
                val targetKhz = freqs[idx]
                if (targetKhz > highestSelectedKhz) highestSelectedKhz = targetKhz
                shellCmds.add("chmod 644 $policyPath/scaling_governor $policyPath/scaling_max_freq 2>/dev/null")
                shellCmds.add("echo $governor > $policyPath/scaling_governor 2>/dev/null")
                shellCmds.add("echo $targetKhz > $policyPath/scaling_max_freq 2>/dev/null")
            }
        } else {
            shellCmds.add(
                "for p in /sys/devices/system/cpu/cpufreq/policy*; do " +
                    "echo $governor > \$p/scaling_governor 2>/dev/null; " +
                    "if [ -f \$p/cpuinfo_max_freq ] && [ -f \$p/cpuinfo_min_freq ]; then " +
                    "min=\$(cat \$p/cpuinfo_min_freq); max=\$(cat \$p/cpuinfo_max_freq); " +
                    "target=\$(( min + (max - min) * ${(clamped * 100).roundToInt()} / 100 )); " +
                    "echo \$target > \$p/scaling_max_freq 2>/dev/null; fi; done"
            )
            highestSelectedKhz = (960_000L + (clamped * 1_450_000L).toLong())
        }

        if (shellCmds.isNotEmpty()) {
            executeRootCommand(shellCmds.joinToString(" ; "))
        }

        val ghz = ((highestSelectedKhz / 1_000_000f) * 100f).roundToInt() / 100f
        return Pair(if (ghz > 0.3f) ghz else (1.0f + clamped * 1.4f), governor)
    }

    fun readCpuTemperature(): Int {
        for (path in THERMAL_PATHS) {
            if (path in unreadablePaths) continue
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val raw = file.readText().trim().toIntOrNull() ?: continue
                    val celsius = if (raw > 1000) raw / 1000 else if (raw > 200) raw / 10 else raw
                    if (celsius in 15..115) return celsius
                } else {
                    unreadablePaths.add(path)
                }
            } catch (_: Exception) {
                unreadablePaths.add(path)
            }
        }
        if (hasMagiskRoot) {
            val out = executeRootCommandWithOutput("cat /sys/class/thermal/thermal_zone0/temp 2>/dev/null")
            val raw = out.trim().toIntOrNull()
            if (raw != null) {
                val celsius = if (raw > 1000) raw / 1000 else raw
                if (celsius in 15..115) return celsius
            }
        }
        return -1
    }

    fun readBigClusterFreqGhz(): Float = readFreqFromPaths(BIG_FREQ_PATHS)
    fun readLittleClusterFreqGhz(): Float = readFreqFromPaths(LITTLE_FREQ_PATHS)

    private fun readFreqFromPaths(paths: List<String>): Float {
        for (path in paths) {
            if (path in unreadablePaths) continue
            try {
                val file = File(path)
                if (file.exists() && file.canRead()) {
                    val rawKhz = file.readText().trim().toFloatOrNull() ?: continue
                    return ((rawKhz / 1_000_000f) * 100f).roundToInt() / 100f
                } else {
                    unreadablePaths.add(path)
                }
            } catch (_: Exception) {
                unreadablePaths.add(path)
            }
        }
        return -1f
    }

    fun readCpuLoadRatio(): Float {
        val statPath = "/proc/stat"
        if (statPath in unreadablePaths) return -1f
        return try {
            val statFile = File(statPath)
            if (!statFile.exists() || !statFile.canRead()) {
                unreadablePaths.add(statPath)
                return -1f
            }
            val firstLine = statFile.bufferedReader().use { it.readLine() } ?: return -1f
            if (!firstLine.startsWith("cpu ")) return -1f

            val parts = firstLine.trim().split("\\s+".toRegex()).drop(1).mapNotNull { it.toLongOrNull() }
            if (parts.size < 4) return -1f

            val idle = parts[3] + (parts.getOrNull(4) ?: 0L)
            val total = parts.sum()

            val diffIdle = idle - prevIdleTime
            val diffTotal = total - prevTotalTime
            prevIdleTime = idle
            prevTotalTime = total

            if (diffTotal <= 0L) return 0.2f
            ((diffTotal - diffIdle).toFloat() / diffTotal.toFloat()).coerceIn(0.02f, 1.0f)
        } catch (_: Exception) {
            -1f
        }
    }

    fun writeThermalLimit(limitCelsius: Int) {
        if (!hasMagiskRoot) {
            requestMagiskRoot()
        }
        val millicelsius = limitCelsius * 1000
        executeRootCommand(
            "echo $millicelsius > /sys/class/thermal/thermal_zone0/trip_point_0_temp 2>/dev/null ; " +
                "echo $millicelsius > /sys/class/thermal/thermal_zone1/trip_point_0_temp 2>/dev/null"
        )
    }

    /**
     * Writes system-wide brightness via `Settings.System` (disabling Adaptive Brightness so manual
     * value takes effect immediately) AND scales real sysfs `max_brightness` when rooted.
     */
    fun writeDisplayBrightness(context: Context, normalizedBrightness: Float) {
        val clamped = normalizedBrightness.coerceIn(0.02f, 1.0f)
        val intVal255 = (clamped * 255f).roundToInt().coerceIn(5, 255)
        try {
            if (Settings.System.canWrite(context)) {
                val cr = context.contentResolver
                Settings.System.putInt(
                    cr,
                    Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                )
                Settings.System.putInt(
                    cr,
                    Settings.System.SCREEN_BRIGHTNESS,
                    intVal255
                )
                return
            }
        } catch (_: Exception) {}

        if (hasMagiskRoot) {
            val pct = (clamped * 100f).roundToInt().coerceIn(2, 100)
            val cmds = BACKLIGHT_DIRS.map { dir ->
                "if [ -f $dir/brightness ]; then " +
                    "max=\$(cat $dir/max_brightness 2>/dev/null || echo 255); " +
                    "val=\$(( max * $pct / 100 )); " +
                    "echo \$val > $dir/brightness 2>/dev/null; fi"
            } + "settings put system screen_brightness_mode 0 ; settings put system screen_brightness $intVal255"
            executeRootCommand(cmds.joinToString(" ; "))
        }
    }

    fun setWifiEnabledRoot(enabled: Boolean): Boolean {
        val cmdState = if (enabled) "enabled" else "disabled"
        val svcState = if (enabled) "enable" else "disable"
        return executeRootCommand("cmd wifi set-wifi-enabled $cmdState 2>/dev/null || svc wifi $svcState")
    }

    fun setBluetoothEnabledRoot(enabled: Boolean): Boolean {
        val state = if (enabled) "enable" else "disable"
        return executeRootCommand("cmd bluetooth_manager $state 2>/dev/null || svc bluetooth $state")
    }

    fun setWirelessAdbEnabled(enabled: Boolean) {
        val port = if (enabled) "5555" else "-1"
        executeRootCommand("setprop service.adb.tcp.port $port && stop adbd && start adbd")
    }

    fun enforceImmersiveStatusBarRoot(packageName: String) {
        if (!hasMagiskRoot) return
        executeRootCommand(
            "settings put global policy_control immersive.full=$packageName 2>/dev/null"
        )
    }

    fun triggerSystemRecentsRoot(): Boolean {
        if (!hasMagiskRoot) return false
        return executeRootCommand("input keyevent 187")
    }

    /**
     * Auto-grant BIND_APPWIDGET permission silently via root.
     */
    fun grantAppWidgetBindRoot(packageName: String): Boolean {
        if (!hasMagiskRoot && !requestMagiskRoot()) return false
        return executeRootCommand("appwidget grantbind --package $packageName --user 0 2>/dev/null")
    }

    /**
     * Auto-enable NotificationListenerService silently via root.
     */
    fun enableNotificationListenerRoot(context: Context): Boolean {
        if (!hasMagiskRoot && !requestMagiskRoot()) return false
        val component = "${context.packageName}/com.galaxy.m21.desktop.service.MultiOsNotificationListener"
        return executeRootCommand("cmd notification allow_listener $component 2>/dev/null")
    }

    /**
     * Adapt / Eliminate crDroid 16 stock lock screen:
     * Disables Keyguard and unlocks screen immediately so MultiOS AOD -> MultiOS Home takes over!
     */
    fun setSystemLockScreenDisabledRoot(disabled: Boolean): Boolean {
        if (!hasMagiskRoot && !requestMagiskRoot()) return false
        return if (disabled) {
            executeRootCommand("locksettings set-disabled true 2>/dev/null ; wm dismiss-keyguard 2>/dev/null")
        } else {
            executeRootCommand("locksettings set-disabled false 2>/dev/null")
        }
    }

    /**
     * Fetches real OS task snapshots via root from `/data/system_ce/0/snapshots/`!
     * Returns a map of Package Name -> Bitmap thumbnail.
     */
    fun fetchRecentTaskSnapshotsRoot(context: Context): Map<String, Bitmap> {
        if (!hasMagiskRoot && !requestMagiskRoot()) return emptyMap()

        val results = mutableMapOf<String, Bitmap>()
        val cacheSnapDir = File(context.cacheDir, "recents_snaps").apply { mkdirs() }

        runCatching {
            // Map taskId -> packageName from dumpsys
            val recentsDump = executeRootCommandWithOutput("dumpsys activity recents 2>/dev/null")
            val taskPkgMap = mutableMapOf<String, String>()
            var currentTaskId: String? = null

            recentsDump.lines().forEach { line ->
                val trimmed = line.trim()
                if (trimmed.startsWith("* Recent #") || trimmed.startsWith("RecentTaskInfo #") || trimmed.contains("taskId=")) {
                    val match = Regex("taskId=(\\d+)").find(trimmed)
                    if (match != null) {
                        currentTaskId = match.groupValues[1]
                    }
                }
                if (currentTaskId != null && (trimmed.contains("realActivity=") || trimmed.contains("affinity="))) {
                    val pkgMatch = Regex("(?:realActivity|affinity)=([a-zA-Z0-9_.]+)/").find(trimmed)
                        ?: Regex("(?:realActivity|affinity)=([a-zA-Z0-9_.]+)").find(trimmed)
                    if (pkgMatch != null) {
                        val pkg = pkgMatch.groupValues[1]
                        if (pkg != context.packageName) {
                            taskPkgMap[currentTaskId!!] = pkg
                        }
                    }
                }
            }

            // Copy snapshots to accessible cache folder
            val snapshotPaths = listOf(
                "/data/system_ce/0/snapshots",
                "/data/system/users/0/snapshots"
            )

            for ((taskId, pkg) in taskPkgMap.entries.take(12)) {
                if (results.containsKey(pkg)) continue
                val targetCachedFile = File(cacheSnapDir, "${taskId}_snap.jpg")
                var copied = false

                for (sDir in snapshotPaths) {
                    val reduced = "$sDir/${taskId}_reduced.jpg"
                    val full = "$sDir/$taskId.jpg"
                    val cmd = "if [ -f $reduced ]; then cp $reduced ${targetCachedFile.absolutePath} ; chmod 666 ${targetCachedFile.absolutePath}; " +
                        "elif [ -f $full ]; then cp $full ${targetCachedFile.absolutePath} ; chmod 666 ${targetCachedFile.absolutePath}; fi"
                    executeRootCommand(cmd)
                    if (targetCachedFile.exists() && targetCachedFile.length() > 200L) {
                        copied = true
                        break
                    }
                }

                if (copied && targetCachedFile.exists()) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 1 }
                    val bmp = BitmapFactory.decodeFile(targetCachedFile.absolutePath, opts)
                    if (bmp != null) {
                        results[pkg] = bmp
                    }
                }
            }
        }
        return results
    }

    /**
     * Magisk Root Bypass Charging for Galaxy M21 / Exynos 9611:
     * Powers phone directly from AC stand without charging battery cells (0W battery input, zero thermal degradation).
     */
    fun setBypassChargingRoot(enabled: Boolean): Boolean {
        if (!hasMagiskRoot && !requestMagiskRoot()) return false
        val suspendVal = if (enabled) "1" else "0"
        val chargeVal = if (enabled) "0" else "1"
        val slateVal = if (enabled) "1" else "0"

        val cmds = listOf(
            "echo $suspendVal > /sys/class/power_supply/battery/input_suspend 2>/dev/null",
            "echo $chargeVal > /sys/class/power_supply/battery/charging_enabled 2>/dev/null",
            "echo $slateVal > /sys/class/power_supply/battery/batt_slate_mode 2>/dev/null",
            "echo $slateVal > /sys/class/power_supply/battery/store_mode 2>/dev/null"
        )
        return executeRootCommand(cmds.joinToString(" ; "))
    }

    /**
     * Hardware Sensor Privacy Killswitch via Root (1 = Mic, 2 = Camera).
     */
    fun setSensorPrivacyRoot(sensorType: Int, blocked: Boolean): Boolean {
        if (!hasMagiskRoot && !requestMagiskRoot()) return false
        val state = if (blocked) "true" else "false"
        return executeRootCommand("cmd sensor_privacy set $sensorType $state 2>/dev/null")
    }

    /**
     * Instant Keyguard & crDroid Lock Screen Dismissal via Root.
     */
    fun dismissKeyguardRoot(): Boolean {
        if (!hasMagiskRoot && !requestMagiskRoot()) return false
        return executeRootCommand("wm dismiss-keyguard 2>/dev/null ; input keyevent 82 2>/dev/null")
    }

    fun setLocationEnabledRoot(enabled: Boolean): Boolean {
        val state = if (enabled) "3" else "0"
        return executeRootCommand("settings put secure location_mode $state 2>/dev/null")
    }

    fun setAirplaneModeRoot(enabled: Boolean): Boolean {
        val state = if (enabled) "1" else "0"
        return executeRootCommand(
            "settings put global airplane_mode_on $state 2>/dev/null ; " +
                "am broadcast -a android.intent.action.AIRPLANE_MODE --ez state $enabled 2>/dev/null"
        )
    }

    fun executeRootCommand(command: String): Boolean {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(2500, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                false
            } else {
                val ok = process.exitValue() == 0
                if (ok) hasMagiskRoot = true
                ok
            }
        } catch (e: Exception) {
            Log.w(TAG, "Root command skipped: $command")
            false
        }
    }

    private fun executeRootCommandWithOutput(command: String): String {
        return try {
            val process = ProcessBuilder("su", "-c", command)
                .redirectErrorStream(true)
                .start()
            val finished = process.waitFor(2500, TimeUnit.MILLISECONDS)
            if (!finished) {
                process.destroyForcibly()
                ""
            } else {
                process.inputStream.bufferedReader().use { it.readText() }
            }
        } catch (_: Exception) {
            ""
        }
    }
}
