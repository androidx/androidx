/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.cash.paparazzi

import app.cash.paparazzi.SnapshotHandler.FrameHandler
import app.cash.paparazzi.internal.PaparazziJson
import com.google.common.base.CharMatcher
import java.awt.image.BufferedImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.imageio.ImageIO
import okio.BufferedSink
import okio.HashingSink
import okio.blackholeSink
import okio.buffer
import okio.sink
import okio.source
import org.jcodec.api.awt.AWTSequenceEncoder

/**
 * Creates an HTML report that avoids writing files that have already been written.
 *
 * Images and videos are named by hashes of their contents. Paparazzi won't write two images or videos with the same
 * contents. Note that the images/ directory includes the individual frames of each video.
 *
 * Runs are named by their date.
 *
 * ```
 * images
 *   088c60580f06efa95c37fd8e754074729ee74a06.png
 *   93f9a81cb594280f4b3898d90dfad8c8ea969b01.png
 *   22d37abd0841ba2a8d0bd635954baf7cbfaa269b.png
 *   a4769e43cc5901ef28c0d46c46a44ea6429cbccc.png
 * videos
 *   d1cddc5da2224053f2af51f4e69a76de4e61fc41.mov
 * runs
 *   20190626002322_b9854e.js
 *   20190626002345_b1e882.js
 * index.html
 * index.js
 * paparazzi.js
 * ```
 */
