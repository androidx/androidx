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

package androidx.lifecycle

import androidx.lifecycle.utils.processClass
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InvalidCasesTest(val name: String, val errorMsg: String) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "failingCase({0})")
        fun data(): Collection<Array<Any>> = listOf(
                arrayOf<Any>("foo.InvalidFirstArg1", ErrorMessages.INVALID_FIRST_ARGUMENT),
                arrayOf<Any>("foo.InvalidFirstArg2", ErrorMessages.INVALID_FIRST_ARGUMENT),
                arrayOf<Any>("foo.InvalidSecondArg", ErrorMessages.INVALID_SECOND_ARGUMENT),
                arrayOf<Any>("foo.TooManyArgs1", ErrorMessages.TOO_MANY_ARGS),
                arrayOf<Any>("foo.TooManyArgs2", ErrorMessages.TOO_MANY_ARGS_NOT_ON_ANY),
                arrayOf<Any>("foo.InvalidMethodModifier",
                        ErrorMessages.INVALID_METHOD_MODIFIER),
                arrayOf<Any>("foo.InvalidClassModifier", ErrorMessages.INVALID_CLASS_MODIFIER),
                arrayOf<Any>("foo.InvalidInheritance1",
                        ErrorMessages.INVALID_STATE_OVERRIDE_METHOD),
                arrayOf<Any>("foo.InvalidInheritance2",
                        ErrorMessages.INVALID_STATE_OVERRIDE_METHOD)
        )
    }

    @Test
    fun shouldFailWithError() {
        processClass(name).failsToCompile().withErrorContaining(errorMsg)
    }
}
