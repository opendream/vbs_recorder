package th.co.opendream.vbs_recorder.processors.post

interface IPostAudioProcessor {
    fun process(input: ByteArray) : ByteArray
}