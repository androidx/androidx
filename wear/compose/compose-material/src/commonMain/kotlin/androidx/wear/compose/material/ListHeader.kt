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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A slot based composable for creating a list header item. List header items are typically expected
 * to be text. The contents provided will have text and colors effects applied based on the
 * MaterialTheme. The contents will be start and end padded by default and will fill the max width
 * of the parent.
 *
 * Example usage:
 * @sample androidx.wear.compose.material.samples.ScalingLazyColumnWithHeaders

 * @param modifier The modifier for the list header
 * @param backgroundColor The background color to apply - typically Color.Tranparent
 * @param contentColor The color to apply to content
 */
@Composable
public fun ListHeader(
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    contentColor: Color = MaterialTheme.colors.onSurfaceVariant,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = modifier.height(48.dp)
            .fillMaxWidth()
            .wrapContentSize()
            .background(backgroundColor)
            .padding(horizontal = 14.dp)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides contentColor,
            LocalTextStyle provides MaterialTheme.typography.button,
            content = content
        )
    }
}