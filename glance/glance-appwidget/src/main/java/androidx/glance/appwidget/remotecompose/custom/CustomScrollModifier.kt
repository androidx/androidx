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
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.DebugRemoteCompose

/**
 * Alternative to the scroll impl in core
 *
 * @param direction either [VERTICAL] or [HORIZONTAL]
 * @param touchPositionVariable A document variable for the touch position
 * @param scrollPositionExpr An expression calculating the scroll position
 * @param numItems
 * @param scrollContainerSizePx
 * @param snapScrolling true if snap scroll, false for standard scrolling
 */
internal class CustomScrollModifier(
    private var direction: Int,
    private val touchPositionVariable: Float,
    private val scrollPositionExpr: Float,
    private var numItems: Int,
    private var scrollContainerSizePx: Float,
    private val snapScrolling: Boolean = true,
) : RecordingModifier.Element {
    var positionId: Float = 0f
    var customTouch: CustomTouch? = null

    interface CustomTouch {
        fun touch(max: Float, notchMax: Float): Float
    }

    override fun write(writer: RemoteComposeWriter) {
        addModifierCustomScroll(
            writer,
            direction,
            scrollPositionExpr,
            touchPositionVariable,
            numItems,
            scrollContainerSizePx,
        )
    }

    fun addModifierCustomScroll(
        writer: RemoteComposeWriter,
        direction: Int,
        scrollPositionExpr: Float,
        touchPositionVariable: Float,
        numItems: Int,
        scrollContainerSizePx: Float,
    ) {

        val notchMaxVariable: Float = writer.reserveFloatVariable()
        val touchExpressionDirection: Float =
            if (direction == VERTICAL) RemoteContext.FLOAT_TOUCH_POS_Y
            else RemoteContext.FLOAT_TOUCH_POS_X

        ScrollModifierOperation.apply(
            writer.buffer.buffer,
            direction,
            scrollPositionExpr,
            scrollContainerSizePx,
            notchMaxVariable,
        )

        writer.buffer.addTouchExpression(
            Utils.idFromNan(touchPositionVariable),
            0f, // initial value of touchPosition
            0f, // min
            numItems.toFloat(), // max
            0f,
            3, // TODO: maps to HapticFeedbackConstantsCompat.KEYBOARD_TAP
            floatArrayOf(
                touchExpressionDirection,
                scrollContainerSizePx,
                Rc.FloatExpression.DIV,
                numItems.toFloat(),
                AnimatedFloatExpression.MUL,
                -1f,
                AnimatedFloatExpression.MUL,
            ),
            if (snapScrolling) TouchExpression.STOP_NOTCHES_EVEN else TouchExpression.STOP_GENTLY,
            if (snapScrolling) floatArrayOf(numItems.toFloat())
            else null, // describes how many notches
            writer.easing(MaxTimeToSettle, MaxAcceleration, MaxVelocity),
        )

        if (DebugRemoteCompose) {
            val touchVariableId: Int = Utils.idFromNan(touchPositionVariable)
            writer.addDebugMessage(
                "CustomScrollModifier.kt: touchPosition [id = $touchVariableId]: ",
                touchPositionVariable,
            )
            writer.addDebugMessage("CustomScrollModifier.kt: scrollPos: ", scrollPositionExpr)
            writer.addDebugMessage(
                "CustomScrollModifier.kt: maxScrollPxExpr id = ${Utils.idFromNan(scrollContainerSizePx)}"
            )
            writer.addDebugMessage(
                "CustomScrollModifier.kt: scrollContainerSizePx: ",
                scrollContainerSizePx,
            )
        }

        writer.getBuffer().addContainerEnd()
    }

    companion object {
        const val VERTICAL: Int = 0
        const val HORIZONTAL: Int = 1

        const val MaxTimeToSettle = .6f // seconds
        const val MaxAcceleration = 1f // probably pixels per second squared
        const val MaxVelocity = .5f // max velocity per second
    }
}
