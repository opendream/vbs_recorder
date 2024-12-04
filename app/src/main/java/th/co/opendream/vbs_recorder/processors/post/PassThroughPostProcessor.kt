package th.co.opendream.vbs_recorder.processors.post

class PassThroughPostProcessor : IPostAudioProcessor {
    override fun process(input: ByteArray): ByteArray {
        return input
    }
}