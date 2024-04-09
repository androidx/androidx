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
                                    name = "nullableY",
                                    type = Types.int.asNullable()
                                ),
                                Parameter(
                                    name = "foo",
                                    type = Type(packageName = "com.mysdk", simpleName = "Foo")
                                ),
                                Parameter(
                                    name = "callback",
                                    type = Type(
                                        packageName = "com.mysdk",
                                        simpleName = "MySdkCallback"
                                    )
                                ),
                                Parameter(
                                    name = "myInterface",
                                    type = Type(
                                        packageName = "com.mysdk",
                                        simpleName = "MyInterface"
                                    )
                                ),
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
                AnnotatedDataClass(
                    type = Type(packageName = "com.mysdk", simpleName = "Foo"),
                    properties = listOf(
                        ValueProperty(
                            name = "bar",
                            type = Type(packageName = "com.mysdk", simpleName = "Bar"),
                        )
                    ),
                ),
                AnnotatedDataClass(
                    type = Type(packageName = "com.mysdk", simpleName = "Bar"),
                    properties = emptyList(),
                )
            ),
            callbacks = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdkCallback"),
                    methods = listOf(
                        Method(
                            name = "onComplete",
                            parameters = listOf(
                                Parameter(
                                    name = "result",
                                    type = Types.int
                                ),
                            ),
                            returnType = Types.unit,
                            isSuspend = false,
                        ),
                    )
                )
            ),
            interfaces = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MyInterface"),
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
                                ),
                                Parameter(
                                    name = "callback",
                                    type = Type(
                                        packageName = "com.mysdk",
                                        simpleName = "MySdkCallback"
                                    )
                                ),
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
    fun serviceExtendsUiAdapter_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    superTypes = listOf(Types.sandboxedUiAdapter),
                ),
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Interfaces annotated with @PrivacySandboxService may not extend any other " +
                "interface. To define a SandboxedUiAdapter, use @PrivacySandboxInterface and " +
                "return it from this service."
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
            ),
            interfaces = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdkInterface"),
                    methods = listOf(
                        Method(
                            name = "returnSomethingInInterface",
                            parameters = listOf(),
                            returnType = Types.string,
                            isSuspend = false,
                        )
                    )
                )
            ),
            callbacks = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdkCallback"),
                    methods = listOf(
                        Method(
                            name = "returnSomethingInCallback",
                            parameters = listOf(),
                            returnType = Types.string,
                            isSuspend = false,
                        )
                    )
                )
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Error in com.mysdk.MySdk.returnSomethingNow: functions with return values " +
                "should be suspending functions.",
            "Error in com.mysdk.MySdk.returnSomethingElseNow: functions with return values " +
                "should be suspending functions.",
            "Error in com.mysdk.MySdkInterface.returnSomethingInInterface: " +
                "functions with return values should be suspending functions.",
            "Error in com.mysdk.MySdkCallback.returnSomethingInCallback: " +
                "functions with return values should be suspending functions."
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
            "Error in com.mysdk.MySdk.returnFoo: only primitives, lists, data/enum classes " +
                "annotated with @PrivacySandboxValue, interfaces annotated with " +
                "@PrivacySandboxInterface, and SdkActivityLaunchers are supported as return types.",
            "Error in com.mysdk.MySdk.receiveFoo: only primitives, lists, data/enum classes " +
                "annotated with @PrivacySandboxValue, interfaces annotated with " +
                "@PrivacySandboxCallback or @PrivacySandboxInterface, and SdkActivityLaunchers " +
                "are supported as parameter types."
        )
    }

    @Test
    fun nestedList_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    methods = listOf(
                        Method(
                            name = "processNestedList",
                            parameters = listOf(
                                Parameter(
                                    name = "foo",
                                    type = Types.list(Types.list(Types.int))
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
            "Invalid type parameter in list, found kotlin.collections.List."
        )
    }

    @Test
    fun listWithNullable_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    methods = listOf(
                        Method(
                            name = "processNestedList",
                            parameters = listOf(
                                Parameter(
                                    name = "foo",
                                    type = Types.list(Types.int.asNullable())
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
            "Nullable type parameters are not supported in lists, found kotlin.Int"
        )
    }

    @Test
    fun valueWithIllegalProperty_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(type = Type(packageName = "com.mysdk", simpleName = "MySdk")),
            ),
            values = setOf(
                AnnotatedDataClass(
                    type = Type(packageName = "com.mysdk", simpleName = "Foo"),
                    properties = listOf(
                        ValueProperty("bar", Type("com.mysdk", "Bar"))
                    )
                )
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Error in com.mysdk.Foo.bar: only primitives, lists, data/enum classes annotated " +
                "with @PrivacySandboxValue, interfaces annotated with @PrivacySandboxInterface, " +
                "and SdkActivityLaunchers are supported as properties."
        )
    }

    @Test
    fun callbackReceivingCallbacks_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(type = Type(packageName = "com.mysdk", simpleName = "MySdk")),
            ),
            callbacks = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdkCallback"),
                    methods = listOf(
                        Method(
                            name = "foo",
                            parameters = listOf(
                                Parameter("otherCallback", Type("com.mysdk", "MySdkCallback"))
                            ),
                            returnType = Types.unit,
                            isSuspend = false,
                        ),
                    )
                )
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Error in com.mysdk.MySdkCallback.foo: only primitives, lists, data/enum classes " +
                "annotated with @PrivacySandboxValue, interfaces annotated with " +
                "@PrivacySandboxInterface, and SdkActivityLaunchers are supported as callback " +
                "parameter types."
        )
    }

    @Test
    fun propertyWithKeywordName_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(type = Type(packageName = "com.mysdk", simpleName = "MySdk")),
            ),
            values = setOf(
                AnnotatedDataClass(
                    type = Type(packageName = "com.mysdk", simpleName = "Foo"),
                    properties = listOf(
                        ValueProperty("import", Types.int)
                    )
                )
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Error in com.mysdk.Foo.import: property name must not be a Java keyword."
        )
    }

    @Test
    fun enumConstantWithKeywordName_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(type = Type(packageName = "com.mysdk", simpleName = "MySdk")),
            ),
            values = setOf(
                AnnotatedEnumClass(
                    type = Type(packageName = "com.mysdk", simpleName = "Foo"),
                    variants = listOf(
                        "boolean"
                    )
                )
            )
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Error in com.mysdk.Foo.boolean: enum constant name must not be a Java keyword."
        )
    }

    @Test
    fun methodWithKeywordName_throws() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    methods = listOf(
                        Method(
                            name = "char",
                            parameters = listOf(),
                            returnType = Types.unit,
                            isSuspend = false,
                        )
                    )
                ),
            ),
        )
        val validationResult = ModelValidator.validate(api)
        assertThat(validationResult.isFailure).isTrue()
        assertThat(validationResult.errors).containsExactly(
            "Error in com.mysdk.MySdk.char: method name must not be a Java keyword."
        )
    }
}
