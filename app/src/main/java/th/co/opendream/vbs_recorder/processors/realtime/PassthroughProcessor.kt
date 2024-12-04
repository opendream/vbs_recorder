package th.co.opendream.vbs_recorder.processors.realtime

class PassthroughProcessor : IChunkProcessor {
    override fun processChunk(chunk: ShortArray): ShortArray {
        return chunk
    }
}