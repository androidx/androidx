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

package androidx.ui.gestures.gesture_tester

import androidx.ui.engine.window.Window
import androidx.ui.gestures.arena.GestureArenaManager
import androidx.ui.gestures.binding.GestureBinding
import androidx.ui.gestures.pointer_router.PointerRouter
import com.nhaarman.mockitokotlin2.mock
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat

internal fun ensureGestureBinding() {
    GestureBinding.initInstance(Window(), mock(), mock())
    assertThat(GestureBinding.instance, `is`(notNullValue()))
}

internal val gestureArena: GestureArenaManager
    get() = GestureBinding.instance!!.gestureArena

internal val pointerRouter: PointerRouter
    get() = GestureBinding.instance!!.pointerRouter
