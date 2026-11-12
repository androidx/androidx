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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.rf
import androidx.compose.runtime.Immutable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Immutable
public class RemoteOffset(
    public val x: RemoteFloat,
    public val y: RemoteFloat,
) {
    public val minDimension: RemoteFloat
        get() = x.min(y)

    public companion object {
        public val Zero: RemoteOffset = RemoteOffset(0.rf, 0.rf)

        public val Infinite: RemoteOffset =
            RemoteOffset(Float.POSITIVE_INFINITY.rf, Float.POSITIVE_INFINITY.rf)
    }
}
