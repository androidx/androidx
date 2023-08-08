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

package androidx.privacysandbox.sdkruntime.client.loader.impl.injector

import android.os.IBinder
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import java.lang.reflect.Constructor

/**
 * Create instance of [AppOwnedSdkSandboxInterfaceCompat] class loaded by SDK Classloader.
 */
internal class AppOwnedSdkInterfaceProxyFactory(
    private val appOwnedSdkSandboxInterfaceCompatConstructor: Constructor<out Any>
) {

    /**
     * Creates instance of [AppOwnedSdkSandboxInterfaceCompat] class loaded by SDK Classloader.
     *
     * @param source instance of AppOwnedSdkSandboxInterfaceCompat loaded by app classloader.
     * @return instance of AppOwnedSdkSandboxInterfaceCompat loaded by SDK classloader.
     */
    fun createFrom(source: AppOwnedSdkSandboxInterfaceCompat): Any {
        return appOwnedSdkSandboxInterfaceCompatConstructor.newInstance(
            /* parameter1 */ source.getName(),
            /* parameter2 */ source.getVersion(),
            /* parameter3 */ source.getInterface()
        )
    }

    companion object {
        fun createFor(classLoader: ClassLoader): AppOwnedSdkInterfaceProxyFactory {
            val appOwnedSdkSandboxInterfaceCompatClass = Class.forName(
                AppOwnedSdkSandboxInterfaceCompat::class.java.name,
                /* initialize = */ false,
                classLoader
            )
            val appOwnedSdkSandboxInterfaceCompatConstructor =
                appOwnedSdkSandboxInterfaceCompatClass.getConstructor(
                    /* name      */ String::class.java,
                    /* version   */ Long::class.java,
                    /* interface */ IBinder::class.java
                )
            return AppOwnedSdkInterfaceProxyFactory(appOwnedSdkSandboxInterfaceCompatConstructor)
        }
    }
}
