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

/**
 * A federated credential fetched from a federated identity provider (FedCM).
 *
 * Note: the FedCM proposal is still under public discussion and its constructor will be exposed
 * after the proposal is final.
 */
internal class FederatedCredential private constructor() : Credential(
    TYPE_FEDERATED_CREDENTIAL,
    Bundle(),
) {
    companion object {
        /** The type value for federated credential related operations. */
        const val TYPE_FEDERATED_CREDENTIAL: String = "type.federated_credential"
    }
}
