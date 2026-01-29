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

package androidx.compose.remote.creation.compose.layout

import androidx.annotation.RestrictTo

public val createIds: CreateIds = CreateIds()

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class CreateIds {
    public var IdIndices: Int = 0

    public operator fun component1(): Int {
        return IdIndices++
    }

    public operator fun component2(): Int {
        return IdIndices++
    }

    public operator fun component3(): Int {
        return IdIndices++
    }

    public operator fun component4(): Int {
        return IdIndices++
    }

    public operator fun component5(): Int {
        return IdIndices++
    }

    public operator fun component6(): Int {
        return IdIndices++
    }

    public operator fun component7(): Int {
        return IdIndices++
    }

    public operator fun component8(): Int {
        return IdIndices++
    }
}
