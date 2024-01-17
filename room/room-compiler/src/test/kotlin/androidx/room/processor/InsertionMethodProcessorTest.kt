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

import androidx.kruth.assertThat
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_INSERT_RESULT_ADAPTER
import androidx.room.processor.ProcessorErrors.INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT
import androidx.room.processor.ProcessorErrors.INSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH
import androidx.room.processor.ProcessorErrors.INSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH
import androidx.room.vo.InsertionMethod
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class InsertionMethodProcessorTest :
    InsertOrUpsertShortcutMethodProcessorTest<InsertionMethod>(Insert::class) {
    override fun noParamsError(): String = INSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_INSERT

    override fun missingPrimaryKey(partialEntityName: String, primaryKeyName: List<String>):
    String {
        return ProcessorErrors.missingPrimaryKeysInPartialEntityForInsert(
            partialEntityName,
            primaryKeyName
        )
    }

    override fun noAdapter(): String = CANNOT_FIND_INSERT_RESULT_ADAPTER

    override fun multiParamAndSingleReturnMismatchError():
        String = INSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH

    override fun singleParamAndMultiReturnMismatchError():
        String = INSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH

    @Test
    fun onConflict_Default() {
        singleInsertUpsertShortcutMethod(
            """
                @Insert
                abstract public void foo(User user);
                """
        ) { insertion, _ ->
            assertThat(insertion.onConflict).isEqualTo(OnConflictStrategy.ABORT)
        }
    }

    @Test
    fun onConflict_Invalid() {
        singleInsertUpsertShortcutMethod(
            """
                @Insert(onConflict = -1)
                abstract public void foo(User user);
                """
        ) { _, invocation ->
            invocation.assertCompilationResult {
                hasErrorContaining(
                    ProcessorErrors.INVALID_ON_CONFLICT_VALUE
                )
            }
        }
    }

    @Test
    fun onConflict_EachValue() {
        listOf(
            Pair("NONE", 0),
            Pair("REPLACE", 1),
            Pair("ROLLBACK", 2),
            Pair("ABORT", 3),
            Pair("FAIL", 4),
            Pair("IGNORE", 5)
        ).forEach { pair ->
            singleInsertUpsertShortcutMethod(
                """
                @Insert(onConflict=OnConflictStrategy.${pair.first})
                abstract public void foo(User user);
                """
            ) { insertion, _ ->
                assertThat(insertion.onConflict).isEqualTo(pair.second)
            }
        }
    }

    override fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement
    ): InsertionMethod {
        return InsertionMethodProcessor(baseContext, containing, executableElement).process()
    }
}
