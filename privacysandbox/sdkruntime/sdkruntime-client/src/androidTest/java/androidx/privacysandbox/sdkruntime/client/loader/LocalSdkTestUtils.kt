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
import androidx.lifecycle.Lifecycle
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkCompat
import androidx.privacysandbox.sdkruntime.core.SandboxedSdkProviderCompat
import androidx.privacysandbox.sdkruntime.core.Versions
import androidx.privacysandbox.sdkruntime.core.activity.SdkSandboxActivityHandlerCompat
import androidx.privacysandbox.sdkruntime.core.controller.SdkSandboxControllerCompat
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.lang.reflect.UndeclaredThrowableException
import java.util.concurrent.CountDownLatch

/** Extract value of [Versions.API_VERSION] from loaded SDK. */
internal fun LocalSdkProvider.extractApiVersion(): Int = extractVersionValue("API_VERSION")

/** Extract value of [Versions.CLIENT_VERSION] from loaded SDK. */
internal fun LocalSdkProvider.extractClientVersion(): Int = extractVersionValue("CLIENT_VERSION")

/** Extract value of static [versionFieldName] from [Versions] class. */
private fun LocalSdkProvider.extractVersionValue(versionFieldName: String): Int =
    extractSdkProviderClassloader()
        .getClass(Versions::class.java.name)
        .getStaticField(versionFieldName) as Int

/** Extract [SandboxedSdkProviderCompat.context] from loaded SDK. */
internal fun LocalSdkProvider.extractSdkContext(): Context =
    sdkProvider.callMethod("getContext") as Context

/** Extract field value from [SandboxedSdkProviderCompat] */
internal inline fun <reified T> LocalSdkProvider.extractSdkProviderFieldValue(
    fieldName: String
): T = sdkProvider.getField(fieldName) as T

/** Extract classloader that was used for loading of [SandboxedSdkProviderCompat]. */
internal fun LocalSdkProvider.extractSdkProviderClassloader(): ClassLoader =
    sdkProvider.javaClass.classLoader!!

/** Reflection wrapper for [SdkSandboxControllerCompat] */
internal class SdkControllerWrapper(private val controller: Any) {
    fun loadSdk(sdkName: String, sdkParams: Bundle): SandboxedSdkWrapper {
        try {
            val rawSandboxedSdkCompat =
                controller.callSuspendMethod("loadSdk", sdkName, sdkParams) as Any
            return SandboxedSdkWrapper(rawSandboxedSdkCompat)
        } catch (ex: Exception) {
            throw tryRebuildCompatException(ex)
        }
    }

    private fun tryRebuildCompatException(rawException: Throwable): Throwable {
        if (rawException.javaClass.name != LoadSdkCompatException::class.java.name) {
            return rawException
        }
        val errorCode = rawException.callMethod("getLoadSdkErrorCode") as Int
        val params = rawException.callMethod("getExtraInformation") as Bundle
        return LoadSdkCompatException(errorCode, rawException.message, rawException.cause, params)
    }

    fun getSandboxedSdks(): List<SandboxedSdkWrapper> {
        val sdks = controller.callMethod(methodName = "getSandboxedSdks") as List<*>
        return sdks.map { SandboxedSdkWrapper(it!!) }
    }

    fun getAppOwnedSdkSandboxInterfaces(): List<AppOwnedSdkWrapper> {
        val sdks = controller.callMethod(methodName = "getAppOwnedSdkSandboxInterfaces") as List<*>
        return sdks.map { AppOwnedSdkWrapper(it!!) }
    }

    fun registerSdkSandboxActivityHandler(handler: CatchingSdkActivityHandler): IBinder {
        val classLoader = controller.javaClass.classLoader!!

        val proxy =
            classLoader.createProxyFor(
                SdkSandboxActivityHandlerCompat::class.java.name,
                "onActivityCreated"
            ) {
                handler.setResult(it!![0]!!)
            }

        val token = controller.callMethod("registerSdkSandboxActivityHandler", proxy) as IBinder
        handler.proxy = proxy

        return token
    }

    fun unregisterSdkSandboxActivityHandler(handler: CatchingSdkActivityHandler) {
        controller.callMethod("unregisterSdkSandboxActivityHandler", handler.proxy)
        handler.proxy = null
    }

    fun getClientPackageName(): String = controller.callMethod("getClientPackageName") as String
}

/** Reflection wrapper for [SandboxedSdkCompat] */
internal class SandboxedSdkWrapper(private val sdk: Any) {
    fun getInterface(): IBinder? {
        return sdk.callMethod(methodName = "getInterface") as IBinder?
    }

    fun getSdkName(): String? {
        val sdkInfo = getSdkInfo()
        if (sdkInfo != null) {
            return sdkInfo.callMethod(methodName = "getName") as String
        }
        return null
    }

    fun getSdkVersion(): Long? {
        val sdkInfo = getSdkInfo()
        if (sdkInfo != null) {
            return sdkInfo.callMethod(methodName = "getVersion") as Long
        }
        return null
    }

    private fun getSdkInfo(): Any? {
        return sdk.callMethod(methodName = "getSdkInfo")
    }
}

/** Reflection wrapper for [AppOwnedSdkSandboxInterfaceCompat] */
internal class AppOwnedSdkWrapper(private val sdk: Any) {
    fun getName(): String {
        return sdk.callMethod(methodName = "getName") as String
    }

    fun getVersion(): Long {
        return sdk.callMethod(methodName = "getVersion") as Long
    }

    fun getInterface(): IBinder {
        return sdk.callMethod(methodName = "getInterface") as IBinder
    }
}

