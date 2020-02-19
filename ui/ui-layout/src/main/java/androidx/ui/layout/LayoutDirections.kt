/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.ui.layout

import androidx.ui.core.LayoutDirection
import androidx.ui.core.LayoutModifier
import androidx.ui.unit.Density

/**
 * A layout modifier that changes the layout direction of the corresponding layout node.
 */
object LayoutDirectionModifier {
    val Ltr: LayoutModifier = LayoutDirectionModifierImpl(LayoutDirection.Ltr)
    val Rtl: LayoutModifier = LayoutDirectionModifierImpl(LayoutDirection.Rtl)
}

private data class LayoutDirectionModifierImpl(
    val newLayoutDirection: LayoutDirection
) : LayoutModifier {
    override fun Density.modifyLayoutDirection(layoutDirection: LayoutDirection) =
        newLayoutDirection
}
