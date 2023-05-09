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

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.IBinder
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import java.lang.reflect.Proxy
import java.util.concurrent.CountDownLatch
import kotlin.reflect.cast

/**
 * Extract value of [Versions.API_VERSION] from loaded SDK.
 */
internal fun LocalSdkProvider.extractApiVersion(): Int =
    extractVersionValue("API_VERSION")

/**
 * Extract value of [Versions.CLIENT_VERSION] from loaded SDK.
 */
internal fun LocalSdkProvider.extractClientVersion(): Int =
    extractVersionValue("CLIENT_VERSION")

/**
 * Extract [SandboxedSdkProviderCompat.context] from loaded SDK.
 */
internal fun LocalSdkProvider.extractSdkContext(): Context {
    val getContextMethod = sdkProvider
        .javaClass
        .getMethod("getContext")

    val rawContext = getContextMethod.invoke(sdkProvider)

    return Context::class.cast(rawContext)
}

/**
 * Extract field value from [SandboxedSdkProviderCompat]
 */
internal inline fun <reified T> LocalSdkProvider.extractSdkProviderFieldValue(
    fieldName: String
): T {
    return sdkProvider
        .javaClass
        .getField(fieldName)
        .get(sdkProvider)!! as T
}

/**
 * Extract classloader that was used for loading of [SandboxedSdkProviderCompat].
 */
internal fun LocalSdkProvider.extractSdkProviderClassloader(): ClassLoader =
    sdkProvider.javaClass.classLoader!!

/**
 * Reflection wrapper for TestSDK object.
 * Underlying TestSDK should implement and delegate to
 * [androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat]:
 *  1) getSandboxedSdks() : List<SandboxedSdkCompat>
 *  2) registerSdkSandboxActivityHandler(SdkSandboxActivityHandlerCompat) : IBinder
 *  3) unregisterSdkSandboxActivityHandler(SdkSandboxActivityHandlerCompat)
 */
internal class TestSdkWrapper(
    private val sdk: Any
) {
    fun getSandboxedSdks(): List<SandboxedSdkWrapper> {
        val sdks = sdk.callMethod(
            methodName = "getSandboxedSdks"
        ) as List<*>
        return sdks.map { SandboxedSdkWrapper(it!!) }
    }

    fun registerSdkSandboxActivityHandler(handler: CatchingSdkActivityHandler): IBinder {
        val classLoader = sdk.javaClass.classLoader!!
        val activityHandlerClass = Class.forName(
            SdkSandboxActivityHandlerCompat::class.java.name,
            false,
            classLoader
        )

        val proxy = Proxy.newProxyInstance(
            classLoader,
            arrayOf(activityHandlerClass)
        ) { proxy, method, args ->
            when (method.name) {
                "hashCode" -> hashCode()
                "equals" -> proxy === args[0]
                "onActivityCreated" -> handler.setResult(args[0])
                else -> {
                    throw UnsupportedOperationException(
                        "Unexpected method call object:$proxy, method: $method, args: $args"
                    )
                }
            }
        }

        val registerMethod = sdk.javaClass
            .getMethod("registerSdkSandboxActivityHandler", activityHandlerClass)

        val token = registerMethod.invoke(sdk, proxy) as IBinder
        handler.proxy = proxy

        return token
    }

    fun unregisterSdkSandboxActivityHandler(handler: CatchingSdkActivityHandler) {
        val classLoader = sdk.javaClass.classLoader!!
        val activityHandlerClass = Class.forName(
            SdkSandboxActivityHandlerCompat::class.java.name,
            false,
            classLoader
        )

        val unregisterMethod = sdk.javaClass
            .getMethod("unregisterSdkSandboxActivityHandler", activityHandlerClass)

        unregisterMethod.invoke(sdk, handler.proxy)
        handler.proxy = null
    }
}

/**
 * Reflection wrapper for [SandboxedSdkCompat]
 */
internal class SandboxedSdkWrapper(
    private val sdk: Any
) {
    fun getInterface(): IBinder? {
        return sdk.callMethod(
            methodName = "getInterface"
        ) as IBinder?
    }

    fun getSdkName(): String? {
        val sdkInfo = getSdkInfo()
        if (sdkInfo != null) {
            return sdkInfo.callMethod(
                methodName = "getName"
            ) as String
        }
        return null
    }

    fun getSdkVersion(): Long? {
        val sdkInfo = getSdkInfo()
        if (sdkInfo != null) {
            return sdkInfo.callMethod(
                methodName = "getVersion"
            ) as Long
        }
        return null
    }

    private fun getSdkInfo(): Any? {
        return sdk.callMethod(
            methodName = "getSdkInfo"
        )
    }
}

/**
 * ActivityHandler to use with [TestSdkWrapper.registerSdkSandboxActivityHandler].
 * Store received ActivityHolder.
 */
internal class CatchingSdkActivityHandler {
    var proxy: Any? = null
    var result: ActivityHolderWrapper? = null
    val async = CountDownLatch(1)

    fun waitForActivity(): ActivityHolderWrapper {
        async.await()
        return result!!
    }
}

private fun CatchingSdkActivityHandler.setResult(activityHolder: Any) {
    result = ActivityHolderWrapper(activityHolder)
    async.countDown()
}

/**
 * Reflection wrapper for [androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder]
 */
internal class ActivityHolderWrapper(
    private val activityHolder: Any
) {
    fun getActivity(): Activity {
        return activityHolder.callMethod(
            methodName = "getActivity"
        ) as Activity
    }
}

/**
 * Load SDK and wrap it as TestSDK.
 * @see [TestSdkWrapper]
 */
internal fun LocalSdkProvider.loadTestSdk(): TestSdkWrapper {
    return onLoadSdk(Bundle()).asTestSdk()
}

/**
 * Wrap SandboxedSdkCompat as TestSDK.
 * @see [SandboxedSdkWrapper]
 */
internal fun SandboxedSdkCompat.asTestSdk(): TestSdkWrapper {
    return TestSdkWrapper(sdk = getInterface()!!)
}

private fun Any.callMethod(methodName: String): Any? {
    return javaClass
        .getMethod(methodName)
        .invoke(this)
}

private fun LocalSdkProvider.extractVersionValue(versionFieldName: String): Int {
    val versionsClass = Class.forName(
        Versions::class.java.name,
        false,
        extractSdkProviderClassloader()
    )
    return versionsClass.getDeclaredField(versionFieldName).get(null) as Int
}