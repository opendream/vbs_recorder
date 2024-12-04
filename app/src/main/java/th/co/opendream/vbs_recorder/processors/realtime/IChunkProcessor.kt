package th.co.opendream.vbs_recorder.processors.realtime

interface IChunkProcessor {
    fun processChunk(chunk: ShortArray): ShortArray
}