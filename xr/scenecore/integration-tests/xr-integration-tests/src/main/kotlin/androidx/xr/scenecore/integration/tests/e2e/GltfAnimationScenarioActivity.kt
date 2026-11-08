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

@file:kotlin.OptIn(androidx.xr.scenecore.ExperimentalGltfAnimationApi::class)

package androidx.xr.scenecore.integration.tests.e2e

import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.GltfAnimation
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.scene
import java.nio.file.Paths
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout

/**
 * Loads a glTF model, creates an entity from it, and starts playing an animation, exercising the
 * native AssetLoader and AssetAnimator JNI paths.
 */
class GltfAnimationScenarioActivity : ScenarioActivity() {

    override suspend fun runScenario(session: Session) {
        val model = GltfModel.create(session, Paths.get("models", "RobotExpressive.glb"))
        val entity =
            GltfModelEntity.create(
                session = session,
                model = model,
                pose = Pose(translation = Vector3(0f, 0f, -1.5f)),
                parent = session.scene.activitySpace,
            )

        val animation = checkNotNull(entity.getAnimations().firstOrNull()) { "No animations" }
        val playing = CompletableDeferred<Unit>()
        animation.addAnimationStateListener(
            Consumer { state ->
                if (state == GltfAnimation.AnimationState.PLAYING) playing.complete(Unit)
            }
        )
        animation.loop = true
        animation.start()
        if (animation.animationState == GltfAnimation.AnimationState.PLAYING) {
            playing.complete(Unit)
        }
        withTimeout(5.seconds) { playing.await() }
    }
}
