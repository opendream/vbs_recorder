package th.co.opendream.vbs_recorder.processors.realtime

class ChunkProcessorComposer(
    private val processors: List<IChunkProcessor>
) : IChunkProcessor {
    override fun processChunk(chunk: ShortArray): ShortArray {
        if (processors.isEmpty()) {
            return chunk
        }
        var processedChunk = chunk
        for (processor in processors) {
            processedChunk = processor.processChunk(processedChunk)
        }
        return processedChunk
    }
}