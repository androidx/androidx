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

package androidx.xr.scenecore.spatial.rendering

import android.util.Log
import androidx.annotation.MainThread
import androidx.xr.scenecore.impl.impress.ImpressApi
import androidx.xr.scenecore.impl.impress.ImpressNode
import androidx.xr.scenecore.runtime.GltfAnimationFeature
import androidx.xr.scenecore.runtime.GltfEntity
import java.util.Collections
import java.util.concurrent.Executor
import java.util.function.Consumer
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class GltfAnimationFeatureImpl(
    private val impressApi: ImpressApi,
    private val modelImpressNode: ImpressNode,
    private val index: Int,
    private val name: String?,
    private val duration: Float,
    private val executor: Executor,
) : GltfAnimationFeature {
    private var currentAnimationJob: Job? = null
    private val defaultAnimationStartTime = 0f
    private val defaultAnimationSpeed = 1f

    @GltfEntity.AnimationStateValue
    override var animationState: Int = GltfEntity.AnimationState.STOPPED
        private set(value) {
            if (field != value) {
                field = value
                synchronized(animationStateListeners) {
                    animationStateListeners.forEach { (listener, executor) ->
                        executor.execute { listener.accept(value) }
                    }
                }
            }
        }

    override val animationIndex: Int = index

    override val animationName: String? = name?.ifEmpty { null }

    override val animationDuration: Float = duration

    private val animationStateListeners: MutableMap<Consumer<Int>, Executor> =
        Collections.synchronizedMap(mutableMapOf())

    @MainThread
    override fun startAnimation(loop: Boolean, speed: Float?, seekStartTimeSeconds: Float?) {
        // TODO: b/362826747 - Add a listener interface so that the application can be
        // notified that the animation has stopped, been cancelled (by starting another animation)
        // and / or shown an error state if something went wrong.

        currentAnimationJob?.cancel()
        val coroutineDispatcher = executor.asCoroutineDispatcher()

        animationState = GltfEntity.AnimationState.PLAYING
        currentAnimationJob =
            CoroutineScope(coroutineDispatcher).launch {
                try {
                    // The @MainThread annotation is a "Lint" check. As soon as you call launch, you
                    // are creating a new asynchronous task. The Dispatcher you pass to launch
                    // decides where that task runs. If you try to access that context from a
                    // background thread (which is where executor put you), the native code looks
                    // for the context, doesn't find it (or finds a mismatch), and fails or crashes
                    withContext(Dispatchers.Main) {
                        impressApi.animateGltfModel(
                            /* modelImpressNode= */ modelImpressNode,
                            /* animationName= */ name,
                            /* loop= */ loop,
                            /* speed= */ speed ?: defaultAnimationSpeed,
                            /* startTime= */ seekStartTimeSeconds ?: defaultAnimationStartTime,
                            /* channelId= */ index,
                        )
                    }
                } catch (e: Exception) {
                    if (e is CancellationException) throw e
                    // Some other error happened.  Log it and stop the animation.
                    Log.e("GltfAnimationFeatureImpl", "Could not start animation: $e")
                } finally {
                    if (currentAnimationJob === coroutineContext[Job]) {
                        animationState = GltfEntity.AnimationState.STOPPED
                    }
                }
            }
    }

    override fun stopAnimation() {
        if (
            animationState == GltfEntity.AnimationState.PLAYING ||
                animationState == GltfEntity.AnimationState.PAUSED
        ) {
            impressApi.stopGltfModelAnimation(modelImpressNode, /* channelId= */ index)
            animationState = GltfEntity.AnimationState.STOPPED
        }
    }

    override fun pauseAnimation() {
        if (animationState == GltfEntity.AnimationState.PLAYING) {
            impressApi.toggleGltfModelAnimation(
                modelImpressNode,
                /* playing= */ false,
                /* channelId= */ index,
            )
            animationState = GltfEntity.AnimationState.PAUSED
        }
    }

    override fun resumeAnimation() {
        if (animationState == GltfEntity.AnimationState.PAUSED) {
            impressApi.toggleGltfModelAnimation(
                modelImpressNode,
                /* playing= */ true,
                /* channelId= */ index,
            )
            animationState = GltfEntity.AnimationState.PLAYING
        }
    }

    override fun seekAnimation(startTime: Float) {
        if (
            animationState == GltfEntity.AnimationState.PLAYING ||
                animationState == GltfEntity.AnimationState.PAUSED
        ) {
            impressApi.setGltfModelAnimationPlaybackTime(
                modelImpressNode,
                /* playbackTime= */ startTime,
                /* channelId= */ index,
            )
        }
    }

    override fun setAnimationSpeed(speed: Float) {
        if (
            animationState == GltfEntity.AnimationState.PLAYING ||
                animationState == GltfEntity.AnimationState.PAUSED
        ) {
            impressApi.setGltfModelAnimationSpeed(
                modelImpressNode,
                /* speed= */ speed,
                /* channelId= */ index,
            )
        }
    }

    override fun addAnimationStateListener(executor: Executor, listener: Consumer<Int>) {
        animationStateListeners.putIfAbsent(listener, executor)
    }

    override fun removeAnimationStateListener(listener: Consumer<Int>) {
        animationStateListeners.remove(listener)
    }
}
