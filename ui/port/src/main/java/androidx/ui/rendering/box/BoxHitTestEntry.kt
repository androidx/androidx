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
import androidx.ui.foundation.diagnostics.describeIdentity
import androidx.ui.gestures.hit_test.HitTestEntry

// /// A hit test entry used by [RenderBox].
class BoxHitTestEntry(
    target: RenderBox,
    // /// The position of the hit test in the local coordinates of [target].
    val localPosition: Offset
) : HitTestEntry(target) {
    override fun toString(): String = "${describeIdentity(target)}@$localPosition"
}