/*
 * Copyright (C) 2016 The Android Open Source Project
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

package app.cash.paparazzi.internal

import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Graphics2D
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.KEY_INTERPOLATION
import java.awt.RenderingHints.KEY_RENDERING
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON
import java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR
import java.awt.RenderingHints.VALUE_RENDER_QUALITY
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.io.File.separatorChar
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.math.max
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail

/**
 * Utilities related to image processing.
 */
internal object ImageUtils {
  /**
   * Normally, this test will fail when there is a missing thumbnail. However, when
   * you create creating a new test, it's useful to be able to turn this off such that
   * you can generate all the missing thumbnails in one go, rather than having to run
   * the test repeatedly to get to each new render assertion generating its thumbnail.
   */
  private val FAIL_ON_MISSING_THUMBNAIL = true

  private const val THUMBNAIL_SIZE = 1000

  /** Directory where to write the thumbnails and deltas. */
  private val failureDir: File
    get() {
      val workingDirString = System.getProperty("user.dir")
      val failureDir = File(workingDirString, "out/failures")
      failureDir.mkdirs()
      return failureDir
    }

  @Throws(IOException::class)
  fun requireSimilar(
    relativePath: String,
    image: BufferedImage,
    maxPercentDifference: Double
  ) {
    val scale = getThumbnailScale(image)
    val thumbnail = scale(image, scale, scale)

    val `is` = ImageUtils::class.java.classLoader.getResourceAsStream(relativePath)
    if (`is` ==
      null
    ) {
      var message = "Unable to load golden thumbnail: $relativePath\n"
      message = saveImageAndAppendMessage(thumbnail, message, relativePath)
      if (FAIL_ON_MISSING_THUMBNAIL) {
        fail(message)
      } else {
        println(message)
      }
    } else {
      try {
        val goldenImage = ImageIO.read(`is`)
        assertImageSimilar(
          relativePath,
          goldenImage,
          thumbnail,
          maxPercentDifference
        )
      } finally {
        `is`.close()
      }
    }
  }

