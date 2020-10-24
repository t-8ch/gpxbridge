package net.weissschuh.gpxbridge

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.content.FileProvider
import android.util.Log
import java.io.File
import java.io.InputStream
import java.net.URL
import java.net.URLConnection
import javax.mail.internet.ContentDisposition
import javax.net.ssl.HttpsURLConnection

open class OABridge : Activity() {
    companion object

    val TAG = OABridge::class.java.simpleName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val tourUri = intent.data

        if (tourUri == null || tourUri.scheme != "outdooractivecommon" || tourUri.host != "tour") {
            return
        }

        val lastPathSegment = tourUri.lastPathSegment ?: return

        val tourId = Integer.valueOf(lastPathSegment)
        val gpxDir = File(cacheDir, "gpx")
        Downloader(gpxDir, application).execute(tourId)
        finish()
    }

    internal class Downloader(private val gpxDir: File, private val ctx: Context) : AsyncTask<Int, Downloader.Downloaded, Unit>() {
        data class Downloaded(val tourId: Int, val file: File)

        private companion object

        val TAG = Downloader::class.java.simpleName

        private fun downloadTour(tourId: Int): Pair<String?, InputStream>? {
            val uri = Uri.Builder()
                    .scheme("https")
                    .authority("www.outdooractive.com")
                    .path("download.tour.gpx")
                    .appendQueryParameter("i", tourId.toString())
                    .build()

            val url = URL(uri.toString())
            Log.d(TAG, "Retrieving $uri")
            val conn = url.openConnection() as HttpsURLConnection
            conn.connect()

            Log.d(TAG, "Got ${conn.responseCode}")

            if (conn.responseCode != 200) {
                Log.w(TAG, "Unexpected response code ${conn.responseCode}, aborting")
                return null
            }

            return extractFileName(conn) to conn.inputStream
        }

        override fun doInBackground(vararg params: Int?) {
            for (tourId in params) {
                if (tourId == null) {
                    continue
                }

                val downloaded = downloadTour(tourId) ?: continue
                val (filename, tourData) = downloaded
                gpxDir.mkdirs()
                val path = createTempDir("gpxpath-", "-$tourId", gpxDir)
                val output = File(path, filename ?: "$tourId.gpx")
                Log.d(TAG, "writing to $output")

                tourData.use { input ->
                    output.outputStream().use {
                        input.copyTo(it)
                    }
                }

                Log.d(TAG, "Content (20 bytes): ${output.readText().firstNCharacters(20)}")

                publishProgress(Downloaded(tourId, output))
            }
        }

        private fun extractFileName(conn: URLConnection): String? {
            val conDis = conn.getHeaderField("Content-Disposition") ?: return null
            return ContentDisposition(conDis).getParameter("filename")
        }

        override fun onProgressUpdate(vararg values: Downloaded?) {
            super.onProgressUpdate(*values)

            for (value in values) {
                if (value == null) {
                    continue
                }
                sendIntent(value.file)
            }
        }

        private fun sendIntent(content: File) {
            val intent = Intent().also {
                it.action = Intent.ACTION_VIEW
                it.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                it.setDataAndType(
                        FileProvider.getUriForFile(ctx, "net.weissschuh.gpxbridge.fileprovider", content),
                        "application/gpx+xml"
                )
            }

            if (shouldSendIntent(intent)) {
                Log.d(TAG, "Sending intent $intent")
                ctx.startActivity(intent)
            } else {
                Log.d(TAG, "*Not* Sending intent because nobody can handle it: $intent")
            }
        }

        private fun shouldSendIntent(intent: Intent): Boolean {
            return intent.resolveActivity(ctx.packageManager) != null
        }
    }
}

private fun String.firstNCharacters(n: Int): String {
    return this.substring(0, kotlin.math.min(n, this.length))
}