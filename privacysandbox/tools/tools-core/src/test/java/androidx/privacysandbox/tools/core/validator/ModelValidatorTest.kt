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

package androidx.privacysandbox.tools.core.validator

import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.AnnotatedValue
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModelValidatorTest {
    @Test
    fun validModel_ok() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    methods = listOf(
                        Method(
                            name = "doStuff",
                            parameters = listOf(
                                Parameter(
                                    name = "x",
                                    type = Types.int
                                ),
                                Parameter(
                                    name = "foo",
                                    type = Type(packageName = "com.mysdk", simpleName = "Foo")
                                )
                            ),
                            returnType = Types.string,
                            isSuspend = true,
                        ),
                        Method(
                            name = "fireAndForget",
                            parameters = listOf(),
                            returnType = Types.unit,
                            isSuspend = false,
                        )
                    )
                )
            ),
            values = setOf(
                AnnotatedValue(
                    type = Type(packageName = "com.mysdk", simpleName = "Foo"),
                    properties = emptyList(),
                )
            )
        )
        assertThat(ModelValidator.validate(api).isSuccess).isTrue()
    }

    @Test
    fun multipleServices_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(type = Type(packageName = "com.mysdk", simpleName = "MySdk")),
                AnnotatedInterface(type = Type(packageName = "com.mysdk", simpleName = "MySdk2")),
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Multiple services are not supported. Found: com.mysdk.MySdk, com.mysdk.MySdk2."
        )
    }

    @Test
    fun nonSuspendFunctionReturningValue_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    methods = listOf(
                        Method(
                            name = "returnSomethingNow",
                            parameters = listOf(),
                            returnType = Types.string,
                            isSuspend = false,
                        ),
                        Method(
                            name = "returnSomethingElseNow",
                            parameters = listOf(),
                            returnType = Types.int,
                            isSuspend = false,
                        ),
                        Method(
                            name = "returnNothingNow",
                            parameters = listOf(),
                            returnType = Types.unit,
                            isSuspend = false,
                        ),
                        Method(
                            name = "returnSomethingLater",
                            parameters = listOf(),
                            returnType = Types.string,
                            isSuspend = true,
                        ),
                    ),
                ),
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Error in com.mysdk.MySdk.returnSomethingNow: functions with return values " +
                "should be suspending functions.",
            "Error in com.mysdk.MySdk.returnSomethingElseNow: functions with return values " +
                "should be suspending functions."
        )
    }

    @Test
    fun invalidParameterOrReturnType_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    methods = listOf(
                        Method(
                            name = "returnFoo",
                            parameters = listOf(),
                            returnType = Type(packageName = "com.mysdk", simpleName = "Foo"),
                            isSuspend = true,
                        ),
                        Method(
                            name = "receiveFoo",
                            parameters = listOf(
                                Parameter(
                                    name = "foo",
                                    type = Type(packageName = "com.mysdk", simpleName = "Foo")
                                )
                            ),
                            returnType = Types.unit,
                            isSuspend = true,
                        ),
                    ),
                ),
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Error in com.mysdk.MySdk.returnFoo: only primitives and data classes annotated with " +
                "@PrivacySandboxValue are supported as parameter and return types.",
            "Error in com.mysdk.MySdk.receiveFoo: only primitives and data classes annotated " +
                "with @PrivacySandboxValue are supported as parameter and return types."
        )
    }
}