  @Throws(IOException::class)
  fun assertImageSimilar(
    relativePath: String,
    goldenImage: BufferedImage,
    image: BufferedImage,
    maxPercentDifferent: Double
  ) {
    @Suppress("NAME_SHADOWING") var goldenImage = goldenImage
    if (goldenImage.type != TYPE_INT_ARGB) {
      val temp = BufferedImage(
        goldenImage.width,
        goldenImage.height,
        TYPE_INT_ARGB
      )
      temp.graphics.drawImage(goldenImage, 0, 0, null)
      goldenImage = temp
    }
    assertEquals(TYPE_INT_ARGB.toLong(), goldenImage.type.toLong())

    val imageWidth = Math.min(goldenImage.width, image.width)
    val imageHeight = Math.min(goldenImage.height, image.height)

    // Blur the images to account for the scenarios where there are pixel
    // differences
    // in where a sharp edge occurs
    // goldenImage = blur(goldenImage, 6);
    // image = blur(image, 6);

    val width = 3 * imageWidth
    val deltaImage = BufferedImage(width, imageHeight, TYPE_INT_ARGB)
    val g = deltaImage.graphics

    // Compute delta map
    var delta: Long = 0
    for (y in 0 until imageHeight) {
      for (x in 0 until imageWidth) {
        val goldenRgb = goldenImage.getRGB(x, y)
        val rgb = image.getRGB(x, y)
        if (goldenRgb == rgb) {
          deltaImage.setRGB(imageWidth + x, y, 0x00808080)
          continue
        }

        // If the pixels have no opacity, don't delta colors at all
        if (goldenRgb and -0x1000000 == 0 && rgb and -0x1000000 == 0) {
          deltaImage.setRGB(imageWidth + x, y, 0x00808080)
          continue
        }

        val deltaR = (rgb and 0xFF0000).ushr(16) - (goldenRgb and 0xFF0000).ushr(16)
        val newR = 128 + deltaR and 0xFF
        val deltaG = (rgb and 0x00FF00).ushr(8) - (goldenRgb and 0x00FF00).ushr(8)
        val newG = 128 + deltaG and 0xFF
        val deltaB = (rgb and 0x0000FF) - (goldenRgb and 0x0000FF)
        val newB = 128 + deltaB and 0xFF

        val avgAlpha =
          ((goldenRgb and -0x1000000).ushr(24) + (rgb and -0x1000000).ushr(24)) / 2 shl 24

        val newRGB = avgAlpha or (newR shl 16) or (newG shl 8) or newB
        deltaImage.setRGB(imageWidth + x, y, newRGB)

        delta += Math.abs(deltaR)
          .toLong()
        delta += Math.abs(deltaG)
          .toLong()
        delta += Math.abs(deltaB)
          .toLong()
      }
    }

    // 3 different colors, 256 color levels
    val total = imageHeight.toLong() * imageWidth.toLong() * 3L * 256L
    val percentDifference = (delta * 100 / total.toDouble()).toFloat()

    var error: String? = null
    val imageName = getName(relativePath)
    if (percentDifference > maxPercentDifferent) {
      error = String.format("Images differ (by %.1f%%)", percentDifference)
    } else if (Math.abs(goldenImage.width - image.width) >= 2) {
      error = "Widths differ too much for " + imageName + ": " +
        goldenImage.width + "x" + goldenImage.height +
        "vs" + image.width + "x" + image.height
    } else if (Math.abs(goldenImage.height - image.height) >= 2) {
      error = "Heights differ too much for " + imageName + ": " +
        goldenImage.width + "x" + goldenImage.height +
        "vs" + image.width + "x" + image.height
    }

    if (error != null) {
      // Expected on the left
      // Golden on the right
      g.drawImage(goldenImage, 0, 0, null)
      g.drawImage(image, 2 * imageWidth, 0, null)

      // Labels
      if (imageWidth > 80) {
        g.color = Color.RED
        g.drawString("Expected", 10, 20)
        g.drawString("Actual", 2 * imageWidth + 10, 20)
      }

      val output = File(failureDir, "delta-$imageName")
      if (output.exists()) {
        val deleted = output.delete()
        assertTrue(deleted)
      }
      ImageIO.write(deltaImage, "PNG", output)
      error += " - see details in file://" + output.path + "\n"
      error = saveImageAndAppendMessage(image, error, relativePath)
      println(error)
      fail(error)
    }

    g.dispose()
  }

