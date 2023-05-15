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

package androidx.privacysandbox.tools.apigenerator.parser

import androidx.privacysandbox.tools.apigenerator.mergedClasspath
import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.core.model.Types.asNullable
import androidx.privacysandbox.tools.core.model.ValueProperty
import androidx.privacysandbox.tools.testing.CompilationTestHelper.assertCompiles
import androidx.room.compiler.processing.util.Source
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApiStubParserTest {
    @Test
    fun annotatedInterface_isParsed() {
        val source = Source.kotlin(
            "com/mysdk/TestSandboxSdk.kt",
            """
                    |package com.mysdk
                    |
                    |import androidx.privacysandbox.tools.PrivacySandboxCallback
                    |import androidx.privacysandbox.tools.PrivacySandboxInterface
                    |import androidx.privacysandbox.tools.PrivacySandboxService
                    |import androidx.privacysandbox.tools.PrivacySandboxValue
                    |import androidx.privacysandbox.ui.core.SandboxedUiAdapter
                    |
                    |@PrivacySandboxService
                    |interface MySdk {
                    |  fun doSomething(magicNumber: Int, awesomeString: String?)
                    |  suspend fun getPayload(request: PayloadRequest): PayloadResponse
                    |  suspend fun getInterface(): MyInterface
                    |  suspend fun getUiInterface(): MyUiInterface
                    |  suspend fun processList(list: List<Long>): List<Long>
                    |}
                    |
                    |@PrivacySandboxInterface
                    |interface MyInterface {
                    |  suspend fun getMorePayload(request: PayloadRequest): PayloadResponse
                    |}
                    |
                    |@PrivacySandboxInterface
                    |interface MyUiInterface : SandboxedUiAdapter {}
                    |
                    |@PrivacySandboxValue
                    |data class PayloadType(val size: Long, val appId: String)
                    |
                    |@PrivacySandboxValue
                    |data class PayloadResponse(val url: String)
                    |
                    |@PrivacySandboxValue
                    |data class PayloadRequest(val type: PayloadType)
                    |
                    |@PrivacySandboxCallback
                    |interface CustomCallback {
                    |  fun onComplete(status: Int)
                    |}
                """.trimMargin(),
        )

        val expectedPayloadType = AnnotatedValue(
            type = Type("com.mysdk", "PayloadType"),
            properties = listOf(
                ValueProperty("size", Types.long),
                ValueProperty("appId", Types.string),
            )
        )
        val expectedPayloadRequest = AnnotatedValue(
            type = Type("com.mysdk", "PayloadRequest"),
            properties = listOf(
                ValueProperty("type", expectedPayloadType.type),
            )
        )
        val expectedPayloadResponse = AnnotatedValue(
            type = Type("com.mysdk", "PayloadResponse"),
            properties = listOf(
                ValueProperty("url", Types.string),
            )
        )
        val expectedService =
            AnnotatedInterface(
                type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                methods = listOf(
                    Method(
                        name = "doSomething",
                        parameters = listOf(
                            Parameter(
                                "magicNumber",
                                Types.int
                            ),
                            Parameter("awesomeString", Types.string.asNullable())
                        ),
                        returnType = Types.unit,
                        isSuspend = false,
                    ),
                    Method(
                        name = "getInterface",
                        parameters = listOf(),
                        returnType = Type("com.mysdk", "MyInterface"),
                        isSuspend = true,
                    ),
                    Method(
                        name = "getPayload",
                        parameters = listOf(
                            Parameter(
                                "request",
                                expectedPayloadRequest.type
                            )
                        ),
                        returnType = expectedPayloadResponse.type,
                        isSuspend = true,
                    ),
                    Method(
                        name = "getUiInterface",
                        parameters = listOf(),
                        returnType = Type("com.mysdk", "MyUiInterface"),
                        isSuspend = true,
                    ),
                    Method(
                        name = "processList",
                        parameters = listOf(
                            Parameter(
                                "list",
                                Types.list(Types.long)
                            )
                        ),
                        returnType = Types.list(Types.long),
                        isSuspend = true,
                    ),
                )
            )
        val expectedInterfaces = listOf(
            AnnotatedInterface(
                type = Type(packageName = "com.mysdk", simpleName = "MyInterface"),
                methods = listOf(
                    Method(
                        name = "getMorePayload",
                        parameters = listOf(
                            Parameter(
                                "request",
                                expectedPayloadRequest.type
                            )
                        ),
                        returnType = expectedPayloadResponse.type,
                        isSuspend = true,
                    )
                )
            ),
            AnnotatedInterface(
                type = Type(packageName = "com.mysdk", simpleName = "MyUiInterface"),
                superTypes = listOf(Types.sandboxedUiAdapter),
                methods = listOf(),
            ),
        )
        val expectedCallback = AnnotatedInterface(
            type = Type(packageName = "com.mysdk", simpleName = "CustomCallback"),
                methods = listOf(
                    Method(
                        name = "onComplete",
                        parameters = listOf(
                            Parameter(
                                "status",
                                Types.int
                            ),
                        ),
                        returnType = Types.unit,
                        isSuspend = false,
                    ),
                )
        )

        val actualApi = compileAndParseApi(source)
        assertThat(actualApi.services).containsExactly(expectedService)
        assertThat(actualApi.values).containsExactly(
            expectedPayloadType,
            expectedPayloadRequest,
            expectedPayloadResponse,
        )
        assertThat(actualApi.callbacks).containsExactly(expectedCallback)
        assertThat(actualApi.interfaces).containsExactlyElementsIn(expectedInterfaces)
    }

    @Test
    fun nonAnnotatedClasses_areSafelyIgnored() {
        val interfaces = compileAndParseApi(
            Source.kotlin(
                "com/mysdk/TestSandboxSdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk
                """
            ), Source.kotlin(
                "com/mysdk/NonAnnotatedInterface.kt", """
                    package com.mysdk
                    interface NonAnnotatedInterface
                """
            ), Source.kotlin(
                "com/mysdk/NonAnnotatedClass.kt", """
                    package com.mysdk
                    class NonAnnotatedClass
                """
            ), Source.java(
                "com/mysdk/NonAnnotatedJavaClass", """
                    package com.mysdk;
                    class NonAnnotatedJavaClass {}
                """
            )
        ).services

        assertThat(interfaces).containsExactly(
            AnnotatedInterface(
                Type(
                    packageName = "com.mysdk",
                    simpleName = "MySdk",
                )
            )
        )
    }

    @Test
    fun annotatedInterfaceWithEmptyPackageName_isHandledSafely() {
        val source = Source.kotlin(
            "TestSandboxSdk.kt", """
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk
                """
        )

        assertThat(compileAndParseApi(source).services).containsExactly(
            AnnotatedInterface(
                Type(
                    packageName = "",
                    simpleName = "MySdk"
                )
            )
        )
    }

    @Test
    fun nonKotlinAnnotatedInterface_throws() {
        val source = Source.java(
            "com/mysdk/MySdk", """
                    package com.mysdk;
                    import androidx.privacysandbox.tools.PrivacySandboxService;
                    @PrivacySandboxService
                    interface MySdk {}
                """
        )

        assertThrows<PrivacySandboxParsingException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "Missing Kotlin metadata annotation in com/mysdk/MySdk. Is this a valid Kotlin class?"
        )
    }

    @Test
    fun kotlinClassAnnotatedAsService_throws() {
        val source = Source.kotlin(
            "com/mysdk/TestSandboxSdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    class MySdk
                """
        )

        assertThrows<PrivacySandboxParsingException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "com.mysdk.MySdk is not a Kotlin interface but it's annotated with " +
                "@PrivacySandboxService"
        )
    }

    @Test
    fun nonDataClassAnnotatedAsValue_throws() {
        val source = Source.kotlin(
            "com/mysdk/TestSandboxSdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    import androidx.privacysandbox.tools.PrivacySandboxValue
                    @PrivacySandboxService
                    interface MySdk
                    @PrivacySandboxValue
                    class Value
                """
        )

        assertThrows<PrivacySandboxParsingException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "com.mysdk.Value is not a Kotlin data class but it's annotated with " +
                "@PrivacySandboxValue"
        )
    }

    @Test
    fun interfaceAnnotatedAsValue_throws() {
        val source = Source.kotlin(
            "com/mysdk/TestSandboxSdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    import androidx.privacysandbox.tools.PrivacySandboxValue
                    @PrivacySandboxService
                    interface MySdk
                    @PrivacySandboxValue
                    interface Value
                """
        )

        assertThrows<PrivacySandboxParsingException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "com.mysdk.Value is not a Kotlin data class but it's annotated with " +
                "@PrivacySandboxValue"
        )
    }

    @Test
    fun valueWithMutableProperties_throws() {
        val source = Source.kotlin(
            "com/mysdk/TestSandboxSdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    import androidx.privacysandbox.tools.PrivacySandboxValue
                    @PrivacySandboxService
                    interface MySdk
                    @PrivacySandboxValue
                    data class Value(var message: String)
                """
        )

        assertThrows<PrivacySandboxParsingException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "Error in com.mysdk.Value.message: mutable properties are not allowed in data " +
                "classes annotated with @PrivacySandboxValue."
        )
    }

    @Test
    fun annotatedKotlinInnerInterface_throws() {
        val source = Source.kotlin(
            "com/mysdk/OuterMySdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    interface OuterMySdk {
                      @PrivacySandboxService
                      interface InnerMySdk
                    }
                """
        )

        assertThrows<PrivacySandboxParsingException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "Error in com.mysdk.OuterMySdk.InnerMySdk: Inner types are not supported in API " +
                "definitions"
        )
    }

    @Test
    fun missingAnnotatedInterface_throws() {
        val source = Source.kotlin(
            "com/mysdk/TestSandboxSdk.kt", """
                    package com.mysdk
                    interface MySdk
                """
        )

        assertThrows<PrivacySandboxParsingException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "Unable to find valid interfaces annotated with @PrivacySandboxService."
        )
    }

    private fun compileAndParseApi(vararg sources: Source): ParsedApi {
        val classpath = mergedClasspath(assertCompiles(sources.toList()))
        return ApiStubParser.parse(classpath)
    }
}
