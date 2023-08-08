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

package androidx.credentials.webauthn

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class PublicKeyCredentialRpEntity(val name: String, val id: String)

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class PublicKeyCredentialUserEntity(
  val name: String,
  val id: ByteArray,
  val displayName: String
)

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class PublicKeyCredentialParameters(val type: String, val alg: Long)

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class PublicKeyCredentialDescriptor(
  val type: String,
  val id: ByteArray,
  val transports: List<String>
)

@RestrictTo(RestrictTo.Scope.LIBRARY)
data class AuthenticatorSelectionCriteria(
  val authenticatorAttachment: String,
  val residentKey: String,
  val requireResidentKey: Boolean = false,
  val userVerification: String = "preferred"
)
