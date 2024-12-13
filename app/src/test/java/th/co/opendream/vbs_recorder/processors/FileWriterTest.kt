package th.co.opendream.vbs_recorder.processors

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test
import th.co.opendream.vbs_recorder.processors.realtime.FileWriter
import java.io.File
import java.io.RandomAccessFile

class FileWriterTest {

    private lateinit var fileWriter: FileWriter
    private val baseFilePath = "testFile"

    @Before
    fun setUp() {
        fileWriter = FileWriter(baseFilePath)
        fileWriter.createNewFile()
    }

    @After
    fun tearDown() {
        fileWriter.close()
        File(fileWriter.filePath!!).delete()
    }

    @Test
    fun testCreateNewFile() {
        val file = File(fileWriter.filePath!!)
        assert(file.exists())
    }

    @Test
    fun testSwitchToNextFile() {
        val oldFilePath = fileWriter.switchToNextFile()
        val newFilePath = fileWriter.filePath!!

        assert(File(oldFilePath).exists())
        assert(File(newFilePath).exists())
        assertNotEquals(oldFilePath, newFilePath)
    }

    @Test
    fun testWrite() = runBlocking {
        val data = "Hello, World!".toByteArray()
        val writtenSize = fileWriter.write(data)

        assertEquals(data.size, writtenSize)

        val file = RandomAccessFile(fileWriter.filePath!!, "r")
        val readData = ByteArray(data.size)
        file.read(readData)
        file.close()

        assertEquals(String(data), String(readData))
    }

}