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

package androidx.ui.core

import androidx.ui.unit.Density

/**
 * A [Modifier.Element] that changes the way a UI component is measured and laid out.
 */
interface ParentDataModifier : Modifier.Element {
    /**
     * Provides a parentData given the [parentData] already provided through the modifier's chain.
     */
    fun Density.modifyParentData(parentData: Any?): Any? = parentData
}
