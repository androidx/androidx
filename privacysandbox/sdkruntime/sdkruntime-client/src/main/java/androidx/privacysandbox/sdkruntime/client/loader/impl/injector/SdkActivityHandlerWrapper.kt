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

import android.annotation.SuppressLint
import androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import java.lang.reflect.Method

/**
 * Creates reflection wrapper for implementation of [SdkSandboxActivityHandlerCompat] interface
 * loaded by SDK classloader.
 */
internal class SdkActivityHandlerWrapper private constructor(
    private val activityHolderProxyFactory: ActivityHolderProxyFactory,
    private val handlerOnActivityCreatedMethod: Method,
) {

    fun wrapSdkSandboxActivityHandlerCompat(handlerCompat: Any): SdkSandboxActivityHandlerCompat =
        WrappedHandler(handlerCompat, handlerOnActivityCreatedMethod, activityHolderProxyFactory)

    private class WrappedHandler(
        private val originalHandler: Any,
        private val handlerOnActivityCreatedMethod: Method,
        private val activityHolderProxyFactory: ActivityHolderProxyFactory
    ) : SdkSandboxActivityHandlerCompat {

        @SuppressLint("BanUncheckedReflection") // using reflection on library classes
        override fun onActivityCreated(activityHolder: ActivityHolder) {
            val activityHolderProxy = activityHolderProxyFactory.createProxyFor(activityHolder)
            handlerOnActivityCreatedMethod.invoke(originalHandler, activityHolderProxy)
        }
    }

    companion object {
        fun createFor(classLoader: ClassLoader): SdkActivityHandlerWrapper {
            val sdkSandboxActivityHandlerCompatClass = Class.forName(
                SdkSandboxActivityHandlerCompat::class.java.name,
                /* initialize = */ false,
                classLoader
            )
            val activityHolderClass = Class.forName(
                ActivityHolder::class.java.name,
                /* initialize = */ false,
                classLoader
            )
            val handlerOnActivityCreatedMethod =
                sdkSandboxActivityHandlerCompatClass.getMethod(
                    "onActivityCreated",
                    activityHolderClass
                )

            val activityHolderProxyFactory = ActivityHolderProxyFactory.createFor(classLoader)

            return SdkActivityHandlerWrapper(
                activityHolderProxyFactory = activityHolderProxyFactory,
                handlerOnActivityCreatedMethod = handlerOnActivityCreatedMethod
            )
        }
    }
}