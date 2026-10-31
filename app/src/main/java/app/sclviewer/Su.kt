package app.sclviewer

import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** Runs a command through `su -c` (needed only to read dmesg and talk to the module). */
object Su {
    data class Res(val rc: Int, val out: String, val err: String)

    private const val TIMEOUT_S = 10L

    fun run(cmd: String): Res = exec(cmd, null)

    /** Like run(), but feeds [input] to the command's stdin (used to write files as root). */
    fun runWithInput(cmd: String, input: String): Res = exec(cmd, input)

    private fun exec(cmd: String, input: String?): Res {
        return try {
            val p = ProcessBuilder("su", "-c", cmd).start()
            val out = StringBuilder()
            val err = StringBuilder()
            // Drain both pipes in parallel: reading them one after the other can deadlock
            // when the unread one fills up.
            val tOut = thread(isDaemon = true) { out.append(p.inputStream.bufferedReader().readText()) }
            val tErr = thread(isDaemon = true) { err.append(p.errorStream.bufferedReader().readText()) }
            try {
                p.outputStream.use { if (input != null) it.write(input.toByteArray(Charsets.UTF_8)) }
            } catch (_: java.io.IOException) {
                // process ended before reading stdin; the exit code tells the rest
            }
            // A su that waits for a root grant must not block the caller forever.
            if (!p.waitFor(TIMEOUT_S, TimeUnit.SECONDS)) {
                p.destroyForcibly()
                return Res(-1, "", "su timed out after ${TIMEOUT_S}s")
            }
            tOut.join(2000)
            tErr.join(2000)
            Res(p.exitValue(), out.toString(), err.toString())
        } catch (e: Exception) {
            Res(-1, "", e.message ?: e.toString())
        }
    }
}
