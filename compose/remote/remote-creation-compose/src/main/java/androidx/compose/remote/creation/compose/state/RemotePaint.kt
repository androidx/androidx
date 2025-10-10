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
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import androidx.annotation.ColorInt
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope
import androidx.compose.remote.creation.compose.capture.RemoteComposeCreationState

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

    /** The current [RemoteColorFilter] if any. */
    public var remoteColorFilter: RemoteColorFilter? = null
        set(remoteColorFilter) {
            field = remoteColorFilter
            // We don't want both a ColorFilter and a RemoteColorFilter.
            super.setColorFilter(null)
        }

    override fun setColorFilter(filter: ColorFilter?): ColorFilter? {
        // We don't want both a ColorFilter and a RemoteColorFilter.
        remoteColorFilter = null
        return super.setColorFilter(filter)
    }

    /**
     * The [RemoteColor] to paint with, if any. If non-null overrides the value passed into
     * [setColor].
     */
    public var remoteColor: RemoteColor? = null
        set(value) {
            field = value
            // We don't want both a Color and a RemoteColor, but color is not optional so instead
            // set to a known value.
            if (value != null) {
                super.setColor(Color.TRANSPARENT)
            }
        }

    override fun setColor(@ColorInt color: Int) {
        // We don't want both a Color and a RemoteColor.
        remoteColor = null // Note this clears the color to transparent as a sideeffect.
        super.setColor(color)
    }

    internal fun getColorLong(creationState: RemoteComposeCreationState): Long? {
        remoteColor?.let {
            return it.constantValue?.let { it.pack() }
                ?: it.getIdForCreationState(creationState).toLong()
        }
        return null
    }
}
