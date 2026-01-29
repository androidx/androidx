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
@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.compose.state

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState
import androidx.compose.remote.creation.compose.layout.RemoteSize
import androidx.compose.remote.creation.compose.shaders.RemoteBrush
import androidx.compose.remote.creation.compose.shaders.RemoteSolidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/** Base type for [ColorFilter]s that are parameterized by expressions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) public interface RemoteColorFilter

/**
 * An [RemoteColorFilter] that represents a [BlendModeColorFilter] where the [color] is
 * parameterized by a [RemoteColor] expression.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteBlendModeColorFilter(
    public val color: RemoteColor,
    public val blendMode: BlendMode,
) : RemoteColorFilter

/**
 * An extension of [Paint] that supports binding expressions where we can't do that via existing
 * APIs.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public open class RemotePaint : Paint {
    /**
     * Constructs a [RemotePaint] with the default arguments.
     *
     * @see [Paint]'s default constructor.
     */
    public constructor() : super()

    /**
     * Constructs a [RemotePaint] with the with the provided flags.
     *
     * @see [Paint]'s constructor with a flag.
     */
    public constructor(flags: Int) : super(flags)

    /**
     * Constructs a [RemotePaint] with the all the settings from the provided paint.
     *
     * @see [Paint]'s copy constructor.
     */
    public constructor(paint: Paint) : super(paint) {
        if (paint is RemotePaint) {
            if (paint.remoteColorFilter != null) {
                remoteColorFilter = paint.remoteColorFilter
            }
            if (paint.remoteColor != null) {
                remoteColor = paint.remoteColor
            }
        }
    }

    init {
        if (typeface == null) {
            typeface = Typeface.DEFAULT
        }
    }

    /**
     * The current [RemoteColorFilter] if any.
     *
     * Note if this is assigned with a [RemoteBlendModeColorFilter] with a constant [RemoteColor]
     * then this will also call [setColorFilter] with either the corresponding
     * [BlendModeColorFilter] or null if the [RemoteColor] is not constant.
     */
    public var remoteColorFilter: RemoteColorFilter? = null
        set(remoteColorFilter) {
            field = remoteColorFilter
            when {
                remoteColorFilter is RemoteBlendModeColorFilter -> {
                    val constantValue = remoteColorFilter.color.constantValueOrNull
                    if (constantValue != null) {
                        super.setColorFilter(
                            BlendModeColorFilter(
                                constantValue.toArgb(),
                                remoteColorFilter.blendMode,
                            )
                        )
                    } else {
                        super.setColorFilter(null)
                    }
                }

                else -> super.setColorFilter(null)
            }
        }

    override fun setColorFilter(filter: ColorFilter?): ColorFilter? {
        // We don't want both a ColorFilter and a RemoteColorFilter.
        remoteColorFilter = null
        return super.setColorFilter(filter)
    }

    /**
     * The [RemoteColor] to paint with, if any.
     *
     * Note if this is assigned with a constant [RemoteColor] then this will also call [setColor]
     * with the corresponding ARGB value, or [Color.TRANSPARENT] if the [RemoteColor] is not
     * constant.
     */
    public var remoteColor: RemoteColor? = null
        set(value) {
            field = value
            if (value != null) {
                val constantValue = value.constantValueOrNull
                if (constantValue != null) {
                    super.setColor(constantValue.toArgb())
                } else {
                    // If the remote color isn't a constant value then we don't have a way of
                    // accuratly its via setColor, so set it to a known value.
                    super.setColor(android.graphics.Color.TRANSPARENT)
                }
            }
        }

    override fun setColor(@ColorInt color: Int) {
        // We don't want both a Color and a RemoteColor.
        remoteColor = null // Note this clears the color to transparent as a sideeffect.
        super.setColor(color)
    }

    public fun RemoteStateScope.applyRemoteBrush(remoteBrush: RemoteBrush, size: RemoteSize) {
        if (remoteBrush.hasShader) {
            shader = with(remoteBrush) { createShader(size) }
            remoteColor = null
        } else if (remoteBrush is RemoteSolidColor) {
            remoteColor = remoteBrush.color
            shader = null
        } else {
            throw UnsupportedOperationException("Unsupported brush type: $remoteBrush")
        }
    }

    internal fun getColorLong(creationState: RemoteComposeCreationState): Long? {
        remoteColor?.let {
            return it.constantValueOrNull?.pack()
                ?: it.getIdForCreationState(creationState).toLong()
        }
        return null
    }
}
