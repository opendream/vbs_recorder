package th.co.opendream.vbs_recorder.processors.realtime

interface IAudioProcessor {

    suspend fun processAudioChunk(buffer: ShortArray, size: Int)

    fun close()
}