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

package androidx.compose.integration.macrobenchmark

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.Environment
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okio.Buffer
import okio.source
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/** An image served by [ImageMockServerRule] at `/image/<id>/<width>x<height>.png`. */
class ImageSpec(val id: Int, val width: Int, val height: Int)

/**
 * Serves generated PNG images, similar to the Pokedex hero benchmark's mock server. All [images]
 * are generated before the server starts and stored on disk, so they are reused across runs and
 * requests only cost IO while the benchmark runs. Requests for other images return 404.
 */
class ImageMockServerRule(private val images: List<ImageSpec>) : TestWatcher() {
    private val imagesDir =
        File(
            InstrumentationRegistry.getInstrumentation()
                .context
                .getExternalFilesDir(Environment.DIRECTORY_PICTURES)
                ?: error("Failed to get images folder"),
            "generated",
        )

    private val dispatcher = ImageDispatcher(imagesDir)
    private val server = MockWebServer().apply { dispatcher = this@ImageMockServerRule.dispatcher }

    /** Base URL of the server, ending with `/`. */
    val baseUrl: String
        get() = server.url("/").toString()

    /**
     * Blocks until all in-flight image requests have been served, then throws if any request since
     * the last call failed, so that measurements only run with every image loaded.
     */
    fun awaitImagesServed(timeout: Duration = DefaultAwaitTimeout) {
        runBlocking { withTimeout(timeout) { dispatcher.awaitPendingRequests() } }
        val failed = dispatcher.takeFailedRequests()
        check(failed.isEmpty()) { "Failed image requests: $failed" }
    }

    override fun starting(description: Description?) {
        imagesDir.mkdirs()
        images.forEach { createImageFile(imagesDir, it) }
        server.start()
        super.starting(description)
    }

    override fun finished(description: Description?) {
        super.finished(description)
        server.shutdown()
    }
}

private class ImageDispatcher(private val imagesDir: File) : Dispatcher() {
    private val imagePathRegex = Regex("/image/(\\d+)/(\\d+)x(\\d+)\\.png")
    private val activeRequests = MutableStateFlow(0)
    private val failedRequests = MutableStateFlow(emptyList<String>())

    override fun dispatch(request: RecordedRequest): MockResponse {
        activeRequests.update { it + 1 }
        val response =
            try {
                imageResponse(request.path)
            } catch (e: Exception) {
                MockResponse().setResponseCode(500).setBody(e.message ?: e.toString())
            }
        if (response.status.split(' ').getOrNull(1) != "200") {
            failedRequests.update { it + "${request.path}: ${response.status}" }
        }
        activeRequests.update { it - 1 }
        return response
    }

    private fun imageResponse(path: String?): MockResponse {
        val match =
            path?.let { imagePathRegex.matchEntire(it) }
                ?: return MockResponse().setResponseCode(404)
        val (id, width, height) = match.destructured
        val image = imageFile(imagesDir, id.toInt(), width.toInt(), height.toInt())
        if (!image.exists()) return MockResponse().setResponseCode(404)
        val body = Buffer().apply { image.source().use { writeAll(it) } }
        return MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "image/png")
            .setBody(body)
    }

    /** Suspends until no image request is in flight. */
    suspend fun awaitPendingRequests() {
        activeRequests.first { it == 0 }
    }

    fun takeFailedRequests(): List<String> = failedRequests.getAndUpdate { emptyList() }
}

private val DefaultAwaitTimeout = 5.seconds

private fun imageFile(imagesDir: File, id: Int, width: Int, height: Int): File =
    File(imagesDir, "${id}_${width}x$height.png")

/** Generates the image for [spec] unless a previous run already stored it. */
private fun createImageFile(imagesDir: File, spec: ImageSpec) {
    val file = imageFile(imagesDir, spec.id, spec.width, spec.height)
    if (file.exists()) return
    val temp = File(imagesDir, "${file.name}.tmp")
    temp.outputStream().use { output ->
        createImage(spec.id, spec.width, spec.height)
            .compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    temp.renameTo(file)
}

/** Creates a gradient image with a few circles, seeded by [id] so that every image differs. */
private fun createImage(id: Int, width: Int, height: Int): Bitmap {
    val random = Random(id)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    paint.shader =
        LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            randomOpaqueColor(random),
            randomOpaqueColor(random),
            Shader.TileMode.CLAMP,
        )
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
    paint.shader = null
    repeat(6) {
        paint.color = randomOpaqueColor(random)
        canvas.drawCircle(
            random.nextFloat() * width,
            random.nextFloat() * height,
            random.nextFloat() * width / 4 + 8f,
            paint,
        )
    }
    return bitmap
}

private fun randomOpaqueColor(random: Random): Int = random.nextInt() or 0xFF000000.toInt()
