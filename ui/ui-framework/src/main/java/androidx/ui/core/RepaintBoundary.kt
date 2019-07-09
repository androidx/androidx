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

import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer

/**
 * A repaint boundary blocks parents from having to repaint when contained children
 * are invalidated. This is used when children are invalidated together
 * and don't affect the parent. For example, a Button with a contained ripple does not
 * need to invalidate its container when animating the ripple.
 *
 * @param name The (optional) name of the RepaintBoundary. This is used for debugging and
 * will be given to the tag on a `View` or the name of a `RenderNode`.
 * @param children The contained children.
 */
@Composable
fun RepaintBoundary(name: String? = null, @Children children: @Composable() () -> Unit) {
    <RepaintBoundaryNode name=name>
        <children/>
    </RepaintBoundaryNode>
}