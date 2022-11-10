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

package androidx.privacysandbox.sdkruntime.client.loader

import android.content.Context
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import kotlin.reflect.cast

/**
 * Extract value of [Versions.API_VERSION] from loaded SDK.
 */
internal fun LocalSdk.extractApiVersion(): Int =
    extractVersionValue("API_VERSION")

/**
 * Extract value of [Versions.CLIENT_VERSION] from loaded SDK.
 */
internal fun LocalSdk.extractClientVersion(): Int =
    extractVersionValue("CLIENT_VERSION")

/**
 * Extract [SandboxedSdkProviderCompat.context] from loaded SDK.
 */
internal fun LocalSdk.extractSdkContext(): Context {
    val getContextMethod = sdkProvider
        .javaClass
        .getMethod("getContext")

    val rawContext = getContextMethod.invoke(sdkProvider)

    return Context::class.cast(rawContext)
}

/**
 * Extract field value from [SandboxedSdkProviderCompat]
 */
internal inline fun <reified T> LocalSdk.extractSdkProviderFieldValue(fieldName: String): T {
    return sdkProvider
        .javaClass
        .getField(fieldName)
        .get(sdkProvider)!! as T
}

/**
 * Extract classloader that was used for loading of [SandboxedSdkProviderCompat].
 */
internal fun LocalSdk.extractSdkProviderClassloader(): ClassLoader =
    sdkProvider.javaClass.classLoader!!

private fun LocalSdk.extractVersionValue(versionFieldName: String): Int {
    val versionsClass = Class.forName(
        Versions::class.java.name,
        false,
        extractSdkProviderClassloader()
    )
    return versionsClass.getDeclaredField(versionFieldName).get(null) as Int
}