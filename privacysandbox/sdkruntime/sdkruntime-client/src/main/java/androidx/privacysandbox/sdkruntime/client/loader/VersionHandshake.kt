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

import android.annotation.SuppressLint
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.Versions
import java.lang.reflect.InvocationTargetException

/** Performing version handshake. */
internal class VersionHandshake(
    /** Override client version during handshake. */
    private val overrideClientVersion: Int? = null,
    /** Override sdk version during handshake. */
    private val overrideSdkVersion: Int? = null,
) {

    @SuppressLint("BanUncheckedReflection") // calling method on Versions class
    fun perform(classLoader: ClassLoader?): Int {
        val versionsClass =
            Class.forName(
                "androidx.privacysandbox.sdkruntime.core.Versions",
                /* initialize = */ false,
                classLoader,
            )
        val handShakeMethod = versionsClass.getMethod("handShake", Int::class.javaPrimitiveType)

        val clientVersion = overrideClientVersion ?: Versions.API_VERSION
        try {
            val sdkVersion = handShakeMethod.invoke(null, clientVersion) as Int

            return overrideSdkVersion ?: sdkVersion
        } catch (ex: InvocationTargetException) {
            throw LoadSdkCompatException(
                LoadSdkCompatException.LOAD_SDK_NOT_FOUND,
                "Failed to perform version handshake: " + ex.targetException.message,
            )
        }
    }

    companion object {
        val DEFAULT = VersionHandshake()
    }
}
