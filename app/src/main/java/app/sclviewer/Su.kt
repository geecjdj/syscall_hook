package app.sclviewer

/** Runs a command through `su -c` (needed only to read dmesg). */
object Su {
    data class Res(val rc: Int, val out: String, val err: String)

    fun run(cmd: String): Res = exec(cmd, null)

    /** Like run(), but feeds [input] to the command's stdin (used to write files as root). */
    fun runWithInput(cmd: String, input: String): Res = exec(cmd, input)

    private fun exec(cmd: String, input: String?): Res {
        return try {
            val p = ProcessBuilder("su", "-c", cmd).start()
            // stderr is drained on its own thread: a full pipe must never block the stdout read
            var err = ""
            val t = Thread { err = runCatching { p.errorStream.bufferedReader().readText() }.getOrDefault("") }
            t.start()
            p.outputStream.use { os -> input?.let { os.write(it.toByteArray(Charsets.UTF_8)) } }
            val out = p.inputStream.bufferedReader().readText()
            val rc = p.waitFor()
            t.join(2000)
            p.destroy()
            Res(rc, out, err)
        } catch (e: Exception) {
            Res(-1, "", e.message ?: e.toString())
        }
    }
}
