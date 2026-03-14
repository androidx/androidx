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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.components.EmittableM3IconButton
import androidx.glance.appwidget.components.IconButtonShape
import androidx.glance.appwidget.remotecompose.TranslationContext
import androidx.glance.appwidget.remotecompose.convertGlanceModifierToRemoteComposeModifier
import androidx.glance.appwidget.remotecompose.eagerlyResolveColor
import androidx.glance.appwidget.toPixels
import androidx.glance.unit.ColorProvider

private val TAG = "RcMaterial3Button"

private interface IconButtonDefaults {
    val corners: Dp
    val padding: Dp
    val iconSize: Dp
    val minSize: Dp
    val defaultBackground: Int
        get() = Color.Transparent.toArgb()
}

private object M3SquareIconButtonDefaults : IconButtonDefaults {
    override val corners = 16.dp
    override val padding = 18.dp
    override val iconSize = 24.dp
    override val minSize = 60.dp
}

private object M3CircleIconButtonDefaults : IconButtonDefaults {
    override val corners = 100.dp
    override val padding = 12.dp
    override val iconSize = 24.dp
    override val minSize = 48.dp
}

internal class RcMaterial3IconButton(
    emittableButton: EmittableM3IconButton,
    translationContext: TranslationContext,
) : RcElement(translationContext) { // end-class
    override val outputModifier: RecordingModifier
    private val imageNode: RcImage

    init {
        val context = translationContext.context
        val defaults =
            when (emittableButton.shape) {
                IconButtonShape.Square -> M3SquareIconButtonDefaults
                IconButtonShape.Circle -> M3CircleIconButtonDefaults
            }
        val modifier =
            convertGlanceModifierToRemoteComposeModifier(
                modifiers = emittableButton.modifier,
                translationContext = translationContext,
            )

        val cornerRadius = defaults.corners.toPixels(translationContext.context).toFloat()

        // tODO: handle day night color providers
        @ColorInt
        val backgroundColor: Int =
            emittableButton.backgroundColor?.getColor(translationContext.context)?.toArgb()
                ?: defaults.defaultBackground

        val paddingPx = defaults.padding.toPixels(context)
        val minSizePx = defaults.minSize.toPixels(context).toFloat()
        val iconSizePx = defaults.iconSize.toPixels(context)
        val contentColor: ColorProvider = emittableButton.contentColor!!

        outputModifier =
            RecordingModifier()
                //                .heightIn(minSizePx, -1f) // TODO, a bug is causing heightIn to
                // misbhave 2025/5
                //                .widthIn(minSizePx, -1f)
                .clip(RoundedRectShape(cornerRadius, cornerRadius, cornerRadius, cornerRadius))
                .background(backgroundColor)
                .then(modifier)
                .padding(paddingPx)

        imageNode =
            RcImage(
                provider = emittableButton.imageProvider!!,
                recordingModifier = RecordingModifier().size(iconSizePx),
                tint = eagerlyResolveColor(contentColor, context),
                translationContext = translationContext,
                targetDecodeWidth = iconSizePx,
                targetDecodeHeight = iconSizePx,
            )
    } // end-init

    override fun writeComponent(translationContext: TranslationContext) {
        translationContext.remoteComposeContext.box(
            modifier = outputModifier,
            horizontal = BoxLayout.CENTER,
            vertical = BoxLayout.CENTER,
            content = { imageNode.writeComponent(translationContext) },
        )
    }
}
