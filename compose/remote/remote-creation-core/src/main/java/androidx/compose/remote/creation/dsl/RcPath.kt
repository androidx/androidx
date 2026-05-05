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

@file:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)

package androidx.compose.remote.creation.dsl

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.Rc
import androidx.compose.remote.creation.RemoteComposeWriter

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@JvmInline
public value class RcDynamicPath(public val id: Int)

/** Path types for polar and other generated paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcPathType(public val value: Int) {
    Spline(Rc.PathExpression.SPLINE_PATH),
    Loop(Rc.PathExpression.LOOP_PATH),
    Monotonic(Rc.PathExpression.MONOTONIC_PATH),
    Linear(Rc.PathExpression.LINEAR_PATH),
    Polar(Rc.PathExpression.POLAR_PATH),
}

/** Path operations for combining paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcPathCombineOp(public val value: Byte) {
    Difference(RemoteComposeWriter.COMBINE_DIFFERENCE),
    Intersect(RemoteComposeWriter.COMBINE_INTERSECT),
    ReverseDifference(RemoteComposeWriter.COMBINE_REVERSE_DIFFERENCE),
    Union(RemoteComposeWriter.COMBINE_UNION),
    Xor(RemoteComposeWriter.COMBINE_XOR),
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP) @JvmInline public value class RcPath(public val id: Int)

/** Path fill types for complex paths. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public enum class RcPathFillType(public val value: Int) {
    Winding(0),
    EvenOdd(1),
}
