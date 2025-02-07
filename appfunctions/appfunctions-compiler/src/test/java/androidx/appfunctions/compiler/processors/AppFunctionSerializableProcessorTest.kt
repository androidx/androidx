/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.appfunctions.compiler.processors

import androidx.appfunctions.compiler.testings.CompilationTestHelper
import java.io.File
import org.junit.Before
import org.junit.Test

class AppFunctionSerializableProcessorTest {

    private lateinit var compilationTestHelper: CompilationTestHelper

    @Before
    fun setup() {
        compilationTestHelper =
            CompilationTestHelper(
                testFileSrcDir = File("src/test/test-data/input"),
                goldenFileSrcDir = File("src/test/test-data/output"), // unused
                symbolProcessorProviders = listOf(AppFunctionSerializableProcessor.Provider())
            )
    }

    // TODO(b/392587953): break down test by parameter types (e.g. EntityWithPrimitive,
    //  EntityWithNullablePrimitive) when all types are supported.
    // TODO(b/392587953): test recursive type properties
    @Test
    fun testProcessor_validProperties_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("EntityWithValidProperties.KT", "InputSerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$EntityWithValidPropertiesFactory.kt",
            goldenFileName = "\$EntityWithValidPropertiesFactory.KT"
        )
    }

    @Test
    fun testProcessor_differentPackageSerializableProperty_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "EntityWithDiffPackageSerializableProperty.KT",
                        "DiffPackageInputSerializable.KT"
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$EntityWithDiffPackageSerializablePropertyFactory.kt",
            goldenFileName = "\$EntityWithDiffPackageSerializablePropertyFactory.KT"
        )
    }

    @Test
    fun testProcessor_nonPropertyParameter_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("EntityWithNonPropertyParameter.KT")
            )
        compilationTestHelper.assertErrorWithMessage(
            report,
            "All parameters in @AppFunctionSerializable primary constructor must have getters"
        )
    }

    @Test
    fun testProcessor_invalidPropertyType_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("EntityWithInvalidParameterType.KT")
            )
        compilationTestHelper.assertErrorWithMessage(
            report,
            "AppFunctionSerializable properties must be one of the following types:\n"
        )
    }

    @Test
    fun testProcessor_invalidPropertyListType_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("EntityWithInvalidListParameterType.KT")
            )
        compilationTestHelper.assertErrorWithMessage(
            report,
            "AppFunctionSerializable properties must be one of the following types:\n"
        )
    }
}
