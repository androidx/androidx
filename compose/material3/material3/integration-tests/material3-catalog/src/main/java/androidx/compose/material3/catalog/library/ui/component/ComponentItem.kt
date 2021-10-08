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

package androidx.compose.material3.catalog.library.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.catalog.library.model.Component
import androidx.compose.material3.catalog.library.ui.common.compositeBorderColor
import androidx.compose.material3.catalog.library.ui.common.gridItemBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

// TODO: Use components/values from Material3 when available
@Composable
fun ComponentItem(
    component: Component,
    onClick: (component: Component) -> Unit,
    index: Int,
    cellsCount: Int,
) {
    Box(
        modifier = Modifier
            .height(ComponentItemHeight)
            .clickable { onClick(component) }
            .gridItemBorder(
                itemIndex = index,
                cellsCount = cellsCount,
                color = compositeBorderColor()
            )
            .padding(ComponentItemPadding)
    ) {
        Image(
            painter = painterResource(id = component.icon),
            contentDescription = null,
            modifier = Modifier
                .size(ComponentItemIconSize)
                .align(Alignment.Center),
            colorFilter = if (component.tintIcon) {
                ColorFilter.tint(LocalContentColor.current.copy(alpha = ContentAlpha.disabled))
            } else {
                null
            },
            contentScale = ContentScale.Inside
        )
        Text(
            text = component.name,
            modifier = Modifier.align(Alignment.BottomStart),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

private val ComponentItemHeight = 180.dp
private val ComponentItemPadding = 16.dp
private val ComponentItemIconSize = 80.dp
