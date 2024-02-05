/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.privacysandbox.tools.apicompiler.parser

import androidx.privacysandbox.tools.apicompiler.util.checkSourceFails
import androidx.privacysandbox.tools.apicompiler.util.parseSources
import androidx.privacysandbox.tools.core.model.AnnotatedDataClass
import androidx.privacysandbox.tools.core.model.AnnotatedEnumClass
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.core.model.Types.asNullable
import androidx.privacysandbox.tools.core.model.ValueProperty
import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ValueParserTest {

    @Test
    fun parseDataClass_ok() {
        val source = Source.kotlin(
            "com/mysdk/MySdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    import androidx.privacysandbox.tools.PrivacySandboxValue
                    @PrivacySandboxService
                    interface MySdk {
                        suspend fun doStuff(request: MySdkRequest): MySdkResponse
                    }
                    @PrivacySandboxValue
                    data class MySdkRequest(val id: Int, val message: String?)
                    @PrivacySandboxValue
                    data class MySdkResponse(
                        val magicPayload: MagicPayload, val isTrulyMagic: Boolean)
                    @PrivacySandboxValue
                    data class MagicPayload(val magicList: List<Long>)
                """
        )
        assertThat(parseSources(source)).isEqualTo(
            ParsedApi(
                services = setOf(
                    AnnotatedInterface(
                        type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                        methods = listOf(
                            Method(
                                name = "doStuff",
                                parameters = listOf(
                                    Parameter("request", Type("com.mysdk", "MySdkRequest"))
                                ),
                                returnType = Type("com.mysdk", "MySdkResponse"),
                                isSuspend = true
                            )
                        )
                    )
                ),
                values = setOf(
                    AnnotatedDataClass(
                        type = Type(packageName = "com.mysdk", simpleName = "MySdkRequest"),
                        properties = listOf(
                            ValueProperty("id", Types.int),
                            ValueProperty("message", Types.string.asNullable()),
                        )
                    ),
                    AnnotatedDataClass(
                        type = Type(packageName = "com.mysdk", simpleName = "MySdkResponse"),
                        properties = listOf(
                            ValueProperty(
                                "magicPayload",
                                Type(packageName = "com.mysdk", simpleName = "MagicPayload")
                            ),
                            ValueProperty("isTrulyMagic", Types.boolean),
                        )
                    ),
                    AnnotatedDataClass(
                        type = Type(packageName = "com.mysdk", simpleName = "MagicPayload"),
                        properties = listOf(ValueProperty("magicList", Types.list(Types.long)))
                    ),
                )
            )
        )
    }

    @Test
    fun parseEnumClass_ok() {
        val source = Source.kotlin(
            "com/mysdk/MySdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    import androidx.privacysandbox.tools.PrivacySandboxValue
                    @PrivacySandboxService
                    interface MySdk {
                    }
                    @PrivacySandboxValue
                    enum class MyEnum { FOO, BAR }
                """
        )
        assertThat(parseSources(source).values).isEqualTo(
            setOf(
                AnnotatedEnumClass(
                    Type(packageName = "com.mysdk", simpleName = "MyEnum"),
                    listOf("FOO", "BAR")
                )
            )
        )
    }

    @Test
    fun enumClassWithFields_fails() {
        val source =
            annotatedValue(
                """
                    enum class MyEnum(val x: Int, val y: String) {
                        FOO(0, "")
                    }
                """
            )
        checkSourceFails(source).containsExactlyErrors(
            "Error in com.mysdk.MyEnum: Enum classes annotated with @PrivacySandboxValue " +
                "may not declare properties (x, y)"
        )
    }

    @Test
    fun enumClassWithMethods_fails() {
        val source =
            annotatedValue(
                """
                    enum class HungryState {
                        HUNGRY, FULL;
                        abstract fun eat()
                    }
                """
            )
        checkSourceFails(source).containsExactlyErrors(
            "Error in com.mysdk.HungryState: types annotated with @PrivacySandboxValue may " +
                "not declare methods (eat)"
        )
    }

    @Test
    fun enumClassWithVariantMethods_fails() {
        val source =
            annotatedValue(
                """
                    @PrivacySandboxValue
                    enum class Direction {
                        LEFT {
                            fun bearing(): Int = 270
                        },
                        RIGHT;
                    }
                """
            )
        checkSourceFails(source).containsExactlyErrors(
            "Error in com.mysdk.Direction: types annotated with @PrivacySandboxValue " +
                "may not declare methods (bearing)"
        )
    }

    @Test
    fun enumClassImplementingInterface_fails() {
        val source = Source.kotlin(
            "com/mysdk/MySdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    import androidx.privacysandbox.tools.PrivacySandboxValue
                    @PrivacySandboxService
                    interface MySdk {
                    }
                    interface Transmogrifable {}
                    @PrivacySandboxValue
                    enum class MoveState : Transmogrifable {
                        STOP, GO;
                    }
                """
        )
        checkSourceFails(source).containsExactlyErrors(
            "Error in com.mysdk.MoveState: values annotated with @PrivacySandboxValue" +
                " may not inherit other types (com.mysdk.Transmogrifable)"
        )
    }

    @Test
    fun interfaceValue_fails() {
        checkSourceFails(annotatedValue("interface MySdkRequest(val id: Int)"))
            .containsExactlyErrors(
                "Only data classes and enum classes can be annotated " +
                    "with @PrivacySandboxValue."
            )
    }

    @Test
    fun nonDataClassValue_fails() {
        checkSourceFails(annotatedValue("private class MySdkRequest(val id: Int)"))
            .containsExactlyErrors(
                "Only data classes and enum classes can be annotated with" +
                    " @PrivacySandboxValue."
            )
    }

    @Test
    fun privateValue_fails() {
        checkSourceFails(annotatedValue("private data class MySdkRequest(val id: Int)"))
            .containsExactlyErrors(
                "Error in com.mysdk.MySdkRequest: annotated values should be public."
            )
    }

    @Test
    fun dataClassWithCompanionObject_fails() {
        val dataClass = annotatedValue(
            """
            |data class MySdkRequest(val id: Int) {
            |   companion object {
            |       val someConstant = 12
            |   }
            |}
        """.trimMargin()
        )
        checkSourceFails(dataClass)
            .containsExactlyErrors(
                "Error in com.mysdk.MySdkRequest: annotated values cannot declare companion " +
                    "objects."
            )
    }

    @Test
    fun dataClassWithTypeParameters_fails() {
        val dataClass = annotatedValue("data class MySdkRequest<T>(val id: Int, val data: T)")
        checkSourceFails(dataClass)
            .containsError(
                "Error in com.mysdk.MySdkRequest: annotated values cannot declare type " +
                    "parameters (T)."
            )
    }

    @Test
    fun dataClassWithMutableProperty_fails() {
        val dataClass = annotatedValue(
            "data class MySdkRequest(val id: Int, var data: String)"
        )
        checkSourceFails(dataClass)
            .containsExactlyErrors(
                "Error in com.mysdk.MySdkRequest.data: properties cannot be mutable."
            )
    }

    @Test
    fun dataClassWithInvalidPropertyType_fails() {
        val dataClass = annotatedValue(
            "data class MySdkRequest(val foo: IntArray)"
        )
        checkSourceFails(dataClass)
            .containsExactlyErrors(
                "Error in com.mysdk.MySdkRequest.foo: only primitives, lists, data classes " +
                    "annotated with @PrivacySandboxValue, interfaces annotated with " +
                    "@PrivacySandboxInterface, and SdkActivityLaunchers are supported as " +
                    "properties."
            )
    }

    @Test
    fun dataClassWithMethod_fails() {
        val dataClass = annotatedValue(
            """
                |data class MySdkRequest(val id: Int, val data: String) {
                |   abstract fun sendRequest()
                |}
            """.trimMargin()
        )
        checkSourceFails(dataClass)
            .containsExactlyErrors(
                "Error in com.mysdk.MySdkRequest: types annotated with @PrivacySandboxValue may" +
                    " not declare methods (sendRequest)"
            )
    }

    private fun annotatedValue(declaration: String) = Source.kotlin(
        "com/mysdk/MySdk.kt", """
            package com.mysdk
            import androidx.privacysandbox.tools.PrivacySandboxService
            import androidx.privacysandbox.tools.PrivacySandboxValue
            @PrivacySandboxService
            interface MySdk
            @PrivacySandboxValue
            $declaration
        """
    )
}
