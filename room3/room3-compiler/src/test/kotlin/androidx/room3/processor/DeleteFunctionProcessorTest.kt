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
package androidx.room3.processor

import androidx.room3.Delete
import androidx.room3.compiler.processing.XMethodElement
import androidx.room3.compiler.processing.XType
import androidx.room3.processor.ProcessorErrors.CANNOT_FIND_DELETE_RESULT_ADAPTER
import androidx.room3.processor.ProcessorErrors.DELETE_MISSING_PARAMS
import androidx.room3.vo.DeleteFunction
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class DeleteFunctionProcessorTest :
    DeleteOrUpdateShortcutFunctionProcessorTest<DeleteFunction>(Delete::class) {
    override fun invalidReturnTypeError(): String = CANNOT_FIND_DELETE_RESULT_ADAPTER

    override fun noParamsError(): String = DELETE_MISSING_PARAMS

    override fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement,
    ): DeleteFunction {
        return DeleteFunctionProcessor(baseContext, containing, executableElement).process()
    }
}
