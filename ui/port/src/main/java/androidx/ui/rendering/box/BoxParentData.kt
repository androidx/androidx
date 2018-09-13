/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.ui.rendering.box

import androidx.ui.engine.geometry.Offset
import androidx.ui.rendering.obj.ParentData

// / Parent data used by [RenderBox] and its subclasses.
open class BoxParentData : ParentData() {
    // / The offset at which to paint the child in the parent's coordinate system.
    var offset = Offset.zero

    override fun toString() = "offset=$offset"
}