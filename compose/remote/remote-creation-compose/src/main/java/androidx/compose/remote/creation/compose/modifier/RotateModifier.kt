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

package androidx.compose.remote.creation.compose.modifier

import androidx.annotation.RestrictTo
import androidx.compose.remote.creation.compose.state.RemoteFloat

/**
 * Sets the degrees the view is rotated around the center of the composable. Increasing values
 * result in clockwise rotation. Negative degrees are used to rotate in the counter clockwise
 * direction
 *
 * @param degrees Degrees to rotate the content.
 * @sample androidx.compose.remote.creation.compose.samples.RotateSample
 * @see graphicsLayer
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun RemoteModifier.rotate(degrees: RemoteFloat): RemoteModifier =
    graphicsLayer(rotationZ = degrees)
