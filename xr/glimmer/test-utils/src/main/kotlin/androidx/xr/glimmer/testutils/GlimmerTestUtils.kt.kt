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

package androidx.xr.glimmer.testutils

import android.annotation.SuppressLint
import android.os.Build
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage as captureToImageOriginal
import org.junit.Assume

/**
 * A wrapper around [captureToImage] that skips the test if the device does not satisfy the Glimmer
 * min SDK requirements.
 */
fun SemanticsNodeInteraction.captureToImage(): ImageBitmap {
    assumeGlimmerMinSdk()
    return this.captureToImageOriginal()
}

/**
 * If the device is running an SDK lower than 33, this method will skip the test.
 *
 * The expected min SDK for Glimmer is 35, however, we test on 33+ for wider device coverage (some
 * APIs are not available below 33).
 */
@SuppressLint("NewApi", "ObsoleteSdkInt") // We manually handle the API check below
internal fun assumeGlimmerMinSdk() {
    Assume.assumeTrue(
        "Skipping test: Glimmer tests should only run on SDK 33 or higher.",
        Build.VERSION.SDK_INT >= 33,
    )
}
