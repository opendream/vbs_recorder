package th.co.opendream.vbs_recorder.processors.post

/*
* Constraint:
* - nValue > 0
* - acceptedIndex >= 0 && acceptedIndex < nValue
*
* eg. nValue = 4, acceptedIndex = 0
* begin with index 0, if index mod 4 == 0 then write out the chunk
*/
class EveryNOutOneChunkPostProcessor(private val sampleRate: Int, private val chunkSizeInMs: Int,
                                     private val nValue: Int, private val acceptedIndex: Int = 0)
    : IPostAudioProcessor {
    override fun process(input: ByteArray): ByteArray {
        val sampleFor200msInShort = (sampleRate * chunkSizeInMs) / 1000
        val sampleFor200msInBytes = sampleFor200msInShort * 2
        val numberOfChunks = input.size / sampleFor200msInBytes
        var output = ByteArray(0)
        // write out one chunk then skip by skipValue
        for (i in 0 until numberOfChunks) {
            if (i % nValue == acceptedIndex) {
                // write out one chunk
                val start = i * sampleFor200msInBytes
                val end = start + sampleFor200msInBytes
                output += input.sliceArray(start until end)
            }
        }
        return output
    }
}