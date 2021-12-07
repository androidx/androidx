/*
 * Copyright (C) 2016 The Android Open Source Project
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

import androidx.room.Delete
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.ext.GuavaUtilConcurrentTypeNames
import androidx.room.ext.LifecyclesTypeNames
import androidx.room.ext.ReactiveStreamsTypeNames
import androidx.room.ext.RxJava2TypeNames
import androidx.room.ext.RxJava3TypeNames
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_DELETE_RESULT_ADAPTER
import androidx.room.processor.ProcessorErrors.DELETION_MISSING_PARAMS
import androidx.room.vo.DeletionMethod
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class DeletionMethodProcessorTest : ShortcutMethodProcessorTest<DeletionMethod>(Delete::class) {
    override fun invalidReturnTypeError(): String = CANNOT_FIND_DELETE_RESULT_ADAPTER

    override fun noParamsError(): String = DELETION_MISSING_PARAMS

    override fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement
    ): DeletionMethod {
        return DeletionMethodProcessor(baseContext, containing, executableElement).process()
    }

    @Test
    fun badUsingMultipleConcurrencyPatterns() {
        listOf(
            "${RxJava2TypeNames.FLOWABLE}<Int>",
            "${RxJava2TypeNames.OBSERVABLE}<Int>",
            "${RxJava2TypeNames.MAYBE}<Int>",
            "${RxJava2TypeNames.SINGLE}<Int>",
            "${RxJava2TypeNames.COMPLETABLE}",
            "${RxJava3TypeNames.FLOWABLE}<Int>",
            "${RxJava3TypeNames.OBSERVABLE}<Int>",
            "${RxJava3TypeNames.MAYBE}<Int>",
            "${RxJava3TypeNames.SINGLE}<Int>",
            "${RxJava3TypeNames.COMPLETABLE}",
            "${LifecyclesTypeNames.LIVE_DATA}<Int>",
            "${LifecyclesTypeNames.COMPUTABLE_LIVE_DATA}<Int>",
            "${GuavaUtilConcurrentTypeNames.LISTENABLE_FUTURE}<Int>",
            "${ReactiveStreamsTypeNames.PUBLISHER}<Int>"
        ).forEach { type ->
            singleShortcutMethodKotlin(
                """
                @Delete
                abstract suspend fun foo(user: User): $type
                """
            ) { _, invocation ->
                invocation.assertCompilationResult {
                    hasErrorContaining(ProcessorErrors.USING_MULTIPLE_CONCURRENCY_PATTERNS)
                }
            }
        }
    }
}
