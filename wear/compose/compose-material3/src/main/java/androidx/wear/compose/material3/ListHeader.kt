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

package androidx.wear.compose.material3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A slot based composable for creating a list header item. [ListHeader]s are typically expected
 * to be a few words of text on a single line.
 * The contents will be start and end padded.
 *
 * TODO(b/261838497) Add Material3 UX guidance links
 *
 * Example of a [ListHeader]:
 * @sample androidx.wear.compose.material3.samples.ListHeaderSample
 *
 * @param modifier The modifier for the [ListHeader].
 * @param backgroundColor The background color to apply - typically Color.Transparent
 * @param contentColor The color to apply to content.
 * @param contentPadding The spacing values to apply internally between the container
 * and the content.
 * @param content Slot for [ListHeader] content, expected to be a single line of text.
 */
@Composable
fun ListHeader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    contentPadding: PaddingValues = ListHeaderDefaults.HeaderContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = modifier
            .defaultMinSize(minHeight = ListHeaderDefaults.Height)
            .height(IntrinsicSize.Min)
            .wrapContentSize()
            .background(backgroundColor)
            .padding(contentPadding)
            .semantics(mergeDescendants = true) { heading() }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.titleMedium,
        ) {
            content()
        }
    }
}

/**
 * A two slot based composable for creating a list subheader item.
 * [ListSubheader]s offer slots for an icon and for a text label.
 * The contents will be start and end padded.
 *
 * TODO(b/261838497) Add Material3 UX guidance links
 *
 * Example of a [ListSubheader]:
 * @sample androidx.wear.compose.material3.samples.ListSubheaderSample
 *
 * Example of a [ListSubheader] with an icon:
 * @sample androidx.wear.compose.material3.samples.ListSubheaderWithIconSample
 *
 * @param modifier The modifier for the [ListSubheader].
 * @param backgroundColor The background color to apply - typically Color.Transparent
 * @param contentColor The color to apply to content.
 * @param contentPadding The spacing values to apply internally between the container
 * and the content.
 * @param icon A slot for providing icon to the [ListSubheader].
 * @param label A slot for providing label to the [ListSubheader].
 */
@Composable
fun ListSubheader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colorScheme.onBackground,
    contentPadding: PaddingValues = ListHeaderDefaults.SubheaderContentPadding,
    icon: (@Composable BoxScope.() -> Unit)? = null,
    label: @Composable RowScope.() -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = modifier
            .defaultMinSize(minHeight = ListHeaderDefaults.Height)
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .wrapContentSize(align = Alignment.CenterStart)
            .background(backgroundColor)
            .padding(contentPadding)
            .semantics(mergeDescendants = true) { heading() }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.titleMedium,
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier.wrapContentSize(align = Alignment.CenterStart),
                    content = icon
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            label()
        }
    }
}

object ListHeaderDefaults {
    private val TopPadding = 16.dp
    private val SubheaderBottomPadding = 8.dp
    private val HeaderBottomPadding = 12.dp
    private val HorizontalPadding = 14.dp
    internal val Height = 48.dp

    val HeaderContentPadding = PaddingValues(
        HorizontalPadding,
        TopPadding,
        HorizontalPadding,
        HeaderBottomPadding
    )
    val SubheaderContentPadding = PaddingValues(
        HorizontalPadding,
        TopPadding,
        HorizontalPadding,
        SubheaderBottomPadding
    )
}
