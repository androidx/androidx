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

import androidx.navigation.safe.args.generator.NavType.BOOLEAN
import androidx.navigation.safe.args.generator.NavType.FLOAT
import androidx.navigation.safe.args.generator.NavType.INT
import androidx.navigation.safe.args.generator.NavType.REFERENCE
import androidx.navigation.safe.args.generator.NavType.STRING
import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.ResReference
import com.squareup.javapoet.ClassName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavParserTest {

    @Test
    fun test() {
        val id: (String) -> ResReference = { id -> ResReference("a.b", "id", id) }
        val navGraph = NavParser.parseNavigationFile(testData("naive_test.xml"),
            "a.b", "foo.app", Context())

        val nameFirst = ClassName.get("androidx.navigation.testapp", "MainFragment")
        val nameNext = ClassName.get("foo.app", "NextFragment")
        val expectedFirst = Destination(id("first_screen"), nameFirst, "fragment",
                listOf(Argument("myarg1", STRING, StringValue("one"))),
                listOf(Action(id("next"), id("next_fragment"), listOf(
                        Argument("myarg2", STRING),
                        Argument("randomArgument", STRING),
                        Argument("intArgument", INT, IntValue("261"))
                ))))

        val expectedNext = Destination(id("next_fragment"), nameNext, "fragment",
                listOf(Argument("myarg2", STRING)),
                listOf(Action(id("next"), id("first_screen")),
                        Action(id("finish"), null)))

        val expectedGraph = Destination(null, null, "navigation", emptyList(), emptyList(),
                listOf(expectedFirst, expectedNext))
        assertThat(navGraph, `is`(expectedGraph))
    }

    @Test
    fun testReferenceParsing() {
        assertThat(parseReference("@+id/next", "a.b"), `is`(ResReference("a.b", "id", "next")))
        assertThat(parseReference("@id/next", "a.b"), `is`(ResReference("a.b", "id", "next")))
        assertThat(parseReference("@android:string/text", "a.b"),
                `is`(ResReference("android", "string", "text")))
        assertThat(parseReference("@android:id/text", "a.b"),
                `is`(ResReference("android", "id", "text")))
        assertThat(parseReference("@not.android:string/text", "a.b"),
                `is`(ResReference("not.android", "string", "text")))
    }

    @Test
    fun testIntValueParsing() {
        assertThat(parseIntValue("foo"), nullValue())
        assertThat(parseIntValue("10"), `is`(IntValue("10")))
        assertThat(parseIntValue("-10"), `is`(IntValue("-10")))
        assertThat(parseIntValue("0xA"), `is`(IntValue("0xA")))
        assertThat(parseIntValue("0xFFFFFFFF"), `is`(IntValue("0xFFFFFFFF")))
        assertThat(parseIntValue("0x1FFFFFFFF"), nullValue())
    }

    @Test
    fun testArgInference() {
        val infer = { value: String -> inferArgument("foo", value, "a.b") }
        val intArg = { value: String -> Argument("foo", INT, IntValue(value)) }
        val floatArg = { value: String -> Argument("foo", FLOAT, FloatValue(value)) }
        val stringArg = { value: String -> Argument("foo", STRING, StringValue(value)) }
        val boolArg = { value: String -> Argument("foo", BOOLEAN, BooleanValue(value)) }
        val referenceArg = { pName: String, type: String, value: String ->
            Argument("foo", REFERENCE, ReferenceValue(ResReference(pName, type, value)))
        }

        assertThat(infer("spb"), `is`(stringArg("spb")))
        assertThat(infer("10"), `is`(intArg("10")))
        assertThat(infer("0x10"), `is`(intArg("0x10")))
        assertThat(infer("@android:id/some_la"), `is`(referenceArg("android", "id", "some_la")))
        assertThat(infer("@foo"), `is`(stringArg("@foo")))
        assertThat(infer("@+id/foo"), `is`(referenceArg("a.b", "id", "foo")))
        assertThat(infer("@foo:stuff"), `is`(stringArg("@foo:stuff")))
        assertThat(infer("@/stuff"), `is`(stringArg("@/stuff")))
        assertThat(infer("10101010100100"), `is`(floatArg("10101010100100")))
        assertThat(infer("1."), `is`(floatArg("1.")))
        assertThat(infer("1.2e-4"), `is`(floatArg("1.2e-4")))
        assertThat(infer(".4"), `is`(floatArg(".4")))
        assertThat(infer("true"), `is`(boolArg("true")))
        assertThat(infer("false"), `is`(boolArg("false")))
    }
}