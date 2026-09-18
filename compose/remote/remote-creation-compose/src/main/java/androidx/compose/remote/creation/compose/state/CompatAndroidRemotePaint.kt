/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.compose.remote.creation.compose.state

import android.graphics.BlendModeColorFilter as AndroidBlendModeColorFilter
import android.graphics.ColorFilter as AndroidColorFilter
import android.graphics.Paint as AndroidPaint
import android.graphics.fonts.FontVariationAxis
import android.util.Log
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.layout.toAndroidBlendMode
import androidx.compose.remote.creation.compose.layout.toComposeBlendMode
import androidx.compose.remote.creation.compose.layout.toPaintingStyle
import androidx.compose.remote.creation.compose.layout.toStrokeCap
import androidx.compose.remote.creation.compose.layout.toStrokeJoin
import androidx.compose.remote.creation.compose.shaders.RemoteShader
import androidx.compose.remote.creation.compose.text.RemoteTypeface
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidColorFilter
import androidx.compose.ui.graphics.nativePaint
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposePathEffect
import androidx.compose.ui.text.font.FontVariation

/**
 * An implementation of [android.graphics.Paint] that supports [RemoteColor] and [RemoteShader].
 *
 * This class provides a bridge for using remote-first types with standard Android paint APIs,
 * allowing it to be easily converted to a [RemotePaint].
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class CompatAndroidRemotePaint : AndroidPaint {
    public constructor() : super()

    public constructor(flags: Int) : super(flags)

    public constructor(paint: AndroidPaint) : super(paint) {
        if (paint is CompatAndroidRemotePaint) {
            remoteColor = paint.remoteColor
            remoteColorFilter = paint.remoteColorFilter
            remoteShader = paint.remoteShader
        }
    }

    /**
     * The [RemoteColor] associated with this paint.
     *
     * Setting this property also updates the underlying [android.graphics.Paint.setColor] if the
     * [RemoteColor] has a constant value.
     */
    public var remoteColor: RemoteColor? = null
        set(value) {
            field = value
            if (value != null) {
                val constantValue = value.constantValueOrNull
                if (constantValue != null) {
                    super.setColor(constantValue.toArgb())
                } else {
                    super.setColor(android.graphics.Color.TRANSPARENT)
                }
            }
        }

    public override fun setColor(@ColorInt color: Int) {
        remoteColor = null
        super.setColor(color)
    }

    /**
     * The [RemoteColorFilter] associated with this paint.
     *
     * Setting this property also updates the underlying [android.graphics.Paint.setColorFilter] if
     * the [RemoteColorFilter] is a type that can be converted to a platform color filter.
     */
    public var remoteColorFilter: RemoteColorFilter? = null
        set(value) {
            field = value
            when (value) {
                is RemoteBlendModeColorFilter -> {
                    val constantValue = value.color.constantValueOrNull
                    if (constantValue != null) {
                        super.setColorFilter(
                            AndroidBlendModeColorFilter(
                                constantValue.toArgb(),
                                value.blendMode.toAndroidBlendMode(),
                            )
                        )
                    } else {
                        super.setColorFilter(null)
                    }
                }
                is ComposeRemoteColorFilter -> {
                    super.setColorFilter(value.composeColorFilter.asAndroidColorFilter())
                }
                null -> super.setColorFilter(null)
            }
        }

    public override fun setColorFilter(filter: AndroidColorFilter?): AndroidColorFilter? {
        remoteColorFilter = null
        return super.setColorFilter(filter)
    }

    /**
     * The [RemoteShader] associated with this paint.
     *
     * This is a convenience property that aliases [android.graphics.Paint.setShader].
     */
    public var remoteShader: RemoteShader?
        get() = shader.toRemoteShader()
        set(value) {
            shader = value.toAndroidShader()
        }
}

/** Converts a [androidx.compose.ui.graphics.Paint] to a [RemotePaint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun Paint.asRemotePaint(): RemotePaint {
    val remotePaint = StandardRemotePaint()
    remotePaint.isAntiAlias = isAntiAlias
    remotePaint.blendMode = blendMode
    remotePaint.style = style
    remotePaint.strokeWidth = strokeWidth.rf
    remotePaint.strokeCap = strokeCap
    remotePaint.strokeJoin = strokeJoin
    remotePaint.filterQuality = filterQuality
    remotePaint.shader = shader.toRemoteShader()
    remotePaint.pathEffect = pathEffect
    remotePaint.color = color.rc
    remotePaint.colorFilter = colorFilter?.let { ComposeRemoteColorFilter(it) }
    remotePaint.textSize = nativePaint.textSize.rf
    remotePaint.typeface = RemoteTypeface.fromAndroidTypeface(nativePaint.typeface)
    remotePaint.fontVariationSettings =
        parseFontVariationSettings(nativePaint.fontVariationSettings)
    return remotePaint
}

/** Converts an [android.graphics.Paint] to a [RemotePaint]. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun android.graphics.Paint.asRemotePaint(): RemotePaint {
    val compat = this as? CompatAndroidRemotePaint
    val remotePaint = StandardRemotePaint()
    remotePaint.isAntiAlias = isAntiAlias
    remotePaint.blendMode = blendMode?.toComposeBlendMode() ?: BlendMode.SrcOver
    remotePaint.style = style.toPaintingStyle()
    remotePaint.strokeWidth = strokeWidth.rf
    remotePaint.strokeCap = strokeCap.toStrokeCap()
    remotePaint.strokeJoin = strokeJoin.toStrokeJoin()
    remotePaint.filterQuality = if (isFilterBitmap) FilterQuality.Low else FilterQuality.None
    remotePaint.shader = compat?.remoteShader ?: shader.toRemoteShader()
    remotePaint.pathEffect = pathEffect?.toComposePathEffect()
    remotePaint.color = compat?.remoteColor ?: RemoteColor(color)
    remotePaint.colorFilter =
        compat?.remoteColorFilter
            ?: (colorFilter as? AndroidBlendModeColorFilter)?.let {
                RemoteBlendModeColorFilter(Color(it.color).rc, it.mode.toComposeBlendMode())
            }
    remotePaint.textSize = textSize.rf
    remotePaint.typeface = RemoteTypeface.fromAndroidTypeface(typeface)
    remotePaint.fontVariationSettings = parseFontVariationSettings(fontVariationSettings)
    return remotePaint
}

@Suppress("deprecation")
private class AndroidRemoteShaderWrapper(val remoteShader: RemoteShader) :
    android.graphics.Shader() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AndroidRemoteShaderWrapper) return false
        return remoteShader == other.remoteShader
    }

    override fun hashCode(): Int = remoteShader.hashCode()
}

private fun android.graphics.Shader?.toRemoteShader(): RemoteShader? =
    (this as? AndroidRemoteShaderWrapper)?.remoteShader

private fun RemoteShader?.toAndroidShader(): android.graphics.Shader? =
    this?.let { AndroidRemoteShaderWrapper(it) }

private fun parseFontVariationSettings(settingsString: String?): FontVariation.Settings? {
    if (settingsString == null) return null
    try {
        val axes = FontVariationAxis.fromFontVariationSettings(settingsString)
        if (axes != null) {
            val settingsList = axes.map { axis -> FontVariation.Setting(axis.tag, axis.styleValue) }
            return FontVariation.Settings(*settingsList.toTypedArray())
        }
    } catch (e: IllegalArgumentException) {
        Log.w("RemoteCompose", "invalid font variation string", e)
    }
    return null
}
