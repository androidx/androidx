/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.wear.compose.material

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * A slot based composable for creating a list header item. List header items are typically expected
 * to be text. The contents provided will have text and colors effects applied based on the
 * MaterialTheme. The contents will be start and end padded and should cover up to 3 lines of text.
 *
 * Example usage:
 *
 * @sample androidx.wear.compose.material.samples.ScalingLazyColumnWithHeaders
 * @param modifier The modifier for the list header
 * @param backgroundColor The background color to apply - typically Color.Transparent
 * @param contentColor The color to apply to content
 * @param content Slot for displayed header text
 */
@Composable
public fun ListHeader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colors.onSurfaceVariant,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier =
            modifier
                .defaultMinSize(minHeight = 48.dp)
                .height(IntrinsicSize.Min)
                .wrapContentSize()
                .background(backgroundColor)
                .padding(horizontal = 14.dp)
                .semantics(mergeDescendants = true) { heading() }
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.button,
        ) {
            content()
        }
    }
}
