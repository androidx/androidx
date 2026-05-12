/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.compose.ui.test.UIKitInstrumentedTest
import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIView
import platform.UIKit.UIWindow

internal val loupeClassNames = listOf(
    "_UITextMagnifiedLoupeView",
    "_UITextLoupeView",
    "LoupeView"
)

@OptIn(BetaInteropApi::class)
private fun objcClassName(view: UIView): String? = view.`class`()?.let { NSStringFromClass(it) }

internal val UIView.isLoupeView: Boolean get() {
    val name = objcClassName(this) ?: return false
    return loupeClassNames.any { name.contains(it) }
}

private fun findFirstDescendant(root: UIView, predicate: (UIView) -> Boolean): UIView? {
    if (predicate(root)) return root
    root.subviews.forEach { view ->
        view as UIView
        val hit = findFirstDescendant(view, predicate)
        if (hit != null) return hit
    }
    return null
}

internal fun UIKitInstrumentedTest.findFirstDescendant(predicate: (UIView) -> Boolean): UIView? {
    val windowScene = viewController.view.window?.windowScene ?: return null
    windowScene.windows.forEach { window ->
        window as UIWindow
        val hit = findFirstDescendant(window, predicate)
        if (hit != null) return hit
    }
    return null
}