  /**
   * Resize the given image
   *
   * @param source the image to be scaled
   * @param xScale x scale
   * @param yScale y scale
   * @return the scaled image
   */
  fun scale(
    source: BufferedImage,
    xScale: Double,
    yScale: Double
  ): BufferedImage {
    @Suppress("NAME_SHADOWING") var source = source

    var sourceWidth = source.width
    var sourceHeight = source.height
    val destWidth = Math.max(1, (xScale * sourceWidth).toInt())
    val destHeight = Math.max(1, (yScale * sourceHeight).toInt())
    var imageType = source.type
    if (imageType == BufferedImage.TYPE_CUSTOM) {
      imageType = BufferedImage.TYPE_INT_ARGB
    }
    if (xScale > 0.5 && yScale > 0.5) {
      val scaled = BufferedImage(destWidth, destHeight, imageType)
      val g2 = scaled.createGraphics()
      g2.composite = AlphaComposite.Src
      g2.color = Color(0, true)
      g2.fillRect(0, 0, destWidth, destHeight)
      if (xScale == 1.0 && yScale == 1.0) {
        g2.drawImage(source, 0, 0, null)
      } else {
        setRenderingHints(g2)
        g2.drawImage(source, 0, 0, destWidth, destHeight, 0, 0, sourceWidth, sourceHeight, null)
      }
      g2.dispose()
      return scaled
    } else {
      // When creating a thumbnail, using the above code doesn't work very well;
      // you get some visible artifacts, especially for text. Instead use the
      // technique of repeatedly scaling the image into half; this will cause
      // proper averaging of neighboring pixels, and will typically (for the kinds
      // of screen sizes used by this utility method in the layout editor) take
      // about 3-4 iterations to get the result since we are logarithmically reducing
      // the size. Besides, each successive pass in operating on much fewer pixels
      // (a reduction of 4 in each pass).
      //
      // However, we may not be resizing to a size that can be reached exactly by
      // successively diving in half. Therefore, once we're within a factor of 2 of
      // the final size, we can do a resize to the exact target size.
      // However, we can get even better results if we perform this final resize
      // up front. Let's say we're going from width 1000 to a destination width of 85.
      // The first approach would cause a resize from 1000 to 500 to 250 to 125, and
      // then a resize from 125 to 85. That last resize can distort/blur a lot.
      // Instead, we can start with the destination width, 85, and double it
      // successfully until we're close to the initial size: 85, then 170,
      // then 340, and finally 680. (The next one, 1360, is larger than 1000).
      // So, now we *start* the thumbnail operation by resizing from width 1000 to
      // width 680, which will preserve a lot of visual details such as text.
      // Then we can successively resize the image in half, 680 to 340 to 170 to 85.
      // We end up with the expected final size, but we've been doing an exact
      // divide-in-half resizing operation at the end so there is less distortion.

      var iterations = 0 // Number of halving operations to perform after the initial resize
      var nearestWidth = destWidth // Width closest to source width that = 2^x, x is integer
      var nearestHeight = destHeight
      while (nearestWidth < sourceWidth / 2) {
        nearestWidth *= 2
        nearestHeight *= 2
        iterations++
      }

      var scaled = BufferedImage(nearestWidth, nearestHeight, imageType)

      var g2 = scaled.createGraphics()
      setRenderingHints(g2)
      g2.drawImage(source, 0, 0, nearestWidth, nearestHeight, 0, 0, sourceWidth, sourceHeight, null)
      g2.dispose()

      sourceWidth = nearestWidth
      sourceHeight = nearestHeight
      source = scaled

      for (iteration in iterations - 1 downTo 0) {
        val halfWidth = sourceWidth / 2
        val halfHeight = sourceHeight / 2
        scaled = BufferedImage(halfWidth, halfHeight, imageType)
        g2 = scaled.createGraphics()
        setRenderingHints(g2)
        g2.drawImage(source, 0, 0, halfWidth, halfHeight, 0, 0, sourceWidth, sourceHeight, null)
        g2.dispose()

        sourceWidth = halfWidth
        sourceHeight = halfHeight
        source = scaled
        iterations--
      }
      return scaled
    }
  }

  fun getThumbnailScale(image: BufferedImage): Double {
    val maxDimension = max(image.width, image.height)
    return THUMBNAIL_SIZE / maxDimension.toDouble()
  }

  private fun setRenderingHints(g2: Graphics2D) {
    g2.setRenderingHint(KEY_INTERPOLATION, VALUE_INTERPOLATION_BILINEAR)
    g2.setRenderingHint(KEY_RENDERING, VALUE_RENDER_QUALITY)
    g2.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
  }

  /**
   * Saves the generated thumbnail image and appends the info message to an initial message
   */
  @Throws(IOException::class)
  private fun saveImageAndAppendMessage(
    image: BufferedImage,
    initialMessage: String,
    relativePath: String
  ): String {
    @Suppress("NAME_SHADOWING") var initialMessage = initialMessage
    val output = File(
      failureDir,
      getName(relativePath)
    )
    if (output.exists()) {
      val deleted = output.delete()
      assertTrue(deleted)
    }
    ImageIO.write(image, "PNG", output)
    initialMessage += "Thumbnail for current rendering stored at file://" + output.path
    //        initialMessage += "\nRun the following command to accept the changes:\n";
    //        initialMessage += String.format("mv %1$s %2$s", output.getPath(),
    //                ImageUtils.class.getResource(relativePath).getPath());
    // The above has been commented out, since the destination path returned is in out dir
    // and it makes the tests pass without the code being actually checked in.
    return initialMessage
  }

  private fun getName(relativePath: String): String {
    return relativePath.substring(relativePath.lastIndexOf(separatorChar) + 1)
  }
}
