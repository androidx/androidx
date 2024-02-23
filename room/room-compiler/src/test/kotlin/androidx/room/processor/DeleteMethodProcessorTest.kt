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
import androidx.room.processor.ProcessorErrors.CANNOT_FIND_DELETE_RESULT_ADAPTER
import androidx.room.processor.ProcessorErrors.DELETE_MISSING_PARAMS
import androidx.room.vo.DeleteMethod
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
@RunWith(JUnit4::class)
class DeleteMethodProcessorTest :
    DeleteOrUpdateShortcutMethodProcessorTest<DeleteMethod>(Delete::class) {
    override fun invalidReturnTypeError(): String = CANNOT_FIND_DELETE_RESULT_ADAPTER

    override fun noParamsError(): String = DELETE_MISSING_PARAMS

    override fun process(
        baseContext: Context,
        containing: XType,
        executableElement: XMethodElement
    ): DeleteMethod {
        return DeleteMethodProcessor(baseContext, containing, executableElement).process()
    }
}
