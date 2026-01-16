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

package androidx.xr.scenecore.testapp.surfaceinteraction

import androidx.media3.exoplayer.ExoPlayer
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.InputEvent

open class ClickVideoInputHandler(val player: ExoPlayer) : VideoInputManager.InputHandler {
    override fun onClick(
        pointerType: InputEvent.Pointer,
        origin: Vector3,
        direction: Vector3,
        count: Int,
    ) {
        when (count) {
            1 -> {
                // Single Click: Play/Pause
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
            }

            2 -> {
                // Double Click: Restart the video
                player.seekTo(0)
                player.play()
            }
        }
    }
}
