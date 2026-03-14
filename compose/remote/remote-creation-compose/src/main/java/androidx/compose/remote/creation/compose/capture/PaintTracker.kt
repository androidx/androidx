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

import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Build
import androidx.annotation.RestrictTo
import androidx.compose.remote.core.operations.paint.PaintBundle
import androidx.compose.remote.creation.compose.layout.toAndroidCap
import androidx.compose.remote.creation.compose.layout.toAndroidJoin
import androidx.compose.remote.creation.compose.layout.toAndroidStyle
import androidx.compose.remote.creation.compose.layout.toComposeBlendMode
import androidx.compose.remote.creation.compose.layout.toInt
import androidx.compose.remote.creation.compose.shaders.RemoteShader
import androidx.compose.remote.creation.compose.state.ComposeRemoteColorFilter
import androidx.compose.remote.creation.compose.state.RemoteBlendModeColorFilter
import androidx.compose.remote.creation.compose.state.RemoteColor
import androidx.compose.remote.creation.compose.state.RemoteColorFilter
import androidx.compose.remote.creation.compose.state.RemotePaint
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.toArgb

/** Tracks the state of a [RemotePaint] to optimize serialization by only sending deltas. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PaintTracker {
    var force: Boolean = false
    var isChanged: Boolean = false
    var remoteColor: RemoteColor? = null
    var colorValue: Int = -1
    var colorIsId: Boolean = false
    var strokeWidth: Float = -1f
    var textSize: Float = -1f
    var strokeCap: Int = -1
    var strokeJoin: Int = -1
    var style: Int = -1
    var typefaceId: Int = -1
    var typefaceWeight: Int = -1
    var typefaceIsItalic: Boolean = false
    var colorFilter: RemoteColorFilter? = null
    var blendMode: BlendMode? = null
    var shader: RemoteShader? = null
    var usingShaderMatrix: Boolean = false

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
        newValue: Float,
        currentValue: Float,
        update: (Float) -> Unit,
    ): Float {
        if (force || newValue.toRawBits() != currentValue.toRawBits()) {
            update(newValue)
            isChanged = true
        }
        return newValue
    }

    @SuppressLint("ObsoleteSdkInt")
    fun updateWithPaint(newPaint: RemotePaint, paintBundle: PaintBundle, scope: RecordingCanvas) {
        val creationState = scope.creationState

        // Color
        val targetRemoteColor = newPaint.color
        val colorVal: Int
        val colorIsId: Boolean
        val constantValue = targetRemoteColor.constantValueOrNull
        if (constantValue != null) {
            colorVal = constantValue.toArgb()
            colorIsId = false
        } else {
            colorVal = targetRemoteColor.getIdForCreationState(creationState)
            colorIsId = true
        }

        if (force || this.colorValue != colorVal || this.colorIsId != colorIsId) {
            this.colorValue = colorVal
            this.colorIsId = colorIsId
            this.remoteColor = targetRemoteColor
            if (colorIsId) {
                paintBundle.setColorId(colorVal)
            } else {
                paintBundle.setColor(colorVal)
            }
            isChanged = true
        }

        strokeWidth =
            updateFloatIfChanged(
                newPaint.strokeWidth.getFloatIdForCreationState(creationState),
                strokeWidth,
            ) {
                paintBundle.setStrokeWidth(it)
            }

        val targetCap = newPaint.strokeCap.toAndroidCap().ordinal
        updateIfChanged(targetCap, strokeCap) {
            strokeCap = targetCap
            paintBundle.setStrokeCap(targetCap)
        }

        val targetJoin = newPaint.strokeJoin.toAndroidJoin().ordinal
        updateIfChanged(targetJoin, strokeJoin) {
            strokeJoin = targetJoin
            paintBundle.setStrokeJoin(targetJoin)
        }

        val targetStyle = newPaint.style.toAndroidStyle().ordinal
        updateIfChanged(targetStyle, style) {
            style = targetStyle
            paintBundle.setStyle(targetStyle)
        }

        textSize =
            updateFloatIfChanged(
                newPaint.textSize.getFloatIdForCreationState(creationState),
                textSize,
            ) {
                paintBundle.setTextSize(it)
            }

        val paintTypeface = newPaint.typeface
        val targetTypefaceId = getTypefaceId(paintTypeface)
        val targetWeight =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                paintTypeface?.weight ?: 0
            } else {
                0
            }

        val targetIsItalic = paintTypeface?.isItalic ?: false
        if (
            force ||
                typefaceId != targetTypefaceId ||
                typefaceWeight != targetWeight ||
                typefaceIsItalic != targetIsItalic
        ) {
            typefaceId = targetTypefaceId
            typefaceWeight = targetWeight
            typefaceIsItalic = targetIsItalic
            paintBundle.setTextStyle(targetTypefaceId, targetWeight, targetIsItalic)
            isChanged = true
        }

        val targetColorFilter = newPaint.colorFilter
        updateIfChanged(targetColorFilter, colorFilter) {
            val wasSet = colorFilter != null
            colorFilter = targetColorFilter
            when (targetColorFilter) {
                null -> {
                    if (wasSet) {
                        paintBundle.clearColorFilter()
                    }
                }
                is RemoteBlendModeColorFilter -> {
                    val constantColor = targetColorFilter.color.constantValueOrNull
                    if (constantColor != null) {
                        paintBundle.setColorFilter(
                            constantColor.toArgb(),
                            targetColorFilter.blendMode.toInt(),
                        )
                    } else {
                        val colorId = targetColorFilter.color.getIdForCreationState(creationState)
                        paintBundle.setColorFilterId(colorId, targetColorFilter.blendMode.toInt())
                    }
                }

                is ComposeRemoteColorFilter -> {
                    val native = targetColorFilter.composeColorFilter.asAndroidColorFilter()
                    if (native is android.graphics.BlendModeColorFilter) {
                        paintBundle.setColorFilter(
                            native.color,
                            native.mode.toComposeBlendMode().toInt(),
                        )
                    } else {
                        TODO("Native color filter not supported: " + native)
                    }
                }
            }
        }

        val composeBlendMode = newPaint.blendMode
        updateIfChanged(composeBlendMode, blendMode) {
            blendMode = composeBlendMode
            paintBundle.setBlendMode(composeBlendMode.toInt())
        }

        val targetShader = newPaint.shader
        if (force || this.shader != targetShader) {
            this.shader = targetShader as? RemoteShader
            when (targetShader) {
                is RemoteShader -> {
                    targetShader.apply(creationState, paintBundle)
                    if (usingShaderMatrix || targetShader.remoteMatrix3x3 != null) {
                        val remoteMatrix3x3 = targetShader.remoteMatrix3x3
                        if (remoteMatrix3x3 != null) {
                            paintBundle.setShaderMatrix(
                                remoteMatrix3x3.getFloatIdForCreationState(creationState)
                            )
                            usingShaderMatrix = true
                        } else {
                            paintBundle.setShaderMatrix(0f)
                            usingShaderMatrix = false
                        }
                    }
                }

                null -> {
                    paintBundle.setShader(0)
                }

                else -> {
                    TODO("Support shader $targetShader")
                }
            }
            isChanged = true
        }
    }

    private fun getTypefaceId(paintTypeface: Typeface?): Int {
        return when (paintTypeface) {
            null -> PaintBundle.FONT_TYPE_DEFAULT
            Typeface.DEFAULT -> PaintBundle.FONT_TYPE_DEFAULT
            Typeface.DEFAULT_BOLD -> PaintBundle.FONT_TYPE_DEFAULT
            Typeface.SERIF -> PaintBundle.FONT_TYPE_SERIF
            Typeface.SANS_SERIF -> PaintBundle.FONT_TYPE_SANS_SERIF
            Typeface.MONOSPACE -> PaintBundle.FONT_TYPE_MONOSPACE
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    when (paintTypeface.systemFontFamilyName) {
                        "serif" -> PaintBundle.FONT_TYPE_SERIF
                        "sans-serif" -> PaintBundle.FONT_TYPE_SANS_SERIF
                        "monospace" -> PaintBundle.FONT_TYPE_MONOSPACE
                        else -> PaintBundle.FONT_TYPE_DEFAULT
                    }
                } else {
                    PaintBundle.FONT_TYPE_DEFAULT
                }
            }
        }
    }
}
