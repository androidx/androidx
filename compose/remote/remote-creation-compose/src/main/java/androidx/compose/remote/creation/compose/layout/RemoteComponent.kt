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
import androidx.compose.remote.creation.compose.action.RemoteAction
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.drawWithContent
import androidx.compose.remote.creation.compose.state.RemoteFloat
import androidx.compose.remote.creation.compose.state.RemoteStateScope

/** Dynamic layout measurements of a RemoteCompose component in pixels. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComponent private constructor(private val context: RemoteFloatContext) {
    internal constructor(scope: RemoteStateScope) : this(RemoteFloatContext(scope))

    internal constructor(token: RemoteComponentToken) : this(RemoteFloatContext(token))

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

    /** Horizontal offset of this component relative to its parent in pixels. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val x: RemoteFloat
        get() = context.componentX()

    /** Vertical offset of this component relative to its parent in pixels. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val y: RemoteFloat
        get() = context.componentY()

    /** Horizontal offset of this component relative to the root in pixels. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val rootX: RemoteFloat
        get() = context.componentRootX()

    /** Vertical offset of this component relative to the root in pixels. */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public val rootY: RemoteFloat
        get() = context.componentRootY()
}

/**
 * Returns a [RemoteComponent] bound to this [RemoteStateScope]'s active component.
 *
 * * Resolves to the target layout component when called from a [RemoteModifier] or [RemoteAction]
 *   scope (such as [drawWithContent]).
 * * Resolves to the component currently being drawn when called from a [RemoteDrawScope] or
 *   [RemoteCanvas].
 *
 * @return [RemoteComponent] bound to the active component in this [RemoteStateScope]
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteStateScope.remoteComponent(): RemoteComponent {
    val token = resolveComponentToken()
    return if (token != null) RemoteComponent(token) else RemoteComponent(this)
}
