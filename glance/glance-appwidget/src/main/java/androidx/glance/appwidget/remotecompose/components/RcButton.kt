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

import androidx.annotation.ColorInt
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.EmittableButton
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.remotecompose.TranslationContext
import androidx.glance.appwidget.remotecompose.convertGlanceModifierToRemoteComposeModifier
import androidx.glance.appwidget.toPixels
import androidx.glance.text.FontFamily
import androidx.glance.text.FontWeight
import androidx.glance.text.TextStyle
import androidx.glance.toEmittableText

private val TAG = "RcButton"

private object Defaults {
    val fontSize = 14.sp // overall style is Label/Large
    val fontWeight = FontWeight.Medium
    val fontFamily = FontFamily.SansSerif
    val corners = 100.dp
    val horizontalPadding = 24.dp
    val verticalPaddingApprox = 8.dp // todo
    val horizontalAlignment = BoxLayout.CENTER
    val verticalAlignment = BoxLayout.CENTER
}

internal class RcButton(emittableButton: EmittableButton, translationContext: TranslationContext) :
    RcElement(translationContext) {

    // output values
    @ColorInt private val contentColor: Int
    override val outputModifier: RecordingModifier
    private val buttonTextElement: RcText

    /**
     * Translate an EmittableButton into a Remote Compose button. design:
     * https://www.figma.com/design/LEjevYbhqcSvUvRxuzXPIh/Material-3-Design-Kit-(Community)?node-id=53923-27633&t=9a4QrlYVpISkbQxq-4
     *
     * TODO: can we enforce a minHeight to ~48dp?
     *
     * TODO use the rem command
     */
    init {
        with(translationContext.remoteComposeContext) {
            val modifier =
                convertGlanceModifierToRemoteComposeModifier(
                    modifiers = emittableButton.modifier,
                    translationContext = translationContext,
                )

            val corners =
                Defaults.corners
                    .toPixels(translationContext.context)
                    .toFloat() // md.sys.shape.corner.full
            @ColorInt val backgroundFallback: Int = Color.Transparent.toArgb()
            @ColorInt
            val backgroundColor: Int =
                emittableButton.colors
                    ?.backgroundColor
                    ?.getColor(translationContext.context)
                    ?.toArgb() ?: backgroundFallback

            @ColorInt val contentColorFallback: Int = Color.Black.toArgb()
            contentColor =
                emittableButton.colors?.contentColor?.getColor(translationContext.context)?.toArgb()
                    ?: contentColorFallback

            val hPad =
                Defaults.horizontalPadding.toPixels(
                    translationContext.context
                ) // https://m3.material.io/components/buttons/specs
            val vPad =
                Defaults.verticalPaddingApprox.toPixels(
                    translationContext.context
                ) //  TODO: ballpark estimate

            outputModifier =
                RecordingModifier()
                    .clip(RoundedRectShape(corners, corners, corners, corners))
                    .background(backgroundColor)
                    .then(modifier)
                    .padding(hPad, vPad, hPad, vPad) // todo: is there a way to set minHeight?

            // Apply button text defaults
            val buttonTextEmittable =
                emittableButton.toEmittableText().also { it.modifier = GlanceModifier }
            var textStyle = buttonTextEmittable.style ?: TextStyle()
            if (
                textStyle.fontSize == null &&
                    textStyle.fontWeight == null &&
                    textStyle.fontFamily == null
            ) {
                // todo, move this logic into Button.kt
                buttonTextEmittable.style =
                    textStyle.copy(
                        fontSize = Defaults.fontSize,
                        fontWeight = Defaults.fontWeight,
                        fontFamily = Defaults.fontFamily,
                    )
            }

            buttonTextElement =
                RcText.create(
                    emittable = buttonTextEmittable,
                    translationContext = translationContext,
                )
        }
    }

    override fun writeComponent(translationContext: TranslationContext) {
        translationContext.remoteComposeContext.box(
            modifier = outputModifier,
            horizontal = Defaults.horizontalAlignment,
            vertical = Defaults.verticalAlignment,
            content = { buttonTextElement.writeComponent(translationContext) },
        )
    }
}
