package com.edgeplus

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object RootShell {

    private val executor = Executors.newSingleThreadExecutor()

    @Volatile
    private var process: Process? = null

    @Volatile
    private var writer: OutputStreamWriter? = null

    @Synchronized
    private fun ensureShell(): OutputStreamWriter? {
        val current = process
        if (current != null && current.isAlive && writer != null) {
            return writer
        }
        closeInternal()
        return try {
            val proc = ProcessBuilder("su")
                .redirectErrorStream(true)
                .start()
            process = proc
            // Drain stdout/stderr so the buffer never blocks
            Thread({
                try {
                    proc.inputStream.bufferedReader().use { reader ->
                        while (reader.readLine() != null) {
                            // discard output
                        }
                    }
                } catch (_: Throwable) {}
            }, "RootShell-Drain").apply {
                isDaemon = true
                start()
            }
            OutputStreamWriter(proc.outputStream).also { writer = it }
        } catch (_: Throwable) {
            closeInternal()
            null
        }
    }

    @Synchronized
    private fun closeInternal() {
        try {
            writer?.write("exit\n")
            writer?.flush()
            writer?.close()
        } catch (_: Throwable) {}
        writer = null
        try {
            process?.destroy()
        } catch (_: Throwable) {}
        process = null
    }

    fun warmUp() {
        executor.execute {
            ensureShell()
        }
    }

    fun exec(command: String) {
        executor.execute {
            if (!writeCommand(command)) {
                closeInternal()
                writeCommand(command)
            }
        }
    }

    @Synchronized
    private fun writeCommand(command: String): Boolean {
        return try {
            val w = ensureShell() ?: return false
            w.write(command)
            w.write("\n")
            w.flush()
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun requestRootAndGrantPermissions(packageName: String): Boolean {
        val granted = checkRootBlocking()
        if (!granted) return false
        try {
            val cmd = "appops set $packageName SYSTEM_ALERT_WINDOW allow; appops set $packageName WRITE_SETTINGS allow"
            val proc = ProcessBuilder("su", "-c", cmd)
                .redirectErrorStream(true)
                .start()
            proc.waitFor(5, TimeUnit.SECONDS)
        } catch (_: Throwable) {}
        warmUp()
        return true
    }

    fun checkRootBlocking(): Boolean {
        return try {
            val proc = ProcessBuilder("su", "-c", "id")
                .redirectErrorStream(true)
                .start()
            val output = BufferedReader(InputStreamReader(proc.inputStream)).use { it.readText() }
            val finished = proc.waitFor(5, TimeUnit.SECONDS)
            if (!finished) {
                proc.destroyForcibly()
                return false
            }
            proc.exitValue() == 0 && output.contains("uid=0")
        } catch (_: Throwable) {
            false
        }
    }

    fun close() {
        executor.execute {
            closeInternal()
        }
    }
}
