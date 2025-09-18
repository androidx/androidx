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

import android.os.Build
import android.window.BackEvent
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal fun NavigationEvent(backEvent: BackEvent): NavigationEvent {
    return NavigationEvent(
        touchX = backEvent.touchX,
        touchY = backEvent.touchY,
        progress = backEvent.progress,
        swipeEdge =
            when (backEvent.swipeEdge) {
                BackEvent.EDGE_LEFT -> NavigationEventSwipeEdge.Left
                BackEvent.EDGE_RIGHT -> NavigationEventSwipeEdge.Right
                BackEvent.EDGE_NONE -> NavigationEventSwipeEdge.None
                else -> error("Unexpected 'swipeEdge' value: ${backEvent.swipeEdge}")
            },
        frameTimeMillis = if (Build.VERSION.SDK_INT >= 36) backEvent.frameTimeMillis else 0,
    )
}
