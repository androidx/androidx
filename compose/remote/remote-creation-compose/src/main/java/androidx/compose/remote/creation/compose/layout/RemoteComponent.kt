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

/**
 * A class that provides access to remote component information.
 *
 * @param scope The scope instance.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RemoteComponent(private val scope: RemoteDrawScope) {
    private val context = RemoteFloatContext(scope.remoteComposeCreationState)

    /** The width of the component as a [RemoteFloat]. */
    public val width: RemoteFloat
        get() = context.componentWidth()

    /** The height of the component as a [RemoteFloat]. */
    public val height: RemoteFloat
        get() = context.componentHeight()

    /** The x-coordinate of the component center as a [RemoteFloat]. */
    public val centerX: RemoteFloat
        get() = context.componentCenterX()

    /** The y-coordinate of the component center as a [RemoteFloat]. */
    public val centerY: RemoteFloat
        get() = context.componentCenterY()
}
