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

package androidx.xr.scenecore.testing

import androidx.annotation.RestrictTo
import androidx.xr.runtime.XrExtensionsHolder
import androidx.xr.scenecore.runtime.extensions.XrExtensionsHolderProvider
import androidx.xr.scenecore.testing.FakeXrExtensionsHolderProvider.Companion.fakeHolder
import androidx.xr.scenecore.testing.FakeXrExtensionsHolderProvider.Companion.fakeHolderLegacy

/**
 * A fake implementation of [XrExtensionsHolderProvider] for use in tests.
 *
 * This class allows tests to provide custom [XrExtensionsHolder] instances to simulate different XR
 * extension availability.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class FakeXrExtensionsHolderProvider : XrExtensionsHolderProvider {
    /** Returns the [fakeHolder] if both [fakeHolder] and [fakeHolderLegacy] is set. */
    override val holder: XrExtensionsHolder<*>?
        get() =
            if (fakeHolderLegacy == null) {
                null
            } else {
                fakeHolder
            }

    /** Returns the [fakeHolderLegacy] if [fakeHolderLegacy] is set. */
    override val holderLegacy: XrExtensionsHolder<*>?
        get() = fakeHolderLegacy

    public companion object {
        /**
         * The value to be returned by [holder].
         *
         * The instance held in this [XrExtensionsHolder] should typically be the `underlyingObject`
         * of the instance held in [fakeHolderLegacy].
         *
         * To use this in tests, set the expected [XrExtensionsHolder] instance on this property.
         */
        public var fakeHolder: XrExtensionsHolder<*>? = null

        /**
         * The value to be returned by [holderLegacy].
         *
         * To use this in tests, set the expected [XrExtensionsHolder] instance on this property.
         */
        public var fakeHolderLegacy: XrExtensionsHolder<*>? = null
    }
}
