package com.chargeanim.pro

import android.app.ActivityManager
import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.Process
import android.provider.MediaStore
import com.chargeanim.pro.diagnostics.DiagnosticLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Last-resort crash recorder. It is installed from Application.onCreate(),
 * before any Activity, Service, Compose UI, or BroadcastReceiver work.
 */
object CrashCapture {
    private const val DIR = "chargeflow_crashes"
    private const val LATEST = "latest_crash.txt"
    private const val PREVIOUS_EXIT = "previous_exit.txt"
    private const val PREFS = "chargeflow_crash_capture"
    private const val BREADCRUMBS = "breadcrumbs"
    private const val MAX_BREADCRUMBS = 120
    private val lock = Any()
    @Volatile private var installed = false
    private var previousHandler: Thread.UncaughtExceptionHandler? = null

    fun install(application: Application) {
        synchronized(lock) {
            if (installed) return
            installed = true
            previousHandler = Thread.getDefaultUncaughtExceptionHandler()
            val handler = Thread.UncaughtExceptionHandler { thread, throwable ->
                capture(application, thread, throwable)
                runCatching { previousHandler?.uncaughtException(thread, throwable) }
            }
            // Android may have a thread-specific handler on the main thread.
            // Install there explicitly as well as process-wide.
            runCatching { Thread.currentThread().uncaughtExceptionHandler = handler }
            Thread.setDefaultUncaughtExceptionHandler(handler)
            recordPreviousExit(application)
        }
    }

    fun breadcrumb(context: Context, message: String) {
        val app = context.applicationContext
        val line = "${timestamp()}  $message"
        synchronized(lock) {
            runCatching {
                val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val old = prefs.getString(BREADCRUMBS, "") ?: ""
                val lines = (old.split("\n").filter { it.isNotBlank() } + line).takeLast(MAX_BREADCRUMBS)
                prefs.edit().putString(BREADCRUMBS, lines.joinToString("\n")).commit()
            }
        }
    }

    private fun capture(application: Application, thread: Thread, throwable: Throwable) {
        val report = buildString {
            appendLine("ChargeFlow crash report")
            appendLine("=======================")
            appendLine("Time: ${timestamp()}")
            appendLine("Package: ${application.packageName}")
            appendLine("Process ID: ${Process.myPid()}")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Thread: ${thread.name} (${thread.id})")
            appendLine("Exception: ${throwable.javaClass.name}")
            appendLine("Message: ${throwable.message}")
            appendLine()
            appendLine("Stack trace:")
            appendLine(throwable.stackTraceToString())
            appendLine()
            appendLine("Recent ChargeFlow breadcrumbs:")
            val crumbs = runCatching {
                application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                    .getString(BREADCRUMBS, "") ?: ""
            }.getOrDefault("")
            appendLine(if (crumbs.isBlank()) "(none)" else crumbs)
            appendLine()
            appendLine("Recent DiagnosticLog:")
            val diagnostics = runCatching {
                DiagnosticLog.readAll(application).take(100).asReversed()
            }.getOrDefault(emptyList())
            if (diagnostics.isEmpty()) appendLine("(unavailable or empty)")
            else diagnostics.forEach(::appendLine)
        }

        // Synchronous local copies first. These are the most important writes.
        writeAtomically(File(application.filesDir, "$DIR/$LATEST"), report)
        application.getExternalFilesDir(null)?.let { root ->
            writeAtomically(File(root, "$DIR/$LATEST"), report)
        }

        // Public Downloads copy is convenient on Android 10+.
        persistToDownloads(application, report)
    }

    private fun recordPreviousExit(application: Application) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        runCatching {
            val manager = application.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val previous = manager.getHistoricalProcessExitReasons(application.packageName, 0, 5).firstOrNull()
                ?: return@runCatching
            val reason = previous.reason
            if (reason == android.app.ApplicationExitInfo.REASON_CRASH ||
                reason == android.app.ApplicationExitInfo.REASON_CRASH_NATIVE ||
                reason == android.app.ApplicationExitInfo.REASON_ANR ||
                reason == android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE) {
                val report = buildString {
                    appendLine("ChargeFlow previous process exit")
                    appendLine("==============================")
                    appendLine("Detected: ${timestamp()}")
                    appendLine("Reason: ${exitReasonName(reason)} ($reason)")
                    appendLine("Description: ${previous.description ?: "(none)"}")
                    appendLine("Importance: ${previous.importance}")
                    appendLine("Status: ${previous.status}")
                    appendLine("PID: ${previous.pid}")
                }
                writeAtomically(File(application.filesDir, "$DIR/$PREVIOUS_EXIT"), report)
                application.getExternalFilesDir(null)?.let { root ->
                    writeAtomically(File(root, "$DIR/$PREVIOUS_EXIT"), report)
                }
            }
        }
    }

    private fun exitReasonName(reason: Int): String = when (reason) {
        android.app.ApplicationExitInfo.REASON_CRASH -> "CRASH"
        android.app.ApplicationExitInfo.REASON_CRASH_NATIVE -> "CRASH_NATIVE"
        android.app.ApplicationExitInfo.REASON_ANR -> "ANR"
        android.app.ApplicationExitInfo.REASON_EXCESSIVE_RESOURCE_USAGE -> "EXCESSIVE_RESOURCE_USAGE"
        android.app.ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        android.app.ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        android.app.ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_REQUESTED"
        else -> "OTHER"
    }

    private fun persistToDownloads(context: Context, report: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        runCatching {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, "ChargeFlow_crash_latest.txt")
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/ChargeFlow")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return@runCatching
            try {
                resolver.openOutputStream(uri)?.use { output ->
                    output.write(report.toByteArray(Charsets.UTF_8))
                    output.flush()
                } ?: error("Unable to open crash report output stream")
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
            } catch (t: Throwable) {
                resolver.delete(uri, null, null, null)
            }
        }
    }

    private fun writeAtomically(target: File, text: String) {
        runCatching {
            target.parentFile?.mkdirs()
            val temp = File(target.parentFile, target.name + ".tmp")
            FileOutputStream(temp).use { output ->
                output.write(text.toByteArray(Charsets.UTF_8))
                output.flush()
                runCatching { output.fd.sync() }
            }
            if (!temp.renameTo(target)) {
                target.outputStream().use { it.write(text.toByteArray(Charsets.UTF_8)) }
                temp.delete()
            }
        }
    }

    private fun timestamp(): String =
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
}
