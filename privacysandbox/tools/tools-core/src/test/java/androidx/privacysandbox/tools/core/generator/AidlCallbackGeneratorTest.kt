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

import androidx.privacysandbox.tools.core.model.AnnotatedInterface
import androidx.privacysandbox.tools.core.model.Method
import androidx.privacysandbox.tools.core.model.Parameter
import androidx.privacysandbox.tools.core.model.ParsedApi
import androidx.privacysandbox.tools.core.model.Type
import androidx.privacysandbox.tools.core.model.Types
import androidx.privacysandbox.tools.testing.loadFilesFromDirectory
import com.google.common.truth.Truth
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AidlCallbackGeneratorTest {

    @Test
    fun generate() {
        val api = ParsedApi(
            services = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MySdk"),
                    methods = listOf(
                        Method(
                            name = "doStuff",
                            parameters = listOf(
                                Parameter(
                                    "callback", Type("com.mysdk", "MyCallback")
                                )
                            ),
                            returnType = Types.unit,
                            isSuspend = false,
                        ),
                    )
                )
            ), callbacks = setOf(
                AnnotatedInterface(
                    type = Type(packageName = "com.mysdk", simpleName = "MyCallback"),
                    methods = listOf(
                        Method(
                            name = "onComplete",
                            parameters = listOf(
                                Parameter("result", Types.boolean),
                            ),
                            returnType = Types.unit,
                            isSuspend = false,
                        )
                    )
                )
            )
        )

        val (aidlGeneratedSources, javaGeneratedSources) = AidlTestHelper.runGenerator(api)
        Truth.assertThat(javaGeneratedSources.map { it.packageName to it.interfaceName })
            .containsExactly(
                "com.mysdk" to "IMySdk",
                "com.mysdk" to "IMyCallback",
            )

        val outputTestDataDir = File("src/test/test-data/aidlcallbackgeneratortest/output")
        val expectedSources = loadFilesFromDirectory(outputTestDataDir)

        Truth.assertThat(aidlGeneratedSources.map { it.relativePath to it.content })
            .containsExactlyElementsIn(expectedSources)
    }
}
