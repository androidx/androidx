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
import androidx.a2ui.model.protocol.A2uiException
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.a2ui.MaterialA2uiDefaults
import androidx.compose.material3.a2ui.icons.A2uiIcon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** A Jetpack Compose Material 3 implementation of the A2UI Basic Catalog `"Icon"` component. */
internal object MaterialA2uiBasicCatalogV1Icon : A2uiBasicCatalogV1.Icon {

    @Composable
    override fun A2uiComponentScope.TypedContent(
        source: A2uiBasicCatalogV1.Icon.Source,
        accessibility: A2uiBasicCatalogV1.AccessibilityAttributes?,
        modifier: Modifier,
    ) {
        val icon =
            when (source) {
                is A2uiBasicCatalogV1.Icon.BuiltIn -> {
                    A2uiIcon.fromName(source.value)
                }
                is A2uiBasicCatalogV1.Icon.SvgPath -> {
                    rememberVectorFromPath(source.svgPath)
                }
                is A2uiBasicCatalogV1.Icon.Unrecognized -> {
                    null
                }
            }

        AnimatedContent(
            modifier = modifier,
            targetState = icon,
            transitionSpec = MaterialA2uiDefaults.transitionSpec(),
            contentKey = { state ->
                if (state != null) {
                    "icon"
                } else {
                    "loading"
                }
            },
            label = "IconTransition",
        ) { state ->
            if (state == null) {
                SideEffect(source) {
                    val errorMessage =
                        when (source) {
                            is A2uiBasicCatalogV1.Icon.BuiltIn ->
                                "Unknown icon '${source.value}'. Expected a valid icon token or " +
                                    "an object with 'svgPath'."
                            is A2uiBasicCatalogV1.Icon.SvgPath ->
                                "Failed to parse SVG path '${source.svgPath}'."
                            is A2uiBasicCatalogV1.Icon.Unrecognized ->
                                "Unknown icon '${source.name}'. Expected a valid icon token or " +
                                    "an object with 'svgPath'."
                        }

                    // TODO(b/549592297): Add the path to the problematic property in the error
                    //  context once available.
                    reportError(A2uiException.A2uiRuntimeException(message = errorMessage))
                }

                MaterialA2uiDefaults.LoadingIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(imageVector = state, contentDescription = accessibility?.label)
            }
        }
    }
}

@Composable
private fun rememberVectorFromPath(pathData: String): ImageVector? =
    remember(pathData) {
        runCatching {
                val viewportSize = 24f
                val pathNodes = PathParser().parsePathString(pathData).toNodes()

                ImageVector.Builder(
                        name = "SvgIcon",
                        defaultWidth = viewportSize.dp,
                        defaultHeight = viewportSize.dp,
                        viewportWidth = viewportSize,
                        viewportHeight = viewportSize,
                    )
                    .addPath(pathData = pathNodes, fill = DefaultSvgFill)
                    .build()
            }
            .getOrNull()
    }

private val DefaultSvgFill = SolidColor(Color.Black)
