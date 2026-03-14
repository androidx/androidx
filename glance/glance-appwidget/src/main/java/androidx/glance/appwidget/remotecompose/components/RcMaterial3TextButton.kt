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
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.core.operations.layout.modifiers.ShapeType
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.modifiers.RoundedRectShape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.appwidget.components.EmittableM3TextButton
import androidx.glance.appwidget.remotecompose.TranslationContext
import androidx.glance.appwidget.remotecompose.convertGlanceModifierToRemoteComposeModifier
import androidx.glance.appwidget.toPixels
import androidx.glance.text.FontFamily
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight

private val TAG = "RcMaterial3Button"

private object M3ButtonDefaults {
    val fontSize = 14.sp // overall style is Label/Large
    val fontWeight = FontWeight.Medium
    val fontFamily = FontFamily.SansSerif
    val fontStyle = FontStyle.Normal
    val corners = 100.dp
    val iconSize = 18.dp
    val horizontalPadding = 16.dp
    val verticalPaddingApprox = 10.dp
    const val horizontalAlignment = RowLayout.CENTER
    const val verticalAlignment = RowLayout.CENTER
    val minHeight = 40.dp
}

internal class RcMaterial3TextButton(
    emittableButton: EmittableM3TextButton,
    translationContext: TranslationContext,
) : RcElement(translationContext) {
    override val outputModifier: RecordingModifier

    private val buttonModifier: RecordingModifier
    @ColorInt private val backgroundColor: Int
    @ColorInt private val contentColor: Int
    private val rcImage: RcImage?
    private val rcText: RcText

    init {
        val context = translationContext.context
        with(translationContext.remoteComposeContext) {
            outputModifier =
                convertGlanceModifierToRemoteComposeModifier(
                    modifiers = emittableButton.modifier,
                    translationContext = translationContext,
                )
            val cornerRadius =
                M3ButtonDefaults.corners.toPixels(translationContext.context).toFloat()

            // tODO: handle day night color providers
            backgroundColor =
                emittableButton.backgroundTint.getColor(translationContext.context).toArgb()

            contentColor =
                emittableButton.contentColor.getColor(translationContext.context).toArgb()

            val hPad = M3ButtonDefaults.horizontalPadding.toPixels(translationContext.context)
            val vPad = M3ButtonDefaults.verticalPaddingApprox.toPixels(translationContext.context)

            val borderWidth = 1.dp.toPixels(context).toFloat()

            val maybeBackground: RecordingModifier
            val maybeOutline: RecordingModifier

            if (emittableButton.isOutlineButton) {
                maybeOutline =
                    RecordingModifier()
                        .border(
                            borderWidth,
                            cornerRadius,
                            contentColor,
                            ShapeType.ROUNDED_RECTANGLE,
                        )
                maybeBackground = RecordingModifier()
            } else {
                maybeOutline = RecordingModifier()
                maybeBackground = RecordingModifier().background(backgroundColor)
            }

            //            val heightInMin = M3ButtonDefaults.minHeight.toPixels(context).toFloat()

            buttonModifier =
                RecordingModifier()
                    .then(maybeOutline)
                    .clip(RoundedRectShape(cornerRadius, cornerRadius, cornerRadius, cornerRadius))
                    .then(maybeBackground)
                    .then(outputModifier)
                    .padding(hPad, vPad, hPad, vPad)
            //                    .heightIn(
            //                        M3ButtonDefaults.minHeight.toPixels(context).toFloat(),
            //                        -1f,
            //                    ) // tODO, heightIn is misbehaving 2025/5

            rcText =
                RcText.createForMaterial3Button(
                    translationContext = translationContext,
                    modifier = RecordingModifier().wrapContentSize(),
                    text = emittableButton.text,
                    color = contentColor,
                    fontSize = M3ButtonDefaults.fontSize,
                    fontStyle = M3ButtonDefaults.fontStyle.style,
                    fontWeight = M3ButtonDefaults.fontWeight.value,
                    fontFamily = M3ButtonDefaults.fontFamily.family,
                )

            val imageProvider = emittableButton.icon
            if (imageProvider != null) {
                val sizePx = M3ButtonDefaults.iconSize.toPixels(context)
                rcImage =
                    RcImage(
                        provider = imageProvider,
                        recordingModifier =
                            RecordingModifier().padding(0, 0, 8.dp.toPixels(context), 0)
                        //                                .size(sizePx, sizePx),  // TODO: size
                        // modifier removed for now to work around size bug that causes image to not
                        // display, and button to  grow in size greatly
                        ,
                        tint = contentColor, // todo, don't resolve here
                        translationContext = translationContext,
                        targetDecodeWidth = sizePx,
                        targetDecodeHeight = sizePx,
                    )
            } else {
                rcImage = null
            }
        }
    }

    override fun writeComponent(translationContext: TranslationContext) {
        translationContext.remoteComposeContext.row(
            modifier = buttonModifier,
            content = {
                rcImage?.writeComponent(translationContext)
                rcText.writeComponent(translationContext)
            },
        )
    }
}
