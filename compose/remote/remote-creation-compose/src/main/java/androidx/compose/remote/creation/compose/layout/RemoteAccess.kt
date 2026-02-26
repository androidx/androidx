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
import androidx.compose.remote.core.RemoteContext.FLOAT_CONTINUOUS_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_DAY_OF_MONTH
import androidx.compose.remote.core.RemoteContext.FLOAT_OFFSET_TO_UTC
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_HR
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_MIN
import androidx.compose.remote.core.RemoteContext.FLOAT_TIME_IN_SEC
import androidx.compose.remote.core.RemoteContext.FLOAT_WEEK_DAY
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.compose.state.AnimatedRemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteFloat

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
    public val component: RemoteComponent = RemoteComponent()

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

    /** A class that provides access to remote time information. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public inner class RemoteTime {
        public fun Hour(): RemoteFloat = RemoteFloat(FLOAT_TIME_IN_HR)

        public fun Minutes(): RemoteFloat = RemoteFloat(FLOAT_TIME_IN_MIN)

        public fun Seconds(): RemoteFloat = RemoteFloat(FLOAT_TIME_IN_SEC)

        public fun ContinuousSec(): RemoteFloat = RemoteFloat(FLOAT_CONTINUOUS_SEC)

        public fun UtcOffset(): RemoteFloat = RemoteFloat(FLOAT_OFFSET_TO_UTC)

        public fun DayOfWeek(): RemoteFloat = RemoteFloat(FLOAT_WEEK_DAY)

        public fun DayOfMonth(): RemoteFloat = RemoteFloat(FLOAT_DAY_OF_MONTH)
    }

    /** A class that provides access to remote component information. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public inner class RemoteComponent {
        private val context = RemoteFloatContext(scope.remoteComposeCreationState)

        public val width: RemoteFloat
            get() = context.componentWidth()

        public val height: RemoteFloat
            get() = context.componentHeight()

        public val centerX: RemoteFloat
            get() = context.componentCenterX()

        public val centerY: RemoteFloat
            get() = context.componentCenterY()
    }
}
