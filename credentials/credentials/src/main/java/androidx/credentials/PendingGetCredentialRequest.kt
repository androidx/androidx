/*
 * Copyright 2024 The Android Open Source Project
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

import android.view.View

/**
 * Request to be set on an Android [View], which will be invoked when the [View] is focused/clicked
 * by the user.
 *
 * Note that the [callback] only handles a final [GetCredentialResponse] and no errors are
 * propagated to the callback.
 *
 * See [View.setPendingCredentialRequest] for details on how this request will be used.
 *
 * @property request the [GetCredentialRequest] to be invoked when a given view on which this
 *   request is set is focused
 * @property callback the callback on which the final [GetCredentialResponse] is returned, after the
 *   user has made its selections
 */
class PendingGetCredentialRequest(
    val request: GetCredentialRequest,
    val callback: (GetCredentialResponse) -> Unit
)
