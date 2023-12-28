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

import androidx.window.RequiresWindowSdkExtension
import androidx.window.WindowSdkExtensions
import androidx.window.embedding.ActivityEmbeddingOptionsImpl.getOverlayAttributes
import androidx.window.extensions.embedding.ActivityEmbeddingComponent
import androidx.window.extensions.embedding.ActivityStackAttributes

@RequiresWindowSdkExtension(5)
internal class OverlayControllerImpl(
    private val embeddingExtension: ActivityEmbeddingComponent,
    private val adapter: EmbeddingAdapter,
) {

    init {
        WindowSdkExtensions.getInstance().requireExtensionVersion(5)

        embeddingExtension.setActivityStackAttributesCalculator { params ->
            // TODO(b/295804279): Add OverlayAttributesCalculator support
            val overlayAttributes = params.launchOptions.getOverlayAttributes()
                ?: throw IllegalArgumentException(
                    "Can't retrieve overlay attributes from launch options"
                )
            return@setActivityStackAttributesCalculator ActivityStackAttributes.Builder()
                .setRelativeBounds(
                    EmbeddingBounds.translateEmbeddingBounds(
                        overlayAttributes.bounds,
                        adapter.translate(params.parentContainerInfo),
                    ).toRect()
                ).setWindowAttributes(adapter.translateWindowAttributes())
                .build()
        }
    }
}
