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

@file:JvmName("IntentHandlerConverters")
@file:SuppressLint("ClassVerificationFailure")

package androidx.credentials.provider

import android.annotation.SuppressLint
import android.content.Intent
import android.service.credentials.CredentialProviderService
import androidx.annotation.RequiresApi
import androidx.credentials.CreateCredentialResponse
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.provider.utils.BeginGetCredentialUtil

/**
 * IntentHandlerConverters is a util class for PendingIntentHandler
 * that can be used for tests. It contains methods that convert the
 * intents back into their original credential objects.
 */

/**
 * Returns the stored create credential exception from the intent.
 */
@RequiresApi(34)
fun Intent.getCreateCredentialException():
    android.credentials.CreateCredentialException? {
   var key = CredentialProviderService.EXTRA_CREATE_CREDENTIAL_EXCEPTION
   if (!hasExtra(key)) {
       return null
   }

   return getParcelableExtra(
       key,
       android.credentials.CreateCredentialException::class.java)
}

/**
 * Returns the stored get credential exception from the intent.
 */
@RequiresApi(34)
fun Intent.getGetCredentialException(): android.credentials.GetCredentialException? {
   var key = CredentialProviderService.EXTRA_GET_CREDENTIAL_EXCEPTION
   if (!hasExtra(key)) {
       return null
   }

   return getParcelableExtra(
       key,
       android.credentials.GetCredentialException::class.java)
}

/**
 * Returns the begin get response from the intent.
 */
@RequiresApi(34)
fun Intent.getBeginGetResponse(): BeginGetCredentialResponse? {
   var key = CredentialProviderService.EXTRA_BEGIN_GET_CREDENTIAL_RESPONSE
   if (!hasExtra(key)) {
       return null
   }

   var res = getParcelableExtra(
       key,
       android.service.credentials.BeginGetCredentialResponse::class.java)
   if (res == null) {
       return null
   }

   return BeginGetCredentialUtil.convertToJetpackResponse(res)
}

/**
 * Returns the get response from the intent.
 */
@RequiresApi(34)
fun Intent.getGetCredentialResponse(): android.credentials.GetCredentialResponse? {
   var key = CredentialProviderService.EXTRA_GET_CREDENTIAL_RESPONSE
   if (!hasExtra(key)) {
       return null
   }

   return getParcelableExtra(
      key,
      android.credentials.GetCredentialResponse::class.java)
}

/**
 * Returns the create response from the intent.
 */
@RequiresApi(34)
fun Intent.getCreateCredentialCredentialResponse():
    android.credentials.CreateCredentialResponse? {
   var key = CredentialProviderService.EXTRA_CREATE_CREDENTIAL_RESPONSE
   if (!hasExtra(key)) {
       return null
   }

   return getParcelableExtra(
       key,
       android.credentials.CreateCredentialResponse::class.java)
}
