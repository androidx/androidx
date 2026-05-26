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
import androidx.compose.remote.creation.actions.ValueFloatChange
import androidx.compose.remote.creation.minus
import androidx.compose.remote.creation.modifiers.DrawWithContentModifier
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.times
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.glance.Emittable
import androidx.glance.appwidget.lazy.EmittableLazyColumn
import androidx.glance.appwidget.lazy.VerticalScrollMode
import androidx.glance.appwidget.remotecompose.GlanceRemoteComposeTranslator
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.DebugRemoteCompose
import androidx.glance.appwidget.remotecompose.TranslationContext
import androidx.glance.appwidget.remotecompose.components.Pagination.maxDots
import androidx.glance.appwidget.remotecompose.convertGlanceModifierToRemoteComposeModifier
import androidx.glance.appwidget.remotecompose.custom.CustomScrollModifier
import androidx.glance.appwidget.remotecompose.toColumnLayoutEnum
import androidx.glance.appwidget.toPixels
import androidx.glance.unit.ColorProvider

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

    private var touchPositionVariable: Float = Float.NaN
    private var scrollPositionExpr: Float = 0f // needs assignment

    private val paginationDotColorPrimary: ColorProvider? = emittable.paginationDotColorPrimary
    private val paginationDotColorSecondary: ColorProvider? = emittable.paginationDotColorSecondary

    // end workaround
    // TODO ^^^^^^

    init {

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
                is VerticalScrollMode.SnapScrollMatchHeight -> children.size - 1
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

        val scrollModifier: RecordingModifier.Element =
            makeCustomSnapScrollModifier(
                numItems = notches,
                rcContext = translationContext.remoteComposeContext,
                snapScrolling = this.verticalScrollMode != VerticalScrollMode.Normal,
            )
        outputModifier = userSpecifiedModifier.then(scrollModifier)
    }

    override fun writeComponent(translationContext: TranslationContext) {
        val rcContext = translationContext.remoteComposeContext
        val writer: RemoteComposeWriterAndroid =
            translationContext.remoteComposeContext.writer as RemoteComposeWriterAndroid

        val isTouchDownHappening = writer.addFloatConstant(0f)
        val touchDownId = Utils.idFromNan(isTouchDownHappening)

        rcContext.column(
            modifier =
                outputModifier
                    .then(DrawWithContentModifier())
                    .then(
                        RecordingModifier()
                            .onTouchUp(ValueFloatChange(touchDownId, 0f))
                            .onTouchDown(ValueFloatChange(touchDownId, 1f))
                    ),
            horizontal = horizontalAlign, // horizontal align
        ) {
            writer.startCanvasOperations()
            val interpolatedAlpha: RFloat =
                rf(isTouchDownHappening).anim(2f, Rc.Animate.CUBIC_STANDARD or (2 * 1024))

            val scrollColumnDynamicHeightId: Float = writer.addComponentHeightValue()
            val computedHeight =
                writer.floatExpression(scrollColumnDynamicHeightId, 1f, Rc.FloatExpression.MUL)

            writer.drawComponentContent() // draws the normal content

            if (verticalScrollMode !is VerticalScrollMode.Normal) {
                if (children.size > 1) {
                    drawDots(
                        computedHeight = computedHeight,
                        mainColor = paginationDotColorPrimary?.getColor(translationContext.context),
                        fadedColor =
                            paginationDotColorSecondary?.getColor(translationContext.context),
                        interpolatedAlpha = interpolatedAlpha,
                    )
                }

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

                writer.endCanvasOperations()
                // ^^^^ end: height hack ^^^^
            } else {
                writer.endCanvasOperations()
            }

            // common code path: write the children as a part of this component
            @Suppress("ListIterator")
            for (child in children) {
                child.writeComponent(translationContext)
            }
        } // end column
    }

    private fun makeCustomSnapScrollModifier(
        numItems: Int,
        snapScrolling: Boolean = true,
        rcContext: RemoteComposeContext,
    ): RecordingModifier.Element {

        touchPositionVariable = rcContext.reserveFloatVariable()
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

    private fun RemoteComposeContext.drawDots(
        computedHeight: Float,
        mainColor: Color?,
        fadedColor: Color?,
        interpolatedAlpha: RFloat,
    ) {
        if (mainColor == null || fadedColor == null) {
            return
        }

        val interpolatedAlphaFloat: Float = interpolatedAlpha.toFloat()

        val widthVariableId = writer.addComponentWidthValue()
        val writer: RemoteComposeWriterAndroid = writer as RemoteComposeWriterAndroid

        writer.painter.setAlpha(interpolatedAlpha.toFloat()).commit()

        val numDots = Math.min(maxDots, children.size.toFloat())

        val density = rf(Rc.System.DENSITY)
        val dotRadius: RFloat = Pagination.dotRadius * density
        val dotDiameter = dotRadius * 2f

        val dotColumnXPadding: RFloat = Pagination.dotColumnXPadding * density
        val dotYPadding = Pagination.dotYPadding * density

        val centerX: RFloat = (widthVariableId - dotColumnXPadding).flush()
        val scrollSectionHeight =
            ((numDots - 1f) * dotDiameter + (numDots - 1) * dotYPadding + dotDiameter).flush()

        val scrollSectionY0: RFloat =
            ((rf(computedHeight) / rf(2f)) - (scrollSectionHeight / 2f)).flush()

        val centerXVariableId = centerX.toFloat()

        for (child in 0 until children.size) {
            if (DebugRemoteCompose) {
                writer.addDebugMessage("Drawing child ${child+1} of ${children.size}  ")
            }

            // now, we can draw an overlay
            writer.painter.setColor(fadedColor.toArgb()).setAlpha(interpolatedAlphaFloat).commit()
            writer.drawCircle(
                centerXVariableId,
                (scrollSectionY0 + child.toFloat() * (dotDiameter + dotYPadding)).toFloat(),
                dotRadius.toFloat(),
            )
        }

        // Next, draw the pill at the right spot
        writer.painter.setColor(mainColor.toArgb()).setAlpha(interpolatedAlphaFloat).commit()

        val dotOffset = (rf(touchPositionVariable) * rf((dotDiameter + dotYPadding)))
        if (DebugRemoteCompose) {
            writer.addDebugMessage("scrollSectionY0: ", scrollSectionY0.toFloat())
            writer.addDebugMessage("touchPositionVariable: ", touchPositionVariable)
            writer.addDebugMessage("dotDiameter: ", dotDiameter.toFloat())
            writer.addDebugMessage("dotYPadding: ", dotYPadding.toFloat())
            writer.addDebugMessage("dotOffset: ", dotOffset.toFloat())
        }

        val pillYExpr = (scrollSectionY0 + dotOffset).toFloat()

        writer.drawCircle(centerXVariableId, pillYExpr, dotRadius.toFloat())
        writer.painter.setAlpha(1f).commit() // reset alpha to a normal value

        if (DebugRemoteCompose) {
            addDebugMessage("RcLazyColumn: -----------")
        }
    }
}

private fun Float.toIntId() = Utils.idFromNan(this)

/** Constants for drawing the pagination dots */
private object Pagination {
    const val maxDots = 7f
    const val dotRadius = 3f
    const val dotColumnXPadding = 12f
    const val dotYPadding = 4f
}
