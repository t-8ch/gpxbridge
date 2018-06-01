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
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.shadows.ShadowAsyncTask
import java.io.File

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
class DownloaderShadow() : ShadowAsyncTask<Int, Void, File>() {

    @Implementation
    fun doInBackground(vararg params: Int?): File? {
        assertEquals(1, params.size)
        assertEquals(1550935, params[0])
        return File("/foo/1550935.gpx")
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE,
        shadows=arrayOf(FileProviderShadow::class, DownloaderShadow::class))
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
        val activity = Robolectric.buildActivity(OABridge::class.java, intent).create().get()
        val sentIntent = activity.sentIntent
        assertNotNull(sentIntent)
        sentIntent!!

        assertEquals("application/gpx+xml", sentIntent.type)
        sentIntent.data.apply {
            assertEquals("content", scheme)
            assertEquals("foo.gpx", lastPathSegment)
        }
    }
}
