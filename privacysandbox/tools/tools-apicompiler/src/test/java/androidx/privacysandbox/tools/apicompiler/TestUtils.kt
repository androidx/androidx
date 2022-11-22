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

package androidx.privacysandbox.tools.apicompiler

import androidx.privacysandbox.tools.testing.CompilationTestHelper
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationResult

/**
 * Compile the given sources using the PrivacySandboxKspCompiler.
 *
 * Default parameters will set required options like AIDL compiler path and use the latest
 * Android platform API stubs that support the Privacy Sandbox.
 */
fun compileWithPrivacySandboxKspCompiler(
    sources: List<Source>,
    platformStubs: PlatformStubs = PlatformStubs.SDK_RUNTIME_LIBRARY,
    extraProcessorOptions: Map<String, String> = mapOf(),
): TestCompilationResult {
    val provider = PrivacySandboxKspCompiler.Provider()

    val processorOptions = buildMap {
        val aidlPath = (System.getProperty("aidl_compiler_path")
            ?: throw IllegalArgumentException("aidl_compiler_path flag not set."))
        put("aidl_compiler_path", aidlPath)
        putAll(extraProcessorOptions)
    }

    return CompilationTestHelper.compileAll(
        sources + platformStubs.sources,
        symbolProcessorProviders = listOf(provider),
        processorOptions = processorOptions,
    )
}

enum class PlatformStubs(val sources: List<Source>) {
    API_33(syntheticApi33PrivacySandboxStubs),
    SDK_RUNTIME_LIBRARY(syntheticSdkRuntimeLibraryStubs),
}

// SDK Runtime library is not available in AndroidX prebuilts, so while that's the case we use fake
// stubs to run our compilation tests.
private val syntheticSdkRuntimeLibraryStubs = listOf(
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/core/SandboxedSdkCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.core
        |
        |import android.os.IBinder
        |
        |@Suppress("UNUSED_PARAMETER")
        |sealed class SandboxedSdkCompat {
        |    abstract fun getInterface(): IBinder?
        |
        |    companion object {
        |        fun create(binder: IBinder): SandboxedSdkCompat = throw RuntimeException("Stub!")
        |    }
        |}
        |""".trimMargin()
    ),
    Source.kotlin(
        "androidx/privacysandbox/sdkruntime/core/SandboxedSdkProviderCompat.kt", """
        |package androidx.privacysandbox.sdkruntime.core
        |
        |import android.content.Context
        |import android.os.Bundle
        |import android.view.View
        |
        |@Suppress("UNUSED_PARAMETER")
        |abstract class SandboxedSdkProviderCompat {
        |   var context: Context? = null
        |       private set
        |   fun attachContext(context: Context): Unit = throw RuntimeException("Stub!")
        |
        |   abstract fun onLoadSdk(params: Bundle): SandboxedSdkCompat
        |
        |   open fun beforeUnloadSdk() {}
        |
        |   abstract fun getView(
        |       windowContext: Context,
        |       params: Bundle,
        |       width: Int,
        |       height: Int
        |   ): View
        |}
        |""".trimMargin()
    )
)

// PrivacySandbox platform APIs are not available in AndroidX prebuilts nor are they stable, so
// while that's the case we use fake stubs to run our compilation tests.
val syntheticApi33PrivacySandboxStubs = listOf(
    Source.java(
        "android.app.sdksandbox.SandboxedSdk", """
        |package android.app.sdksandbox;
        |
        |import android.os.IBinder;
        |
        |public final class SandboxedSdk {
        |    public SandboxedSdk(IBinder sdkInterface) {}
        |    public IBinder getInterface() { throw new RuntimeException("Stub!"); }
        |}
        |""".trimMargin()
    ),
    Source.java(
        "android.app.sdksandbox.SandboxedSdkProvider", """
        |package android.app.sdksandbox;
        |
        |import android.content.Context;
        |import android.os.Bundle;
        |import android.view.View;
        |
        |public abstract class SandboxedSdkProvider {
        |    public final void attachContext(Context context) {
        |        throw new RuntimeException("Stub!");
        |    }
        |    public final Context getContext() {
        |        throw new RuntimeException("Stub!");
        |    }
        |    public abstract SandboxedSdk onLoadSdk(Bundle params)
        |        throws LoadSdkException;
        |
        |    public void beforeUnloadSdk() {}
        |
        |    public abstract View getView(
        |        Context windowContext, Bundle params, int width, int height);
        |}
        |""".trimMargin()
    ),
    Source.java(
        "android.app.sdksandbox.LoadSdkException", """
        |package android.app.sdksandbox;
        |
        |@SuppressWarnings("serial")
        |public final class LoadSdkException extends Exception {}
        |""".trimMargin()
    ),
    Source.java(
        "android.app.sdksandbox.SandboxedSdkContext", """
        |package android.app.sdksandbox;
        |
        |public final class SandboxedSdkContext {}
        |""".trimMargin()
    ),
)
