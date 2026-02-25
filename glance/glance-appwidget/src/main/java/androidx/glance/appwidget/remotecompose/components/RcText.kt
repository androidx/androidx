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

import android.content.Context
import android.util.Log
import android.util.TypedValue
import androidx.annotation.ColorInt
import androidx.compose.remote.core.operations.Theme
import androidx.compose.remote.creation.RemoteComposeContext
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.glance.appwidget.remotecompose.RemoteComposeConstants.Text.DefaultFontSize
import androidx.glance.appwidget.remotecompose.TAG
import androidx.glance.appwidget.remotecompose.TranslationContext
import androidx.glance.appwidget.remotecompose.convertGlanceModifierToRemoteComposeModifier
import androidx.glance.color.DayNightColorProvider
import androidx.glance.text.EmittableText
import androidx.glance.unit.ColorProvider
import androidx.glance.unit.FixedColorProvider
import androidx.glance.unit.ResourceColorProvider

internal class RcText(
    translationContext: TranslationContext,
    override val outputModifier: RecordingModifier,
    private val textId: Int,
    private val colorProvider: ColorProvider,
    private val fontSizePx: Float,
    private val fontStyle: Int,
    private val fontWeight: Float,
    private val fontFamily: String?,
    private val maxLines: Int,
) : RcElement(translationContext) {

    companion object {
        private val defaultFontWeight: Float = 400f // todo: don't hardcode
        private val defaultFontStyle: Int = 0 // todo: don't hardcode

        fun create(
            emittable: EmittableText,
            translationContext: TranslationContext,
            @ColorInt fallbackColor: Int? = null,
        ): RcText {
            val outputModifier =
                convertGlanceModifierToRemoteComposeModifier(
                    modifiers = emittable.modifier,
                    translationContext = translationContext,
                )

            val defaultColor: ColorProvider =
                fallbackColor?.let { FixedColorProvider(Color(it)) } ?: ColorProvider(Color.Black)

            val textId = translationContext.remoteComposeContext.textCreateId(emittable.text)

            // TODO: don't resolve color in provider's process
            Log.w(
                "GlanceRemoteCompose",
                "Warning, resolving color in provider's process. TODO: fix me",
            )
            val colorProvider = emittable.style?.color ?: defaultColor

            // TODO(b/203656358): Can we support Em here too?
            val fontSizeSp: Float =
                emittable.style?.fontSize?.let {
                    if (!it.isSp) {
                        throw IllegalArgumentException(
                            "Only Sp is currently supported for font sizes"
                        )
                    }
                    return@let it.value
                } ?: DefaultFontSize

            val fontSizePx =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    fontSizeSp,
                    translationContext.context.resources.displayMetrics,
                )

            val style = emittable.style

            return RcText(
                translationContext = translationContext,
                outputModifier = outputModifier,
                textId = textId,
                colorProvider = colorProvider,
                fontSizePx = fontSizePx,
                fontStyle = style?.fontStyle?.style ?: defaultFontStyle, // fontStyle
                fontWeight = style?.fontWeight?.value?.toFloat() ?: defaultFontWeight, // fontWeight
                fontFamily = style?.fontFamily?.family, // fontFamily TODO, don't hardcode
                maxLines = emittable.maxLines,
            )
        }

        fun createForMaterial3Button(
            translationContext: TranslationContext,
            modifier: RecordingModifier,
            text: String,
            @ColorInt color: Int, // todo: handle day/night color providers
            fontSize: TextUnit,
            fontStyle: Int,
            fontWeight: Int,
            fontFamily: String?,
            maxLines: Int = Integer.MAX_VALUE,
            // TODO: other fields like align, overflow,
        ): RcText {
            val fontSizeSp: Float =
                if (fontSize.isSp) {
                    fontSize.value
                } else {
                    throw IllegalArgumentException("Only Sp is currently supported for font sizes")
                }

            val fontSizePx: Float =
                TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_SP,
                    fontSizeSp,
                    translationContext.context.resources.displayMetrics,
                )

            val textId = translationContext.remoteComposeContext.textCreateId(text)

            return RcText(
                translationContext = translationContext,
                outputModifier = modifier,
                textId = textId,
                colorProvider = FixedColorProvider(Color(color)), // todo temp hack
                fontSizePx = fontSizePx,
                fontStyle = fontStyle,
                fontWeight = fontWeight.toFloat(),
                fontFamily = fontFamily,
                maxLines = maxLines,
            )
        }
    } // end companion object

    // outputs

    override fun writeComponent(translationContext: TranslationContext) {
        writeTextComponent(
            context = translationContext.context,
            translationContext.remoteComposeContext,
            outputModifier, // remote modifier
            textId, // textId
            colorProvider, // color
            fontSizePx, // fontSize
            fontStyle,
            fontWeight,
            fontFamily,
            -1, // textAlign TODO: what is this?
            1, // overflow TODO
            maxLines = maxLines,
        )
    }
}

private fun writeTextComponent(
    context: Context,
    remoteComposeContext: RemoteComposeContext,
    outputModifier: RecordingModifier,
    textId: Int,
    colorProvider: ColorProvider,
    fontSizePx: Float,
    fontStyle: Int,
    fontWeight: Float,
    fontFamily: String?,
    textAlign: Int,
    overflow: Int,
    maxLines: Int,
) {
    fun writeTextComponentWithColor(colorArgb: Int) {
        // todo: as a workaround, we can wrap this in a box
        //        remoteComposeContext.startBox(outputModifier)
        remoteComposeContext.writer.startTextComponent(
            outputModifier, // remote modifier // todo undo
            textId, // textId
            colorArgb, // color
            fontSizePx, // fontSize
            fontStyle, // fontStyle
            fontWeight, // fontWeight
            fontFamily, // fontFamily
            textAlign, // textAlign
            overflow,
            maxLines,
        )
        remoteComposeContext.endTextComponent()
        //        remoteComposeContext.endBox() // todo: undo
    }

    fun DayNightColorProvider.dayArgb() = getColor(isNightMode = false).toArgb()
    fun DayNightColorProvider.nightArgb() = getColor(isNightMode = true).toArgb()

    // Write component to document. If there's a day night provider, respect that
    when (colorProvider) {
        is FixedColorProvider ->
            writeTextComponentWithColor(colorArgb = colorProvider.color.toArgb())
        is ResourceColorProvider -> {
            // TODO: Figure out how to handle ResourceColorProvider
            Log.w(
                TAG,
                "Warning: ResourceColorProvider is resolving resource in provider process for remote compose compatibility",
            )
            writeTextComponentWithColor(
                colorArgb = colorProvider.getColor(context = context).toArgb()
            )
        }
        is DayNightColorProvider -> {
            remoteComposeContext.setTheme(Theme.LIGHT)
            writeTextComponentWithColor(colorProvider.dayArgb())
            remoteComposeContext.setTheme(Theme.DARK)
            writeTextComponentWithColor(colorProvider.nightArgb())
            remoteComposeContext.setTheme(Theme.UNSPECIFIED)
        }
        else -> throw IllegalStateException("unexpected color provider: $colorProvider")
    }.let {} // exhaustive
}
