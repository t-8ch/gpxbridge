package net.weissschuh.gpxbridge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.content.FileProvider
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowPausedAsyncTask
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

@Implements(FileProvider::class)
class FileProviderShadow {
    companion object {
        @JvmStatic
        @Implementation
        protected fun getUriForFile(context: Context, authority: String, file: File): Uri {
            return Uri.Builder()
                    .scheme("content")
                    .path("a/b/c/foo.gpx")
                    .build()
        }
    }
}

@Implements(OABridge.Downloader::class)
internal class DownloaderShadow : ShadowPausedAsyncTask<Int, OABridge.Downloader.Downloaded, Unit>() {
    @Implementation
    fun downloadTour(tourId: Int): Pair<String?, InputStream>? {
        assertEquals(1550935, tourId)
        return null to ByteArrayInputStream("".toByteArray(StandardCharsets.UTF_8))
    }

    @Implementation
    fun shouldSendIntent(intent: Intent): Boolean {
        return true
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE,
        shadows = [FileProviderShadow::class, DownloaderShadow::class])
class OABridgeTest {

    @Test
    fun textXMLBridge() {
        val intent = Intent()
        intent.data = Uri.Builder()
                .scheme("outdooractivecommon")
                .authority("tour")
                .path("1550935")
                .build()
        assertNotSame("application/gpx+xml", intent.type)
        val activity = Robolectric.buildActivity(OABridge::class.java, intent).setup().get()
        val sentIntent = Shadows.shadowOf(activity.application).nextStartedActivity
        assertNotNull(sentIntent)
        sentIntent!!

        assertEquals("application/gpx+xml", sentIntent.type)
        sentIntent.data!!.apply {
            assertEquals("content", scheme)
            assertEquals("foo.gpx", lastPathSegment)
        }
    }
}