class HtmlReportWriter @JvmOverloads constructor(
  private val runName: String = defaultRunName(),
  private val rootDirectory: File = File("build/reports/paparazzi"),
  snapshotRootDirectory: File = File("src/test/snapshots")
) : SnapshotHandler {
  private val runsDirectory: File = File(rootDirectory, "runs")
  private val imagesDirectory: File = File(rootDirectory, "images")
  private val videosDirectory: File = File(rootDirectory, "videos")

  private val goldenImagesDirectory = File(snapshotRootDirectory, "images")
  private val goldenVideosDirectory = File(snapshotRootDirectory, "videos")

  private val shots = mutableListOf<Snapshot>()

  private val isRecording: Boolean =
    System.getProperty("paparazzi.test.record")?.toBoolean() == true

  init {
    runsDirectory.mkdirs()
    imagesDirectory.mkdirs()
    videosDirectory.mkdirs()
    writeStaticFiles()
    writeRunJs()
    writeIndexJs()
  }

  override fun newFrameHandler(
    snapshot: Snapshot,
    frameCount: Int,
    fps: Int
  ): FrameHandler {
    return object : FrameHandler {
      val hashes = mutableListOf<String>()

      override fun handle(image: BufferedImage) {
        hashes += writeImage(image)
      }

      override fun close() {
        if (hashes.isEmpty()) return

        val shot = if (hashes.size == 1) {
          val original = File(imagesDirectory, "${hashes[0]}.png")
          if (isRecording) {
            val goldenFile = File(goldenImagesDirectory, snapshot.toFileName("_", "png"))
            original.copyTo(goldenFile, overwrite = true)
          }
          snapshot.copy(file = original.toJsonPath())
        } else {
          val hash = writeVideo(hashes, fps)

          if (isRecording) {
            for ((index, frameHash) in hashes.withIndex()) {
              val originalFrame = File(imagesDirectory, "$frameHash.png")
              val frameSnapshot = snapshot.copy(name = "${snapshot.name} $index")
              val goldenFile = File(goldenImagesDirectory, frameSnapshot.toFileName("_", "png"))
              if (!goldenFile.exists()) {
                originalFrame.copyTo(goldenFile)
              }
            }
          }
          val original = File(videosDirectory, "$hash.mov")
          if (isRecording) {
            val goldenFile = File(goldenVideosDirectory, snapshot.toFileName("_", "mov"))
            if (!goldenFile.exists()) {
              original.copyTo(goldenFile)
            }
          }
          snapshot.copy(file = original.toJsonPath())
        }

        shots += shot
      }
    }
  }

  /** Returns the hash of the image. */
  private fun writeImage(image: BufferedImage): String {
    val hash = hash(image)
    val file = File(imagesDirectory, "$hash.png")
    if (!file.exists()) {
      file.writeAtomically(image)
    }
    return hash
  }

  /** Returns a SHA-1 hash of the pixels of [image]. */
  private fun hash(image: BufferedImage): String {
    val hashingSink = HashingSink.sha1(blackholeSink())
    hashingSink.buffer().use { sink ->
      for (y in 0 until image.height) {
        for (x in 0 until image.width) {
          sink.writeInt(image.getRGB(x, y))
        }
      }
    }
    return hashingSink.hash.hex()
  }

  private fun writeVideo(
    frameHashes: List<String>,
    fps: Int
  ): String {
    val hash = hash(frameHashes)
    val file = File(videosDirectory, "$hash.mov")
    if (!file.exists()) {
      val tmpFile = File(videosDirectory, "$hash.mov.tmp")
      val encoder = AWTSequenceEncoder.createSequenceEncoder(tmpFile, fps)
      for (frameHash in frameHashes) {
        val frame = ImageIO.read(File(imagesDirectory, "$frameHash.png"))
        encoder.encodeImage(frame)
      }
      encoder.finish()
      tmpFile.renameTo(file)
    }
    return hash
  }

  /** Returns a SHA-1 hash of [lines]. */
  private fun hash(lines: List<String>): String {
    val hashingSink = HashingSink.sha1(blackholeSink())
    hashingSink.buffer().use { sink ->
      for (hash in lines) {
        sink.writeUtf8(hash)
        sink.writeUtf8("\n")
      }
    }
    return hashingSink.hash.hex()
  }

  /** Release all resources and block until everything has been written to the file system. */
  override fun close() {
    writeRunJs()
  }

  /**
   * Emits the all runs index, which reads like JSON with an executable header.
   *
   * ```
   * window.all_runs = [
   *   "20190319153912aaab",
   *   "20190319153917bcfe"
   * ];
   * ```
   */
  private fun writeIndexJs() {
    val runNames = mutableListOf<String>()
    val runs = runsDirectory.list().sorted()
    for (run in runs) {
      if (run.endsWith(".js")) {
        runNames += run.substring(0, run.length - 3)
      }
    }

    File(rootDirectory, "index.js").writeAtomically {
      writeUtf8("window.all_runs = ")
      PaparazziJson.listOfStringsAdapter.toJson(this, runNames)
      writeUtf8(";")
    }
  }

  /**
   * Emits a run index, which reads like JSON with an executable header.
   *
   * ```
   * window.runs["20190319153912aaab"] = [
   *   {
   *     "name": "loading",
   *     "testName": "app.cash.CelebrityTest#testSettings",
   *     "timestamp": "2019-03-20T10:27:43Z",
   *     "tags": ["redesign"],
   *     "file": "loading.png"
   *   },
   *   {
   *     "name": "error",
   *     "testName": "app.cash.CelebrityTest#testSettings",
   *     "timestamp": "2019-03-20T10:27:43Z",
   *     "tags": ["redesign"],
   *     "file": "error.png"
   *   }
   * ];
   * ```
   */
  private fun writeRunJs() {
    val runJs = File(runsDirectory, "${runName.sanitizeForFilename()}.js")
    runJs.writeAtomically {
      writeUtf8("window.runs[\"$runName\"] = ")
      PaparazziJson.listOfShotsAdapter.toJson(this, shots)
      writeUtf8(";")
    }
  }

  private fun writeStaticFiles() {
    for (staticFile in listOf("index.html", "paparazzi.js")) {
      File(rootDirectory, staticFile).writeAtomically {
        writeAll(HtmlReportWriter::class.java.classLoader.getResourceAsStream(staticFile).source())
      }
    }
  }

  private fun File.writeAtomically(bufferedImage: BufferedImage) {
    val tmpFile = File(parentFile, "$name.tmp")
    ImageIO.write(bufferedImage, "PNG", tmpFile)
    delete()
    tmpFile.renameTo(this)
  }

  private fun File.writeAtomically(writerAction: BufferedSink.() -> Unit) {
    val tmpFile = File(parentFile, "$name.tmp")
    tmpFile.sink()
      .buffer()
      .use { sink ->
        sink.writerAction()
      }
    delete()
    tmpFile.renameTo(this)
  }

  private fun File.toJsonPath(): String = relativeTo(rootDirectory).invariantSeparatorsPath
}

internal fun defaultRunName(): String {
  val now = Date()
  val timestamp = SimpleDateFormat("yyyyMMddHHmmss", Locale.US).format(now)
  val token = UUID.randomUUID().toString().substring(0, 6)
  return "${timestamp}_$token"
}

internal val filenameSafeChars = CharMatcher.inRange('a', 'z')
  .or(CharMatcher.inRange('0', '9'))
  .or(CharMatcher.anyOf("_-.~@^()[]{}:;,"))

internal fun String.sanitizeForFilename(): String? {
  return filenameSafeChars.negate().replaceFrom(lowercase(Locale.US), '_')
}
