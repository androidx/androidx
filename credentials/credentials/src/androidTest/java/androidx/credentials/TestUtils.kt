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

import android.content.Context
import android.content.pm.SigningInfo
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.graphics.drawable.toBitmap
import androidx.credentials.provider.BeginGetCredentialOption
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BeginGetCredentialResponse
import androidx.credentials.provider.CallingAppInfo
import androidx.credentials.provider.CredentialEntry
import androidx.credentials.provider.CustomCredentialEntry
import androidx.credentials.provider.PasswordCredentialEntry
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import androidx.credentials.provider.PublicKeyCredentialEntry
import com.google.common.truth.Truth.assertThat
import org.junit.Assert

/** True if the two Bundles contain the same elements, and false otherwise. */
@Suppress("DEPRECATION")
fun equals(a: Bundle, b: Bundle): Boolean {
    if (a.keySet().size != b.keySet().size) {
        return false
    }
    for (key in a.keySet()) {
        if (!b.keySet().contains(key)) {
            return false
        }

        val valA = a.get(key)
        val valB = b.get(key)
        if (valA is Bundle && valB is Bundle && !equals(valA, valB)) {
            return false
        } else {
            val isEqual = (valA?.equals(valB) ?: (valB == null))
            if (!isEqual) {
                return false
            }
        }
    }
    return true
}

@Suppress("DEPRECATION")
fun assertEquals(a: Bundle, b: Bundle) {
    assertThat(a.keySet()).containsExactlyElementsIn(b.keySet())
    for (key in a.keySet()) {
        val valA = a.get(key)
        val valB = b.get(key)
        if (valA is Bundle && valB is Bundle) {
            assertEquals(valA, valB)
        } else {
            assertThat(valA).isEqualTo(valB)
        }
    }
}

/**
 * Allows deep copying a bundle prior to API 26. Can adjust for more types, but currently that is
 * not needed.
 */
@Suppress("DEPRECATION")
fun deepCopyBundle(bundle: Bundle): Bundle {
    val newBundle = Bundle()
    for (key in bundle.keySet()) {
        val value = bundle.get(key)
        if (value is Boolean) {
            newBundle.putBoolean(key, value)
        } else if (value is String) {
            newBundle.putString(key, value)
        }
    }
    return newBundle
}

/** Used to maintain compatibility across API levels. */
const val MAX_CRED_MAN_PRE_FRAMEWORK_API_LEVEL = Build.VERSION_CODES.TIRAMISU

/**
 * True if the device running the test is post framework api level, false if pre framework api
 * level.
 */
fun isPostFrameworkApiLevel(): Boolean {
    return Build.VERSION.SDK_INT >= 34
}

fun equals(a: Icon, b: Icon): Boolean {
    if (Build.VERSION.SDK_INT <= 28) {
        return true
    }
    return a.type == b.type && a.resId == b.resId
}

