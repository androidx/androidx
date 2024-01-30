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

package androidx.credentials.samples

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.annotation.Sampled
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.CreateCustomCredentialResponse
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.GetCredentialException
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

@Sampled
/** Sample showing how to invoke getCredential API. */
fun callGetCredential(
    context: Context,
    activity: Activity,
    signInWithCredential: (Credential) -> Unit,
    handleGetCredentialFailure: (GetCredentialException) -> Unit,
) {
    val credentialManager = CredentialManager.create(context)

    val getPasswordOption = GetPasswordOption()

    val getPublicKeyCredentialOption = GetPublicKeyCredentialOption(
        requestJson = generateGetPasskeyRequestJsonFromServer(),
        // No need to fill this unless you are a browser and are making an origin-based request
        clientDataHash = null,
    )

    val request = GetCredentialRequest(
        credentialOptions = listOf(getPasswordOption, getPublicKeyCredentialOption)
    )

    // The API call will launch a credential selector UI for the user to pick a login credential.
    // It will be canceled if this coroutine scope is canceled. If you want the operation to persist
    // through your UI lifecycle (e.g. configuration changes), choose a coroutine scope that is
    // broader than your UI lifecycle (e.g. ViewModelScope)
    yourCoroutineScope.launch {
        try {
            val response = credentialManager.getCredential(
                // Important: use an Activity context to ensure that the system credential selector
                // ui is launched within the same activity stack to avoid undefined UI transition
                // behavior.
                context = activity,
                request = request,
            )
            signInWithCredential(response.credential)
        } catch (e: GetCredentialException) {
            handleGetCredentialFailure(e)
        }
    }
}

val yourCoroutineScope = MainScope()

fun generateGetPasskeyRequestJsonFromServer(): String {
    throw NotImplementedError("Apps using this sample code should " +
         "add a call here to generate the passkey request json from " +
         "their own server")
}

const val TAG: String = "TAG"

/**
 * Sample showing how to use a [CreateCredentialResponse] object.
 */
@Sampled
fun processCreateCredentialResponse(
    response: CreateCredentialResponse,
    loginWithPasskey: (String) -> Unit,
    loginWithPassword: () -> Unit,
    loginWithExampleCustomCredential: (CreateExampleCustomCredentialResponse) -> Unit
) {
    when (response) {
        is CreatePasswordResponse ->
            // Password saved successfully, proceed to signed in experience.
            loginWithPassword()
        is CreatePublicKeyCredentialResponse ->
            // Validate and register the registration json from your server, and if successful
            // proceed to signed in experience.
            loginWithPasskey(response.registrationResponseJson)
        is CreateCustomCredentialResponse -> {
            // If you are also using any external sign-in libraries, parse them here with the
            // utility functions provided they provided.
            if (response.type == ExampleCustomCredential.TYPE) {
                try {
                    val createExampleCustomCredentialResponse =
                        CreateExampleCustomCredentialResponse.createFrom(response)
                    loginWithExampleCustomCredential(createExampleCustomCredentialResponse)
                } catch (e: CreateExampleCustomCredentialResponse.ParsingException) {
                    // Unlikely to happen. If it does, you likely need to update the dependency
                    // version of your external sign-in library.
                    Log.e(TAG, "Failed to parse a CreateExampleCustomCredentialResponse", e)
                }
            } else {
                Log.w(
                    TAG,
                    "Received unrecognized response type ${response.type}. " +
                        "This shouldn't happen")
            }
        }
        else -> {
            Log.w(
                TAG,
                "Received unrecognized response type ${response.type}. This shouldn't happen")
        }
    }
}

@Sampled
/**
 * Sample showing how to use a [Credential] object - as a passkey credential, a password credential,
 * or any custom credential.
 */
fun processCredential(
    credential: Credential,
    fidoAuthenticateWithServer: (String) -> String,
    loginWithPasskey: (String) -> Unit,
    loginWithPassword: (String, String) -> Unit,
    loginWithExampleCustomCredential: (ExampleCustomCredential) -> Unit,
) {
    when (credential) {
        is PublicKeyCredential -> {
            val responseJson = credential.authenticationResponseJson
            val userCredential = fidoAuthenticateWithServer(responseJson)
            loginWithPasskey(userCredential)
        }
        is PasswordCredential -> {
            val userName = credential.id
            val password = credential.password
            loginWithPassword(userName, password)
        }
        is CustomCredential -> {
            // If you are also using any external sign-in libraries, parse them here with the
            // utility functions provided they provided.
            if (credential.type == ExampleCustomCredential.TYPE) {
                try {
                    val exampleCustomCredential =
                        ExampleCustomCredential.createFrom(credential.data)
                    loginWithExampleCustomCredential(exampleCustomCredential)
                } catch (e: ExampleCustomCredential.ExampleCustomCredentialParsingException) {
                    // Unlikely to happen. If it does, you likely need to update the dependency
                    // version of your external sign-in library.
                    Log.e(TAG, "Failed to parse an ExampleCustomCredential", e)
                }
            } else {
                Log.w(
                    TAG,
                    "Received unrecognized credential type ${credential.type}. " +
                        "This shouldn't happen")
            }
        }
        else -> {
            Log.w(
                TAG,
                "Received unrecognized credential type ${credential.type}. This shouldn't happen")
        }
    }
}

class ExampleCustomCredential(
    data: Bundle
) : CustomCredential(
    type = TYPE,
    data = data
) {
    companion object {
        const val TYPE = "androidx.credentials.samples.ExampleCustomCredential"

        @JvmStatic
        fun createFrom(data: Bundle): ExampleCustomCredential = ExampleCustomCredential(data)
    }

    class ExampleCustomCredentialParsingException(e: Throwable? = null) : Exception(e)
}

class CreateExampleCustomCredentialResponse(
    data: Bundle
) : CreateCustomCredentialResponse(
    type = ExampleCustomCredential.TYPE,
    data = data
) {
    companion object {
        @JvmStatic
        fun createFrom(
            response: CreateCredentialResponse
        ): CreateExampleCustomCredentialResponse =
            CreateExampleCustomCredentialResponse(response.data)
    }

    class ParsingException(e: Throwable? = null) : Exception(e)
}
