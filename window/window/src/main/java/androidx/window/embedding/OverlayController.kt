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

import android.content.Context
import android.os.Bundle
import androidx.window.RequiresWindowSdkExtension
import androidx.window.core.ExperimentalWindowApi

@ExperimentalWindowApi
internal class OverlayController(val backend: EmbeddingBackend) {

    @RequiresWindowSdkExtension(5)
    internal fun setOverlayCreateParams(
        options: Bundle,
        overlayCreateParams: OverlayCreateParams,
    ): Bundle = backend.setOverlayCreateParams(options, overlayCreateParams)

    companion object {
        /**
         * Obtains an instance of [OverlayController].
         *
         * @param context the [Context] to initialize the controller with
         */
        @JvmStatic
        fun getInstance(context: Context): OverlayController {
            val backend = EmbeddingBackend.getInstance(context)
            return OverlayController(backend)
        }
    }
}
