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
import androidx.privacysandbox.sdkruntime.core.Versions

/**
 * Performing version handshake.
 *
 */
internal class VersionHandshake(
    /**
     * Override version by using [overrideApiVersion] as client and sdk version during handshake.
     */
    private val overrideApiVersion: Int? = null
) {

    @SuppressLint("BanUncheckedReflection") // calling method on Versions class
    fun perform(classLoader: ClassLoader?): Int {
        val versionsClass = Class.forName(
            Versions::class.java.name,
            false,
            classLoader
        )
        val handShakeMethod = versionsClass.getMethod("handShake", Int::class.javaPrimitiveType)

        val clientVersion = overrideApiVersion ?: Versions.API_VERSION
        val sdkVersion = handShakeMethod.invoke(null, clientVersion) as Int

        return overrideApiVersion ?: sdkVersion
    }

    companion object {
        val DEFAULT = VersionHandshake()
    }
}
