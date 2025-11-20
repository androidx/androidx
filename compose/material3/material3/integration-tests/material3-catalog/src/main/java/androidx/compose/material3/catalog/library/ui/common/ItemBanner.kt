/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3.catalog.library.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Draws a banner at the top and corner of a component.
 *
 * @param text The text to display.
 * @param bannerSize The size of the banner. Adjust according to the expected text size.
 * @param modifier The modifier to be applied to the banner.
 */
@Composable
fun ItemBanner(text: String, bannerSize: Dp, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
        val badgeColor = MaterialTheme.colorScheme.secondaryContainer
        Box(
            modifier =
                Modifier.graphicsLayer {
                        rotationZ = if (isRtl) -45f else 45f
                        translationX = if (isRtl) -size.width / 2f else size.width / 2f
                        translationY = -size.height / 2f
                    }
                    .size(bannerSize)
                    .background(color = badgeColor),
            contentAlignment = Alignment.BottomCenter,
        ) {
            ProvideTextStyle(MaterialTheme.typography.labelMedium) {
                Text(text = text, color = contentColorFor(badgeColor))
            }
        }
    }
}
