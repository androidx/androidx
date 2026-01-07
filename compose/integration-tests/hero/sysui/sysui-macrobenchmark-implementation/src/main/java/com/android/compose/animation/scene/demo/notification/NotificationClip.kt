/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.compose.animation.scene.demo.notification

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.Dp

internal fun Modifier.notificationClip(
    path: Path,
    topRadius: () -> Dp,
    bottomRadius: () -> Dp,
): Modifier {
    return drawWithCache {
        val topRadius = topRadius()
        val bottomRadius = bottomRadius()
        val topCornerRadius = CornerRadius(topRadius.toPx())
        val bottomCornerRadius = CornerRadius(bottomRadius.toPx())

        path.reset()
        path.addRoundRect(
            RoundRect(
                0f,
                0f,
                size.width,
                size.height,
                topLeftCornerRadius = topCornerRadius,
                topRightCornerRadius = topCornerRadius,
                bottomLeftCornerRadius = bottomCornerRadius,
                bottomRightCornerRadius = bottomCornerRadius,
            )
        )

        onDrawWithContent { clipPath(path) { this@onDrawWithContent.drawContent() } }
    }
}
