package th.co.opendream.vbs_recorder.processors.realtime

class RandomDropProcessor(private val dropRate: Float) : IChunkProcessor {
    override fun processChunk(chunk: ShortArray): ShortArray {
        if (chunk.isEmpty()) return chunk
        
        if (Math.random() > dropRate) {
            // return empty array
            return shortArrayOf()
        }

        return chunk
    }
}