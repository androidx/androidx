/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.appfunctions.compiler.AppFunctionCompiler
import androidx.appfunctions.compiler.testings.CompilationTestHelper
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Before
import org.junit.Test

class AppFunctionServiceEntryPointCompilerTest {
    private lateinit var compilationTestHelper: CompilationTestHelper

    @Before
    fun setup() {
        compilationTestHelper =
            CompilationTestHelper(
                testFileSrcDir = File("src/test/test-data/input"),
                goldenFileSrcDir = File("src/test/test-data/output"),
                stubSourceFileNames = listOf(),
                symbolProcessorProviders = listOf(AppFunctionCompiler.Provider()),
            )
    }

    @Test
    fun testAppFunctionEntryPoint_generatesServiceClass() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("entrypoints/valid/SimpleEntryPoint.KT"),
                processorOptions = emptyMap(),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "MySimpleService.kt",
            goldenFileName = "entrypoints/MySimpleService.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$SimpleEntryPoint_AppFunctionInventory.kt",
            goldenFileName = "inventory/\$SimpleEntryPoint_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "my_simple_service.xml",
            goldenFileName = "entrypoints/my_simple_service.xml",
        )
        val legacySchemaXmlFile =
            report.generatedResourceFiles.singleOrNull { resourceFile ->
                resourceFile.resource.relativePath.contains("my_simple_service-v1.xml")
            }
        assertThat(legacySchemaXmlFile).isNull()
    }

    @Test
    fun testAppFunctionEntryPoint_generatesV1Xml() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("entrypoints/valid/SimpleEntryPoint.KT"),
                processorOptions = mapOf("appfunctions:generateV1Xml" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "my_simple_service-v1.xml",
            goldenFileName = "entrypoints/my_simple_service-v1.xml",
        )
    }

    @Test
    fun testExtensionAppFunctionEntryPoint_generatesServiceClass() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("entrypoints/valid/ExtensionEntryPoint.KT"),
                processorOptions = emptyMap(),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "MyExtensionService.kt",
            goldenFileName = "entrypoints/MyExtensionService.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$ExtensionEntryPoint_AppFunctionInventory.kt",
            goldenFileName = "inventory/\$ExtensionEntryPoint_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "my_extension_service.xml",
            goldenFileName = "entrypoints/my_extension_service.xml",
        )
        val legacySchemaXmlFile =
            report.generatedResourceFiles.singleOrNull { resourceFile ->
                resourceFile.resource.relativePath.contains("my_extension_service-v1.xml")
            }
        assertThat(legacySchemaXmlFile).isNull()
    }

    @Test
    fun testAppFunctionEntryPoint_withRequiresApi_generatesServiceClassWithRequiresApi() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("entrypoints/valid/RequiresApiEntryPoint.KT"),
                processorOptions = emptyMap(),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "MyRequiresApiService.kt",
            goldenFileName = "entrypoints/MyRequiresApiService.KT",
        )
    }

    @Test
    fun testMissingSuperClass_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("entrypoints/invalid/MissingSuperClassEntryPoint.KT"),
                processorOptions = emptyMap(),
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "Class must extend either " +
                "androidx.appfunctions.AppFunctionService or " +
                "androidx.appfunctions.ExtensionsAppFunctionService",
        )
    }

    @Test
    fun testHiltIncorrectSuperClass_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "entrypoints/invalid/HiltNonAppFunctionServiceEntryPoint.KT",
                        "dagger/hilt/android/AndroidEntryPoint.JAVA",
                    ),
                processorOptions = emptyMap(),
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "Class must extend either " +
                "androidx.appfunctions.AppFunctionService or " +
                "androidx.appfunctions.ExtensionsAppFunctionService",
        )
    }

    @Test
    fun testNotAbstract_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("entrypoints/invalid/NotAbstractEntryPoint.KT"),
                processorOptions = emptyMap(),
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "The class being annotated with AppFunctionServiceEntryPoint should be an abstract class",
        )
    }

    @Test
    fun testImplementedOnExecuteFunction_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("entrypoints/invalid/ImplementedExecuteFunctionEntryPoint.KT"),
                processorOptions = emptyMap(),
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "The abstract class cannot implement onExecuteFunction. This would be generated by the compiler",
        )
    }

    @Test
    fun testEmptyAppFunctionEntryPoint_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("entrypoints/invalid/EmptyAppFunctionEntryPoint.KT"),
                processorOptions = emptyMap(),
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "Class must have at least one AppFunction",
        )
    }

    @Test
    fun testHiltAppFunctionServiceEntryPoint_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "entrypoints/valid/HiltAppFunctionServiceEntryPoint.KT",
                        "dagger/hilt/android/AndroidEntryPoint.JAVA",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "MyHiltAppFunctionServiceEntryPoint.kt",
            goldenFileName = "entrypoints/MyHiltAppFunctionServiceEntryPoint.KT",
        )
    }
}
