package net.weissschuh.gpxbridge

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.util.Log
import android.widget.Toast
import java.io.File
import java.net.URL
import java.net.URLConnection
import javax.mail.internet.ContentDisposition
import javax.net.ssl.HttpsURLConnection

open class OABridge : Activity() {
    val TAG = OABridge::class.java.simpleName
    var sentIntent : Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tourUri = intent.data

        if (tourUri == null || tourUri.scheme != "outdooractivecommon" || tourUri.host != "tour") {
            return
        }

        val lastPathSegment = tourUri.lastPathSegment

        if (lastPathSegment == null) {
            return
        }

        val tourId = Integer.valueOf(lastPathSegment)
        val gpxDir = File(cacheDir, "gpx")
        gpxDir.mkdirs()
        Downloader(gpxDir).execute(tourId)

    }

    fun sendIntent(content: File) {
        val intent = Intent();
        intent.action = Intent.ACTION_VIEW
        intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION;

        intent.setDataAndType(
                FileProvider.getUriForFile(this, "net.weissschuh.gpxbridge.fileprovider", content),
                "application/gpx+xml"
        )

        Log.d(TAG, "Sending intent ${intent}")
        sentIntent = intent

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent)
        }
        finish()

    }

    @SuppressLint("StaticFieldLeak")
    inner class Downloader(val gpxDir: File) : AsyncTask<Int, Void, File>() {
        private fun warn(text: String) {
            Log.w(TAG, text)
            showToast(text, Toast.LENGTH_LONG)
        }
        private fun showToast(text: String, duration: Int = Toast.LENGTH_SHORT) {
            runOnUiThread {
                val toast = Toast.makeText(applicationContext, text, duration)
                toast.show()
            }
        }

        override fun doInBackground(vararg params: Int?): File? {
            params.forEach { tourId ->
                val uri = Uri.Builder()
                        .scheme("https")
                        .authority("www.outdooractive.com")
                        .path("download.tour.gpx")
                        .appendQueryParameter("i", tourId.toString())
                        .build()

                val path = createTempDir("gpxpath-", "-" + tourId.toString(), gpxDir)

                val url = URL(uri.toString())
                Log.d(TAG, "Retrieving ${uri}")
                val conn = url.openConnection() as HttpsURLConnection
                conn.connect()

                Log.d(TAG, "Got ${conn.responseCode}")

                if (conn.responseCode != 200) {
                    warn("Unexpected response code ${conn.responseCode}, aborting")
                }

                val filename = extractFileName(conn) ?: tourId.toString() + ".gpx"

                val output = File(path, filename)
                Log.d(TAG, "writing to ${output}")

                conn.getInputStream().use { input ->
                    output.outputStream().use {
                        input.copyTo(it)
                    }
                }

                Log.d(TAG,"Content (20 bytes): ${output.readText().subSequence(0, 20)}")

                return output
            }

            return null
        }

        private fun extractFileName(conn: URLConnection) : String? {
            val conDis = conn.getHeaderField("Content-Disposition") ?: return null
            return ContentDisposition(conDis).getParameter("filename")
        }

        override fun onPostExecute(result: File?) {
            super.onPostExecute(result)

            result ?: return

            sendIntent(result)
        }
    }
}
