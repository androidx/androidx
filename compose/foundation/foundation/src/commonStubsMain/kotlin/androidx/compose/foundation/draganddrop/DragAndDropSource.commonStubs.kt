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

package androidx.compose.foundation.draganddrop

import androidx.compose.foundation.implementedInJetBrainsFork
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.graphics.drawscope.DrawScope

internal actual object DragAndDropSourceDefaults {
    actual val DefaultStartDetector: DragAndDropStartDetector = implementedInJetBrainsFork()
}

internal actual class CacheDrawScopeDragShadowCallback actual constructor() {
    actual fun drawDragShadow(drawScope: DrawScope): Unit = implementedInJetBrainsFork()

    actual fun cachePicture(scope: CacheDrawScope): DrawResult = implementedInJetBrainsFork()
}
