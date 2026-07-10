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

package com.android.compose.animation.scene.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.android.compose.animation.scene.Back
import com.android.compose.animation.scene.ContentScope
import com.android.compose.animation.scene.ElementKey
import com.android.compose.animation.scene.SceneKey
import com.android.compose.animation.scene.Swipe
import com.android.compose.grid.VerticalGrid

object Bouncer {
    fun userActions(lockscreenScene: SceneKey) =
        mapOf(Back to lockscreenScene, Swipe.Down to lockscreenScene)

    object Elements {
        val Background = ElementKey("BouncerBackground")
        val Content = ElementKey("BouncerContent")
    }
}

@Composable
fun ContentScope.Bouncer(
    onBouncerCancelled: () -> Unit,
    onBouncerSolved: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier) {
        Box(
            Modifier.element(Bouncer.Elements.Background)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        )

        val nColumns = 3
        val nRows = 4
        val gridSpacing = 24.dp
        val normalButtonColor = MaterialTheme.colorScheme.surfaceBright
        val actionButtonColor = MaterialTheme.colorScheme.tertiary
        val actionIconColor = MaterialTheme.colorScheme.onTertiary

        VerticalGrid(
            columns = 3,
            Modifier.overscroll(verticalOverscrollEffect)
                .align(Alignment.BottomCenter)
                .padding(bottom = 100.dp)
                .element(Bouncer.Elements.Content),
            horizontalSpacing = gridSpacing,
            verticalSpacing = gridSpacing,
        ) {
            repeat(nRows * nColumns) { i ->
                val row = i / nColumns
                val col = i % nColumns

                val isLastRow = row == nRows - 1
                val isFirstColumn = col == 0
                val isLastColumn = col == nColumns - 1
                val isCancelButton = isLastRow && isFirstColumn
                val isSolveButton = isLastRow && isLastColumn

                val color =
                    if (isCancelButton || isSolveButton) {
                        actionButtonColor
                    } else {
                        normalButtonColor
                    }

                Button(
                    onClick = {
                        when {
                            isCancelButton -> onBouncerCancelled()
                            isSolveButton -> onBouncerSolved()
                        }
                    },
                    modifier = Modifier.size(80.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = color),
                    shape = CircleShape,
                    elevation = null,
                ) {
                    val icon: ImageVector? =
                        when {
                            isCancelButton -> Icons.AutoMirrored.Filled.ArrowBack
                            isSolveButton -> Icons.Default.Check
                            else -> null
                        }

                    icon?.let { Icon(it, null, tint = actionIconColor) }
                }
            }
        }
    }
}
