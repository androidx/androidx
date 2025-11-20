/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.navigationevent

import android.window.BackEvent
import androidx.annotation.RequiresApi

@Suppress("FunctionName")
@RequiresApi(34)
internal fun TestBackEvent(
    touchX: Float = 0.0F,
    touchY: Float = 0.0F,
    progress: Float = 0.0F,
    swipeEdge: Int = BackEvent.EDGE_LEFT,
): BackEvent {
    return BackEvent(touchX, touchY, progress, swipeEdge)
}
