package th.co.opendream.vbs_recorder

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat.startForegroundService
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ServiceTestRule
import androidx.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule
import th.co.opendream.vbs_recorder.services.Record2Service
import th.co.opendream.vbs_recorder.utils.SettingsUtil

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class Record2ServiceTest {

    @get:Rule
    private val serviceRule = ServiceTestRule()

    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("th.co.opendream.vbs_recorder", appContext.packageName)
    }

    @Test
    fun testRecord2ServiceDependOnSetting() {
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext<Context>(),
            Record2Service::class.java
        ).apply {
            putExtra("service_id", Record2Service.SERVICE_ID)
            putExtra("title", "202402010000")
            putExtra("filePath", "vbs_202402010000")
            putExtra("segmentCount", 1)
        }

        val binder = serviceRule.bindService(serviceIntent)
        val service : Record2Service = (binder as Record2Service.LocalBinder).getService()

        assertNotNull(service)

        service.onCreate()
        assertEquals(44100, service.getSampleRate())
        assertEquals(200, service.getChunkSizeMs())
        assertEquals(44100 * 200 / 1000, service.getSampleFor200msInShort())
        assertEquals(3 * 1024 * 1024, service.getMaxFileSize())

        val settings = SettingsUtil(ApplicationProvider.getApplicationContext())
        settings.setSampleRate(48000)
        settings.setChunkSizeMs(100)
        settings.setMaxFileSizeInMB(5)

        service.onCreate()
        assertEquals(48000, service.getSampleRate())
        assertEquals(100, service.getChunkSizeMs())
        assertEquals(48000 * 100 / 1000, service.getSampleFor200msInShort())
        assertEquals(5 * 1024 * 1024, service.getMaxFileSize())
    }
}