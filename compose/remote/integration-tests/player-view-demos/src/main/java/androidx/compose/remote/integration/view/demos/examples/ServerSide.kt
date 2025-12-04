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

package androidx.compose.remote.integration.view.demos.examples

import androidx.compose.remote.core.RcPlatformServices
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.ComponentHeight
import androidx.compose.remote.creation.ComponentWidth
import androidx.compose.remote.creation.ContinuousSec
import androidx.compose.remote.creation.Hour
import androidx.compose.remote.creation.Minutes
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.cos
import androidx.compose.remote.creation.min
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.sin
import androidx.compose.remote.tooling.preview.RemoteDocPreview
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

/** This is a demo of a server side clock that does not depend Android */
val BLUE: Int = 0xFF0000FFL.toInt()
val GRAY: Int = 0xff888888.toInt()
val LTGRAY: Int = 0xffcccccc.toInt()
val WHITE: Int = 0xffffffff.toInt()
val CAP_ROUND = 1

@Suppress("RestrictedApiAndroidX")
val platform =
    object : RcPlatformServices {
        @Suppress("RestrictedApiAndroidX")
        override fun imageToByteArray(image: Any): ByteArray? {
            /** Convert to PNG byte array */
            TODO("Not yet implemented")
        }

        @Suppress("RestrictedApiAndroidX")
        override fun getImageWidth(image: Any): Int {
            /** get the image width */
            TODO("Not yet implemented")
        }

        @Suppress("RestrictedApiAndroidX")
        override fun getImageHeight(image: Any): Int {
            /** get the image height */
            TODO("Not yet implemented")
        }

        @Suppress("RestrictedApiAndroidX")
        override fun isAlpha8Image(image: Any): Boolean {
            /** check if the image is alpha8 */
            TODO("Not yet implemented")
        }

        @Suppress("RestrictedApiAndroidX")
        override fun pathToFloatArray(path: Any): FloatArray? {
            /** Convert to float array */
            TODO("Not yet implemented")
        }

        @Suppress("RestrictedApiAndroidX")
        override fun parsePath(pathData: String): Any {
            /** parse path dat */
            TODO("Not yet implemented")
        }

        @Suppress("RestrictedApiAndroidX")
        override fun log(category: RcPlatformServices.LogCategory, message: String) {
            TODO("Not yet implemented")
        }
    }

@Suppress("RestrictedApiAndroidX")
fun serverClock(): RemoteComposeWriter {

    val rc = RemoteComposeWriter(500, 500, "Clock", 6, 0, platform)

    with(rc) {
        root {
            box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.START) {
                canvas(RecordingModifier().fillMaxSize()) {
                    val w = ComponentWidth() // component.width()
                    val h = ComponentHeight()
                    val cx = w / 2f
                    val cy = h / 2f
                    val rad = min(cx, cy)
                    val rounding = rad / 4f
                    rcPaint.setColor(BLUE).commit()

                    drawRoundRect(
                        0f,
                        0f,
                        w.toFloat(),
                        h.toFloat(),
                        rounding.toFloat(),
                        rounding.toFloat(),
                    )

                    rcPaint.setColor(GRAY).setStrokeWidth(32f).setStrokeCap(CAP_ROUND).commit()
                    save()
                    rotate((Minutes() * 6f).toFloat(), cx.toFloat(), cy.toFloat())
                    drawLine(cx.toFloat(), cy.toFloat(), cx.toFloat(), (cy - rad * 0.8f).toFloat())
                    restore()
                    rcPaint.setColor(LTGRAY).setStrokeWidth(16f).setStrokeCap(CAP_ROUND).commit()
                    save()
                    rotate((Hour() * 30f).toFloat(), cx.toFloat(), cy.toFloat())
                    drawLine(cx.toFloat(), cy.toFloat(), cx.toFloat(), (cy - rad / 2f).toFloat())
                    restore()

                    rcPaint.setColor(WHITE).setStrokeWidth(4f).commit()
                    drawLine(
                        cx.toFloat(),
                        cy.toFloat(),
                        (w / 2f + rad * sin(ContinuousSec() * (2 * Math.PI.toFloat() / 60f)))
                            .toFloat(),
                        (h / 2f - rad * cos(ContinuousSec() * (2 * Math.PI.toFloat() / 60f)))
                            .toFloat(),
                    )
                }
            }
        }
    }
    return rc
}

@Preview @Composable fun ServerClockPreview() = RemoteDocPreview(serverClock())
