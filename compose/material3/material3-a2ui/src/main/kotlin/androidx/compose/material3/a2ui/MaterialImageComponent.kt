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

package androidx.compose.material3.a2ui

import androidx.a2ui.compose.runtime.A2uiComponentProperties
import androidx.a2ui.compose.runtime.A2uiComponentScope
import androidx.a2ui.compose.runtime.A2uiProperty
import androidx.a2ui.compose.ui.A2uiComponent
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
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMap

/**
 * Renders an image for [MaterialImageComponent].
 *
 * Since AndroidX Compose libraries do not include an asynchronous image loader, developers must
 * provide their own implementation (such as Coil or Glide).
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

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Image"` component schema.
 *
 * Displays an image loaded from a URL.
 *
 * **Schema Properties:**
 * * `url` (Dynamic String, required): The URL of the image to load.
 * * `fit` (String Enum, optional): How the image should be scaled to fit its bounds. Valid options:
 *   `"cover"`, `"contain"`, `"fill"`, `"none"`, `"scaleDown"`. Defaults to `"fill"`.
 * * `variant` (String Enum, optional): A visual variant for the image determining size and shape.
 *   Valid options: `"icon"`, `"avatar"`, `"smallFeature"`, `"mediumFeature"`, `"largeFeature"`,
 *   `"header"`. Defaults to `"mediumFeature"`.
 *
 * @property imageRenderer The renderer to use for displaying images.
 */
public class MaterialImageComponent(public val imageRenderer: A2uiImageRenderer) : A2uiComponent {

    private val urlProp =
        A2uiProperty.dynamicString(
            key = "url",
            required = true,
            description = "The URL of the image to display.",
        )

    private val descriptionProp =
        A2uiProperty.dynamicString(
            key = "description",
            description = "Accessibility text for the image.",
        )

    private val fitProp =
        A2uiProperty.enum(
            key = "fit",
            enumValues = ImageFit.entries,
            mapToString = { it.token },
            convertFromString = { ImageFit.fromToken(it) },
            description =
                "Specifies how the image should be resized to fit its container. This corresponds to the CSS 'object-fit' property.",
        )

    private val variantProp =
        A2uiProperty.enum(
            key = "variant",
            enumValues = ImageVariant.entries,
            mapToString = { it.token },
            convertFromString = { ImageVariant.fromToken(it) },
            description = "A hint for the image size and style.",
        )

    override val name: String = "Image"

    override val description: String =
        "Displays an asynchronous image from a URL, with various styling variants."

    override val properties: List<A2uiProperty<*>> =
        listOf(urlProp, descriptionProp, fitProp, variantProp)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(urlProp) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val urlValue =
            checkNotNull(properties.bind(urlProp)) {
                "Required property '${urlProp.key}' is missing."
            }

        val descriptionValue = properties.bind(descriptionProp)

        val fit = properties[fitProp] ?: ImageFit.Fill
        val variant = properties[variantProp] ?: ImageVariant.MediumFeature

        imageRenderer.Image(
            url = urlValue,
            contentDescription = descriptionValue,
            contentScale = fit.contentScale,
            modifier = modifier.applyVariant(variant),
            onError = { throwable ->
                val errorMessage =
                    if (throwable != null && !throwable.message.isNullOrBlank()) {
                        "Image loading error from renderer for $urlValue: ${throwable.message}"
                    } else {
                        "Image loading error from renderer: $urlValue"
                    }
                reportError(A2uiRuntimeException(message = errorMessage))
            },
        )
    }

    private enum class ImageFit(val token: String, val contentScale: ContentScale) {
        Cover("cover", ContentScale.Crop),
        Contain("contain", ContentScale.Fit),
        Fill("fill", ContentScale.FillBounds),
        None("none", ContentScale.None),
        ScaleDown("scaleDown", ContentScale.Inside);

        companion object {
            val AllTokens: List<String> = entries.fastMap { it.token }

            private val TokenMap: Map<String, ImageFit> =
                buildMap(entries.size) {
                    ImageFit.entries.fastForEach { fit -> put(fit.token, fit) }
                }

            fun fromToken(token: String?): ImageFit = token?.let { TokenMap[it] } ?: Fill
        }
    }

    private enum class ImageVariant(val token: String) {
        Icon("icon"),
        Avatar("avatar"),
        SmallFeature("smallFeature"),
        MediumFeature("mediumFeature"),
        LargeFeature("largeFeature"),
        Header("header");

        companion object {
            val AllTokens: List<String> = entries.fastMap { it.token }

            private val TokenMap: Map<String, ImageVariant> =
                buildMap(entries.size) {
                    ImageVariant.entries.fastForEach { variant -> put(variant.token, variant) }
                }

            fun fromToken(token: String?): ImageVariant =
                token?.let { TokenMap[it] } ?: MediumFeature
        }
    }

    private fun Modifier.applyVariant(variant: ImageVariant): Modifier =
        when (variant) {
            ImageVariant.Icon -> this.size(24.dp)
            ImageVariant.Avatar -> this.size(40.dp).clip(CircleShape)
            ImageVariant.SmallFeature -> this.size(64.dp).clip(RoundedCornerShape(8.dp))
            ImageVariant.MediumFeature -> this.size(128.dp).clip(RoundedCornerShape(12.dp))
            ImageVariant.LargeFeature -> this.size(256.dp).clip(RoundedCornerShape(16.dp))
            ImageVariant.Header -> this.fillMaxWidth().height(200.dp)
        }
}
