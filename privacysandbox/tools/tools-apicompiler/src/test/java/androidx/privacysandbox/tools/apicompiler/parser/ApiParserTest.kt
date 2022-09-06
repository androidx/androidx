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

import androidx.privacysandbox.tools.core.AnnotatedInterface
import androidx.privacysandbox.tools.core.Method
import androidx.privacysandbox.tools.core.Parameter
import androidx.privacysandbox.tools.core.ParsedApi
import androidx.privacysandbox.tools.core.Type
import androidx.privacysandbox.tools.apicompiler.util.checkSourceFails
import androidx.privacysandbox.tools.apicompiler.util.parseSource
import androidx.room.compiler.processing.util.Source
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ApiParserTest {

    @Test
    fun parseServiceInterface_ok() {
        val source =
            Source.kotlin(
                "com/mysdk/MySdk.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk {
                        fun doStuff(x: Int, y: Int): String
                        fun doMoreStuff()
                    }
                """
            )
        assertThat(parseSource(source)).isEqualTo(
            ParsedApi(
                services = mutableSetOf(
                    AnnotatedInterface(
                        name = "MySdk",
                        packageName = "com.mysdk",
                        methods = listOf(
                            Method(
                                name = "doStuff",
                                parameters = listOf(
                                    Parameter(
                                        name = "x",
                                        type = Type(
                                            name = "kotlin.Int",
                                        )
                                    ),
                                    Parameter(
                                        name = "y",
                                        type = Type(
                                            name = "kotlin.Int",
                                        )
                                    )
                                ),
                                returnType = Type(
                                    name = "kotlin.String",
                                )
                            ),
                            Method(
                                name = "doMoreStuff",
                                parameters = listOf(),
                                returnType = Type("kotlin.Unit")
                            )
                        )
                    )
                )
            )
        )
    }

    @Test
    fun serviceAnnotatedClass_fails() {
        val source =
            Source.kotlin(
                "com/mysdk/MySdk.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    abstract class MySdk {  // Fails because it's a class, not an interface.
                        abstract fun doStuff(x: Int, y: Int): String
                    }
                """
            )

        checkSourceFails(source)
            .containsError("Only interfaces can be annotated with @PrivacySandboxService.")
    }

    @Test
    fun multipleServices_fails() {
        val source =
            Source.kotlin(
                "com/mysdk/MySdk.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk
                """
            )
        val source2 =
            Source.kotlin(
                "com/mysdk/MySdk2.kt",
                """
                    package com.mysdk
                    import androidx.privacysandbox.tools.PrivacySandboxService
                    @PrivacySandboxService
                    interface MySdk2
                """
            )

        checkSourceFails(source, source2)
            .containsError("Multiple interfaces annotated with @PrivacySandboxService are not " +
                "supported (MySdk, MySdk2).")
    }
}