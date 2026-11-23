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
package androidx.compose.remote.creation.compose.capture

import androidx.compose.remote.core.Operations
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.shaders.RemoteShader
import androidx.compose.remote.creation.compose.state.RemoteBlendMode
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.RemoteFilterQuality
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.remote.creation.compose.state.isIdentity
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.remote.creation.compose.text.RemoteFontVariation
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.remote.creation.compose.text.combineFontSettings
import androidx.compose.remote.document.MaybeVariableFloat
import androidx.compose.remote.document.RemoteColorId
import androidx.compose.remote.document.RemotePaintWriter
import androidx.compose.remote.document.mvf

/** Tracks the state of a [RemotePaint] to optimize serialization by only sending deltas. */
internal class PaintTracker {
    var force: Boolean = false
    var isChanged: Boolean = false
    var remoteColor: RemoteColor? = null
    var colorValue: Int = -1
    var colorIsId: Boolean = false
    var strokeWidth: MaybeVariableFloat = (-1f).mvf
    var textSize: MaybeVariableFloat = (-1f).mvf
    var strokeCap: Int = -1
    var strokeJoin: Int = -1
    var style: Int = -1
    var typefaceId: Int = -1
    var typefaceWeight: Int = -1
    var typefaceIsItalic: Boolean = false
    var colorFilter: RemoteColorFilter? = null
    var blendMode: RemoteBlendMode? = null
    var filterQuality: RemoteFilterQuality? = null
    var shader: RemoteShader? = null
    var usingShaderMatrix: Boolean = false
    var fontVariationSettings: RemoteFontVariation.Settings? = null

    fun reset(force: Boolean) {
        this.force = force
        this.isChanged = force
    }

    private inline fun updateIfChanged(newValue: Any?, currentValue: Any?, update: () -> Unit) {
        if (force || newValue != currentValue) {
            update()
            isChanged = true
        }
    }

    private inline fun updateFloatIfChanged(
        newValue: MaybeVariableFloat,
        currentValue: MaybeVariableFloat,
        update: (MaybeVariableFloat) -> Unit,
    ): MaybeVariableFloat {
        if (force || newValue != currentValue) {
            update(newValue)
            isChanged = true
        }
        return newValue
    }

