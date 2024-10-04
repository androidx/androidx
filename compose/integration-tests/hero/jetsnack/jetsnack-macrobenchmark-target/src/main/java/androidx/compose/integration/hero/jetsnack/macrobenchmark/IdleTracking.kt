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

package androidx.compose.integration.hero.jetsnack.macrobenchmark

import android.view.Choreographer
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Recomposer

internal fun ComponentActivity.launchIdlenessTracking() {
    val contentView: View = findViewById(android.R.id.content)
    val callback: Choreographer.FrameCallback =
        object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (Recomposer.runningRecomposers.value.any { it.hasPendingWork }) {
                    contentView.contentDescription = "COMPOSE-BUSY"
                } else {
                    contentView.contentDescription = "COMPOSE-IDLE"
                }
                Choreographer.getInstance().postFrameCallback(this)
            }
        }
    Choreographer.getInstance().postFrameCallback(callback)
}
