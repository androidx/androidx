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

package androidx.compose.material3.a2ui.catalog

import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.ui.catalog.A2uiBasicCatalogV1
import androidx.a2ui.model.protocol.A2uiException.A2uiRuntimeException
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

/**
 * An abstraction of an asynchronous image loader which should be implemented using the
 * application's image loading library (such as Coil or Glide), used for rendering an image for
 * [A2uiBasicCatalogV1.Image].
 *
 * Example using Coil's `AsyncImage`:
 * ```
 * val imageRenderer =
 *     A2uiImageRenderer { url, contentDescription, contentScale, modifier, onError ->
 *         AsyncImage(
 *             model = url,
 *             contentDescription = contentDescription,
 *             contentScale = contentScale,
 *             modifier = modifier,
 *             onError = { state -> onError(state.result.throwable) },
 *         )
 *     }
 * ```
 */
@Stable
public fun interface A2uiImageRenderer {
    /**
     * Displays an image from a URL.
     *
     * @param url The URL of the image to load.
     * @param contentDescription Accessibility text for the image.
     * @param contentScale How the image should be scaled to fit its bounds.
     * @param modifier The modifier to be applied to the layout.
     * @param onError The callback to be invoked when the image fails to load.
     */
    @Composable
    public fun Image(
        url: String,
        contentDescription: String?,
        contentScale: ContentScale,
        modifier: Modifier,
        onError: (throwable: Throwable?) -> Unit,
    )
}

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Image"` component. */
internal class MaterialA2uiBasicCatalogV1Image(private val imageRenderer: A2uiImageRenderer) :
    A2uiBasicCatalogV1.Image {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        url: String,
        description: String?,
        fit: A2uiBasicCatalogV1.Image.Fit,
        variant: A2uiBasicCatalogV1.Image.Variant,
        modifier: Modifier,
    ) {
        imageRenderer.Image(
            url = url,
            contentDescription = description,
            contentScale = fit.toContentScale(),
            modifier = modifier.applyVariant(variant),
            onError = { throwable ->
                val errorMessage =
                    if (throwable != null && !throwable.message.isNullOrBlank()) {
                        "Image loading error from renderer for $url: ${throwable.message}"
                    } else {
                        "Image loading error from renderer: $url"
                    }
                reportError(A2uiRuntimeException(message = errorMessage))
            },
        )
    }
}

private fun A2uiBasicCatalogV1.Image.Fit.toContentScale(): ContentScale =
    when (this) {
        A2uiBasicCatalogV1.Image.Fit.Cover -> ContentScale.Crop
        A2uiBasicCatalogV1.Image.Fit.Contain -> ContentScale.Fit
        A2uiBasicCatalogV1.Image.Fit.Fill -> ContentScale.FillBounds
        A2uiBasicCatalogV1.Image.Fit.None -> ContentScale.None
        A2uiBasicCatalogV1.Image.Fit.ScaleDown -> ContentScale.Inside
    }

private fun Modifier.applyVariant(variant: A2uiBasicCatalogV1.Image.Variant): Modifier =
    when (variant) {
        A2uiBasicCatalogV1.Image.Variant.Icon -> this.size(24.dp)
        A2uiBasicCatalogV1.Image.Variant.Avatar -> this.size(40.dp).clip(CircleShape)
        A2uiBasicCatalogV1.Image.Variant.SmallFeature ->
            this.size(64.dp).clip(RoundedCornerShape(8.dp))
        A2uiBasicCatalogV1.Image.Variant.MediumFeature ->
            this.size(128.dp).clip(RoundedCornerShape(12.dp))
        A2uiBasicCatalogV1.Image.Variant.LargeFeature ->
            this.size(256.dp).clip(RoundedCornerShape(16.dp))
        A2uiBasicCatalogV1.Image.Variant.Header -> this.fillMaxWidth().height(200.dp)
    }
