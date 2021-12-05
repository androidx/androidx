/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:SuppressLint("ClassVerificationFailure") // Entire file is RequiresApi(19)
@file:Suppress("NOTHING_TO_INLINE")

package androidx.core.graphics

import android.annotation.SuppressLint
import android.graphics.Path
import androidx.annotation.RequiresApi

/**
 * Flattens (or approximate) the [Path] with a series of line segments.
 *
 * @param error The acceptable error for a line on the Path in pixels. Typically this would be
 *              0.5 so that the error is less than half a pixel. This value must be
 *              positive and is set to 0.5 by default.
 *
 * @see Path.approximate
 */
@RequiresApi(26)
public fun Path.flatten(error: Float = 0.5f): Iterable<PathSegment> =
    PathUtils.flatten(this, error)

/**
 * Returns the union of two paths as a new [Path].
 */
@RequiresApi(19)
public inline operator fun Path.plus(p: Path): Path {
    return Path(this).apply {
        op(p, Path.Op.UNION)
    }
}

/**
 * Returns the difference of two paths as a new [Path].
 */
@RequiresApi(19)
public inline operator fun Path.minus(p: Path): Path {
    return Path(this).apply {
        op(p, Path.Op.DIFFERENCE)
    }
}

/**
 * Returns the union of two paths as a new [Path].
 */
@RequiresApi(19)
public inline infix fun Path.or(p: Path): Path = this + p

/**
 * Returns the intersection of two paths as a new [Path].
 * If the paths do not intersect, returns an empty path.
 */
@RequiresApi(19)
public inline infix fun Path.and(p: Path): Path {
    return Path().apply {
        op(this@and, p, Path.Op.INTERSECT)
    }
}

/**
 * Returns the union minus the intersection of two paths as a new [Path].
 */
@RequiresApi(19)
public inline infix fun Path.xor(p: Path): Path {
    return Path(this).apply {
        op(p, Path.Op.XOR)
    }
}
