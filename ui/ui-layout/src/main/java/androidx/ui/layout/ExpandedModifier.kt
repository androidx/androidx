/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.ui.core.Constraints
import androidx.ui.core.DensityScope
import androidx.ui.core.LayoutModifier
import androidx.ui.core.hasBoundedHeight
import androidx.ui.core.hasBoundedWidth
import androidx.ui.core.withTight

/**
 * A layout modifier that forces a target component to fill all available width.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleExpandedWidthModifier
 */
val LayoutExpandedWidth: LayoutModifier = object : LayoutModifier {
    override fun DensityScope.modifyConstraints(constraints: Constraints): Constraints {
        return if (constraints.hasBoundedWidth) {
            constraints.withTight(width = constraints.maxWidth)
        } else {
            constraints
        }
    }
}

/**
 * A layout modifier that forces a a target component to fill all available height.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleExpandedHeightModifier
 */
val LayoutExpandedHeight: LayoutModifier = object : LayoutModifier {
    override fun DensityScope.modifyConstraints(constraints: Constraints): Constraints {
        return if (constraints.hasBoundedHeight) {
            constraints.withTight(height = constraints.maxHeight)
        } else {
            constraints
        }
    }
}

/**
 * A layout modifier that forces a target component to fill all available space.
 *
 * Example usage:
 * @sample androidx.ui.layout.samples.SimpleExpandedModifier
 */
val LayoutExpanded: LayoutModifier = object : LayoutModifier {
    override fun DensityScope.modifyConstraints(constraints: Constraints): Constraints {
        return if (constraints.hasBoundedWidth && constraints.hasBoundedHeight) {
            Constraints.tightConstraints(constraints.maxWidth, constraints.maxHeight)
        } else {
            constraints
        }
    }
}
