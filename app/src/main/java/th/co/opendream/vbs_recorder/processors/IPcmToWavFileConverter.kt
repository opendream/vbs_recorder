package th.co.opendream.vbs_recorder.processors

interface IPcmToWavFileConverter {
    fun convert(pcmFilePath: String, wavFilePath: String, sampleRate: Int = 44100, channels: Int = 1, bitDepth: Int = 16)
}