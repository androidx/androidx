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
import com.google.common.truth.Truth.assertThat
import com.squareup.javapoet.ClassName
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavParserTest {

    @Test
    fun testNaiveGraph() {
        val id: (String) -> ResReference = { id -> ResReference("a.b", "id", id) }
        val navGraph = NavParser.parseNavigationFile(
            testData("naive_test.xml"),
            "a.b", "foo.app", Context()
        )

        val nameFirst = ClassName.get("androidx.navigation.testapp", "MainFragment")
        val nameNext = ClassName.get("foo.app", "NextFragment")
        val expectedFirst = Destination(
            id("first_screen"), nameFirst, "fragment",
            listOf(Argument("myarg1", StringType, StringValue("one"))),
            listOf(
                Action(
                    id("next"), id("next_fragment"),
                    listOf(
                        Argument("myarg2", StringType),
                        Argument("randomArgument", StringType),
                        Argument("intArgument", IntType, IntValue("261")),
                        Argument("referenceZeroDefaultValue", ReferenceType, IntValue("0")),
                        Argument(
                            "activityInfo",
                            ObjectType("android.content.pm.ActivityInfo")
                        ),
                        Argument(
                            "activityInfoNull",
                            ObjectType("android.content.pm.ActivityInfo"),
                            NullValue,
                            true
                        ),
                        Argument("intArrayArg", IntArrayType),
                        Argument("stringArrayArg", StringArrayType),
                        Argument(
                            "objectArrayArg",
                            ObjectArrayType(
                                "android.content.pm.ActivityInfo"
                            )
                        ),
                        Argument(
                            "enumArg",
                            ObjectType("java.nio.file.AccessMode"),
                            EnumValue(ObjectType("java.nio.file.AccessMode"), "READ"),
                            false
                        ),
                        Argument(
                            "objectRelativeArg",
                            ObjectType("a.b.pkg.ClassName")
                        ),
                        Argument(
                            "objectRelativeArg2",
                            ObjectType("a.b.ClassName")
                        ),
                        Argument(
                            "objectRelativeArg3",
                            ObjectType("a.b.OuterClass\$InnerClass")
                        ),
                        Argument("implicitNullString", StringType, NullValue, true),
                        Argument("explicitNullString", StringType, NullValue, true)
                    )
                )
            )
        )

        val expectedNext = Destination(
            id("next_fragment"), nameNext, "fragment",
            listOf(Argument("myarg2", StringType)),
            listOf(
                Action(id("next"), id("first_screen")),
                Action(id("finish"), null)
            )
        )

        val expectedGraph = Destination(
            null, null, "navigation", emptyList(), emptyList(),
            listOf(expectedFirst, expectedNext)
        )
        assertThat(navGraph).isEqualTo(expectedGraph)
    }

    @Test
    fun testNestedGraph() {
        val id: (String) -> ResReference = { id -> ResReference("a.b", "id", id) }
        val navGraph = NavParser.parseNavigationFile(
            testData("nested_login_test.xml"),
            "a.b", "foo.app", Context()
        )

        val expectedMainFragment = Destination(
            id = id("main_fragment"),
            name = ClassName.get("foo.app", "MainFragment"),
            type = "fragment",
            args = emptyList(),
            actions = listOf(Action(id("start_login"), id("login")))
        )

        val expectedNestedFragment1 = Destination(
            id = id("login_fragment"),
            name = ClassName.get("foo.app.account", "LoginFragment"),
            type = "fragment",
            args = emptyList(),
            actions = listOf(Action(id("register"), id("register_fragment")))
        )

        val expectedNestedFragment2 = Destination(
            id = id("register_fragment"),
            name = ClassName.get("foo.app.account", "RegisterFragment"),
            type = "fragment",
            args = emptyList(),
            actions = emptyList()
        )

        val expectedNestedGraph = Destination(
            id = id("login"),
            name = ClassName.get("a.b", "Login"),
            type = "navigation",
            args = emptyList(),
            actions = listOf(Action(id("action_done"), null)),
            nested = listOf(expectedNestedFragment1, expectedNestedFragment2)
        )

        val expectedGraph = Destination(
            null, null, "navigation", emptyList(), emptyList(),
            listOf(expectedMainFragment, expectedNestedGraph)
        )

        assertThat(navGraph).isEqualTo(expectedGraph)
    }

    @Test
    fun testNestedIncludedGraph() {
        val id: (String) -> ResReference = { id -> ResReference("a.b", "id", id) }
        val nestedIncludeNavGraph = NavParser.parseNavigationFile(
            testData("nested_include_login_test.xml"), "a.b", "foo.app", Context()
        )

        val expectedMainFragment = Destination(
            id = id("main_fragment"),
            name = ClassName.get("foo.app", "MainFragment"),
            type = "fragment",
            args = emptyList(),
            actions = listOf(Action(id("start_login"), id("login")))
        )

        val expectedIncluded = IncludedDestination(
            ResReference(
                "a.b", "navigation",
                "to_include_login_test"
            )
        )

        val expectedGraph = Destination(
            null, null, "navigation", emptyList(), emptyList(),
            listOf(expectedMainFragment), listOf(expectedIncluded)
        )

        assertThat(nestedIncludeNavGraph).isEqualTo(expectedGraph)
    }

    @Test
    fun testReferenceParsing() {
        assertThat(parseReference("@+id/next", "a.b"))
            .isEqualTo(ResReference("a.b", "id", "next"))
        assertThat(parseReference("@+id/next", "a.b"))
            .isEqualTo(ResReference("a.b", "id", "next"))
        assertThat(parseReference("@android:string/text", "a.b"))
            .isEqualTo(ResReference("android", "string", "text"))
        assertThat(parseReference("@android:id/text", "a.b"))
            .isEqualTo(ResReference("android", "id", "text"))
        assertThat(parseReference("@not.android:string/text", "a.b"))
            .isEqualTo(ResReference("not.android", "string", "text"))
    }

    @Test
    fun testIntValueParsing() {
        assertThat(parseIntValue("foo")).isNull()
        assertThat(parseIntValue("10")).isEqualTo(IntValue("10"))
        assertThat(parseIntValue("-10")).isEqualTo(IntValue("-10"))
        assertThat(parseIntValue("0xA")).isEqualTo(IntValue("0xA"))
        assertThat(parseIntValue("0xFFFFFFFF")).isEqualTo(IntValue("0xFFFFFFFF"))
        assertThat(parseIntValue("0x1FFFFFFFF")).isNull()
    }

    @Test
    fun testLongValueParsing() {
        assertThat(parseLongValue("foo")).isNull()
        assertThat(parseLongValue("10")).isNull()
        assertThat(parseLongValue("10L")).isEqualTo(LongValue("10L"))
        assertThat(parseLongValue("-10L")).isEqualTo(LongValue("-10L"))
        assertThat(parseLongValue("0xA")).isNull()
        assertThat(parseLongValue("0xAL")).isEqualTo(LongValue("0xAL"))
        assertThat(parseLongValue("0xFFFFFFFFL")).isEqualTo(LongValue("0xFFFFFFFFL"))
        assertThat(parseLongValue("0x1FFFFFFFFL")).isEqualTo(LongValue("0x1FFFFFFFFL"))
        assertThat(parseLongValue("0x1FFFFFFFF1FFFFFFFFL")).isNull()
    }

    @Test
    fun testArgInference() {
        val infer = { value: String -> inferArgument("foo", value, "a.b") }
        val intArg = { value: String -> Argument("foo", IntType, IntValue(value)) }
        val longArg = { value: String -> Argument("foo", LongType, LongValue(value)) }
        val floatArg = { value: String -> Argument("foo", FloatType, FloatValue(value)) }
        val stringArg = { value: String -> Argument("foo", StringType, StringValue(value)) }
        val nullStringArg = Argument("foo", StringType, NullValue, true)
        val boolArg = { value: String -> Argument("foo", BoolType, BooleanValue(value)) }
        val referenceArg = { pName: String, type: String, value: String ->
            Argument("foo", ReferenceType, ReferenceValue(ResReference(pName, type, value)))
        }
        val resolvedReferenceArg = { pName: String, argType: NavType, type: String, value: String ->
            Argument("foo", argType, ReferenceValue(ResReference(pName, type, value)))
        }

        assertThat(infer("spb")).isEqualTo(stringArg("spb"))
        assertThat(infer("@null")).isEqualTo(nullStringArg)
        assertThat(infer("null")).isEqualTo(stringArg("null"))
        assertThat(infer("10")).isEqualTo(intArg("10"))
        assertThat(infer("0x10")).isEqualTo(intArg("0x10"))
        assertThat(infer("@android:id/some_la")).isEqualTo(referenceArg("android", "id", "some_la"))
        assertThat(infer("@foo")).isEqualTo(stringArg("@foo"))
        assertThat(infer("@+id/foo")).isEqualTo(referenceArg("a.b", "id", "foo"))
        assertThat(infer("@foo:stuff")).isEqualTo(stringArg("@foo:stuff"))
        assertThat(infer("@/stuff")).isEqualTo(stringArg("@/stuff"))
        assertThat(infer("10101010100100")).isEqualTo(floatArg("10101010100100"))
        assertThat(infer("1.")).isEqualTo(floatArg("1."))
        assertThat(infer("1.2e-4")).isEqualTo(floatArg("1.2e-4"))
        assertThat(infer(".4")).isEqualTo(floatArg(".4"))
        assertThat(infer("true")).isEqualTo(boolArg("true"))
        assertThat(infer("false")).isEqualTo(boolArg("false"))
        assertThat(infer("123L")).isEqualTo(longArg("123L"))
        assertThat(infer("1234123412341234L")).isEqualTo(longArg("1234123412341234L"))

        assertThat(infer("@integer/test_integer_arg"))
            .isEqualTo(resolvedReferenceArg("a.b", IntType, "integer", "test_integer_arg"))
        assertThat(infer("@dimen/test_dimen_arg"))
            .isEqualTo(resolvedReferenceArg("a.b", IntType, "dimen", "test_dimen_arg"))
        assertThat(infer("@style/AppTheme"))
            .isEqualTo(resolvedReferenceArg("a.b", ReferenceType, "style", "AppTheme"))
        assertThat(infer("@string/test_string_arg"))
            .isEqualTo(resolvedReferenceArg("a.b", StringType, "string", "test_string_arg"))
        assertThat(infer("@color/test_color_arg"))
            .isEqualTo(resolvedReferenceArg("a.b", IntType, "color", "test_color_arg"))
    }

    @Test
    fun testArgSanitizedName() {
        assertThat("camelCaseName")
            .isEqualTo(Argument("camelCaseName", IntType).sanitizedName)
        assertThat("ALLCAPSNAME")
            .isEqualTo(Argument("ALLCAPSNAME", IntType).sanitizedName)
        assertThat("alllowercasename")
            .isEqualTo(Argument("alllowercasename", IntType).sanitizedName)
        assertThat("nameWithUnderscore")
            .isEqualTo(Argument("name_with_underscore", IntType).sanitizedName)
        assertThat("NameWithUnderscore")
            .isEqualTo(Argument("Name_With_Underscore", IntType).sanitizedName)
        assertThat("NAMEWITHUNDERSCORE")
            .isEqualTo(Argument("NAME_WITH_UNDERSCORE", IntType).sanitizedName)
        assertThat("nameWithSpaces")
            .isEqualTo(Argument("name with spaces", IntType).sanitizedName)
        assertThat("nameWithDot")
            .isEqualTo(Argument("name.with.dot", IntType).sanitizedName)
        assertThat("nameWithDollars")
            .isEqualTo(Argument("name\$with\$dollars", IntType).sanitizedName)
        assertThat("nameWithBangs")
            .isEqualTo(Argument("name!with!bangs", IntType).sanitizedName)
        assertThat("nameWithHyphens")
            .isEqualTo(Argument("name-with-hyphens", IntType).sanitizedName)
    }
}