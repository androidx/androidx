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

import androidx.privacysandbox.tools.apicompiler.util.CompilationResultSubject.Companion.assertThat
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import androidx.room.compiler.processing.util.Source
import java.nio.file.Files
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PrivacySandboxKspCompilerTest {
    @Test
    fun compileServiceInterface_ok() {
        val source =
            Source.kotlin(
                "com/mysdk/MySdk.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk {
                        suspend fun doStuff(x: Int, y: Int): String
                        fun doMoreStuff()
                    }
                """
            )
        val provider = PrivacySandboxKspCompiler.Provider()
        // Check that compilation is successful
        assertThat(
            compile(
                Files.createTempDirectory("test").toFile(),
                TestCompilationArguments(
                    sources = listOf(source) + getSyntheticAndroidClasses(),
                    symbolProcessorProviders = listOf(provider),
                    processorOptions = getProcessorOptions(),
                )
            )
        ).also {
            it.generatesExactlySources(
                "com/mysdk/IMySdk.java",
                "com/mysdk/ICancellationSignal.java",
                "com/mysdk/IStringTransactionCallback.java",
                "com/mysdk/AbstractSandboxedSdkProvider.kt",
                "com/mysdk/MySdkStubDelegate.kt",
                "com/mysdk/TransportCancellationCallback.kt",
            )
        }.also {
            it.generatesSourcesWithContents(
                "com/mysdk/AbstractSandboxedSdkProvider.kt" to """
                    |package com.mysdk
                    |
                    |import android.app.sdksandbox.SandboxedSdk
                    |import android.app.sdksandbox.SandboxedSdkProvider
                    |import android.content.Context
                    |import android.os.Bundle
                    |import android.view.View
                    |import kotlin.Int
                    |import kotlin.Unit
                    |
                    |public abstract class AbstractSandboxedSdkProvider : SandboxedSdkProvider() {
                    |  public override fun onLoadSdk(params: Bundle): SandboxedSdk {
                    |    val sdk = createMySdk(context!!)
                    |    return SandboxedSdk(MySdkStubDelegate(sdk))
                    |  }
                    |
                    |  public override fun getView(
                    |    windowContext: Context,
                    |    params: Bundle,
                    |    width: Int,
                    |    height: Int,
                    |  ): View {
                    |    TODO("Implement")
                    |  }
                    |
                    |  public override fun onDataReceived(`data`: Bundle,
                    |      callback: SandboxedSdkProvider.DataReceivedCallback): Unit {
                    |  }
                    |
                    |  protected abstract fun createMySdk(context: Context): MySdk
                    |}
                    |
                """.trimMargin(),
                "com/mysdk/MySdkStubDelegate.kt" to """
                    |package com.mysdk
                    |
                    |import kotlin.Int
                    |import kotlin.Unit
                    |import kotlinx.coroutines.CoroutineScope
                    |import kotlinx.coroutines.Dispatchers
                    |import kotlinx.coroutines.GlobalScope
                    |import kotlinx.coroutines.launch
                    |
                    |public class MySdkStubDelegate internal constructor(
                    |  private val `delegate`: MySdk,
                    |) : IMySdk.Stub() {
                    |  public override fun doStuff(
                    |    x: Int,
                    |    y: Int,
                    |    transactionCallback: IStringTransactionCallback,
                    |  ): Unit {
                    |    val job = GlobalScope.launch(Dispatchers.Main) {
                    |      try {
                    |        val result = delegate.doStuff(x, y)
                    |        transactionCallback.onSuccess(result)
                    |      } catch (t: Throwable) {
                    |        transactionCallback.onFailure(404, t.message)
                    |      }
                    |    }
                    |    val cancellationSignal = TransportCancellationCallback(){ job.cancel() }
                    |    transactionCallback.onCancellable(cancellationSignal)
                    |  }
                    |
                    |  public override fun doMoreStuff(): Unit = delegate.doMoreStuff()
                    |}
                    |
                """.trimMargin(),
                "com/mysdk/TransportCancellationCallback.kt" to """
                    |package com.mysdk
                    |
                    |import java.util.concurrent.atomic.AtomicBoolean
                    |import kotlin.Unit
                    |
                    |internal class TransportCancellationCallback internal constructor(
                    |  private val onCancel: () -> Unit,
                    |) : ICancellationSignal.Stub() {
                    |  private val hasCancelled: AtomicBoolean = AtomicBoolean(false)
                    |
                    |  public override fun cancel(): Unit {
                    |    if (hasCancelled.compareAndSet(false, true)) {
                    |      onCancel()
                    |    }
                    |  }
                    |}
                    |
                """.trimMargin(),
            )
        }
    }

    @Test
    fun compileInvalidServiceInterface_fails() {
        val source =
            Source.kotlin(
                "com/mysdk/MySdk.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    abstract class MySdk {
                        fun doStuff(x: Int, y: Int): String
                        fun doMoreStuff()
                    }
                """
            )
        val provider = PrivacySandboxKspCompiler.Provider()
        // Check that compilation fails
        assertThat(
            compile(
                Files.createTempDirectory("test").toFile(),
                TestCompilationArguments(
                    sources = listOf(source) + getSyntheticAndroidClasses(),
                    symbolProcessorProviders = listOf(provider),
                    processorOptions = getProcessorOptions(),
                )
            )
        ).fails()
    }

    private fun getProcessorOptions() =
        mapOf(
            "aidl_compiler_path" to (System.getProperty("aidl_compiler_path")
                ?: throw IllegalArgumentException("aidl_compiler_path flag not set."))
        )

    private fun getSyntheticAndroidClasses() =
        listOf(
            Source.java(
                "android.app.sdksandbox.SandboxedSdk",
                """
                    package android.app.sdksandbox;
                    import android.os.IBinder;
                    public class SandboxedSdk {
                        public SandboxedSdk(IBinder sdkInterface) {}
                    }
                """.trimIndent()
            ),
            Source.java(
                "android.app.sdksandbox.SandboxedSdkProvider",
                """
                    package android.app.sdksandbox;
                    import android.content.Context;
                    import android.os.Bundle;
                    import android.view.View;
                    public abstract class SandboxedSdkProvider {
                        public abstract SandboxedSdk onLoadSdk(Bundle params) throws LoadSdkException;
                        public abstract View getView(
                                Context windowContext, Bundle params, int width, int height);
                        public final Context getContext() {
                            return null;
                        }
                        public abstract void onDataReceived(
                                Bundle data, DataReceivedCallback callback);
                        public interface DataReceivedCallback {
                            void onDataReceivedSuccess(Bundle params);
                            void onDataReceivedError(String errorMessage);
                        }
                    }
                """.trimIndent()
            ),
            Source.java(
                "android.app.sdksandbox.LoadSdkException",
                """
                    package android.app.sdksandbox;
                    import android.os.Parcel;
                    import android.os.Parcelable;
                    public final class LoadSdkException extends Exception implements Parcelable {
                        @Override
                        public int describeContents() {
                            return 0;
                        }
                        @Override
                        public void writeToParcel(Parcel destination, int flags) {
                        }
                    }
                """.trimIndent()
            ),
            Source.java(
                "android.app.sdksandbox.SandboxedSdkContext",
                """
                    package android.app.sdksandbox;
                    public final class SandboxedSdkContext {}
                """.trimIndent()
            ),
        )
}