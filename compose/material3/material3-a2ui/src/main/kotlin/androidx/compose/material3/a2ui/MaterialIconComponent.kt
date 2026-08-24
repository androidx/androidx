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
import androidx.a2ui.model.protocol.A2uiException
import androidx.a2ui.model.schema.A2uiAnySchema
import androidx.a2ui.model.schema.A2uiObjectSchema
import androidx.a2ui.model.schema.A2uiSchema
import androidx.a2ui.model.schema.A2uiSchemaKeyword
import androidx.a2ui.model.schema.A2uiStringSchema
import androidx.a2ui.model.schema.commontypes.A2uiAccessibilityAttributesSchema
import androidx.a2ui.model.schema.commontypes.A2uiDataBindingSchema
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
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

/**
 * A Jetpack Compose Material 3 implementation of the A2UI `"Icon"` component schema.
 *
 * Displays an icon using Material 3 [Icon].
 *
 * **Schema Properties:**
 * * `name` (Dynamic Custom, required): The name of the icon to display. Can be either a predefined
 *   icon name or an object with an `svgPath` string.
 * * `accessibility` (Dynamic Custom, optional): Attributes to enhance accessibility when using
 *   assistive technologies like screen readers.
 */
public object MaterialIconComponent : A2uiComponent {

    private val nameSchema: A2uiSchema =
        A2uiAnySchema(
            description = "The name of the icon to display.",
            keywords =
                listOf(
                    A2uiSchemaKeyword.OneOf(
                        listOf(
                            A2uiStringSchema(
                                keywords = listOf(A2uiSchemaKeyword.Enum(A2uiIcon.AllNames))
                            ),
                            A2uiObjectSchema(
                                properties = mapOf("svgPath" to A2uiStringSchema.INSTANCE),
                                required = setOf("svgPath"),
                                isAdditionalPropertiesAllowed = false,
                            ),
                            A2uiDataBindingSchema.DEFAULT_INSTANCE,
                        )
                    )
                ),
        )

    private val nameProp =
        A2uiProperty.dynamicCustom(
            key = "name",
            required = true,
            schema = nameSchema,
            safeCast = { value ->
                when (value) {
                    is String -> IconName.BuiltIn(value)
                    is Map<*, *> -> (value["svgPath"] as? String)?.let { IconName.SvgPath(it) }
                    else -> null
                }
            },
        )

    private val accessibilityProp =
        A2uiProperty.dynamicCustom(
            key = "accessibility",
            schema = A2uiAccessibilityAttributesSchema.DEFAULT_INSTANCE,
            safeCast = { value ->
                val map = value as? Map<*, *> ?: return@dynamicCustom null
                AccessibilityAttributes(
                    label = map["label"]?.toString(),
                    description = map["description"]?.toString(),
                )
            },
        )

    override val name: String = "Icon"

    override val description: String =
        "Displays an icon from a predefined set of icons or an SVG path."

    override val properties: List<A2uiProperty<*>> = listOf(nameProp, accessibilityProp)

    @Composable
    override fun A2uiComponentScope.isReady(properties: A2uiComponentProperties): Boolean {
        return properties.bind(nameProp) != null
    }

    @Composable
    override fun A2uiComponentScope.Content(
        properties: A2uiComponentProperties,
        modifier: Modifier,
    ) {
        val iconName =
            checkNotNull(properties.bind(nameProp)) {
                "Required property '${nameProp.key}' is missing."
            }
        val accessibility = properties.bind(accessibilityProp)
        val icon =
            when (iconName) {
                is IconName.BuiltIn -> {
                    A2uiIcon.fromName(iconName.name)
                }
                is IconName.SvgPath -> {
                    rememberVectorFromPath(iconName.svgPath)
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
                SideEffect(iconName) {
                    val errorMessage =
                        when (iconName) {
                            is IconName.BuiltIn ->
                                "Unknown icon '${iconName.name}'. Expected a valid icon token or an object with 'svgPath'."
                            is IconName.SvgPath -> "Failed to parse SVG path '${iconName.svgPath}'."
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

private sealed interface IconName {
    data class BuiltIn(val name: String) : IconName

    data class SvgPath(val svgPath: String) : IconName
}

private data class AccessibilityAttributes(
    val label: String? = null,
    val description: String? = null,
)

private val DefaultSvgFill = SolidColor(Color.Black)
