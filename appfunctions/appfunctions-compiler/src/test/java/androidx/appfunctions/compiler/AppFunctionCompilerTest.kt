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

package androidx.appfunctions.compiler

import androidx.appfunctions.compiler.testings.CompilationTestHelper
import com.google.common.truth.Truth
import java.io.File
import org.junit.Before
import org.junit.Test

class AppFunctionCompilerTest {
    private lateinit var compilationTestHelper: CompilationTestHelper

    @Before
    fun setup() {
        compilationTestHelper =
            CompilationTestHelper(
                testFileSrcDir = File("src/test/test-data/input"),
                goldenFileSrcDir = File("src/test/test-data/output"),
                symbolProcessorProviders = listOf(AppFunctionCompiler.Provider())
            )
    }

    @Test
    fun testEmpty() {
        val report = compilationTestHelper.compileAll(sourceFileNames = emptyList())

        Truth.assertThat(report.isSuccess).isTrue()
    }

    @Test
    fun testSimpleFunction_genAppFunctionIds_success() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "SimpleFunctionIds.kt",
            goldenFileName = "SimpleFunctionIds.KT"
        )
    }

    @Test
    fun testMissingFirstParameter_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(sourceFileNames = listOf("MissingFirstParameter.KT"))

        compilationTestHelper.assertErrorWithMessage(
            report,
            "The first parameter of an app function must be " +
                "androidx.appfunctions.AppFunctionContext\n" +
                "    fun missingFirstParameter() {}\n" +
                "    ^"
        )
    }

    @Test
    fun testIncorrectFirstParameter_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(sourceFileNames = listOf("IncorrectFirstParameter.KT"))

        compilationTestHelper.assertErrorWithMessage(
            report,
            "The first parameter of an app function must be " +
                "androidx.appfunctions.AppFunctionContext\n" +
                "    fun incorrectFirstParameter(x: Int) {}\n" +
                "    ^"
        )
    }

    @Test
    fun testSimpleFunction_genAppFunctionInventoryImpl_success() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}SimpleFunction_AppFunctionInventory.kt",
            goldenFileName = "${'$'}SimpleFunction_AppFunctionInventory.KT"
        )
    }

    @Test
    fun testAllPrimitiveInputFunctions_genAppFunctionInventoryImpl_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("AllPrimitiveInputFunctions.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AllPrimitiveInputFunctions_AppFunctionInventory.kt",
            goldenFileName = "${'$'}AllPrimitiveInputFunctions_AppFunctionInventory.KT"
        )
    }

    @Test
    fun testSimpleFunction_genAppFunctionInvokerImpl_success() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf("SimpleFunction.KT"))

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}SimpleFunction_AppFunctionInvoker.kt",
            goldenFileName = "${'$'}SimpleFunction_AppFunctionInvoker.KT",
        )
    }

    @Test
    fun testAllPrimitiveInputFunctions_genAppFunctionInvokerImpl_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("AllPrimitiveInputFunctions.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AllPrimitiveInputFunctions_AppFunctionInvoker.kt",
            goldenFileName = "${'$'}AllPrimitiveInputFunctions_AppFunctionInvoker.KT",
        )
    }

    @Test
    fun testSerializableInputFunctions_genAppFunctionInventoryImpl_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("SerializableInputFunctions.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}SerializableInputFunctions_AppFunctionInventory.kt",
            goldenFileName = "${'$'}SerializableInputFunctions_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testFakeAllPrimitiveParamsImpl_genAppFunctionInventoryImpl_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeAllPrimitiveParamsImpl.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}FakeAllPrimitiveParamsImpl_AppFunctionInventory.kt",
            goldenFileName = "${'$'}FakeAllPrimitiveParamsImpl_AppFunctionInventory.KT",
        )
    }

    // TODO(b/392587953): disabling test temporarily as AppFunctionSerializable types are not
    // supported yet in AppFunctionSerializableFactory.
    //    @Test
    //    fun testRecursiveSerializableInputFunctions_genAppFunctionInventoryImpl_success() {
    //        val report =
    //            compilationTestHelper.compileAll(
    //                sourceFileNames = listOf("RecursiveSerializableInputFunctions.KT")
    //            )
    //
    //        compilationTestHelper.assertSuccessWithSourceContent(
    //            report = report,
    //            expectGeneratedSourceFileName =
    //                "${'$'}RecursiveSerializableInputFunctions_AppFunctionInventory.kt",
    //            goldenFileName =
    //                "${'$'}RecursiveSerializableInputFunctions_AppFunctionInventory.KT",
    //        )
    //    }

    @Test
    fun testBadInputFunctions_genAppFunctionInventoryImpl_hasCompileError() {
        val reportListPrimitiveArrayInputFunction =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("ListPrimitiveArrayInputFunction.KT")
            )
        val reportArrayNonPrimitiveInputFunction =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("ArrayNonPrimitiveInputFunction.KT")
            )
        val reportAnyTypedInputFunction =
            compilationTestHelper.compileAll(sourceFileNames = listOf("AnyTypedInputFunction.KT"))

        compilationTestHelper.assertErrorWithMessage(
            reportListPrimitiveArrayInputFunction,
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types"
        )
        compilationTestHelper.assertErrorWithMessage(
            reportArrayNonPrimitiveInputFunction,
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types"
        )
        compilationTestHelper.assertErrorWithMessage(
            reportAnyTypedInputFunction,
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types"
        )
    }

    @Test
    fun testFakeNoArgImpl_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_app_function.xml"
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledTrue_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_True.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_true_app_function.xml"
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledFalse_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_False.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_false_app_function.xml"
        )
    }

    @Test
    fun testFakeNoArg_freeForm_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArg_FreeForm_Function.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "fakeNoArg_freeForm_function_app_function.xml"
        )
    }

    @Test
    fun testFakeNoArgImpl_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeNoArgImpl_app_function_dynamic_schema.xml"
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledTrue_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_True.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_true_app_function_dynamic_schema.xml"
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledFalse_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArgImpl_IsEnabled_False.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeNoArgImpl_isEnabled_false_app_function_dynamic_schema.xml"
        )
    }

    @Test
    fun testFakeNoArg_freeForm_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeNoArg_FreeForm_Function.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeNoArg_freeForm_function_app_function_dynamic_schema.xml"
        )
    }

    @Test
    fun testFakeAllPrimitiveParams_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeAllPrimitiveParamsImpl.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeAllPrimitiveParams_app_function_dynamic_schema.xml"
        )
    }

    @Test
    fun testFakeAllPrimitiveReturns_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("FakeAllPrimitiveReturnsImpl.KT", "FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName = "fakeAllPrimitiveReturns_app_function_dynamic_schema.xml"
        )
    }

    @Test
    fun testFakeAllNullablePrimitiveParamsWithDefault_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "FakeAllNullablePrimitiveParamsWithDefaultValuesImpl.KT",
                        "FakeSchemas.KT"
                    )
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_dynamic_schema.xml",
            goldenFileName =
                "fakeAllNullablePrimitiveParamsWithDefault_app_function_dynamic_schema.xml"
        )
    }
}
