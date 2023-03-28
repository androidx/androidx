/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.credentials.provider

import android.os.Bundle
import android.service.credentials.CallingAppInfo
import androidx.annotation.RestrictTo

/**
 * Abstract request class for beginning a create credential request.
 *
 * This class is to be extended by structured create credential requests
 * such as [BeginCreatePasswordCredentialRequest].
 */
abstract class BeginCreateCredentialRequest constructor(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val type: String,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val candidateQueryData: Bundle,
    val callingAppInfo: CallingAppInfo?
)