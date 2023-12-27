/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.window.embedding

import android.os.Bundle
import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.extensions.embedding.ActivityEmbeddingOptionsProperties.KEY_OVERLAY_TAG

/**
 * Implements ActivityEmbeddingOptions
 */
internal object ActivityEmbeddingOptionsImpl {

    /**
     * Puts [OverlayCreateParams] information to [android.app.ActivityOptions] bundle to launch
     * the overlay container
     *
     * @param overlayCreateParams The [OverlayCreateParams] to launch the overlay container
     */
    @RequiresWindowSdkExtension(5)
    internal fun setOverlayCreateParams(
        options: Bundle,
        overlayCreateParams: OverlayCreateParams,
    ) {
        WindowSdkExtensions.getInstance().requireExtensionVersion(5)

        options.putString(KEY_OVERLAY_TAG, overlayCreateParams.tag)
    }
}
