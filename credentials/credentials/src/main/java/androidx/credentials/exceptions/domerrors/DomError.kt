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

package androidx.credentials.exceptions.domerrors

import androidx.annotation.RestrictTo
import androidx.annotation.VisibleForTesting
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException

/**
 * While CredentialManager flows use Exceptions, in some cases, the exceptions are focused on a
 * range of widely used, uncommonly named 'errors'. This class should be used to generate subclass
 * errors, packaged under an Exception superclass. Please see the example below for usage details.
 *
 * For this codebase, we use this widely with errors from the public key credential and general web
 * error specs, shown [here](https://webidl.spec.whatwg.org/#idl-DOMException-error-names).
 *
 * In this example, we create a wrapper exception named [CreatePublicKeyCredentialDomException].
 * This contains a constructor that accepts a DomError. We then employ various sub classes to the
 * DomError for individual error types, such as [AbortError].
 * ```
 * class AbortError : DomError(type_var) { ... }
 * ```
 * Then it is expected that when abort errors show up in code, one can create the wrapper exception
 * with the designed DomError subclass.
 * ```
 * // ... (logic checking for abort error) ...
 * val exception = CreatePublicKeyCredentialDomException(AbortError(), e.getMessage())
 * // ... (logic using exception to throw or pass in callback) ...
 * ```
 * This utilization may vary by use case.
 */
abstract class DomError(
    @get:VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    @get:RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    open val type: String
)
