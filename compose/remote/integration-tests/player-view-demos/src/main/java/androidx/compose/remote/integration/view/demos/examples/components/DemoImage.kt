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

package androidx.compose.remote.integration.view.demos.examples.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.compose.remote.creation.RemoteComposeContextAndroid
import androidx.compose.remote.creation.RemoteComposeWriter

/**
 * Atomic demo for the Image component. Demonstrates rendering a bitmap with a specific scaling
 * mode.
 */
@Suppress("RestrictedApiAndroidX")
fun DemoImage(): RemoteComposeWriter {
    // Create dummy bitmap
    val bitmap =
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply {
            Canvas(this).drawCircle(50f, 50f, 40f, Paint().apply { color = Color.RED })
        }

    return RemoteComposeContextAndroid(400, 400, "DemoImage") {
            root {
                val imageId = addBitmap(bitmap)
                // imageId, scaleMode (1=INSIDE), alpha
                image(Modifier.size(200).background(Color.LTGRAY), imageId, 1, 1f)
            }
        }
        .writer
}
