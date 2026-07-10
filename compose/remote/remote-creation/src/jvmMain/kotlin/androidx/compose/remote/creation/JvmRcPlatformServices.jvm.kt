/*
 * Copyright 2025 The Android Open Source Project
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
package androidx.compose.remote.creation

import androidx.compose.remote.core.RcPlatformServices
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

/** Implementation of [RcPlatformServices] intended for multi-platform use on the JVM. */
public class JvmRcPlatformServices : RcPlatformServices {
    @Suppress("NullableCollection")
    override fun imageToByteArray(image: Any): ByteArray? {
        if (image is BufferedImage) {
            val baos = ByteArrayOutputStream()
            // RemoteComposeBuffer assumes the image is a PNG,
            // but other file formats also work in the player.
            // PNG is a safe lossless default
            if (ImageIO.write(image, "png", baos)) {
                return baos.toByteArray()
            }
        }
        return null
    }

    override fun getImageWidth(image: Any): Int {
        if (image is BufferedImage) {
            return image.width
        }
        return 0
    }

    override fun getImageHeight(image: Any): Int {
        if (image is BufferedImage) {
            return image.height
        }
        return 0
    }

    override fun isAlpha8Image(image: Any): Boolean {
        if (image is BufferedImage) {
            // This maps to TYPE_PNG_ALPHA_8
            return image.type == BufferedImage.TYPE_BYTE_GRAY
        }
        return false
    }

    @Suppress("NullableCollection")
    override fun pathToFloatArray(path: Any): FloatArray? {
        if (path is RemotePath) {
            return path.createFloatArray()
        }
        return null
    }

    override fun parsePath(pathData: String): Any {
        return RemotePath(pathData)
    }

    override fun log(category: RcPlatformServices.LogCategory, message: String) {}
}
