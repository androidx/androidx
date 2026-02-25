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
@file:JvmName("NavigationEventKt")

package androidx.navigationevent

import android.annotation.SuppressLint
import android.os.Build
import android.window.BackEvent
import androidx.annotation.RequiresApi

// The suppress is for `swipeEdge`. The constants in NavigationEvent should be the same
// or a superset of the constants in BackEvent.
@SuppressLint("WrongConstant")
@RequiresApi(34)
public fun BackEvent.toNavigationEvent(): NavigationEvent {
    return NavigationEvent(
        touchX = touchX,
        touchY = touchY,
        progress = progress,
        swipeEdge = swipeEdge,
        frameTimeMillis = if (Build.VERSION.SDK_INT >= 36) frameTimeMillis else 0,
    )
}

// The suppress is for `swipeEdge`. The constants in NavigationEvent should be the same
// or a superset of the constants in BackEvent.
@SuppressLint("WrongConstant")
@RequiresApi(34)
public fun NavigationEvent.toBackEvent(): BackEvent {
    return if (Build.VERSION.SDK_INT >= 36) {
        BackEvent(touchX, touchY, progress, swipeEdge, frameTimeMillis)
    } else {
        BackEvent(touchX, touchY, progress, swipeEdge)
    }
}
