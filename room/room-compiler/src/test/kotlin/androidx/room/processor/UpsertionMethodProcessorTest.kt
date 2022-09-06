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

import androidx.room.Upsert
import androidx.room.compiler.processing.XMethodElement
import androidx.room.compiler.processing.XType
import androidx.room.processor.ProcessorErrors.UPSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_UPSERT
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_UPSERT_RESULT_ADAPTER
import androidx.room.processor.ProcessorErrors.UPSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH
import androidx.room.processor.ProcessorErrors.UPSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH
import androidx.room.vo.UpsertionMethod
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class UpsertionMethodProcessorTest :
    InsertOrUpsertShortcutMethodProcessorTest<UpsertionMethod>(Upsert::class) {
    override fun noParamsError(): String = UPSERTION_DOES_NOT_HAVE_ANY_PARAMETERS_TO_UPSERT

    override fun missingPrimaryKey(partialEntityName: String, primaryKeyName: List<String>):
        String {
        return ProcessorErrors.missingPrimaryKeysInPartialEntityForUpsert(
            partialEntityName,
            primaryKeyName
        )
    }

    override fun noAdapter(): String = CANNOT_FIND_UPSERT_RESULT_ADAPTER

    override fun multiParamAndSingleReturnMismatchError():
        String = UPSERT_MULTI_PARAM_SINGLE_RETURN_MISMATCH

    override fun singleParamAndMultiReturnMismatchError():
        String = UPSERT_SINGLE_PARAM_MULTI_RETURN_MISMATCH

    override fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement
    ): UpsertionMethod {
        return UpsertionMethodProcessor(baseContext, containing, executableElement).process()
    }
}
