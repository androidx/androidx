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

package androidx.compose.material3.xr

import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP
import androidx.compose.material3.xr.stub.XrHorizontalOrbiterStub
import androidx.compose.material3.xr.stub.XrVerticalOrbiterStub
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * XR Stubs, which enable libraries like Material3 to call XR APIs without forcing a dependency on
 * the XR libs.
 */
@RestrictTo(LIBRARY_GROUP)
public interface XrStubs {
    @Composable public fun horizontalOrbiterStub(): XrHorizontalOrbiterStub?

    @Composable public fun verticalOrbiterStub(): XrVerticalOrbiterStub?

    @RestrictTo(LIBRARY_GROUP)
    public companion object {
        /** Sets the [LocalXrStubs] in this Compose [content] hierarchy to [stubs]. */
        @Composable
        public fun Set(stubs: XrStubs?, content: @Composable () -> Unit) {
            CompositionLocalProvider(
                LocalXrStubs provides (stubs ?: NoOpXrStubs),
                content = content,
            )
        }

        /** Returns the [XrStubs] set via [Set] in this Compose hierarchy, if any. */
        @Composable public fun get(): XrStubs = LocalXrStubs.current
    }
}

private val LocalXrStubs = staticCompositionLocalOf<XrStubs> { NoOpXrStubs }

private object NoOpXrStubs : XrStubs {
    @Composable override fun horizontalOrbiterStub(): XrHorizontalOrbiterStub? = null

    @Composable override fun verticalOrbiterStub(): XrVerticalOrbiterStub? = null
}
