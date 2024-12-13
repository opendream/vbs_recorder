package th.co.opendream.vbs_recorder.processors.realtime

/**
 * Interface for realtime audio file operations.
 */
interface IFileWriter {
    /*
     * Create a new file.
     */
    fun createNewFile()

    /*
     * Switch to the next file.
     * return the path of the last file. (The file that was just closed)s
     */
    fun switchToNextFile() : String

    /*
     * Write data to the current file.
     */
    suspend fun write(data: ByteArray): Int

    /*
     * Close the current file.
     */
    fun close()

    /*
     * Get the current file path.
     */
    fun getFilePath(): String

    /*
     * Get the current file size.
     */
    fun getCurrentFileSize(): Int
}