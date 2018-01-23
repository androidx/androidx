
/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package android.support.tools.jetifier.core.transform

import android.support.tools.jetifier.core.rules.JavaType
import android.support.tools.jetifier.core.rules.RewriteRule
import com.google.common.truth.Truth
import org.junit.Test

class RewriteRuleTest {

    @Test fun noRegEx_shouldRewrite() {
        RuleTester
            .testThatRule("A/B", "A/C")
            .rewritesType("A/B")
            .into("A/C")
    }

    @Test fun noRegEx_underscore_shouldRewrite() {
        RuleTester
            .testThatRule("A/B_B", "A/C")
            .rewritesType("A/B_B")
            .into("A/C")
    }

    @Test fun groupRegEx_shouldRewrite() {
        RuleTester
            .testThatRule("A/B/(.*)", "A/{0}")
            .rewritesType("A/B/C/D")
            .into("A/C/D")
    }

    @Test fun groupRegEx__innerClass_shouldRewrite() {
        RuleTester
            .testThatRule("A/B/(.*)", "A/{0}")
            .rewritesType("A/B/C\$D")
            .into("A/C\$D")
    }

    @Test fun fieldRule_innerClass_groupRegEx_shouldRewrite() {
        RuleTester
            .testThatRule("A/B$(.*)", "A/C\${0}")
            .rewritesType("A/B\$D")
            .into("A/C\$D")
    }

    @Test fun typeRewrite_ignore() {
        RuleTester
            .testThatRule("A/B", "ignore")
            .rewritesType("A/B")
            .isIgnored()
    }

    @Test fun typeRewrite_ignoreInPreprocessor() {
        RuleTester
            .testThatRule("A/B", "ignoreInPreprocessorOnly")
            .rewritesType("A/B")
            .isIgnored()
    }

    object RuleTester {

        fun testThatRule(from: String, to: String) = RuleTesterStep1(from, to)

        class RuleTesterStep1(val from: String, val to: String) {

            fun rewritesType(inputType: String)
                    = RuleTesterFinalTypeStep(from, to, inputType)
        }

        class RuleTesterFinalTypeStep(val fromType: String,
                                      val toType: String,
                                      val inputType: String) {

            fun into(expectedResult: String) {
                val fieldRule = RewriteRule(fromType, toType)
                val result = fieldRule.apply(JavaType(inputType))

                Truth.assertThat(result).isNotNull()
                Truth.assertThat(result.result!!.fullName).isEqualTo(expectedResult)
            }

            fun isIgnored() {
                val fieldRule = RewriteRule(fromType, toType)
                val result = fieldRule.apply(JavaType(inputType))

                Truth.assertThat(result).isNotNull()
                Truth.assertThat(result.isIgnored).isTrue()
            }
        }

    }
}