    @Suppress("PrimitiveInCollection")
    fun updateWithPaint(
        newPaint: RemotePaint,
        paintWriter: RemotePaintWriter,
        creationState: RemoteComposeCreationState,
    ) {

        // Color
        val targetRemoteColor = newPaint.color
        val colorVal: Int
        val colorIsId: Boolean
        val constantValue = targetRemoteColor.constantValueOrNull
        if (constantValue != null) {
            colorVal = constantValue
            colorIsId = false
        } else {
            colorVal = creationState.getId(targetRemoteColor)
            colorIsId = true
        }

        if (
            force ||
                this.remoteColor == null ||
                this.colorValue != colorVal ||
                this.colorIsId != colorIsId
        ) {
            this.colorValue = colorVal
            this.colorIsId = colorIsId
            this.remoteColor = targetRemoteColor
            if (colorIsId) {
                paintWriter.setColorId(RemoteColorId(colorVal))
            } else {
                paintWriter.setColor(colorVal)
            }
            isChanged = true
        }

        strokeWidth =
            updateFloatIfChanged(
                creationState.getMaybeVariableFloat(newPaint.strokeWidth),
                strokeWidth,
            ) {
                paintWriter.setStrokeWidth(it)
            }

        val targetCap = newPaint.strokeCap.value
        updateIfChanged(targetCap, strokeCap) {
            strokeCap = targetCap
            paintWriter.setStrokeCap(targetCap)
        }

        val targetJoin = newPaint.strokeJoin.value
        updateIfChanged(targetJoin, strokeJoin) {
            strokeJoin = targetJoin
            paintWriter.setStrokeJoin(targetJoin)
        }

        val targetStyle = newPaint.style.value
        updateIfChanged(targetStyle, style) {
            style = targetStyle
            paintWriter.setStyle(targetStyle)
        }

        textSize =
            updateFloatIfChanged(
                creationState.getMaybeVariableFloat(newPaint.textSize),
                textSize,
            ) {
                paintWriter.setTextSize(it)
            }

        val paintTypeface = newPaint.typeface
        val targetTypefaceId = getTypefaceId(paintTypeface, creationState)
        val targetWeight =
            when (paintTypeface) {
                RemoteTypeface.DefaultBold -> 700
                is RemoteTypeface.Named -> paintTypeface.weight
                else -> 400
            }
        val targetIsItalic =
            if (paintTypeface is RemoteTypeface.Named) paintTypeface.isItalic else false
        if (
            force ||
                typefaceId != targetTypefaceId ||
                typefaceWeight != targetWeight ||
                typefaceIsItalic != targetIsItalic
        ) {
            typefaceId = targetTypefaceId
            typefaceWeight = targetWeight
            typefaceIsItalic = targetIsItalic
            paintWriter.setTextStyle(targetTypefaceId, targetWeight, targetIsItalic)
            isChanged = true
        }

        val targetFontVariationSettings =
            combineFontSettings(newPaint.fontFeatureSettings, newPaint.fontVariationSettings)
        val allowEmptyFontAxis =
            creationState.profile.supportedOperations.contains(Operations.CORE_TEXT)
        val settingsToUse =
            if (allowEmptyFontAxis) {
                targetFontVariationSettings
            } else {
                targetFontVariationSettings
                    ?: RemoteFontVariation.Settings(RemoteFontVariation.weight(targetWeight.rf))
            }
        updateIfChanged(settingsToUse, fontVariationSettings) {
            fontVariationSettings = settingsToUse
            if (settingsToUse != null) {
                val (fontAxisNames, fontAxisValues) =
                    extractFontSettings(settingsToUse.settings, creationState)
                if (fontAxisNames != null && fontAxisValues != null) {
                    val axisTags =
                        IntArray(fontAxisNames.size) { i ->
                            creationState.document.addText(fontAxisNames[i]).id
                        }
                    paintWriter.setTextAxis(axisTags, fontAxisValues)
                }
            } else {
                paintWriter.setTextAxis(intArrayOf(), emptyList())
            }
        }

        val targetColorFilter = newPaint.colorFilter
        updateIfChanged(targetColorFilter, colorFilter) {
            val wasSet = colorFilter != null
            colorFilter = targetColorFilter
            if (targetColorFilter == null) {
                if (wasSet || force) {
                    paintWriter.clearColorFilter()
                }
            } else {
                targetColorFilter.applyToPaint(paintWriter, creationState)
            }
        }

        val targetBlendMode = newPaint.blendMode
        updateIfChanged(targetBlendMode, blendMode) {
            blendMode = targetBlendMode
            paintWriter.setBlendMode(targetBlendMode.value)
        }

        val targetFilterQuality = newPaint.filterQuality
        updateIfChanged(targetFilterQuality, filterQuality) {
            filterQuality = targetFilterQuality
            paintWriter.setFilterBitmap(targetFilterQuality != RemoteFilterQuality.None)
        }

        val targetShader = newPaint.shader
        if (force || this.shader != targetShader) {
            this.shader = targetShader
            if (targetShader != null) {
                targetShader.apply(creationState, paintWriter)
                val remoteMatrix3x3 = targetShader.remoteMatrix3x3
                when {
                    !remoteMatrix3x3.isIdentity -> {
                        paintWriter.setShaderMatrix(
                            creationState.getMaybeVariableFloat(remoteMatrix3x3)
                        )
                        usingShaderMatrix = true
                    }
                    usingShaderMatrix -> {
                        paintWriter.setShaderMatrix(0f.mvf)
                        usingShaderMatrix = false
                    }
                }
            } else {
                paintWriter.setShader(0)
            }
            isChanged = true
        }
    }

    private fun getTypefaceId(
        paintTypeface: RemoteTypeface?,
        creationState: RemoteComposeCreationState,
    ): Int {
        return when (paintTypeface) {
            null -> PaintBundle.FONT_TYPE_DEFAULT
            RemoteTypeface.Default -> PaintBundle.FONT_TYPE_DEFAULT
            RemoteTypeface.DefaultBold -> PaintBundle.FONT_TYPE_DEFAULT
            RemoteTypeface.Serif -> PaintBundle.FONT_TYPE_SERIF
            RemoteTypeface.SansSerif -> PaintBundle.FONT_TYPE_SANS_SERIF
            RemoteTypeface.Monospace -> PaintBundle.FONT_TYPE_MONOSPACE
            is RemoteTypeface.Named -> {
                when (paintTypeface.name.lowercase()) {
                    "sans-serif" -> PaintBundle.FONT_TYPE_SANS_SERIF
                    "serif" -> PaintBundle.FONT_TYPE_SERIF
                    "monospace" -> PaintBundle.FONT_TYPE_MONOSPACE
                    "default" -> PaintBundle.FONT_TYPE_DEFAULT
                    else -> creationState.document.addText(paintTypeface.name).id
                }
            }
        }
    }

    @Suppress("PrimitiveInCollection")
    private fun extractFontSettings(
        settings: List<RemoteFontVariation.Setting>?,
        creationState: RemoteComposeCreationState,
    ): Pair<Array<String>?, List<MaybeVariableFloat>?> {
        val size = settings?.size ?: return Pair(null, null)

        val fontAxisNames = Array(size) { settings[it].axisName }
        val fontAxisValues = List(size) { creationState.getMaybeVariableFloat(settings[it].value) }

        return Pair(fontAxisNames, fontAxisValues)
    }
}
