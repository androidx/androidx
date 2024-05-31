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

import android.os.Bundle
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import java.lang.reflect.Constructor

/** Creates instance of [LoadSdkCompatException] class loaded by SDK Classloader. */
internal class LoadSdkCompatExceptionProxyFactory
private constructor(private val loadSdkCompatExceptionConstructor: Constructor<out Any>) {
    /**
     * Creates instance of [LoadSdkCompatException] class loaded by SDK Classloader.
     *
     * @param source instance of LoadSdkCompatException loaded by app classloader.
     * @return instance of LoadSdkCompatException loaded by SDK classloader.
     */
    fun createFrom(source: LoadSdkCompatException): Any {
        return loadSdkCompatExceptionConstructor.newInstance(
            source.loadSdkErrorCode,
            source.message,
            source.cause,
            source.extraInformation,
        )
    }

    companion object {
        fun createFor(classLoader: ClassLoader): LoadSdkCompatExceptionProxyFactory {
            val loadSdkCompatExceptionClass =
                Class.forName(
                    LoadSdkCompatException::class.java.name,
                    /* initialize = */ false,
                    classLoader
                )
            val loadSdkCompatExceptionConstructor =
                loadSdkCompatExceptionClass.getConstructor(
                    /* parameter1 */ Int::class.java,
                    /* parameter2 */ String::class.java,
                    /* parameter3 */ Throwable::class.java,
                    /* parameter4 */ Bundle::class.java,
                )

            return LoadSdkCompatExceptionProxyFactory(
                loadSdkCompatExceptionConstructor = loadSdkCompatExceptionConstructor
            )
        }
    }
}
