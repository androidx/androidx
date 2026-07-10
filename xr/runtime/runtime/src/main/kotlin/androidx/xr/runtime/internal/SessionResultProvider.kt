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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionConfigureResult
import androidx.xr.runtime.SessionCreateResult

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
/**
 * Testing interface that affects the result of [Session.create] and [Session.configure].
 *
 * Users shouldn't use this interface directly. Instead, use
 * [androidx.xr.runtime.testing.SessionTestRule] to control the behavior of Sessions in unit tests.
 */
public interface SessionResultProvider {

    /**
     * If not null, [Session.create] will return this value instead of using its normal behavior.
     */
    public val createResult: SessionCreateResult?

    /**
     * If not null, [Session.configure] will return this value instead of using its normal behavior.
     */
    public val configureResult: SessionConfigureResult?
}
