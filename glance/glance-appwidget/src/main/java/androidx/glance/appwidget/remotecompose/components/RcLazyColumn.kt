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

package androidx.glance.appwidget.remotecompose.components

import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.RFloat
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.RemoteComposeWriterAndroid
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.modifiers.DrawWithContentModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.Emittable
import androidx.glance.appwidget.lazy.EmittableLazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.appwidget.remotecompose.GlanceRemoteComposeTranslator
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.DebugRemoteCompose
import androidx.glance.appwidget.remotecompose.TranslationContext
import androidx.glance.appwidget.remotecompose.convertGlanceModifierToRemoteComposeModifier
import androidx.glance.appwidget.remotecompose.custom.CustomScrollModifier
import androidx.glance.appwidget.remotecompose.toColumnLayoutEnum
import androidx.glance.appwidget.toPixels

internal class RcLazyColumn(
    emittable: EmittableLazyColumn,
    translationContext: TranslationContext,
) : RcElement(translationContext) {
    override val outputModifier: RecordingModifier
    private val horizontalAlign: Int =
        emittable.horizontalAlignment.toColumnLayoutEnum() // horizontal align
    private val children = mutableListOf<RcElement>()

    private val verticalScrollMode: VerticalScrollMode?

    // TODO vvvvvvv
    // workaround for match-parent sizing of snap scrollable children
    private val heightVariableId: Float

    private val visFloatExpId: Float
    private val notVisFloatExpId: Float
    private var touchPositionVariable: Float = 0f
    private var scrollPositionExpr: Float = 0f // needs assignment

    // end workaround
    // TODO ^^^^^^

    init {
        // ------------ variables to toggle two canvas to force refresh
        val doc = translationContext.remoteComposeContext.mRemoteWriter
        visFloatExpId = doc.floatExpression(1f)
        notVisFloatExpId = doc.floatExpression(1f, visFloatExpId, Rc.FloatExpression.SUB)

        // -----------------------------------------

        @Suppress("ListIterator")
        for (child: Emittable in emittable.children) {
            val translation: RcElement =
                GlanceRemoteComposeTranslator.translateEmittable(child, translationContext)
            children.add(translation)
        }

        val userSpecifiedModifier =
            convertGlanceModifierToRemoteComposeModifier(
                modifiers = emittable.modifier,
                translationContext = translationContext,
            )
        this.verticalScrollMode = emittable.verticalScrollMode
        val notches: Int =
            when (emittable.verticalScrollMode) {
                is VerticalScrollMode.SnapScrollMatchHeight,
                is VerticalScrollMode.SnapScroll -> children.size - 1
                is VerticalScrollMode.Normal -> 1 // pass 1 to signify no snap scrolling
            }

        // this will only be referenced if we do snap scrolling .
        val defaultChildHeightF: Float =
            if (verticalScrollMode is VerticalScrollMode.SnapScrollMatchHeight) {
                verticalScrollMode.initialChildHeight.toPixels(translationContext.context).toFloat()
            } else {
                0f
            }

        heightVariableId =
            translationContext.remoteComposeContext.addFloatConstant(defaultChildHeightF)

        val isSnapScroll = this.verticalScrollMode != VerticalScrollMode.Normal
        val scrollModifier: RecordingModifier.Element =
            makeCustomSnapScrollModifier(
                numItems = notches,
                rcContext = translationContext.remoteComposeContext,
                snapScrolling = isSnapScroll,
            )

        outputModifier = userSpecifiedModifier.then(scrollModifier)
    }

    override fun writeComponent(translationContext: TranslationContext) {
        val rcContext = translationContext.remoteComposeContext

        rcContext.column(
            outputModifier.then(DrawWithContentModifier()), // modifier
            horizontalAlign, // horizontal align
        ) {
            val writer: RemoteComposeWriterAndroid =
                translationContext.remoteComposeContext.writer as RemoteComposeWriterAndroid

            writer.startCanvasOperations()
            val scrollColumnDynamicHeightId: Float = writer.addComponentHeightValue()
            val computedHeight =
                writer.floatExpression(scrollColumnDynamicHeightId, 1f, Rc.FloatExpression.MUL)

            // Force refresh when height changes
            refreshOnHeightChange(computedHeight)

            // vvvv Update the scroll view height vvvv
            val action =
                ValueFloatExpressionChange(
                    Utils.idFromNan(heightVariableId),
                    Utils.idFromNan(computedHeight),
                )
            writer.startRunActions()
            writer.addAction(action)
            writer.endRunActions()
            writer.drawComponentContent() // draws the normal content

            //                /// Vvvvv TODO: remove. This is only a reminder for how to write
            // expressions  vvvv
            //                val thing = writer.rf(Rc.Time.ANIMATION_TIME)
            //                val thing2 =
            //                    thing * 4f // example of operator overloading, we can now use
            // normal math
            //                val thing2Expr =
            //                    thing2.toFloat() // convert from expression mode to RPN float
            // expression
            //                // TODO: ^^^^^^^^^^
            if (verticalScrollMode !is VerticalScrollMode.Normal) {
                drawDots(computedHeight = computedHeight)

                writer.endCanvasOperations()
                // ^^^^ end: height hack ^^^^

                /*
                 * This is a workaround for not having (as of 2025/7) a matchParentHeight modifier
                 * for children of a scrollable container. This expression will grab the scrollable
                 * column's height at runtime, and apply it to the child elements
                 */
                if (verticalScrollMode is VerticalScrollMode.SnapScrollMatchHeight) {

                    @Suppress("ListIterator")
                    for (child in children) {
                        child.outputModifier.height(heightVariableId)
                    }
                }
            } else {
                writer.endCanvasOperations()
            }

            // common code path: write the children as a part of this component
            @Suppress("ListIterator")
            for (child in children) {
                child.writeComponent(translationContext)
            }
        } // end column

        //  TODO: snap scrolling  workaround (continued)
        if (verticalScrollMode !is VerticalScrollMode.Normal) {
            rcContext.startCanvas(
                RecordingModifier().visibility(visFloatExpId.toIntId()).height(1).width(1)
            )
            rcContext.endCanvas()
            rcContext.startCanvas(
                RecordingModifier().visibility(notVisFloatExpId.toIntId()).height(1).width(1)
            )
            rcContext.endCanvas()
        }
    }

    /** If the currently computed height does not match the cached height, do a refresh action. */
    private fun RemoteComposeContext.refreshOnHeightChange(computedHeight: Float) {
        conditionalOperations(Rc.Condition.NEQ, heightVariableId, computedHeight)
        if (DebugRemoteCompose) {
            addDebugMessage(" REFRESH ")
        }
        startRunActions()
        val notCalc =
            floatExpression(visFloatExpId, 1f, Rc.FloatExpression.ADD, 2f, Rc.FloatExpression.MOD)
        val refresh = ValueFloatExpressionChange(visFloatExpId.toIntId(), notCalc.toIntId())
        addAction(refresh)
        endRunActions()
        endConditionalOperations()
    }

    private fun makeCustomSnapScrollModifier(
        numItems: Int,
        snapScrolling: Boolean = true,
        rcContext: RemoteComposeContext,
    ): RecordingModifier.Element {

        touchPositionVariable = rcContext.addFloatConstant(0f)
        scrollPositionExpr =
            rcContext.floatExpression(
                touchPositionVariable,
                heightVariableId,
                Rc.FloatExpression.MUL,
            )

        val maxScrollPxExpr: Float =
            rcContext.floatExpression(heightVariableId, numItems.toFloat(), Rc.FloatExpression.MUL)

        if (DebugRemoteCompose) {
            val touchVariableId: Int = Utils.idFromNan(touchPositionVariable)
            rcContext.addDebugMessage(
                "RcLazyColumn:\t touchPosition [id= $touchVariableId]",
                touchPositionVariable,
            )
            rcContext.addDebugMessage("RcLazyColumn:\t heightVariable", heightVariableId)
            rcContext.addDebugMessage("RcLazyColumn:\t scrollPositionExpr", scrollPositionExpr)
            rcContext.addDebugMessage(
                "RcLazyColumn:\t maxScrollPxExpr id = ${Utils.idFromNan(maxScrollPxExpr)}"
            )
        }

        val customScrollModifier =
            CustomScrollModifier(
                direction = CustomScrollModifier.VERTICAL,
                touchPositionVariable = touchPositionVariable,
                scrollPositionExpr = scrollPositionExpr, // outputs an index of page
                numItems = numItems,
                scrollContainerSizePx = maxScrollPxExpr,
                snapScrolling = snapScrolling,
            )

        return customScrollModifier
    }

    private fun RemoteComposeContext.drawDots(computedHeight: Float) {
        val widthVariableId = writer.addComponentWidthValue()
        val writer: RemoteComposeWriterAndroid = writer as RemoteComposeWriterAndroid

        val startingAlpha = 2f // this gives us 1 second of solid alpha before fading

        // time since last touch coerced to (0,2)
        val touchEventTimeExpr =
            writer.floatExpression(
                Rc.Time.ANIMATION_TIME, // current time, in seconds f
                Rc.Touch.TOUCH_EVENT_TIME, // time of the last touch event, in seconds f
                Rc.FloatExpression.SUB,
                ////
                0f,
                Rc.FloatExpression.MAX // clamp a negative value to 0 (probably not needed here)
                ,
                /////
                startingAlpha,
                Rc.FloatExpression.MIN, // clamp a large value to 2
            )

        val alphaExpr =
            writer.floatExpression(startingAlpha, touchEventTimeExpr, Rc.FloatExpression.SUB)
        val clampedAlpha = writer.floatExpression(0f, 1f, alphaExpr, Rc.FloatExpression.CLAMP)
        writer.painter.setAlpha(clampedAlpha).commit()
        if (DebugRemoteCompose) {
            writer.addDebugMessage("alpha ", alphaExpr)
            writer.addDebugMessage("alpha clamped", clampedAlpha)
        }

        val dimenScale = 2f // TODO: do actual dp to px scaling, not this

        val maxDots = 5f
        val numDots = Math.min(maxDots, children.size.toFloat())
        val dotRadius = 3f * dimenScale

        val dotColumnXOffset = 3f * dimenScale * 4 // 4 is arbitrary
        val dotVPad = 4f * dimenScale
        val pillHeight = 6f * dimenScale // todo, change back to 12f
        val centerX =
            writer.floatExpression(widthVariableId, dotColumnXOffset, Rc.FloatExpression.SUB)
        val scrollSectionHeight =
            ((numDots - 1) * (2 * dotRadius) + (numDots - 1) * dotVPad + pillHeight)
        val scrollSectionY0: RFloat = ((rf(computedHeight) / rf(2f)) - (scrollSectionHeight / 2f))

        for (child in 0 until children.size) {
            // now, we can draw an overlay
            writer.painter.setColor(Color.Magenta.toArgb()).setAlpha(clampedAlpha).commit()
            writer.drawCircle(
                centerX,
                (scrollSectionY0 + child * (2 * dotRadius + dotVPad)).toFloat(),
                dotRadius,
            )
        }

        // Next, draw the pill at the right spot
        writer.painter.setColor(Color.Cyan.toArgb()).setAlpha(clampedAlpha).commit()
        val pillYExpr =
            (scrollSectionY0 + (rf(touchPositionVariable) * rf((2 * dotRadius + dotVPad))))
                .toFloat()
        if (DebugRemoteCompose) {
            addDebugMessage("RcLazyColumn: pillYExpr ", pillYExpr)
        }
        writer.drawCircle(centerX, pillYExpr, dotRadius)
        writer.painter.setAlpha(1f).commit() // reset alpha to a normal value
        //                writer.floatExpression(
        //                    Rc.Time.CONTINUOUS_SEC
        //                ) // force repaint // TODO: without this, the fade out animation
        // doesn't run
    }
}

private fun Float.toIntId() = Utils.idFromNan(this)
