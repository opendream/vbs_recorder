package th.co.opendream.vbs_recorder.processors.post

/*
* Constraint:
* - nValue > 0
* - acceptedIndex >= 0 && acceptedIndex < nValue
*
* eg. nValue = 4, acceptedIndex = 0
* begin with index 0, if index mod 4 == 0 then write out the chunk
*/
class EveryNOutOneChunkPostProcessor(private val nValue: Int, private val acceptedIndex: Int = 0) : IPostAudioProcessor {

    private val sampleRate = 44100
    // each chunk is 200ms => 8820 bytes
    private val sampleFor200msInShort = (sampleRate * 200) / 1000

    override fun process(input: ByteArray): ByteArray {
        val numberOfChunks = input.size / sampleFor200msInShort
        var output = ByteArray(0)
        // write out one chunk then skip by skipValue
        for (i in 0 until numberOfChunks) {
            if (i % nValue == acceptedIndex) {
                // write out one chunk
                val start = i * sampleFor200msInShort
                val end = start + sampleFor200msInShort
                output += input.sliceArray(start until end)
            }
        }
        return output
    }
}