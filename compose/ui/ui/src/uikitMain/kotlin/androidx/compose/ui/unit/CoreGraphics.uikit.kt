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

import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake

internal fun CGPoint.asDpOffset(): DpOffset = DpOffset(x.dp, y.dp)
internal fun DpOffset.asCGPoint() = CGPointMake(x.value.toDouble(), y.value.toDouble())

internal fun CGSize.asDpSize(): DpSize = DpSize(width.dp, height.dp)
internal fun DpSize.asCGSize() = CGSizeMake(width.value.toDouble(), width.value.toDouble())

internal fun CGRect.asDpRect(): DpRect = DpRect(origin.asDpOffset(), size.asDpSize())
internal fun DpRect.asCGRect() = CGRectMake(
    left.value.toDouble(),
    top.value.toDouble(),
    width.value.toDouble(),
    height.value.toDouble()
)