fun equals(a: CallingAppInfo, b: CallingAppInfo): Boolean {
    return a.packageName == b.packageName && a.origin == b.origin
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun equals(
    createCredentialRequest: android.service.credentials.CreateCredentialRequest,
    request: ProviderCreateCredentialRequest
) {
    assertThat(createCredentialRequest.type).isEqualTo(request.callingRequest.type)
    equals(createCredentialRequest.data, request.callingRequest.credentialData)
    Assert.assertEquals(
        createCredentialRequest.callingAppInfo.packageName,
        request.callingAppInfo.packageName
    )
    Assert.assertEquals(
        createCredentialRequest.callingAppInfo.origin,
        request.callingAppInfo.origin
    )
}

fun assertEquals(
    actual: ProviderGetCredentialRequest,
    expected: ProviderGetCredentialRequest,
) {
    if (actual === expected) return
    assertThat(actual.callingAppInfo).isEqualTo(expected.callingAppInfo)
    assertThat(actual.biometricPromptResult).isEqualTo(expected.biometricPromptResult)
    assertEquals(actual.credentialOptions, expected.credentialOptions)
}

fun assertEquals(
    actual: List<CredentialOption>,
    expected: List<CredentialOption>,
) {
    if (actual === expected) return
    assertThat(actual).hasSize(expected.size)
    for (i in expected.indices) {
        assertEquals(actual[i], expected[i])
    }
}

fun assertEquals(
    actual: CredentialOption,
    expected: CredentialOption,
) {
    if (actual === expected) return
    assertThat(actual.type).isEqualTo(expected.type)
    assertEquals(actual.requestData, expected.requestData)
    assertEquals(actual.candidateQueryData, expected.candidateQueryData)
    assertThat(actual.isSystemProviderRequired).isEqualTo(expected.isSystemProviderRequired)
    assertThat(actual.isAutoSelectAllowed).isEqualTo(expected.isAutoSelectAllowed)
    assertThat(actual.allowedProviders).containsExactlyElementsIn(expected.allowedProviders)
    assertThat(actual.typePriorityHint).isEqualTo(expected.typePriorityHint)
    when (expected) {
        is GetPasswordOption -> {
            assertThat(actual).isInstanceOf(GetPasswordOption::class.java)
            assertThat((actual as GetPasswordOption).allowedUserIds)
                .containsExactlyElementsIn(expected.allowedUserIds)
        }
        is GetPublicKeyCredentialOption -> {
            assertThat(actual).isInstanceOf(GetPublicKeyCredentialOption::class.java)
            assertThat((actual as GetPublicKeyCredentialOption).requestJson)
                .isEqualTo(expected.requestJson)
            assertThat(actual.clientDataHash).isEqualTo(expected.clientDataHash)
        }
        is GetCustomCredentialOption -> {
            assertThat(actual).isInstanceOf(GetCustomCredentialOption::class.java)
        }
    }
}

fun assertEquals(
    actual: CreateCredentialResponse,
    expected: CreateCredentialResponse,
) {
    if (actual === expected) return
    assertThat(actual.type).isEqualTo(expected.type)
    assertEquals(actual.data, expected.data)
    when (expected) {
        is CreatePasswordResponse -> {
            assertThat(actual).isInstanceOf(CreatePasswordResponse::class.java)
        }
        is CreatePublicKeyCredentialResponse -> {
            assertThat(actual).isInstanceOf(CreatePublicKeyCredentialResponse::class.java)
            assertThat((actual as CreatePublicKeyCredentialResponse).registrationResponseJson)
                .isEqualTo(expected.registrationResponseJson)
        }
        is CreateCustomCredentialResponse -> {
            assertThat(actual).isInstanceOf(CreateCustomCredentialResponse::class.java)
        }
    }
}

fun assertEquals(
    actual: BeginGetCredentialRequest,
    expected: BeginGetCredentialRequest,
) {
    if (actual === expected) return
    assertThat(actual.callingAppInfo).isEqualTo(expected.callingAppInfo)
    assertEqual(actual.beginGetCredentialOptions, expected.beginGetCredentialOptions)
}

fun assertEqual(actual: List<BeginGetCredentialOption>, expected: List<BeginGetCredentialOption>) {
    if (actual === expected) return
    assertThat(actual).hasSize(expected.size)
    for (i in expected.indices) {
        assertEqual(actual[i], expected[i])
    }
}

@RequiresApi(23)
fun assertEquals(
    context: Context,
    actual: BeginGetCredentialResponse,
    expected: BeginGetCredentialResponse,
) {
    if (actual === expected) return
    assertEquals(context, actual.credentialEntries, expected.credentialEntries)
    assertThat(actual.actions).isEqualTo(expected.actions)
    assertThat(actual.authenticationActions).isEqualTo(expected.authenticationActions)
    assertThat(actual.remoteEntry).isEqualTo(expected.remoteEntry)
}

@RequiresApi(23)
fun assertEquals(
    context: Context,
    actual: List<CredentialEntry>,
    expected: List<CredentialEntry>,
) {
    if (actual === expected) return
    assertThat(actual).hasSize(expected.size)
    for (i in expected.indices) {
        assertEquals(context, actual[i], expected[i])
    }
}

@RequiresApi(23)
fun assertEquals(
    context: Context,
    actual: CredentialEntry,
    expected: CredentialEntry,
) {
    if (actual === expected) return
    assertThat(actual.type).isEqualTo(expected.type)
    assertEqual(actual.beginGetCredentialOption, expected.beginGetCredentialOption)
    assertThat(actual.entryGroupId).isEqualTo(expected.entryGroupId)
    assertThat(actual.isDefaultIconPreferredAsSingleProvider)
        .isEqualTo(expected.isDefaultIconPreferredAsSingleProvider)
    assertThat(actual.affiliatedDomain).isEqualTo(expected.affiliatedDomain)
    assertThat(actual.biometricPromptData).isEqualTo(expected.biometricPromptData)
    when (expected) {
        is PasswordCredentialEntry -> {
            assertThat(actual).isInstanceOf(PasswordCredentialEntry::class.java)
            assertThat((actual as PasswordCredentialEntry).username).isEqualTo(expected.username)
            assertThat(actual.displayName).isEqualTo(expected.displayName)
            assertThat(actual.typeDisplayName).isEqualTo(expected.typeDisplayName)
            assertThat(actual.pendingIntent).isEqualTo(expected.pendingIntent)
            if (Build.VERSION.SDK_INT >= 26) {
                assertThat(actual.lastUsedTime).isEqualTo(expected.lastUsedTime)
            }
            assertEquals(context, actual.icon, expected.icon)
            assertThat(actual.isAutoSelectAllowed).isEqualTo(expected.isAutoSelectAllowed)
            assertThat(actual.isAutoSelectAllowedFromOption)
                .isEqualTo(expected.isAutoSelectAllowedFromOption)
            assertThat(actual.hasDefaultIcon).isEqualTo(expected.hasDefaultIcon)
        }
        is PublicKeyCredentialEntry -> {
            assertThat(actual).isInstanceOf(PublicKeyCredentialEntry::class.java)
            assertThat((actual as PublicKeyCredentialEntry).username).isEqualTo(expected.username)
            assertThat(actual.displayName).isEqualTo(expected.displayName)
            assertThat(actual.typeDisplayName).isEqualTo(expected.typeDisplayName)
            assertThat(actual.pendingIntent).isEqualTo(expected.pendingIntent)
            if (Build.VERSION.SDK_INT >= 26) {
                assertThat(actual.lastUsedTime).isEqualTo(expected.lastUsedTime)
            }
            assertEquals(context, actual.icon, expected.icon)
            assertThat(actual.isAutoSelectAllowed).isEqualTo(expected.isAutoSelectAllowed)
            assertThat(actual.isAutoSelectAllowedFromOption)
                .isEqualTo(expected.isAutoSelectAllowedFromOption)
            assertThat(actual.hasDefaultIcon).isEqualTo(expected.hasDefaultIcon)
        }
        is CustomCredentialEntry -> {
            assertThat(actual).isInstanceOf(CustomCredentialEntry::class.java)
            assertThat((actual as CustomCredentialEntry).type).isEqualTo(expected.type)
            assertThat(actual.title).isEqualTo(expected.title)
            assertThat(actual.subtitle).isEqualTo(expected.subtitle)
            assertThat(actual.typeDisplayName).isEqualTo(expected.typeDisplayName)
            assertThat(actual.pendingIntent).isEqualTo(expected.pendingIntent)
            if (Build.VERSION.SDK_INT >= 26) {
                assertThat(actual.lastUsedTime).isEqualTo(expected.lastUsedTime)
            }
            assertEquals(context, actual.icon, expected.icon)
            assertThat(actual.isAutoSelectAllowed).isEqualTo(expected.isAutoSelectAllowed)
            assertThat(actual.isAutoSelectAllowedFromOption)
                .isEqualTo(expected.isAutoSelectAllowedFromOption)
            assertThat(actual.hasDefaultIcon).isEqualTo(expected.hasDefaultIcon)
        }
        else -> {
            assertThat(actual).isInstanceOf(expected.javaClass)
        }
    }
}

fun assertEqual(
    actual: BeginGetCredentialOption,
    expected: BeginGetCredentialOption,
) {
    if (actual === expected) return
    assertThat(actual.type).isEqualTo(expected.type)
    assertThat(actual.id).isEqualTo(expected.id)
    assertEquals(actual.candidateQueryData, expected.candidateQueryData)
}

@RequiresApi(23)
fun assertEquals(
    context: Context,
    actual: ProviderCreateCredentialRequest,
    expected: ProviderCreateCredentialRequest
) {
    if (actual === expected) return
    assertThat(actual.biometricPromptResult).isEqualTo(expected.biometricPromptResult)
    assertThat(actual.callingAppInfo).isEqualTo(expected.callingAppInfo)
    assertEquals(context, actual.callingRequest, expected.callingRequest)
}

@RequiresApi(23)
fun assertEquals(
    context: Context,
    actual: CreateCredentialRequest,
    expected: CreateCredentialRequest
) {
    if (actual === expected) return
    assertThat(actual.type).isEqualTo(expected.type)
    assertEquals(actual.credentialData, expected.credentialData)
    assertEquals(actual.candidateQueryData, expected.candidateQueryData)
    assertThat(actual.isSystemProviderRequired).isEqualTo(expected.isSystemProviderRequired)
    assertThat(actual.isAutoSelectAllowed).isEqualTo(expected.isAutoSelectAllowed)
    assertEquals(context, actual.displayInfo, expected.displayInfo)
    assertThat(actual.origin).isEqualTo(expected.origin)
    assertThat(actual.preferImmediatelyAvailableCredentials)
        .isEqualTo(expected.preferImmediatelyAvailableCredentials)
}

@RequiresApi(23)
fun assertEquals(
    context: Context,
    actual: CreateCredentialRequest.DisplayInfo,
    expected: CreateCredentialRequest.DisplayInfo
) {
    if (actual === expected) return
    assertThat(actual.userId).isEqualTo(expected.userId)
    assertThat(actual.userDisplayName).isEqualTo(expected.userDisplayName)
    assertThat(actual.preferDefaultProvider).isEqualTo(expected.preferDefaultProvider)
    assertEquals(context, actual.credentialTypeIcon, expected.credentialTypeIcon)
}

@RequiresApi(23)
fun assertEquals(
    context: Context,
    actual: Icon?,
    expected: Icon?,
) {
    if (actual === expected) return
    if (actual != null && expected != null) {
        val actualDrawable = actual.loadDrawable(context)
        val expectedDrawable = expected.loadDrawable(context)
        if (actualDrawable != null && expectedDrawable != null) {
            val actualBm = actualDrawable.toBitmap()
            val expectedBm = expectedDrawable.toBitmap()
            assertThat(actualBm.sameAs(expectedBm)).isTrue()
        } else {
            assertThat(actualDrawable).isEqualTo(expectedDrawable)
        }
    } else {
        assertThat(actual).isEqualTo(expected)
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun equals(
    getCredentialRequest: android.service.credentials.GetCredentialRequest,
    request: ProviderGetCredentialRequest
) {
    Assert.assertEquals(
        getCredentialRequest.callingAppInfo.packageName,
        request.callingAppInfo.packageName
    )
    Assert.assertEquals(getCredentialRequest.callingAppInfo.origin, request.callingAppInfo.origin)
    equals(getCredentialRequest.credentialOptions, request.credentialOptions)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun equals(
    credentialOptions: List<android.credentials.CredentialOption>,
    credentialOptions1: List<CredentialOption>
) {
    assertThat(credentialOptions.size).isEqualTo(credentialOptions1.size)
    for (i in credentialOptions.indices) {
        equals(credentialOptions[i], credentialOptions1[i])
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun equals(
    frameworkRequest1: android.credentials.GetCredentialRequest,
    frameworkRequest2: android.credentials.GetCredentialRequest
) {
    equals(frameworkRequest1.data, frameworkRequest2.data)
    credentialOptionsEqual(frameworkRequest1.credentialOptions, frameworkRequest2.credentialOptions)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
private fun credentialOptionsEqual(
    credentialOptions1: List<android.credentials.CredentialOption>,
    credentialOptions2: List<android.credentials.CredentialOption>
) {
    assertThat(credentialOptions1.size).isEqualTo(credentialOptions2.size)
    for (i in credentialOptions1.indices) {
        equals(credentialOptions1[i], credentialOptions2[i])
    }
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun equals(
    credentialOption: android.credentials.CredentialOption,
    credentialOption1: CredentialOption
) {
    assertThat(credentialOption.type).isEqualTo(credentialOption1.type)
    assertThat(credentialOption.isSystemProviderRequired)
        .isEqualTo(credentialOption1.isSystemProviderRequired)
    equals(credentialOption.credentialRetrievalData, credentialOption1.requestData)
    equals(credentialOption.candidateQueryData, credentialOption1.candidateQueryData)
    assertThat(credentialOption.allowedProviders).isEqualTo(credentialOption1.allowedProviders)
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun setUpCreatePasswordRequest(): android.service.credentials.CreateCredentialRequest {
    val passwordReq: CreateCredentialRequest =
        CreatePasswordRequest("test-user-id", "test-password")
    val request =
        android.service.credentials.CreateCredentialRequest(
            android.service.credentials.CallingAppInfo("calling_package", SigningInfo()),
            PasswordCredential.TYPE_PASSWORD_CREDENTIAL,
            passwordReq.credentialData
        )
    return request
}

@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
fun equals(
    credentialOption1: android.credentials.CredentialOption,
    credentialOption2: android.credentials.CredentialOption
) {
    equals(credentialOption1.candidateQueryData, credentialOption2.candidateQueryData)
    equals(credentialOption1.credentialRetrievalData, credentialOption2.credentialRetrievalData)
    assertThat(credentialOption1.type).isEqualTo(credentialOption2.type)
    assertThat(credentialOption1.allowedProviders).isEqualTo(credentialOption2.allowedProviders)
    assertThat(credentialOption1.isSystemProviderRequired)
        .isEqualTo(credentialOption2.isSystemProviderRequired)
}

fun equals(
    getCredentialResponse1: GetCredentialResponse,
    getCredentialResponse2: GetCredentialResponse
) {
    equals(getCredentialResponse1.credential, getCredentialResponse2.credential)
}

fun equals(credential1: Credential, credential2: Credential) {
    assertThat(credential1.type).isEqualTo(credential2.type)
    equals(credential1.data, credential2.data)
}
