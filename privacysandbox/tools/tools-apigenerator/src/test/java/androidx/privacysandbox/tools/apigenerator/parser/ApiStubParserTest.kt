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

import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.Method
import androidx.privacysandbox.tools.core.Parameter
import androidx.privacysandbox.tools.core.Type
import androidx.room.compiler.processing.util.Source
import androidx.room.compiler.processing.util.compiler.TestCompilationArguments
import androidx.room.compiler.processing.util.compiler.compile
import androidx.testutils.assertThrows
import com.google.common.truth.Truth.assertThat
import java.nio.file.Files
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApiStubParserTest {
    @Test
    fun annotatedInterface_isParsed() {
        val source = Source.kotlin(
            "com/mysdk/MySdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk {
                      fun doSomething(magicNumber: Int, awesomeString: String)
                      fun returnMagicNumber(): Int
                    }
                """
        )

        assertThat(compileAndParseApi(source))
            .containsExactly(
                AnnotatedInterface(
                    name = "MySdk", packageName = "com.mysdk", methods = listOf(
                        Method(
                            name = "doSomething",
                            parameters = listOf(
                                Parameter("magicNumber", Type("kotlin.Int")),
                                Parameter("awesomeString", Type("kotlin.String"))
                            ),
                            returnType = Type("kotlin.Unit")
                        ),
                        Method(
                            name = "returnMagicNumber",
                            parameters = listOf(),
                            returnType = Type("kotlin.Int")
                        )
                    )
                )
            )
    }

    @Test
    fun nonAnnotatedClasses_areSafelyIgnored() {
        val interfaces = compileAndParseApi(
            Source.kotlin(
                "com/mysdk/MySdk.kt", """
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
                "com/mysdk/NonAnnotatedJavaClass.java", """
                    package com.mysdk;
                    class NonAnnotatedJavaClass {}
                """
            )
        )

        assertThat(interfaces).containsExactly(AnnotatedInterface("MySdk", "com.mysdk"))
    }

    @Test
    fun annotatedInterfaceWithEmptyPackageName_isHandledSafely() {
        val source = Source.kotlin(
            "MySdk.kt", """
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk
                """
        )

        assertThat(compileAndParseApi(source)).containsExactly(AnnotatedInterface("MySdk", ""))
    }

    @Test
    fun nonKotlinAnnotatedInterface_throws() {
        val source = Source.java(
            "com/mysdk/MySdk.java", """
                    package com.mysdk;
                    import androidx.privacysandbox.tools.PrivacySandboxService;
                    @PrivacySandboxService
                    interface MySdk {}
                """
        )

        assertThrows<IllegalArgumentException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "Missing Kotlin metadata annotation in com/mysdk/MySdk. Is this a valid Kotlin class?"
        )
    }

    @Test
    fun annotatedKotlinClass_throws() {
        val source = Source.kotlin(
            "com/mysdk/MySdk.kt", """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    class MySdk
                """
        )

        assertThrows<IllegalArgumentException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "com.mysdk.MySdk is not a Kotlin interface but it's annotated with " +
                "@PrivacySandboxService"
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

        assertThrows<IllegalArgumentException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "com.mysdk.OuterMySdk.InnerMySdk is an inner interface so it can't be annotated with " +
                "@PrivacySandboxService"
        )
    }

    @Test
    fun missingAnnotatedInterface_throws() {
        val source = Source.kotlin(
            "com/mysdk/MySdk.kt", """
                    package com.mysdk
                    interface MySdk
                """
        )

        assertThrows<IllegalArgumentException> {
            compileAndParseApi(source)
        }.hasMessageThat().contains(
            "Unable to find valid interfaces annotated with @PrivacySandboxService."
        )
    }

    private fun compileAndParseApi(vararg sources: Source): Set<AnnotatedInterface> {
        val result = compile(
            Files.createTempDirectory("test").toFile(), TestCompilationArguments(
                sources = sources.asList(),
            )
        )
        assertThat(result.success).isTrue()
        val stubClassPath = result.outputClasspath.flatMap { it.walk() }.toSet()
        return ApiStubParser.parse(stubClassPath).services
    }
}
