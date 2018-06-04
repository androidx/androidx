/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.safe.args.generator

import androidx.navigation.safe.args.generator.NavParserErrors.UNNAMED_DESTINATION
import androidx.navigation.safe.args.generator.NavParserErrors.invalidDefaultValue
import androidx.navigation.safe.args.generator.NavParserErrors.invalidDefaultValueReference
import androidx.navigation.safe.args.generator.NavParserErrors.invalidId
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class InvalidXmlTest(private val testCase: ErrorMessage) {
    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "({0})")
        fun data() = listOf(
            ErrorMessage("unnamed_destination_with_action.xml", 25, 5, UNNAMED_DESTINATION),
            ErrorMessage("invalid_default_value_reference.xml", 23, 9,
                invalidDefaultValueReference("foo/")),
            ErrorMessage("invalid_default_value_int.xml", 24, 9,
                invalidDefaultValue("101034f", NavType.INT)),
            ErrorMessage("invalid_id_action.xml", 22, 44, invalidId("@+fppid/finish")),
            ErrorMessage("invalid_id_destination.xml", 17, 1, invalidId("@1234234+id/foo")),
            ErrorMessage("action_no_id.xml", 22, 5, mandatoryAttrMissingError("action", "id"))
        )
    }

    @Test
    fun invalidXml() {
        val context = Context()
        val navigationXml = testData("invalid_xmls/${testCase.path}")
        val expectedError = testCase.copy(path = navigationXml.path)
        NavParser.parseNavigationFile(navigationXml, "a.b", "foo.app", context)
        val messages = context.logger.allMessages()
        assertThat(messages.size, `is`(1))
        assertThat(messages.first(), `is`(expectedError))
    }
}
