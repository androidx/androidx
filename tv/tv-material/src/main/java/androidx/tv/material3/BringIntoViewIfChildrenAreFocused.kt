/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.tv.material3

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewResponder
import androidx.compose.foundation.relocation.bringIntoViewResponder
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.debugInspectorInfo

@Suppress("IllegalExperimentalApiUsage") // TODO (b/233188423): Address before moving to beta
@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.bringIntoViewIfChildrenAreFocused(): Modifier = composed(
    inspectorInfo = debugInspectorInfo { name = "bringIntoViewIfChildrenAreFocused" },
    factory = {
        var myRect: Rect = Rect.Zero
        this
            .onSizeChanged {
                myRect = Rect(Offset.Zero, Offset(it.width.toFloat(), it.height.toFloat()))
            }
            .bringIntoViewResponder(
                remember {
                    object : BringIntoViewResponder {
                        // return the current rectangle and ignoring the child rectangle received.
                        @ExperimentalFoundationApi
                        override fun calculateRectForParent(localRect: Rect): Rect = myRect

                        // The container is not expected to be scrollable. Hence the child is
                        // already in view with respect to the container.
                        @ExperimentalFoundationApi
                        override suspend fun bringChildIntoView(localRect: () -> Rect?) {}
                    }
                }
            )
    }
)
