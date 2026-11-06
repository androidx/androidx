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

/** Dynamic layout measurements of a RemoteCompose component in pixels. */
@Stable
public class RemoteComponent private constructor(private val context: RemoteFloatContext) {
    /**
     * Creates a [RemoteComponent] backed by [scope].
     *
     * @param scope [RemoteDocumentScope] providing the active component or target document
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(scope: RemoteDocumentScope) : this(RemoteFloatContext(scope))

    /**
     * Creates a [RemoteComponent] bound to [token].
     *
     * @param token identity token of the target layout component
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public constructor(token: RemoteComponentToken) : this(RemoteFloatContext(token))

    /** Width of this component in pixels. */
    public val width: RemoteFloat
        get() = context.componentWidth()

    /** Height of this component in pixels. */
    public val height: RemoteFloat
        get() = context.componentHeight()

    /** Horizontal center (`width / 2`) of this component in pixels. */
    public val centerX: RemoteFloat
        get() = context.componentCenterX()

    /** Vertical center (`height / 2`) of this component in pixels. */
    public val centerY: RemoteFloat
        get() = context.componentCenterY()

    /**
     * Position of this component's top-left corner relative to its parent in pixels, or `(0, 0)`
     * for the root component.
     */
    public val positionInParent: RemoteOffset
        get() = RemoteOffset(context.componentX(), context.componentY())

    /** Position of this component's top-left corner relative to the root in pixels. */
    public val positionInRoot: RemoteOffset
        get() = RemoteOffset(context.componentRootX(), context.componentRootY())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RemoteComponent) return false
        return context == other.context
    }

    override fun hashCode(): Int = context.hashCode()
}
