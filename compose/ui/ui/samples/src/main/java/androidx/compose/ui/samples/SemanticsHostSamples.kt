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

package androidx.compose.ui.samples

import androidx.annotation.Sampled
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutInfo
import androidx.compose.ui.platform.ExperimentalComposeUiHostApi
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.semanticsConfiguration

@OptIn(ExperimentalComposeUiHostApi::class)
@Sampled
fun SemanticsOwnerHitTestSample(semanticsOwner: SemanticsOwner, positionInRoot: Offset) {
    // A host implementing accessibility hover finds the top-most node under the pointer that isn't
    // hidden from accessibility, without allocating a SemanticsNode for every hit.
    val hovered: LayoutInfo? =
        semanticsOwner.hitTest(positionInRoot) { layoutInfo ->
            val config = layoutInfo.semanticsConfiguration
            // Stop at the top-most node that isn't hidden; keep looking below otherwise.
            config != null && SemanticsProperties.HideFromAccessibility !in config
        }
    // Report hovered?.semanticsId to the accessibility framework, or the host if null.
    println("Hovered semantics id: ${hovered?.semanticsId}")
}
