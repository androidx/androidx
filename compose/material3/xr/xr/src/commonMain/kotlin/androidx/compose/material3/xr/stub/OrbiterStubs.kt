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

package androidx.compose.material3.xr.stub

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.compose.material3.xr.spatial.HorizontalOrbiterProperties
import androidx.compose.material3.xr.spatial.VerticalOrbiterProperties
import androidx.compose.runtime.Composable

@RestrictTo(LIBRARY_GROUP)
public interface XrHorizontalOrbiterStub {
    @Composable
    public fun Orbiter(properties: HorizontalOrbiterProperties, content: @Composable () -> Unit)
}

@RestrictTo(LIBRARY_GROUP)
public interface XrVerticalOrbiterStub {
    @Composable
    public fun Orbiter(properties: VerticalOrbiterProperties, content: @Composable () -> Unit)
}
