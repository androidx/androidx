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

package androidx.privacysandbox.sdkruntime.core

import android.annotation.SuppressLint
import android.os.IBinder
import androidx.annotation.RestrictTo

/**
 * Temporary converter of [AppOwnedSdkSandboxInterfaceCompat] to platform
 * AppOwnedSdkSandboxInterface and back.
 *
 * Could be used only with Developer Preview 8 build.
 * Using reflection to call public methods / constructors.
 *
 * TODO(b/281397807) Should be removed as soon as prebuilt with new API will be available.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class AppOwnedInterfaceConverter {

    private val appOwnedInterfaceClass = Class.forName(
        "android.app.sdksandbox.AppOwnedSdkSandboxInterface"
    )

    private val appOwnedInterfaceConstructor = appOwnedInterfaceClass.getConstructor(
        /* name      */ String::class.java,
        /* version   */ Long::class.java,
        /* interface */ IBinder::class.java
    )

    private val getNameMethod = appOwnedInterfaceClass.getMethod("getName")

    private val getVersionMethod = appOwnedInterfaceClass.getMethod("getVersion")

    private val getInterfaceMethod = appOwnedInterfaceClass.getMethod("getInterface")

    @SuppressLint("BanUncheckedReflection") // calling public non restricted methods
    fun toCompat(platformObject: Any): AppOwnedSdkSandboxInterfaceCompat {
        val name = getNameMethod.invoke(platformObject) as String
        val version = getVersionMethod.invoke(platformObject) as Long
        val binder = getInterfaceMethod.invoke(platformObject) as IBinder
        return AppOwnedSdkSandboxInterfaceCompat(name, version, binder)
    }

    fun toPlatform(compatObject: AppOwnedSdkSandboxInterfaceCompat): Any {
        return appOwnedInterfaceConstructor.newInstance(
            compatObject.getName(),
            compatObject.getVersion(),
            compatObject.getInterface()
        )
    }
}
