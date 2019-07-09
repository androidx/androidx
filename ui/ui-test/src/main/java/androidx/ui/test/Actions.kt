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

package androidx.ui.test

/**
 * Performs a click action on the given component.
 */
fun SemanticsNodeInteraction.doClick(): SemanticsNodeInteraction {
    assertStillExists()

    // TODO(catalintudor): get real coordinates after Semantics API is ready (b/125702443)
    val globalCoordinates = semanticsTreeNode.globalPosition
        ?: throw AssertionError("Semantic Node has no child layout to perform click on!")
    val x = globalCoordinates.x.value + 1f
    val y = globalCoordinates.y.value + 1f

    semanticsTreeInteraction.sendClick(x, y)

    return this
}
