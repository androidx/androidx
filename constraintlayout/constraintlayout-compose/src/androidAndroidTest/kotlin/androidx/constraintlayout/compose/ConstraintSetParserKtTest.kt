/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.constraintlayout.compose

import androidx.constraintlayout.core.state.ConstraintSetParser
import androidx.constraintlayout.core.state.Transition
import org.intellij.lang.annotations.Language
import org.junit.Test

internal class ConstraintSetParserKtTest {

    @Test
    fun parseCustomColor() {
        val coreTransition = Transition { dp -> dp }

        @Language("JSON5")
        val content = """
            {
              id1: {
                custom: {
                  color: '#ffaabbcc' // color that requires u_int or long parsing
                }
              }
            }
        """.trimIndent()
        ConstraintSetParser.parseJSON(content, coreTransition, 0) // Should finish successfully
    }
}