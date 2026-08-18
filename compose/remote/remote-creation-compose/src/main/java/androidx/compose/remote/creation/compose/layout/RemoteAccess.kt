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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.state.AnimatedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.remoteSpring

/**
 * A class that provides access to remote-specific utilities.
 *
 * @param scope The scope instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteAccess(private val scope: RemoteDrawScope) {
    /** Access to remote time information. */
    public val time: RemoteTime = RemoteTime()

    /** Access to remote component information. */
    public val component: RemoteComponent = RemoteComponent(scope)

    /** Wraps a constant value as a [RemoteFloat]. */
    public fun value(v: Float): RemoteFloat = RemoteFloat(v)

    /** Animates a [RemoteFloat]. */
    public fun animateFloat(
        rf: RemoteFloat,
        duration: Float = 1f,
        type: Int = 1,
        spec: FloatArray? = null,
        initialValue: Float = Float.NaN,
        wrap: Float = Float.NaN,
    ): RemoteFloat {
        val anim = RemoteComposeBuffer.packAnimation(duration, type, spec, initialValue, wrap)
        return AnimatedRemoteFloat(rf, anim)
    }

    /** Animates a [RemoteFloat] created in [content]. */
    public fun animateFloat(
        duration: Float = 1f,
        type: Int = 1,
        spec: FloatArray? = null,
        initialValue: Float = Float.NaN,
        wrap: Float = Float.NaN,
        content: () -> RemoteFloat,
    ): RemoteFloat {
        return animateFloat(content(), duration, type, spec, initialValue, wrap)
    }

    /**
     * Animates a [RemoteFloat] using a physics-based Spring engine.
     *
     * @param rf The target RemoteFloat to animate.
     * @param stiffness The spring stiffness/tension.
     * @param dampingRatio The damping ratio (1.0 = critically damped, < 1.0 = bouncy).
     * @param stopThreshold The threshold at which the spring is considered at rest.
     * @param boundaryMode Engine boundary mode (0 = standard/no bounds).
     */
    public fun animateSpring(
        rf: RemoteFloat,
        stiffness: Float = 50f,
        dampingRatio: Float = 1f,
        stopThreshold: Float = 0.001f,
        boundaryMode: Int = 0,
    ): RemoteFloat =
        remoteSpring(
                stiffness = stiffness,
                dampingRatio = dampingRatio,
                stopThreshold = stopThreshold,
                boundaryMode = boundaryMode,
            )
            .animate(rf)

    /** Animates a [RemoteFloat] created in [content] using a physics-based Spring engine. */
    public fun animateSpring(
        stiffness: Float = 50f,
        dampingRatio: Float = 1f,
        stopThreshold: Float = 0.001f,
        boundaryMode: Int = 0,
        content: () -> RemoteFloat,
    ): RemoteFloat =
        animateSpring(
            rf = content(),
            stiffness = stiffness,
            dampingRatio = dampingRatio,
            stopThreshold = stopThreshold,
            boundaryMode = boundaryMode,
        )

    /** Runs [content] in a loop. */
    public fun loop(
        until: Float,
        from: Float = 0f,
        step: Float = 1f,
        content: RemoteDrawScope.(RemoteFloat) -> Unit,
    ) {
        val document = scope.remoteComposeCreationState.document
        val loopIndex = document.addFloatConstant(0f)
        document.startLoop(Utils.idFromNan(loopIndex), from, step, until)
        content.invoke(scope, RemoteFloat(loopIndex))
        document.endLoop()
    }

    /** Runs [content] in a loop. */
    public fun loop(
        until: Int,
        from: Int = 0,
        step: Int = 1,
        content: RemoteDrawScope.(RemoteFloat) -> Unit,
    ) {
        loop(until.toFloat(), from.toFloat(), step.toFloat(), content)
    }
}
