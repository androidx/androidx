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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.CompositeKeyHashCode
import platform.UIKit.UIView

internal class UIKitInteropViewHolder<T : UIView>(
    factory: () -> T,
    interopContainer: InteropContainer,
    properties: UIKitInteropProperties,
    compositeKeyHashCode: CompositeKeyHashCode,
) : UIKitInteropElementHolder<T>(
    factory,
    interopContainer,
    properties,
    compositeKeyHashCode,
) {
    override val userComponentView: UIView
        get() = interopView

    override fun insertInteropView(root: InteropViewGroup, index: Int) {
        root.insertSubview(group, index.toLong())

        super.insertInteropView(root, index)
    }

    override fun removeInteropView(root: InteropViewGroup) {
        group.removeFromSuperview()

        super.removeInteropView(root)
    }
}