/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.credentials

import android.os.Bundle
import androidx.annotation.RestrictTo

/**
 * Base class for getting a specific type of credentials.
 *
 * [GetCredentialRequest] will be composed of a list of [GetCredentialOption] subclasses to indicate
 * the specific credential types and configurations that your app accepts.
 */
open class GetCredentialOption(
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val type: String,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val data: Bundle,
    /** @hide */
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    val requireSystemProvider: Boolean,
)