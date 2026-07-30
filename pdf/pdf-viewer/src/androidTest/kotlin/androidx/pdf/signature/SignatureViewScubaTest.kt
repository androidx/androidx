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

package androidx.pdf.signature

import SCREENSHOT_GOLDEN_DIRECTORY
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.pdf.PdfTestActivity
import androidx.pdf.assertScreenshot
import androidx.pdf.signature.model.Signature
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.screenshot.AndroidXScreenshotTestRule
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
@SdkSuppress(minSdkVersion = 35, maxSdkVersion = 35)
class SignatureViewScubaTest {

    @get:Rule val activityRule = ActivityScenarioRule(PdfTestActivity::class.java)

    @get:Rule val screenshotRule = AndroidXScreenshotTestRule(SCREENSHOT_GOLDEN_DIRECTORY)

    @Test
    fun test_drawnSignature_unselected() {
        setupSignatureView { view ->
            view.signatureData =
                Signature.DrawnSignature(
                    id = "test_drawn",
                    pageNum = 1,
                    xCoord = 0f,
                    yCoord = 0f,
                    width = VIEW_WIDTH.toFloat(),
                    height = VIEW_HEIGHT.toFloat(),
                    isSelected = false,
                    drawnPath = createCheckmarkPath(),
                )
        }

        assertScreenshot(SIGNATURE_VIEW_ID, screenshotRule, DRAWN_SIGNATURE_UNSELECTED)
    }

    @Test
    fun test_drawnSignature_selected() {
        setupSignatureView { view ->
            view.signatureData =
                Signature.DrawnSignature(
                    id = "test_drawn_selected",
                    pageNum = 1,
                    xCoord = 0f,
                    yCoord = 0f,
                    width = VIEW_WIDTH.toFloat(),
                    height = VIEW_HEIGHT.toFloat(),
                    isSelected = true,
                    drawnPath = createCheckmarkPath(),
                )
        }

        assertScreenshot(SIGNATURE_VIEW_ID, screenshotRule, DRAWN_SIGNATURE_SELECTED)
    }

    @Test
    fun test_typedSignature() {
        setupSignatureView { view ->
            view.signatureData =
                Signature.TypedSignature(
                    id = "test_typed",
                    pageNum = 1,
                    xCoord = 0f,
                    yCoord = 0f,
                    width = VIEW_WIDTH.toFloat(),
                    height = VIEW_HEIGHT.toFloat(),
                    isSelected = false,
                    typedText = "Jane Doe",
                    typedFont = Signature.TypedSignature.FONT_SERIF,
                )
        }

        assertScreenshot(SIGNATURE_VIEW_ID, screenshotRule, TYPED_SIGNATURE_UNSELECTED)
    }

    @Test
    fun test_uploadedSignature() {
        setupSignatureView { view ->
            view.signatureData =
                Signature.UploadedSignature(
                    id = "test_uploaded",
                    pageNum = 1,
                    xCoord = 0f,
                    yCoord = 0f,
                    width = VIEW_WIDTH.toFloat(),
                    height = VIEW_HEIGHT.toFloat(),
                    isSelected = false,
                    imageBitmap = createCheckerboardBitmap(),
                )
        }

        assertScreenshot(SIGNATURE_VIEW_ID, screenshotRule, UPLOADED_SIGNATURE_UNSELECTED)
    }

    private fun setupSignatureView(callback: (SignatureView) -> Unit) {
        activityRule.scenario.onActivity { activity ->
            val signatureView = SignatureView(activity).apply { id = SIGNATURE_VIEW_ID }
            activity.container.addView(signatureView, LayoutParams(VIEW_WIDTH, VIEW_HEIGHT))
            // allow caller to do additional setup (like setting signature data)
            callback(signatureView)
        }
    }

    /** Generates a predictable "check mark" vector path for drawn testing. */
    private fun createCheckmarkPath(): Path {
        return Path().apply {
            moveTo(20f, 50f)
            lineTo(40f, 80f)
            lineTo(80f, 20f)
        }
    }

    /** Generates a predictable pattern for image upload testing to avoid flakiness. */
    private fun createCheckerboardBitmap(): Bitmap {
        val width = 100
        val height = 50
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = android.graphics.Paint()

        for (x in 0 until width step 10) {
            for (y in 0 until height step 10) {
                paint.color = if ((x / 10 + y / 10) % 2 == 0) Color.RED else Color.WHITE
                canvas.drawRect(
                    x.toFloat(),
                    y.toFloat(),
                    (x + 10).toFloat(),
                    (y + 10).toFloat(),
                    paint,
                )
            }
        }
        return bitmap
    }

    companion object {
        private val SIGNATURE_VIEW_ID = View.generateViewId()

        // Fixed dimensions for the tests to ensure predictable screenshot sizes
        private const val VIEW_WIDTH = 400
        private const val VIEW_HEIGHT = 200

        // Golden image names
        private const val DRAWN_SIGNATURE_UNSELECTED = "drawn_signature_unselected"
        private const val DRAWN_SIGNATURE_SELECTED = "drawn_signature_selected"
        private const val TYPED_SIGNATURE_UNSELECTED = "typed_signature_unselected"
        private const val UPLOADED_SIGNATURE_UNSELECTED = "uploaded_signature_unselected"
    }
}
