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

package androidx.compose.material3.demos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * The five Material 3 button variants, from highest to lowest emphasis, in each content arrangement
 * and in the disabled state.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ButtonVariantsDemo() {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ContentArrangement.entries.forEach { arrangement ->
            DemoSubtitle(arrangement.title)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ButtonVariant.entries.forEach { variant ->
                    VariantButton(variant, arrangement, enabled = true)
                }
            }
        }
        DemoSubtitle("Disabled")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ButtonVariant.entries.forEach { variant ->
                VariantButton(variant, ContentArrangement.LabelOnly, enabled = false)
            }
        }
    }
}

/**
 * The expressive button size ladder. Each size uses the matching [ButtonDefaults] helpers for its
 * shapes, content padding, icon size, icon spacing and text style.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun ButtonSizesDemo() {
    val sizes =
        listOf(
            "Extra small" to ButtonDefaults.ExtraSmallContainerHeight,
            "Small" to ButtonDefaults.MinHeight,
            "Medium" to ButtonDefaults.MediumContainerHeight,
            "Large" to ButtonDefaults.LargeContainerHeight,
            "Extra large" to ButtonDefaults.ExtraLargeContainerHeight,
        )
    // Bottom-aligned so the height progression is visible at a glance; wraps on narrow screens.
    FlowRow(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        sizes.forEach { (label, height) ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = {},
                    shapes = ButtonDefaults.shapesFor(height),
                    modifier = Modifier.heightIn(height),
                    contentPadding = ButtonDefaults.contentPaddingFor(height, hasStartIcon = true),
                ) {
                    ButtonContent(
                        arrangement = ContentArrangement.LeadingIcon,
                        icon = Icons.Filled.Edit,
                        label = label,
                        iconSize = ButtonDefaults.iconSizeFor(height),
                        iconSpacing = ButtonDefaults.iconSpacingFor(height),
                        textStyle = ButtonDefaults.textStyleFor(height),
                    )
                }
                Text(
                    text = "${height.value.roundToInt()}dp",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private enum class ButtonVariant(val label: String, val icon: ImageVector) {
    Filled("Filled", Icons.Filled.Favorite),
    FilledTonal("Filled tonal", Icons.Filled.Add),
    Elevated("Elevated", Icons.Filled.Share),
    Outlined("Outlined", Icons.Filled.Edit),
    Text("Text", Icons.AutoMirrored.Filled.Send),
}

/** Which slots of a button's content are populated. */
private enum class ContentArrangement(
    val title: String,
    val hasLeadingIcon: Boolean,
    val hasLabel: Boolean,
    val hasTrailingIcon: Boolean,
) {
    LabelOnly("Label", hasLeadingIcon = false, hasLabel = true, hasTrailingIcon = false),
    LeadingIcon("Leading icon", hasLeadingIcon = true, hasLabel = true, hasTrailingIcon = false),
    TrailingIcon("Trailing icon", hasLeadingIcon = false, hasLabel = true, hasTrailingIcon = true),
    IconOnly("Icon only", hasLeadingIcon = true, hasLabel = false, hasTrailingIcon = false),
}

/** Renders [variant] through its Material 3 button composable using the expressive overload. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VariantButton(
    variant: ButtonVariant,
    arrangement: ContentArrangement,
    enabled: Boolean,
) {
    val shapes = ButtonDefaults.shapes()
    // Icon-only buttons use the label-only padding; the icon is the button's sole content.
    val contentPadding =
        ButtonDefaults.contentPaddingFor(
            ButtonDefaults.MinHeight,
            hasStartIcon = arrangement.hasLeadingIcon && arrangement.hasLabel,
            hasEndIcon = arrangement.hasTrailingIcon,
        )
    val content: @Composable RowScope.() -> Unit = {
        ButtonContent(arrangement, variant.icon, variant.label)
    }
    when (variant) {
        ButtonVariant.Filled ->
            Button(
                onClick = {},
                shapes = shapes,
                enabled = enabled,
                contentPadding = contentPadding,
                content = content,
            )
        ButtonVariant.FilledTonal ->
            FilledTonalButton(
                onClick = {},
                shapes = shapes,
                enabled = enabled,
                contentPadding = contentPadding,
                content = content,
            )
        ButtonVariant.Elevated ->
            ElevatedButton(
                onClick = {},
                shapes = shapes,
                enabled = enabled,
                contentPadding = contentPadding,
                content = content,
            )
        ButtonVariant.Outlined ->
            OutlinedButton(
                onClick = {},
                shapes = shapes,
                enabled = enabled,
                contentPadding = contentPadding,
                content = content,
            )
        ButtonVariant.Text ->
            TextButton(
                onClick = {},
                shapes = shapes,
                enabled = enabled,
                contentPadding = contentPadding,
                content = content,
            )
    }
}

/**
 * Lays out a button's content according to [arrangement]: an optional leading [icon], the [label],
 * and an optional trailing arrow, separated by [iconSpacing].
 */
@Composable
private fun RowScope.ButtonContent(
    arrangement: ContentArrangement,
    icon: ImageVector,
    label: String,
    iconSize: Dp = ButtonDefaults.IconSize,
    iconSpacing: Dp = ButtonDefaults.IconSpacing,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    if (arrangement.hasLeadingIcon) {
        Icon(
            imageVector = icon,
            // Without a visible label, the icon must describe the button.
            contentDescription = if (arrangement.hasLabel) null else label,
            modifier = Modifier.size(iconSize),
        )
    }
    if (arrangement.hasLabel) {
        if (arrangement.hasLeadingIcon) Spacer(Modifier.size(iconSpacing))
        Text(label, style = textStyle)
    }
    if (arrangement.hasTrailingIcon) {
        Spacer(Modifier.size(iconSpacing))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
        )
    }
}

@Composable
private fun DemoSubtitle(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall)
}
