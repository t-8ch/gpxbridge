package net.weissschuh.gpxbridge

import android.content.Intent
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest= Config.NONE)
class XMLBridgeTest {

    @Test
    fun textXMLBridge() {
        val intent = Intent()
        Assert.assertNotSame("application/gpx+xml", intent.type)
        val activity = Robolectric.buildActivity(XMLBridge::class.java, intent).create().get()
        Assert.assertEquals("application/gpx+xml", activity.intent.type)
    }
}
