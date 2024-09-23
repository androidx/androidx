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

package androidx.privacysandbox.tools.core.generator

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
import androidx.privacysandbox.tools.testing.loadFilesFromDirectory
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AidlValueGeneratorTest {
    @Test
    fun generate() {
        val innerEnum =
            AnnotatedEnumClass(
                Type(packageName = "com.mysdk", simpleName = "InnerEnum"),
                listOf("ONE, TWO, THREE")
            )
        val innerValue =
            AnnotatedDataClass(
                Type(packageName = "com.mysdk", simpleName = "InnerValue"),
                listOf(
                    ValueProperty("intProperty", Types.int),
                    ValueProperty("booleanProperty", Types.boolean),
                    ValueProperty("longProperty", Types.long),
                    ValueProperty("maybeFloatProperty", Types.float.asNullable()),
                    ValueProperty("maybeByteProperty", Types.byte.asNullable()),
                    ValueProperty("enumProperty", innerEnum.type),
                    ValueProperty("bundleProperty", Types.bundle),
                    ValueProperty("maybeBundleProperty", Types.bundle.asNullable())
                )
            )
        val outerValue =
            AnnotatedDataClass(
                Type(packageName = "com.mysdk", simpleName = "OuterValue"),
                listOf(
                    ValueProperty("innerValue", innerValue.type),
                    ValueProperty("innerValueList", Types.list(innerValue.type)),
                    ValueProperty("maybeInnerValue", innerValue.type.asNullable()),
                )
            )

        val api =
            ParsedApi(
                services =
                    setOf(
                        AnnotatedInterface(
                            type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                            methods =
                                listOf(
                                    Method(
                                        name = "suspendMethodThatReturnsValue",
                                        parameters = listOf(),
                                        returnType = outerValue.type,
                                        isSuspend = true,
                                    ),
                                    Method(
                                        name = "suspendMethodReceivingValue",
                                        parameters =
                                            listOf(Parameter("inputValue", outerValue.type)),
                                        returnType = Types.unit,
                                        isSuspend = true,
                                    ),
                                    Method(
                                        name = "suspendMethodWithListsOfValues",
                                        parameters =
                                            listOf(
                                                Parameter(
                                                    "inputValues",
                                                    Types.list(outerValue.type)
                                                )
                                            ),
                                        returnType = Types.list(outerValue.type),
                                        isSuspend = true,
                                    ),
                                    Method(
                                        name = "suspendMethodWithNullableValues",
                                        parameters =
                                            listOf(
                                                Parameter(
                                                    "maybeValue",
                                                    outerValue.type.asNullable()
                                                )
                                            ),
                                        returnType = outerValue.type.asNullable(),
                                        isSuspend = true,
                                    ),
                                    Method(
                                        name = "methodReceivingValue",
                                        parameters =
                                            listOf(
                                                Parameter(
                                                    name = "value",
                                                    type = outerValue.type,
                                                ),
                                            ),
                                        returnType = Types.unit,
                                        isSuspend = false,
                                    )
                                )
                        )
                    ),
                values = setOf(innerEnum, innerValue, outerValue)
            )

        val (aidlGeneratedSources, javaGeneratedSources) = AidlTestHelper.runGenerator(api)
        assertThat(javaGeneratedSources.map { it.packageName to it.interfaceName })
            .containsExactly(
                "com.mysdk" to "IMySdk",
                "com.mysdk" to "ParcelableOuterValue",
                "com.mysdk" to "ParcelableInnerValue",
                "com.mysdk" to "ParcelableInnerEnum",
                "com.mysdk" to "IUnitTransactionCallback",
                "com.mysdk" to "IOuterValueTransactionCallback",
                "com.mysdk" to "IListOuterValueTransactionCallback",
                "com.mysdk" to "ICancellationSignal",
                "com.mysdk" to "PrivacySandboxThrowableParcel",
                "com.mysdk" to "ParcelableStackFrame",
            )

        val outputTestDataDir = File("src/test/test-data/aidlvaluegeneratortest/output")
        val expectedSources = loadFilesFromDirectory(outputTestDataDir)

        assertThat(aidlGeneratedSources.map { it.relativePath to it.content })
            .containsExactlyElementsIn(expectedSources)
    }
}
