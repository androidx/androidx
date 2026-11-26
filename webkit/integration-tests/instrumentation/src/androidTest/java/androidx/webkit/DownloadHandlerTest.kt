/*
 * Copyright 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.webkit

import android.net.Uri
import androidx.annotation.OptIn
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.webkit.test.common.WebViewOnUiThread
import androidx.webkit.test.common.WebkitUtils
import java.io.File
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

/**
 * Tests for [WebViewCompat.setDownloadHandler], [WebViewCompat.clearDownloadHandler] and
 * [WebDownload.saveTo].
 */
@MediumTest
@RunWith(AndroidJUnit4::class)
@OptIn(WebViewCompat.ExperimentalDownloadHandler::class)
class DownloadHandlerTest {
    @get:Rule val tempFolder = TemporaryFolder()

    private lateinit var webViewOnUiThread: WebViewOnUiThread
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        webViewOnUiThread = WebViewOnUiThread()
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        webViewOnUiThread.cleanUp()
        server.shutdown()
    }

    @Test
    fun setAndClearDownloadHandler_doesNotCrash() {
        WebkitUtils.checkFeature(WebViewFeature.DOWNLOAD_HANDLER_EXPERIMENTAL_V1)
        WebkitUtils.onMainThreadSync {
            val webView = webViewOnUiThread.webViewOnCurrentThread
            WebViewCompat.setDownloadHandler(webView) { _, download -> download.cancel() }
            WebViewCompat.setDownloadHandler(webView, Runnable::run) { _, download ->
                download.cancel()
            }
            WebViewCompat.clearDownloadHandler(webView)
        }
    }

    @Test
    fun saveTo_savesDownloadToFile() {
        WebkitUtils.checkFeature(WebViewFeature.DOWNLOAD_HANDLER_EXPERIMENTAL_V1)
        server.enqueue(
            MockResponse()
                .setBody(CONTENT)
                .addHeader("Content-Disposition", "attachment; filename=\"hello.txt\"")
        )
        val destination = File(tempFolder.root, "hello.txt")

        val savedSizeFuture =
            WebkitUtils.callbackToFuture<Long, DownloadException> { receiver ->
                WebkitUtils.onMainThreadSync {
                    val webView = webViewOnUiThread.webViewOnCurrentThread
                    WebViewCompat.setDownloadHandler(webView) { _, download ->
                        download.saveTo(Uri.fromFile(destination), Runnable::run, receiver)
                    }
                }
            }
        webViewOnUiThread.loadUrl(server.url("/hello.txt").toString())

        assertEquals(CONTENT.length.toLong(), WebkitUtils.waitForFuture(savedSizeFuture))
        assertEquals(CONTENT, destination.readText())
    }

    @Test
    fun webDownload_hasDownloadMetadata() {
        WebkitUtils.checkFeature(WebViewFeature.DOWNLOAD_HANDLER_EXPERIMENTAL_V1)
        val download = startDownload()

        assertEquals(server.url("/hello.txt").toString(), download.url)
        assertEquals("text/plain", download.mimeType)
        assertEquals(CONTENT.length.toLong(), download.contentLength)
        assertEquals("hello.txt", download.suggestedFilename)
        assertTrue(download.isExplicitDownload)
        assertTrue(download.isForMainFrame)
        assertFalse(download.hasGesture())
        WebkitUtils.onMainThreadSync { download.cancel() }
    }

    @Test
    fun saveTo_unwritableDestination_reportsError() {
        WebkitUtils.checkFeature(WebViewFeature.DOWNLOAD_HANDLER_EXPERIMENTAL_V1)
        val download = startDownload()
        // The destination can't be created, because its parent is a file.
        val destination = File(tempFolder.newFile(), "hello.txt")

        val savedSizeFuture =
            WebkitUtils.callbackToFuture<Long, DownloadException> { receiver ->
                WebkitUtils.onMainThreadSync {
                    download.saveTo(Uri.fromFile(destination), Runnable::run, receiver)
                }
            }

        // waitForFuture() wraps the checked DownloadException in a RuntimeException.
        val exception =
            assertThrows(RuntimeException::class.java) {
                WebkitUtils.waitForFuture(savedSizeFuture)
            }
        assertEquals(
            DownloadException.ERROR_DESTINATION_WRITE_FAILED,
            (exception.cause as DownloadException).errorCode,
        )
    }

    @Test
    fun setDownloadHandler_withExecutor_callsHandlerOnExecutor() {
        WebkitUtils.checkFeature(WebViewFeature.DOWNLOAD_HANDLER_EXPERIMENTAL_V1)
        val executorUsed = AtomicBoolean()
        val executor = Executor { task ->
            executorUsed.set(true)
            task.run()
        }

        val download = startDownload(executor)

        assertTrue(executorUsed.get())
        WebkitUtils.onMainThreadSync { download.cancel() }
    }

    /**
     * Loads a URL that starts a download, and returns the [WebDownload] passed to the handler,
     * which is called on [executor].
     */
    private fun startDownload(executor: Executor = Executor(Runnable::run)): WebDownload {
        server.enqueue(
            MockResponse()
                .setBody(CONTENT)
                .setHeader("Content-Length", CONTENT.length)
                .setHeader("Content-Type", "text/plain")
                .setHeader("Content-Disposition", "attachment; filename=\"hello.txt\"")
        )
        val downloadFuture =
            WebkitUtils.callbackToFuture<WebDownload, Throwable> { receiver ->
                WebkitUtils.onMainThreadSync {
                    val webView = webViewOnUiThread.webViewOnCurrentThread
                    WebViewCompat.setDownloadHandler(webView, executor) { _, download ->
                        receiver.onResult(download)
                    }
                }
            }
        webViewOnUiThread.loadUrl(server.url("/hello.txt").toString())
        return WebkitUtils.waitForFuture(downloadFuture)
    }

    private companion object {
        const val CONTENT = "Hello, WebView!"
    }
}
