package th.co.opendream.vbs_recorder.processors.realtime

/*
 * Interface for realtime audio file operations.
 * Goal
 * - Save audio to media store
 * - maintain local database record for each audio file
 */
interface IAudioRepository {
    fun add(filePath: String, currentFileSize: Int) : String
}