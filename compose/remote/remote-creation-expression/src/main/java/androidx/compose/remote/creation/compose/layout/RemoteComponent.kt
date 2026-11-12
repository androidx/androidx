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
import androidx.compose.remote.creation.compose.capture.RemoteDocumentScope
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.runtime.Stable

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Stable
public class RemoteComponent internal constructor(private val context: RemoteFloatContext) {
    public val width: RemoteFloat
        get() = context.componentWidth()

    public val height: RemoteFloat
        get() = context.componentHeight()

    public val centerX: RemoteFloat
        get() = context.componentCenterX()

    public val centerY: RemoteFloat
        get() = context.componentCenterY()

    public val positionInParent: RemoteOffset
        get() = RemoteOffset(context.componentX(), context.componentY())

    public val positionInRoot: RemoteOffset
        get() = RemoteOffset(context.componentRootX(), context.componentRootY())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteComponent) return false
        return context == other.context
    }

    override fun hashCode(): Int = context.hashCode()
}

/**
 * Creates a [RemoteComponent] backed by [scope].
 *
 * @param scope [RemoteDocumentScope] providing the active component or target document
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComponent(scope: RemoteDocumentScope): RemoteComponent =
    RemoteComponent(RemoteFloatContext(scope))

/**
 * Creates a [RemoteComponent] bound to [token].
 *
 * @param token identity token of the target layout component
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteComponent(token: RemoteComponentToken): RemoteComponent =
    RemoteComponent(RemoteFloatContext(token))
