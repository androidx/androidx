/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.room3.processor

import COMMON
import androidx.room3.Dao
import androidx.room3.Transaction
import androidx.room3.compiler.codegen.CodeLanguage
import androidx.room3.compiler.processing.XTypeElement
import androidx.room3.compiler.processing.util.Source
import androidx.room3.compiler.processing.util.XTestInvocation
import androidx.room3.compiler.processing.util.runKspTest
import androidx.room3.ext.GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE
import androidx.room3.ext.KotlinTypeNames.FLOW
import androidx.room3.ext.LifecyclesTypeNames.COMPUTABLE_LIVE_DATA
import androidx.room3.ext.LifecyclesTypeNames.LIVE_DATA
import androidx.room3.ext.ReactiveStreamsTypeNames.PUBLISHER
import androidx.room3.ext.RxJava3TypeNames
import androidx.room3.testing.context
import androidx.room3.vo.TransactionFunction
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class TransactionFunctionProcessorTest {

    companion object {
        const val DAO_PREFIX =
            """
                package foo.bar;
                import androidx.room3.*;
                import java.util.*;
                import androidx.lifecycle.*;
                import com.google.common.util.concurrent.*;
                import org.reactivestreams.*;
                @Dao
                abstract class MyClass {
                """
        const val DAO_SUFFIX = "}"
    }

    @Test
    fun simple() {
        singleTransactionMethod(
            """
                @Transaction
                public String doInTransaction(int param) { return null; }
                """
        ) { transaction, _ ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
        }
    }

    @Test
    fun modifier_private() {
        singleTransactionMethod(
            """
                @Transaction
                private String doInTransaction(int param) { return null; }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.TRANSACTION_FUNCTION_MODIFIERS)
            }
        }
    }

    @Test
    fun modifier_final() {
        singleTransactionMethod(
            """
                @Transaction
                public final String doInTransaction(int param) { return null; }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(ProcessorErrors.TRANSACTION_FUNCTION_MODIFIERS)
            }
        }
    }

    @Test
    fun deferredReturnType_flow() {
        singleTransactionMethod(
            """
                @Transaction
                public kotlinx.coroutines.flow.Flow<String> doInTransaction(int param) {
                    return null;
                }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.transactionFunctionAsync(
                        FLOW.rawTypeName.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    @Test
    fun deferredReturnType_liveData() {
        singleTransactionMethod(
            """
                @Transaction
                public LiveData<String> doInTransaction(int param) { return null; }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.transactionFunctionAsync(
                        LIVE_DATA.rawTypeName.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    @Test
    fun deferredReturnType_computableLiveData() {
        singleTransactionMethod(
            """
                @Transaction
                public ComputableLiveData<String> doInTransaction(int param) { return null; }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.transactionFunctionAsync(
                        COMPUTABLE_LIVE_DATA.rawTypeName.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    @Test
    fun deferredReturnType_rx3_flowable() {
        singleTransactionMethod(
            """
                @Transaction
                public io.reactivex.rxjava3.core.Flowable<String> doInTransaction(int param) { 
                    return null; 
                }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.transactionFunctionAsync(
                        RxJava3TypeNames.FLOWABLE.rawTypeName.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    @Test
    fun deferredReturnType_rx3_completable() {
        singleTransactionMethod(
            """
                @Transaction
                public io.reactivex.rxjava3.core.Completable doInTransaction(int param) { 
                    return null;
                }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.transactionFunctionAsync(
                        RxJava3TypeNames.COMPLETABLE.rawTypeName.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    @Test
    fun deferredReturnType_rx3_single() {
        singleTransactionMethod(
            """
                @Transaction
                public io.reactivex.rxjava3.core.Single<String> doInTransaction(int param) {
                    return null;
                }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.transactionFunctionAsync(
                        RxJava3TypeNames.SINGLE.rawTypeName.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    @Test
    fun deferredReturnType_listenableFuture() {
        singleTransactionMethod(
            """
                @Transaction
                public ListenableFuture<String> doInTransaction(int param) { return null; }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.transactionFunctionAsync(
                        LISTENABLE_FUTURE.rawTypeName.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    @Test
    fun deferredReturnType_publisher() {
        singleTransactionMethod(
            """
                @Transaction
                public Publisher<String> doInTransaction(int param) { return null; }
                """
        ) { transaction, invocation ->
            assertThat(transaction.jvmName, `is`("doInTransaction"))
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.transactionFunctionAsync(
                        PUBLISHER.rawTypeName.toString(CodeLanguage.JAVA)
                    )
                )
            }
        }
    }

    private val TransactionFunction.jvmName: String
        get() = element.jvmName

    private fun singleTransactionMethod(
        vararg input: String,
        handler: (TransactionFunction, XTestInvocation) -> Unit,
    ) {
        val inputSource =
            listOf(
                Source.java("foo.bar.MyClass", DAO_PREFIX + input.joinToString("\n") + DAO_SUFFIX)
            )
        val otherSources =
            listOf(
                COMMON.LIVE_DATA,
                COMMON.COMPUTABLE_LIVE_DATA,
                COMMON.PUBLISHER,
                COMMON.RX3_FLOWABLE,
                COMMON.RX3_COMPLETABLE,
                COMMON.RX3_SINGLE,
                COMMON.LISTENABLE_FUTURE,
                COMMON.FLOW,
            )
        runKspTest(sources = inputSource + otherSources) { invocation ->
            val (owner, methods) =
                invocation.roundEnv
                    .getElementsAnnotatedWith(Dao::class.qualifiedName!!)
                    .filterIsInstance<XTypeElement>()
                    .map {
                        Pair(
                            it,
                            it.getAllMethods()
                                .filter { it.hasAnnotation(Transaction::class) }
                                .toList(),
                        )
                    }
                    .first { it.second.isNotEmpty() }
            val processor =
                TransactionFunctionProcessor(
                    baseContext = invocation.context,
                    containingElement = owner,
                    containingType = owner.type,
                    executableElement = methods.first(),
                )
            val processed = processor.process()
            handler(processed, invocation)
        }
    }
}
