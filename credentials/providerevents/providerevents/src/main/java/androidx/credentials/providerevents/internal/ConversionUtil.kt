/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.credentials.providerevents.internal

import androidx.annotation.RestrictTo
import androidx.credentials.providerevents.exception.ImportCredentialsException
import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException.Companion.TYPE_IMPORT_CREDENTIALS_INVALID_JSON_EXCEPTION
import androidx.credentials.providerevents.exception.ImportCredentialsNoExportOptionException
import androidx.credentials.providerevents.exception.ImportCredentialsNoExportOptionException.Companion.TYPE_IMPORT_CREDENTIALS_NO_EXPORT_OPTION
import androidx.credentials.providerevents.exception.ImportCredentialsProviderConfigurationException
import androidx.credentials.providerevents.exception.ImportCredentialsProviderConfigurationException.Companion.TYPE_IMPORT_CREDENTIALS_PROVIDER_CONFIGURATION_EXCEPTION
import androidx.credentials.providerevents.exception.ImportCredentialsSystemErrorException
import androidx.credentials.providerevents.exception.ImportCredentialsSystemErrorException.Companion.TYPE_IMPORT_CREDENTIALS_SYSTEM_ERROR_EXCEPTION
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownCallerException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownCallerException.Companion.TYPE_IMPORT_CREDENTIALS_UNKNOWN_CALLER_EXCEPTION
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException.Companion.TYPE_IMPORT_CREDENTIALS_UNKNOWN_ERROR_EXCEPTION
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public fun toJetpackGetException(errorType: String, errorMsg: String?): ImportCredentialsException {

    return when (errorType) {
        TYPE_IMPORT_CREDENTIALS_INVALID_JSON_EXCEPTION ->
            ImportCredentialsInvalidJsonException(errorMsg)
        TYPE_IMPORT_CREDENTIALS_PROVIDER_CONFIGURATION_EXCEPTION ->
            ImportCredentialsProviderConfigurationException(errorMsg)
        TYPE_IMPORT_CREDENTIALS_SYSTEM_ERROR_EXCEPTION ->
            ImportCredentialsSystemErrorException(errorMsg)
        TYPE_IMPORT_CREDENTIALS_UNKNOWN_CALLER_EXCEPTION ->
            ImportCredentialsUnknownCallerException(errorMsg)
        TYPE_IMPORT_CREDENTIALS_UNKNOWN_ERROR_EXCEPTION ->
            ImportCredentialsUnknownErrorException(errorMsg)
        TYPE_IMPORT_CREDENTIALS_NO_EXPORT_OPTION ->
            ImportCredentialsNoExportOptionException(errorMsg)
        else -> {
            ImportCredentialsUnknownErrorException(errorMsg)
        }
    }
}

internal fun toRequestJson(credentialTypes: Set<String>, knownExtensions: Set<String>): String {
    val credentialTypesJson = JSONArray(credentialTypes)
    val knownExtensionsJson = JSONArray(knownExtensions)

    return JSONObject()
        .apply {
            put(CREDENTIAL_TYPES_JSON_KEY, credentialTypesJson)
            put(KNOWN_EXTENSIONS_JSON_KEY, knownExtensionsJson)
        }
        .toString()
}

internal fun getCredentialTypes(jsonObject: JSONObject): Set<String> {
    return jsonArrayToStringSet(jsonObject, CREDENTIAL_TYPES_JSON_KEY)
}

internal fun getKnownExtensions(jsonObject: JSONObject): Set<String> {
    try {
        return jsonArrayToStringSet(jsonObject, KNOWN_EXTENSIONS_JSON_KEY)
    } catch (e: JSONException) {}
    return setOf()
}

private fun jsonArrayToStringSet(jsonObject: JSONObject, jsonKey: String): Set<String> {
    val jsonArray = jsonObject.getJSONArray(jsonKey)
    val list = mutableListOf<String>()
    for (i in 0 until jsonArray.length()) {
        list.add(jsonArray.getString(i))
    }
    return list.toSet()
}

internal const val CREDENTIAL_TYPES_JSON_KEY = "credentialTypes"
internal const val KNOWN_EXTENSIONS_JSON_KEY = "knownExtensions"
