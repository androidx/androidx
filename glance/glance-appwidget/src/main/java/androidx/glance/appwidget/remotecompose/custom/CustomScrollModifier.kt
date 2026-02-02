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
@file:Suppress("RestrictedApiAndroidX")

package androidx.glance.appwidget.remotecompose.custom

import androidx.compose.remote.core.RemoteContext
import androidx.compose.remote.core.operations.TouchExpression
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.modifiers.ScrollModifierOperation
import androidx.compose.remote.core.operations.utilities.AnimatedFloatExpression
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier

/** Alternative to the scroll impl in core */
internal class CustomScrollModifier(
    private var direction: Int,
    private val touchPosition: Float,
    private val scrollPosition: Float,
    private var notches: Int,
    private var scrollContainerSizePx: Float,
) : RecordingModifier.Element {
    var mPositionId: Float = 0f
    var mCustom: CustomTouch? = null

    interface CustomTouch {
        fun touch(max: Float, notchMax: Float): Float
    }

    override fun write(writer: RemoteComposeWriter) {
        addModifierCustomScroll(
            writer,
            direction,
            scrollPosition,
            touchPosition,
            notches,
            scrollContainerSizePx,
        )
    }

    fun addModifierCustomScroll(
        writer: RemoteComposeWriter,
        direction: Int,
        scrollPosition: Float,
        touchPosition: Float,
        notches: Int,
        scrollContainerSizePx: Float,
    ) {

        val notchMax = writer.reserveFloatVariable()
        val touchExpressionDirection =
            if (direction == VERTICAL) RemoteContext.FLOAT_TOUCH_POS_Y
            else RemoteContext.FLOAT_TOUCH_POS_X

        ScrollModifierOperation.apply(
            writer.getBuffer().getBuffer(),
            direction,
            scrollPosition,
            scrollContainerSizePx,
            notchMax,
        )

        writer
            .getBuffer()
            .addTouchExpression(
                Utils.idFromNan(touchPosition),
                0f, // initial value of touchPosition
                0f,
                notches.toFloat(),
                0f,
                3,
                floatArrayOf(
                    touchExpressionDirection,
                    scrollContainerSizePx,
                    Rc.FloatExpression.DIV,
                    notches.toFloat(),
                    AnimatedFloatExpression.MUL,
                    -1f,
                    AnimatedFloatExpression.MUL,
                ),
                TouchExpression.STOP_NOTCHES_EVEN,
                floatArrayOf(notches.toFloat()),
                writer.easing(3f, 1f, 2f),
            )

        val touchVariableId: Int = Utils.idFromNan(touchPosition)
        writer.addDebugMessage("~~~ touchPosition [id = $touchVariableId]: ", touchPosition)
        writer.addDebugMessage("~~~ scrollPos: ", scrollPosition)
        writer.addDebugMessage("~~~ maxScrollPxExpr id = ${Utils.idFromNan(scrollContainerSizePx)}")
        writer.addDebugMessage("~~~ scrollContainerSizePx: ", scrollContainerSizePx)

        writer.getBuffer().addContainerEnd()
    }

    companion object {
        const val VERTICAL: Int = 0
        const val HORIZONTAL: Int = 1
    }
}
