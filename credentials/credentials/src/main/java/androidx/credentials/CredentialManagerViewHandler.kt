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

@file:JvmName("CredentialManagerViewHandler")

package androidx.credentials

import android.os.Build
import android.os.OutcomeReceiver
import android.util.Log
import android.view.View
import androidx.annotation.RequiresApi
import androidx.credentials.internal.FrameworkImplHelper

/**
 * An extension API to the [View] class that allows setting of a [PendingGetCredentialRequest], that
 * in-turn contains a [GetCredentialRequest], and a callback to deliver the final
 * [GetCredentialResponse]. The associated request is invoked when the [View] is focused/clicked by
 * the user.
 *
 * A typical scenario for setting this request is a login screen with a username & password field.
 * We recommend calling the [CredentialManager.getCredential] API when this login screen loads, so
 * that the user can be presented with a selector with all credential options to choose from. In
 * addition, we recommend using this API to set the same [GetCredentialRequest] that is passed to
 * [CredentialManager.getCredential], on the username & password views. With that, if the user
 * dismisses the initial selector, and then taps on either the username or the password field, they
 * would see the same suggestions that they saw on the selector, but now on fallback UI experiences
 * such as keyboard suggestions or drop-down lists, depending on the device capabilities.
 *
 * If you have multiple views on the screen that should invoke different requests as opposed to the
 * same, you can simply use this API to set different requests on corresponding views, and hence a
 * different set of suggestions will appear.
 *
 * Note that no errors are propagated to the [PendingGetCredentialRequest.callback]. In a scenario
 * where multiple suggestions are presented to the user as part of the keyboard suggestions for
 * instance, it is possible that the user selects one, but the flow ends up in an error state, due
 * to which the final [GetCredentialResponse] cannot be propagated. In that case, user will be taken
 * back to the suggestions, and can very well select a different suggestion which would this time
 * result in a success. The intermediate error states are not propagated to the developer, and only
 * a final response, if any, is propagated.
 *
 * @property pendingGetCredentialRequest the [GetCredentialRequest] and the associated callback to
 *   be set on the view, and to be exercised when user focused on the view in question
 */
private const val TAG = "ViewHandler"

@Suppress("NewApi")
var View.pendingGetCredentialRequest: PendingGetCredentialRequest?
    get() =
        getTag(R.id.androidx_credential_pendingCredentialRequest) as? PendingGetCredentialRequest
    set(value) {
        setTag(R.id.androidx_credential_pendingCredentialRequest, value)
        if (value != null) {
            if (
                Build.VERSION.SDK_INT >= 35 ||
                    (Build.VERSION.SDK_INT == 34 && Build.VERSION.PREVIEW_SDK_INT > 0)
            ) {
                Api35Impl.setPendingGetCredentialRequest(this, value.request, value.callback)
            }
        } else {
            if (
                Build.VERSION.SDK_INT >= 35 ||
                    (Build.VERSION.SDK_INT == 34 && Build.VERSION.PREVIEW_SDK_INT > 0)
            ) {
                Api35Impl.clearPendingGetCredentialRequest(this)
            }
        }
    }

@RequiresApi(35)
private object Api35Impl {
    fun setPendingGetCredentialRequest(
        view: View,
        request: GetCredentialRequest,
        callback: (GetCredentialResponse) -> Unit,
    ) {
        val frameworkRequest = FrameworkImplHelper.convertGetRequestToFrameworkClass(request)
        val frameworkCallback =
            object :
                OutcomeReceiver<
                    android.credentials.GetCredentialResponse,
                    android.credentials.GetCredentialException
                > {
                override fun onResult(response: android.credentials.GetCredentialResponse) {
                    callback.invoke(FrameworkImplHelper.convertGetResponseToJetpackClass(response))
                }

                override fun onError(error: android.credentials.GetCredentialException) {
                    Log.w(TAG, "Error: " + error.type + " , " + error.message)
                }
            }
        view.setPendingCredentialRequest(frameworkRequest, frameworkCallback)
    }

    fun clearPendingGetCredentialRequest(view: View) {
        view.clearPendingCredentialRequest()
    }
}