/**
 * ActivityHandler to use with [SdkControllerWrapper.registerSdkSandboxActivityHandler]. Store
 * received ActivityHolder.
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

/** Reflection wrapper for [androidx.privacysandbox.sdkruntime.core.activity.ActivityHolder] */
internal class ActivityHolderWrapper(private val activityHolder: Any) {
    fun getActivity(): Activity {
        return activityHolder.callMethod(methodName = "getActivity") as Activity
    }

    fun getLifeCycleCurrentState(): Lifecycle.State {
        val lifecycle = activityHolder.callMethod(methodName = "getLifecycle")
        val currentState = lifecycle!!.callMethod(methodName = "getCurrentState")
        val currentStateString = currentState!!.callMethod(methodName = "name") as String
        return Lifecycle.State.valueOf(currentStateString)
    }
}

/** Create [SandboxedSdkWrapper] using SDK context from [SandboxedSdkProviderCompat.context]. */
internal fun LocalSdkProvider.loadTestSdk(): SdkControllerWrapper {
    return createSdkControllerWrapperFor(extractSdkProviderClassloader(), extractSdkContext())
}

/**
 * Create [SandboxedSdkWrapper] using SDK context from TestSDK.
 *
 * TestSDK must expose public "context" property.
 *
 * @see [SandboxedSdkWrapper]
 */
internal fun SandboxedSdkCompat.asTestSdk(): SdkControllerWrapper {
    val testSdk = getInterface()!!
    val context = testSdk.callMethod("getContext")!!
    return createSdkControllerWrapperFor(testSdk.javaClass.classLoader!!, context)
}

/**
 * Creates [SdkControllerWrapper] for instance of [SdkSandboxControllerCompat] class loaded by SDK
 * classloader.
 */
private fun createSdkControllerWrapperFor(
    classLoader: ClassLoader,
    sdkContext: Any
): SdkControllerWrapper {
    val sdkController =
        classLoader
            .getClass(SdkSandboxControllerCompat::class.java.name)
            .callStaticMethod("from", sdkContext)!!
    return SdkControllerWrapper(sdkController)
}

private fun ClassLoader.getClass(className: String): Class<*> =
    Class.forName(className, false, this)

private fun Class<*>.getStaticField(fieldName: String): Any? = getDeclaredField(fieldName).get(null)

private fun Any.getField(fieldName: String): Any? = javaClass.getField(fieldName).get(this)

/**
 * Call static method and return result. Unwraps [InvocationTargetException] to actual exception.
 */
private fun Class<*>.callStaticMethod(methodName: String, vararg args: Any?): Any? {
    try {
        return methods.single { it.name == methodName }.invoke(null, *args)
    } catch (ex: InvocationTargetException) {
        throw ex.targetException
    }
}

/** Call method and return result. Unwraps [InvocationTargetException] to actual exception. */
private fun Any.callMethod(methodName: String, vararg args: Any?): Any? {
    try {
        return javaClass.methods.single { it.name == methodName }.invoke(this, *args)
    } catch (ex: InvocationTargetException) {
        throw ex.targetException
    }
}

/**
 * Call suspend method and wait for result (via runBlocking).
 *
 * KCallable#callSuspend can't be used here as it will pass Continuation instance loaded by app
 * classloader while SDK will expect instance loaded by SDK classloader.
 *
 * Instead this method calls runBlocking() using classes loaded by SDK classloader.
 */
private fun Any.callSuspendMethod(methodName: String, vararg args: Any?): Any? {
    val classLoader = javaClass.classLoader!!

    val method = javaClass.methods.single { it.name == methodName }

    val coroutineContextClass = classLoader.getClass("kotlin.coroutines.CoroutineContext")
    val functionClass = classLoader.getClass("kotlin.jvm.functions.Function2")
    val runBlockingMethod =
        classLoader
            .getClass("kotlinx.coroutines.BuildersKt")
            .getMethod("runBlocking", coroutineContextClass, functionClass)

    val coroutineContextInstance =
        classLoader.getClass("kotlin.coroutines.EmptyCoroutineContext").getStaticField("INSTANCE")

    val functionProxy =
        classLoader.createProxyFor("kotlin.jvm.functions.Function2", "invoke") {
            try {
                // Receive Continuation as second function argument and pass it as last method
                // argument
                method.invoke(this, *args, it!![1])
            } catch (ex: InvocationTargetException) {
                // Rethrow original exception to correctly handle it later.
                throw ex.targetException
            }
        }

    try {
        return runBlockingMethod.invoke(null, coroutineContextInstance, functionProxy)
    } catch (ex: InvocationTargetException) {
        // First unwrap InvocationTargetException to get exception while calling runBlocking
        val runBlockingException = ex.targetException

        // runBlocking doesn't declare exceptions, need to unwrap actual method exception
        if (runBlockingException is UndeclaredThrowableException) {
            throw runBlockingException.undeclaredThrowable
        } else {
            throw ex
        }
    }
}

/** Create Dynamic Proxy that handles single [methodName]. */
private fun ClassLoader.createProxyFor(
    className: String,
    methodName: String,
    handler: (args: Array<Any?>?) -> Any?
): Any {
    return createProxyFor(className) { calledMethodName, args ->
        if (calledMethodName == methodName) {
            handler.invoke(args)
        } else {
            throw UnsupportedOperationException(
                "Unexpected method call: $calledMethodName, args: $args"
            )
        }
    }
}

/** Create Dynamic Proxy that handles all methods of [className]. */
private fun ClassLoader.createProxyFor(
    className: String,
    handler: (methodName: String, args: Array<Any?>?) -> Any?
): Any {
    return Proxy.newProxyInstance(this, arrayOf(getClass(className))) { proxy, method, args ->
        when (method.name) {
            "equals" -> proxy === args?.get(0)
            "hashCode" -> hashCode()
            "toString" -> toString()
            else -> {
                handler.invoke(method.name, args)
            }
        }
    }
}
