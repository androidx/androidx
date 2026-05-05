/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.unit

import androidx.compose.ui.platform.PlatformInsets
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIEdgeInsets

internal fun CGPoint.toDpOffset(): DpOffset = DpOffset(x.dp, y.dp)
internal fun CValue<CGPoint>.toDpOffset(): DpOffset = useContents { toDpOffset() }

internal fun DpOffset.toCGPoint() = CGPointMake(x.value.toDouble(), y.value.toDouble())

internal fun CGSize.toDpSize(): DpSize = DpSize(width.dp, height.dp)
internal fun DpSize.toCGSize() = CGSizeMake(width.value.toDouble(), height.value.toDouble())

internal fun CGRect.toDpRect(): DpRect = DpRect(origin.toDpOffset(), size.toDpSize())
internal fun CValue<CGRect>.toDpRect() = useContents { toDpRect() }
internal fun CValue<CGRect>.dpSize() = useContents {
    DpSize(size.width.dp, size.height.dp)
}

internal fun DpRect.toCGRect() = CGRectMake(
    left.value.toDouble(),
    top.value.toDouble(),
    width.value.toDouble(),
    height.value.toDouble()
)

internal fun CValue<UIEdgeInsets>.toPlatformInsets(density: Density) = useContents {
    with(density) {
        PlatformInsets(
            left = left.dp,
            top = top.dp,
            right = right.dp,
            bottom = bottom.dp
        )
    }
}