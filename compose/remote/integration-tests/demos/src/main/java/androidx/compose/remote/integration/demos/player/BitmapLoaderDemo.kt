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

package androidx.compose.remote.integration.demos.player

import android.graphics.Bitmap
import android.graphics.Color
import androidx.collection.intListOf
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.layout.RemoteImage
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.size
import androidx.compose.remote.creation.compose.state.rdp
import androidx.compose.remote.creation.compose.state.rememberNamedRemoteBitmap
import androidx.compose.remote.creation.compose.state.rs
import androidx.compose.remote.integration.demos.common.RemoteDemo
import androidx.compose.remote.player.core.platform.BitmapLoader
import androidx.compose.runtime.Composable
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

@Suppress("RestrictedApiAndroidX")
class SolidColorBitmapLoader : BitmapLoader {
    override fun loadBitmap(url: String): InputStream {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val color =
            try {
                if (url.startsWith(PREFIX)) {
                    val hex = url.removePrefix(PREFIX)
                    Color.parseColor("#$hex")
                } else {
                    Color.GRAY
                }
            } catch (e: Exception) {
                Color.GRAY
            }

        bitmap.eraseColor(color)

        val bos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, bos)
        return ByteArrayInputStream(bos.toByteArray())
    }

    companion object {
        const val PREFIX = "color://"
    }
}

@Suppress("RestrictedApiAndroidX")
@Composable
fun BitmapLoaderDemo() {
    val colors = intListOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA)
    RemoteDemo(bitmapLoader = SolidColorBitmapLoader()) {
        RemoteColumn {
            colors.forEach { color ->
                val hex = String.format("%06X", 0xFFFFFF and color)
                val bitmap =
                    rememberNamedRemoteBitmap(
                        name = "color_$hex",
                        url = "${SolidColorBitmapLoader.PREFIX}$hex",
                    )
                RemoteImage(
                    remoteBitmap = bitmap,
                    contentDescription = "Color $hex".rs,
                    modifier = RemoteModifier.size(100.rdp, 100.rdp),
                )
            }
        }
    }
}
