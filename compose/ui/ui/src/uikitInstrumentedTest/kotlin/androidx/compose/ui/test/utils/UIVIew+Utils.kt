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

package androidx.compose.ui.test.utils

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.toCGSize
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.size
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import org.jetbrains.skia.Image
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImagePNGRepresentation
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.posix.memcpy

@OptIn(ExperimentalForeignApi::class)
internal fun UIView.captureToImage(): ImageBitmap {
    val size = frame.toDpRect().size.toCGSize()
    UIGraphicsBeginImageContextWithOptions(size, true, UIScreen.mainScreen().scale)

    drawViewHierarchyInRect(bounds, true)

    val uiImage = UIGraphicsGetImageFromCurrentImageContext()
    val pngData = UIImagePNGRepresentation(uiImage!!)!!
    UIGraphicsEndImageContext()

    val length = pngData.length.toInt()
    val byteArray = ByteArray(length)

    byteArray.usePinned { dst ->
        memcpy(dst.addressOf(0), pngData.bytes, length.convert())
    }

    val image = Image.makeFromEncoded(byteArray)
    return image.toComposeImageBitmap()
}
