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

package androidx.camera.testing.impl

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment.DIRECTORY_PICTURES
import android.os.Environment.getExternalStoragePublicDirectory
import androidx.annotation.VisibleForTesting
import androidx.camera.core.CameraSelector
import androidx.camera.core.Logger
import androidx.test.core.app.ApplicationProvider
import java.io.File

private const val TAG = "LabTestUtil"
private const val LAB_TEST_OUTPUT_RELATIVE_PATH = "Pictures/test_output"
private val DEFAULT_OUTPUT_DIR: File
    get() = File(getExternalStoragePublicDirectory(DIRECTORY_PICTURES), "test_output")

/** Utility methods for CameraX lab test environment operations and artifact exports. */
public object LabTestUtil {

    @get:VisibleForTesting
    @set:VisibleForTesting
    public var isLabEnvironmentOverride: Boolean? = null

    /**
     * Returns true if running within a CameraX lab test environment, or if an override has been set
     * via [isLabEnvironmentOverride].
     */
    @JvmStatic
    public fun isLabEnvironment(): Boolean =
        isLabEnvironmentOverride
            ?: (LabTestRule.isInLabTest() ||
                LabTestRule.isLensFacingEnabledInLabTest(CameraSelector.LENS_FACING_BACK) ||
                LabTestRule.isLensFacingEnabledInLabTest(CameraSelector.LENS_FACING_FRONT))

    /**
     * Saves a [Bitmap] to the test output directory under `Environment.DIRECTORY_PICTURES` for test
     * artifact collection via AndroidFilePullerDecorator.
     *
     * On Android 10+ (API 29+), the bitmap is saved via MediaStore to comply with Scoped Storage
     * permissions. On earlier API levels, direct filesystem writing is used.
     *
     * The bitmap is only saved if executing within a CameraX lab test environment (where
     * `log.tag.MH`, `log.tag.rearCameraE2E`, or `log.tag.frontCameraE2E` system property is set to
     * `DEBUG`) to prevent leftover test images on device storage during manual or local testing.
     *
     * If you need to dump photos during manual or local testing, set the device property prior to
     * running the test: `adb shell setprop log.tag.MH DEBUG` (or `adb shell setprop
     * log.tag.rearCameraE2E DEBUG` / `adb shell setprop log.tag.frontCameraE2E DEBUG`).
     *
     * Note: Saved test images are intentionally not deleted by the test itself so that the lab test
     * runner (`AndroidFilePullerDecorator`) can pull them after execution. Pre-test cleanup is
     * handled automatically by the lab infrastructure before each test run.
     *
     * @param bitmap the [Bitmap] to save.
     * @param name the file name to save the bitmap as (e.g., "testName_methodName").
     * @param format the [Bitmap.CompressFormat] to use (default: [Bitmap.CompressFormat.PNG]).
     * @param quality the compression quality from 0 to 100 (default: 100).
     * @return the saved [File] if saved, or `null` if skipped (when not in a lab environment) or
     *   failed.
     */
    @JvmStatic
    @JvmOverloads
    public fun saveTestBitmap(
        bitmap: Bitmap,
        name: String,
        format: Bitmap.CompressFormat = Bitmap.CompressFormat.PNG,
        quality: Int = 100,
    ): File? {
        if (!isLabEnvironment()) {
            Logger.i(
                TAG,
                "Not in lab environment, skip saving test output bitmap: $name. " +
                    "If you need to dump photos for manual tests, please set device property: " +
                    "adb shell setprop log.tag.MH DEBUG (or rearCameraE2E / frontCameraE2E)",
            )
            return null
        }
        if (bitmap.isRecycled) {
            Logger.e(TAG, "Cannot save recycled bitmap: $name")
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val context = ApplicationProvider.getApplicationContext<Context>()
            val savedFile =
                FileUtil.saveBitmapToMediaStore(
                    context.contentResolver,
                    bitmap,
                    LAB_TEST_OUTPUT_RELATIVE_PATH,
                    name,
                    format,
                    quality,
                )
            if (savedFile != null) {
                return savedFile
            }
            Logger.w(
                TAG,
                "Failed to save bitmap to MediaStore on API ${Build.VERSION.SDK_INT} for: $name",
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // On API 30+, direct file writing to public directories is blocked by Scoped
                // Storage (EPERM).
                return null
            }
        }
        return FileUtil.saveBitmap(bitmap, DEFAULT_OUTPUT_DIR, name, format, quality)
    }
}
