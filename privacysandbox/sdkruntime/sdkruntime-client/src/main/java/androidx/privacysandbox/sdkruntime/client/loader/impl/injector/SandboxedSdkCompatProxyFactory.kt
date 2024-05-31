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

package androidx.privacysandbox.sdkruntime.client.loader.impl.injector

import android.os.IBinder
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkInfo
import java.lang.reflect.Constructor

/** Creates instance of [SandboxedSdkCompat] class loaded by SDK Classloader. */
internal class SandboxedSdkCompatProxyFactory
private constructor(
    private val sandboxedSdkInfoConstructor: Constructor<out Any>,
    private val sandboxedSdkCompatConstructor: Constructor<out Any>,
) {
    /**
     * Creates instance of [SandboxedSdkCompat] class loaded by SDK Classloader.
     *
     * @param source instance of SandboxedSdkCompat loaded by app classloader.
     * @return instance of SandboxedSdkCompat loaded by SDK classloader.
     */
    fun createFrom(source: SandboxedSdkCompat): Any {
        val sdkInfo = createSdkInfoFrom(source.getSdkInfo())
        return sandboxedSdkCompatConstructor.newInstance(source.getInterface(), sdkInfo)
    }

    fun createSdkInfoFrom(source: SandboxedSdkInfo?): Any? {
        if (source == null) {
            return null
        }
        return sandboxedSdkInfoConstructor.newInstance(source.name, source.version)
    }

    companion object {
        fun createFor(classLoader: ClassLoader): SandboxedSdkCompatProxyFactory {
            val sandboxedSdkCompatClass =
                Class.forName(
                    SandboxedSdkCompat::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
            val sandboxedSdkInfoClass =
                Class.forName(
                    SandboxedSdkInfo::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
            val sandboxedSdkCompatConstructor =
                sandboxedSdkCompatClass.getConstructor(
                    /* parameter1 */ IBinder::class.java,
                    /* parameter2 */ sandboxedSdkInfoClass
                )
            val sandboxedSdkInfoConstructor =
                sandboxedSdkInfoClass.getConstructor(
                    /* parameter1 */ String::class.java,
                    /* parameter2 */ Long::class.java
                )
            return SandboxedSdkCompatProxyFactory(
                sandboxedSdkInfoConstructor = sandboxedSdkInfoConstructor,
                sandboxedSdkCompatConstructor = sandboxedSdkCompatConstructor
            )
        }
    }
}
