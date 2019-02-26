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

package androidx.room.processor

import androidx.room.Dao
import androidx.room.Transaction
import androidx.room.testing.TestInvocation
import androidx.room.testing.TestProcessor
import androidx.room.vo.TransactionMethod
import com.google.auto.common.MoreElements
import com.google.auto.common.MoreTypes
import com.google.common.truth.Truth
import com.google.testing.compile.CompileTester
import com.google.testing.compile.JavaFileObjects
import com.google.testing.compile.JavaSourcesSubjectFactory
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class TransactionMethodProcessorTest {

    companion object {
        const val DAO_PREFIX = """
                package foo.bar;
                import androidx.room.*;
                import java.util.*;
                import androidx.lifecycle.*;
                import io.reactivex.*;
                import com.google.common.util.concurrent.*;
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
                """) { transaction, _ ->
            assertThat(transaction.name, `is`("doInTransaction"))
        }.compilesWithoutError()
    }

    @Test
    fun modifier_private() {
        singleTransactionMethod(
                """
                @Transaction
                private String doInTransaction(int param) { return null; }
                """) { transaction, _ ->
            assertThat(transaction.name, `is`("doInTransaction"))
        }.failsToCompile().withErrorContaining(ProcessorErrors.TRANSACTION_METHOD_MODIFIERS)
    }

    @Test
    fun modifier_final() {
        singleTransactionMethod(
                """
                @Transaction
                public final String doInTransaction(int param) { return null; }
                """) { transaction, _ ->
            assertThat(transaction.name, `is`("doInTransaction"))
        }.failsToCompile().withErrorContaining(ProcessorErrors.TRANSACTION_METHOD_MODIFIERS)
    }

    @Test
    fun deferredReturnType_liveData() {
        singleTransactionMethod(
            """
                @Transaction
                public LiveData<String> doInTransaction(int param) { return null; }
                """) { transaction, _ ->
            assertThat(transaction.name, `is`("doInTransaction"))
        }.failsToCompile()
            .withErrorContaining(
                ProcessorErrors.transactionMethodAsync(
                    "androidx.lifecycle.LiveData"
                )
            )
    }

    @Test
    fun deferredReturnType_flowable() {
        singleTransactionMethod(
            """
                @Transaction
                public Flowable<String> doInTransaction(int param) { return null; }
                """) { transaction, _ ->
            assertThat(transaction.name, `is`("doInTransaction"))
        }.failsToCompile()
            .withErrorContaining(
                ProcessorErrors.transactionMethodAsync(
                    "io.reactivex.Flowable"
                )
            )
    }

    @Test
    fun deferredReturnType_completable() {
        singleTransactionMethod(
            """
                @Transaction
                public Completable doInTransaction(int param) { return null; }
                """) { transaction, _ ->
            assertThat(transaction.name, `is`("doInTransaction"))
        }.failsToCompile()
            .withErrorContaining(
                ProcessorErrors.transactionMethodAsync(
                    "io.reactivex.Completable"
                )
            )
    }

    @Test
    fun deferredReturnType_single() {
        singleTransactionMethod(
            """
                @Transaction
                public Single<String> doInTransaction(int param) { return null; }
                """) { transaction, _ ->
            assertThat(transaction.name, `is`("doInTransaction"))
        }.failsToCompile()
            .withErrorContaining(
                ProcessorErrors.transactionMethodAsync(
                    "io.reactivex.Single"
                )
            )
    }

    @Test
    fun deferredReturnType_listenableFuture() {
        singleTransactionMethod(
            """
                @Transaction
                public ListenableFuture<String> doInTransaction(int param) { return null; }
                """) { transaction, _ ->
            assertThat(transaction.name, `is`("doInTransaction"))
        }.failsToCompile()
            .withErrorContaining(
                ProcessorErrors.transactionMethodAsync(
                    "com.google.common.util.concurrent.ListenableFuture"
                )
            )
    }

    private val TransactionMethod.name: String
        get() = element.simpleName.toString()

    private fun singleTransactionMethod(
        vararg input: String,
        handler: (TransactionMethod, TestInvocation) -> Unit
    ): CompileTester {
        return Truth.assertAbout(JavaSourcesSubjectFactory.javaSources())
                .that(listOf(JavaFileObjects.forSourceString("foo.bar.MyClass",
                        TransactionMethodProcessorTest.DAO_PREFIX + input.joinToString("\n") +
                                TransactionMethodProcessorTest.DAO_SUFFIX
                ), COMMON.LIVE_DATA, COMMON.FLOWABLE, COMMON.PUBLISHER, COMMON.COMPLETABLE,
                    COMMON.SINGLE, COMMON.LISTENABLE_FUTURE))
                .processedWith(TestProcessor.builder()
                        .forAnnotations(Transaction::class, Dao::class)
                        .nextRunHandler { invocation ->
                            val (owner, methods) = invocation.roundEnv
                                    .getElementsAnnotatedWith(Dao::class.java)
                                    .map {
                                        Pair(it,
                                                invocation.processingEnv.elementUtils
                                                        .getAllMembers(MoreElements.asType(it))
                                                        .filter {
                                                            MoreElements.isAnnotationPresent(it,
                                                                    Transaction::class.java)
                                                        }
                                        )
                                    }.first { it.second.isNotEmpty() }
                            val processor = TransactionMethodProcessor(
                                    baseContext = invocation.context,
                                    containing = MoreTypes.asDeclared(owner.asType()),
                                    executableElement = MoreElements.asExecutable(methods.first()))
                            val processed = processor.process()
                            handler(processed, invocation)
                            true
                        }
                        .build())
    }
}
