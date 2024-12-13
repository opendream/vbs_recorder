package th.co.opendream.vbs_recorder.processors

import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.anyInt
import org.mockito.Mockito.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import th.co.opendream.vbs_recorder.processors.realtime.AudioProcessor
import th.co.opendream.vbs_recorder.processors.realtime.IAudioRepository
import th.co.opendream.vbs_recorder.processors.realtime.IFileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder



class AudioProcessorTest {
    private lateinit var fileWriter: IFileWriter
    private lateinit var audioRepository: IAudioRepository

    @Before
    fun setUp() {
        fileWriter = mock(IFileWriter::class.java)
        audioRepository = mock(IAudioRepository::class.java)
    }

    @Test
    fun testProcessAudioChunk(): Unit = runBlocking {
        val buffer = ShortArray(1024) { it.toShort() }
        val size = buffer.size

        `when`(fileWriter.write(any())).thenReturn(512)
        `when`(fileWriter.switchToNextFile()).thenReturn("newFilePath")

        val audioProcessor = AudioProcessor(fileWriter, audioRepository, 44100, 1024)
        audioProcessor.processAudioChunk(buffer, size)

        val byteBuffer = ByteBuffer.allocate(size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.forEach { byteBuffer.putShort(it) }

        verify(fileWriter).write(byteBuffer.array())
        verify(fileWriter, never()).switchToNextFile()
        verify(audioRepository, never()).add(anyString(), anyInt())
    }

    @Test
    fun testWriteToFileTwiceButSwitchOnlyOnce(): Unit = runBlocking {
        val buffer1 = ShortArray(1024) { it.toShort() }
        val size1 = buffer1.size
        val buffer2 = ShortArray(200) { it.toShort() }
        val size2 = buffer2.size

        `when`(fileWriter.write(any())).thenReturn(size1)
        `when`(fileWriter.switchToNextFile()).thenReturn("newFilePath")
        `when`(audioRepository.add(anyString(), anyInt())).thenReturn("savedFileName")

        val audioProcessor = AudioProcessor(fileWriter, audioRepository, 44100, 1024)
        audioProcessor.processAudioChunk(buffer1, size1)
        `when`(fileWriter.write(any())).thenReturn(size2)
        audioProcessor.processAudioChunk(buffer2, size2)

        val byteBuffer1 = ByteBuffer.allocate(size1 * 2)
        byteBuffer1.order(ByteOrder.LITTLE_ENDIAN)
        buffer1.forEach { byteBuffer1.putShort(it) }
        verify(fileWriter, times(1)).write(byteBuffer1.array())

        val byteBuffer2 = ByteBuffer.allocate(size2 * 2)
        byteBuffer2.order(ByteOrder.LITTLE_ENDIAN)
        buffer2.forEach { byteBuffer2.putShort(it) }
        verify(fileWriter, times(1)).write(byteBuffer2.array())

        verify(fileWriter, times(1)).switchToNextFile()
        verify(audioRepository, times(1)).add("newFilePath", 1024)
    }

    @Test
    fun testWriteToTwoFiles(): Unit = runBlocking {
        val buffer = ShortArray(1024) { it.toShort() }
        val size = buffer.size

        `when`(fileWriter.write(any())).thenReturn(1024)
        `when`(fileWriter.switchToNextFile()).thenReturn("newFilePath")
        `when`(audioRepository.add(anyString(), anyInt())).thenReturn("savedFileName")

        val audioProcessor = AudioProcessor(fileWriter, audioRepository, 44100, 1024)
        audioProcessor.processAudioChunk(buffer, size)
        audioProcessor.processAudioChunk(buffer, size)

        val byteBuffer = ByteBuffer.allocate(size * 2)
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN)
        buffer.forEach { byteBuffer.putShort(it) }

        verify(fileWriter, times(2)).write(byteBuffer.array())
        verify(fileWriter, times(2)).switchToNextFile()
        verify(audioRepository, times(2)).add("newFilePath", 1024)
    }


}