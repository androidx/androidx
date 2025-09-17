/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.compose.remote.frontend.layout

import androidx.annotation.RestrictTo
import androidx.compose.remote.frontend.state.RemoteFloat
import androidx.compose.ui.geometry.Offset

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteOffset {

    public val x: RemoteFloat
    public val y: RemoteFloat

    public constructor(x: RemoteFloat, y: RemoteFloat) {
        this.x = x
        this.y = y
    }

    public constructor(x: Float, y: Float) : this(RemoteFloat(x), RemoteFloat(y))

    public constructor(x: Float, y: RemoteFloat) : this(RemoteFloat(x), y)

    public constructor(x: RemoteFloat, y: Float) : this(x, RemoteFloat(y))

    public fun asOffset(): Offset {
        return Offset(x.internalAsFloat(), y.internalAsFloat())
    }
}
