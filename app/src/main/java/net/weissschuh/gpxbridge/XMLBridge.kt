package net.weissschuh.gpxbridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log


class XMLBridge : Activity() {
    val TAG = XMLBridge::class.java.simpleName

    // FIXME put logic in onStart?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent

        intent.type = "application/gpx+xml"
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;

        Log.d(TAG, "Sending intent ${intent}")
        intent.toString()

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
        finish()
    }
}
