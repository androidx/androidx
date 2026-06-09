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

package androidx.pdf.constants

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo

/** Defines integer constants to represent path operations in a PDF object. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public object PathOps {
    /** Starts a new sub-path from the given coordinate. */
    public const val MOVE_TO: Int = 0

    /** Draws a line from the previous point to the given coordinate. */
    public const val LINE_TO: Int = 1
}

/** Annotation for path operations. */
@Retention(AnnotationRetention.SOURCE)
@IntDef(PathOps.MOVE_TO, PathOps.LINE_TO)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public annotation class PathOp
