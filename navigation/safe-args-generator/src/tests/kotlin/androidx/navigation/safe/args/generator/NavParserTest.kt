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

import androidx.navigation.safe.args.generator.models.Action
import androidx.navigation.safe.args.generator.models.Argument
import androidx.navigation.safe.args.generator.models.Destination
import androidx.navigation.safe.args.generator.models.IncludedDestination
import androidx.navigation.safe.args.generator.models.ResReference
import com.squareup.javapoet.ClassName
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavParserTest {

    @Test
    fun testNaiveGraph() {
        val id: (String) -> ResReference = { id -> ResReference("a.b", "id", id) }
        val navGraph = NavParser.parseNavigationFile(testData("naive_test.xml"),
            "a.b", "foo.app", Context())

        val nameFirst = ClassName.get("androidx.navigation.testapp", "MainFragment")
        val nameNext = ClassName.get("foo.app", "NextFragment")
        val expectedFirst = Destination(id("first_screen"), nameFirst, "fragment",
                listOf(Argument("myarg1", StringType, StringValue("one"))),
                listOf(Action(id("next"), id("next_fragment"), listOf(
                        Argument("myarg2", StringType),
                        Argument("randomArgument", StringType),
                        Argument("intArgument", IntType, IntValue("261")),
                        Argument(
                                "activityInfo",
                                ObjectType(ClassName.get("android.content.pm", "ActivityInfo"))
                        ),
                        Argument(
                                "activityInfoNull",
                                ObjectType(ClassName.get("android.content.pm", "ActivityInfo")),
                                NullValue,
                                true
                        ),
                        Argument("intArrayArg", IntArrayType),
                        Argument("stringArrayArg", StringArrayType),
                        Argument("objectArrayArg", ObjectArrayType(
                            ClassName.get("android.content.pm", "ActivityInfo"))),
                        Argument(
                            "enumArg",
                            ObjectType(ClassName.get("java.nio.file", "AccessMode")),
                            EnumValue(ClassName.get("java.nio.file", "AccessMode"), "READ"),
                            false
                        ),
                        Argument(
                            "objectRelativeArg",
                            ObjectType(ClassName.get("a.b.pkg", "ClassName"))
                        ),
                        Argument(
                            "objectRelativeArg2",
                            ObjectType(ClassName.get("a.b", "ClassName"))
                        )
                ))))

        val expectedNext = Destination(id("next_fragment"), nameNext, "fragment",
                listOf(Argument("myarg2", StringType)),
                listOf(Action(id("next"), id("first_screen")),
                        Action(id("finish"), null)))

        val expectedGraph = Destination(null, null, "navigation", emptyList(), emptyList(),
                listOf(expectedFirst, expectedNext))
        assertThat(navGraph, `is`(expectedGraph))
    }

    @Test
    fun testNestedGraph() {
        val id: (String) -> ResReference = { id -> ResReference("a.b", "id", id) }
        val navGraph = NavParser.parseNavigationFile(testData("nested_login_test.xml"),
                "a.b", "foo.app", Context())

        val expectedMainFragment = Destination(
                id = id("main_fragment"),
                name = ClassName.get("foo.app", "MainFragment"),
                type = "fragment",
                args = emptyList(),
                actions = listOf(Action(id("start_login"), id("login"))))

        val expectedNestedFragment1 = Destination(
                id = id("login_fragment"),
                name = ClassName.get("foo.app", "LoginFragment"),
                type = "fragment",
                args = emptyList(),
                actions = listOf(Action(id("register"), id("register_fragment"))))

        val expectedNestedFragment2 = Destination(
                id = id("register_fragment"),
                name = ClassName.get("foo.app", "RegisterFragment"),
                type = "fragment",
                args = emptyList(),
                actions = emptyList())

        val expectedNestedGraph = Destination(
                id = id("login"),
                name = ClassName.get("a.b", "Login"),
                type = "navigation",
                args = emptyList(),
                actions = listOf(Action(id("action_done"), null)),
                nested = listOf(expectedNestedFragment1, expectedNestedFragment2))

        val expectedGraph = Destination(null, null, "navigation", emptyList(), emptyList(),
                listOf(expectedMainFragment, expectedNestedGraph))

        assertThat(navGraph, `is`(expectedGraph))
    }

    @Test
    fun testNestedIncludedGraph() {
        val id: (String) -> ResReference = { id -> ResReference("a.b", "id", id) }
        val nestedIncludeNavGraph = NavParser.parseNavigationFile(
                testData("nested_include_login_test.xml"), "a.b", "foo.app", Context())

        val expectedMainFragment = Destination(
                id = id("main_fragment"),
                name = ClassName.get("foo.app", "MainFragment"),
                type = "fragment",
                args = emptyList(),
                actions = listOf(Action(id("start_login"), id("login"))))

        val expectedIncluded = IncludedDestination(ResReference("a.b", "navigation",
                "to_include_login_test"))

        val expectedGraph = Destination(null, null, "navigation", emptyList(), emptyList(),
                listOf(expectedMainFragment), listOf(expectedIncluded))

        assertThat(nestedIncludeNavGraph, `is`(expectedGraph))
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
    fun testLongValueParsing() {
        assertThat(parseLongValue("foo"), nullValue())
        assertThat(parseLongValue("10"), nullValue())
        assertThat(parseLongValue("10L"), `is`(LongValue("10L")))
        assertThat(parseLongValue("-10L"), `is`(LongValue("-10L")))
        assertThat(parseLongValue("0xA"), nullValue())
        assertThat(parseLongValue("0xAL"), `is`(LongValue("0xAL")))
        assertThat(parseLongValue("0xFFFFFFFFL"), `is`(LongValue("0xFFFFFFFFL")))
        assertThat(parseLongValue("0x1FFFFFFFFL"), `is`(LongValue("0x1FFFFFFFFL")))
        assertThat(parseLongValue("0x1FFFFFFFF1FFFFFFFFL"), nullValue())
    }

    @Test
    fun testArgInference() {
        val infer = { value: String -> inferArgument("foo", value, "a.b") }
        val intArg = { value: String -> Argument("foo", IntType, IntValue(value)) }
        val longArg = { value: String -> Argument("foo", LongType, LongValue(value)) }
        val floatArg = { value: String -> Argument("foo", FloatType, FloatValue(value)) }
        val stringArg = { value: String -> Argument("foo", StringType, StringValue(value)) }
        val boolArg = { value: String -> Argument("foo", BoolType, BooleanValue(value)) }
        val referenceArg = { pName: String, type: String, value: String ->
            Argument("foo", ReferenceType, ReferenceValue(ResReference(pName, type, value)))
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
        assertThat(infer("123L"), `is`(longArg("123L")))
        assertThat(infer("1234123412341234L"), `is`(longArg("1234123412341234L")))
    }

    @Test
    fun testArgSanitizedName() {
        assertEquals("camelCaseName",
                Argument("camelCaseName", IntType).sanitizedName)
        assertEquals("ALLCAPSNAME",
                Argument("ALLCAPSNAME", IntType).sanitizedName)
        assertEquals("alllowercasename",
                Argument("alllowercasename", IntType).sanitizedName)
        assertEquals("nameWithUnderscore",
                Argument("name_with_underscore", IntType).sanitizedName)
        assertEquals("NameWithUnderscore",
                Argument("Name_With_Underscore", IntType).sanitizedName)
        assertEquals("NAMEWITHUNDERSCORE",
                Argument("NAME_WITH_UNDERSCORE", IntType).sanitizedName)
        assertEquals("nameWithSpaces",
                Argument("name with spaces", IntType).sanitizedName)
        assertEquals("nameWithDot",
                Argument("name.with.dot", IntType).sanitizedName)
        assertEquals("nameWithDollars",
                Argument("name\$with\$dollars", IntType).sanitizedName)
        assertEquals("nameWithBangs",
                Argument("name!with!bangs", IntType).sanitizedName)
        assertEquals("nameWithHyphens",
                Argument("name-with-hyphens", IntType).sanitizedName)
    }
}