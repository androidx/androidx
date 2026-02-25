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
                proxySourceFileNames =
                    listOf(
                        "androidx/appfunctions/internal/serializableproxies/AppFunctionLocalDateTime.KT",
                        "androidx/appfunctions/internal/serializableproxies/AppFunctionUri.KT",
                    ),
                symbolProcessorProviders = listOf(AppFunctionCompiler.Provider()),
            )
    }

    @Test
    fun testEmpty() {
        val report = compilationTestHelper.compileAll(sourceFileNames = emptyList())

        Truth.assertThat(report.isSuccess).isTrue()
    }

    @Test
    fun testNoAppFunctionDefined_generatesEmptyXmlFiles() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = emptyList(),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "xml/emptyXml_app_function.xml",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/emptyXml_app_function.xml",
        )
    }

    @Test
    fun testMissingFirstParameter_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/invalid/MissingFirstParameter.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "The first parameter of an app function must be " +
                "androidx.appfunctions.AppFunctionContext\n" +
                "    fun missingFirstParameter() {}\n" +
                "    ^",
        )
    }

    @Test
    fun testIncorrectFirstParameter_hasCompileError() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/invalid/IncorrectFirstParameter.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "The first parameter of an app function must be " +
                "androidx.appfunctions.AppFunctionContext\n" +
                "    fun incorrectFirstParameter(x: Int) {}\n" +
                "    ^",
        )
    }

    @Test
    fun testAllPrimitiveInputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/AllPrimitiveInputFunctions.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AllPrimitiveInputFunctions_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}AllPrimitiveInputFunctions_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testAllParcelablePrimitiveInputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/ParcelablePrimitiveFunctions.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}ParcelablePrimitiveFunctions_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}ParcelablePrimitiveFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/parcelablePrimitiveFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testInvalidBaseParcelable_throwsException() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/invalid/InvalidBaseParcelable.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            expectedErrorMessage =
                "Use an implementation of Parcelable, base Parcelable type is " +
                    "not allowed as a type in AppFunctions",
        )
    }

    @Test
    fun testFunctionsWithSerializableProxyInput_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FunctionWithSerializableProxyInput.KT",
                        "serializable/valid/SerializableWithProxyType.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}FunctionWithSerializableProxyInput_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}FunctionWithSerializableProxyInput_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName =
                "xml/functionWithSerializableProxyInput_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testAllPrimitiveInputFunctions_genAppFunctionInvokerImpl_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/AllPrimitiveInputFunctions.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AllPrimitiveInputFunctions_AppFunctionInvoker.kt",
            goldenFileName = "invoker/${'$'}AllPrimitiveInputFunctions_AppFunctionInvoker.KT",
        )
    }

    @Test
    fun testSerializableInputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/SerializableInputFunctions.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}SerializableInputFunctions_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}SerializableInputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/serializableInputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testDerivedSerializableInputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/DerivedSerializableInputFunctions.KT",
                        "serializable/valid/DerivedSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}DerivedSerializableInputFunctions_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}DerivedSerializableInputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/derivedSerializableInputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testNestedDerivedSerializableInputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/NestedDerivedSerializableInputFunctions.KT",
                        "serializable/valid/NestedDerivedSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}NestedDerivedSerializableInputFunctions_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}NestedDerivedSerializableInputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName =
                "xml/nestedDerivedSerializableInputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testDerivedSerializableOutputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/DerivedSerializableOutputFunctions.KT",
                        "serializable/valid/DerivedSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}DerivedSerializableOutputFunctions_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}DerivedSerializableOutputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName =
                "xml/derivedSerializableOutputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testNestedDerivedSerializableOutputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/NestedDerivedSerializableOutputFunctions.KT",
                        "serializable/valid/NestedDerivedSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}NestedDerivedSerializableOutputFunctions_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}NestedDerivedSerializableOutputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName =
                "xml/nestedDerivedSerializableOutputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testDiffPackageSerializableInputFunction_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/AppFunctionWithInputFromDifferentPackage.KT",
                        "serializable/valid/DiffPackageSerializable.KT",
                        "schema/DiffPackageSchemas.KT",
                        "serializable/valid/AnotherDiffPackageSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AppFunctionWithInputFromDifferentPackage_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}AppFunctionWithInputFromDifferentPackage_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName =
                "xml/appFunctionWithInputFromDifferentPackage_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeAllPrimitiveParamsImpl_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/valid/FakeAllPrimitiveParamsImpl.KT", "schema/FakeSchemas.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}FakeAllPrimitiveParamsImpl_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}FakeAllPrimitiveParamsImpl_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}FakeAllPrimitiveArrayParamsImpl_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}FakeAllPrimitiveArrayParamsImpl_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testFakeAllPrimitiveReturnsImpl_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FakeAllPrimitiveReturnsImpl.KT",
                        "schema/FakeSchemas.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}UnitReturnImpl_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}UnitReturnImpl_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testIntValueConstraint_Xml_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/IntEnumValueFunctions.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/intEnumValueFunctions_app_functions_v2.xml",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}AggregatedAppFunctionInventory_Impl.kt",
            goldenFileName = "inventory/${'$'}AggregatedAppFunctionInventory_IntEnum_Impl.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}IntEnumValueFunctions_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}IntEnumValueFunctions_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testStringValueConstraint_Xml_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/StringEnumValueFunctions.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/stringEnumValueFunctions_app_functions_v2.xml",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}StringEnumValueFunctions_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}StringEnumValueFunctions_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testSerializableOutputFunctions_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/SerializableOutputFunctions.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}SerializableOutputFunctions_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}SerializableOutputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/serializableOutputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testDiffPackageSerializableOutputFunction_genAppFunctionInventoryAndIndexXml_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/AppFunctionWithOutputFromDifferentPackage.KT",
                        "serializable/valid/DiffPackageSerializable.KT",
                        "schema/DiffPackageSchemas.KT",
                        "serializable/valid/AnotherDiffPackageSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}AppFunctionWithOutputFromDifferentPackage_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}AppFunctionWithOutputFromDifferentPackage_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName =
                "xml/appFunctionWithOutputFromDifferentPackage_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testRecursiveSerializableInputFunctions_genAppFunctionInventoryImplAndIndexXml_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/RecursiveSerializableInputFunctions.KT",
                        "serializable/valid/RecursiveSerializable.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}RecursiveSerializableInputFunctions_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}RecursiveSerializableInputFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName =
                "xml/recursiveSerializableInputFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testBadInputFunctions_genAppFunctionInventoryImpl_hasCompileError() {
        val reportListPrimitiveArrayInputFunction =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/invalid/ListPrimitiveArrayInputFunction.KT")
            )
        val reportArrayNonPrimitiveInputFunction =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/invalid/ArrayNonPrimitiveInputFunction.KT")
            )
        val reportAnyTypedInputFunction =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/invalid/AnyTypedInputFunction.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            reportListPrimitiveArrayInputFunction,
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types",
        )
        compilationTestHelper.assertErrorWithMessage(
            reportArrayNonPrimitiveInputFunction,
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types",
        )
        compilationTestHelper.assertErrorWithMessage(
            reportAnyTypedInputFunction,
            "App function parameters must be a supported type, or a type annotated as" +
                " @AppFunctionSerializable. See list of supported types",
        )
    }

    @Test
    fun testFunctionWithInvalidSerializableInterface_fail() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/invalid/FunctionWithInvalidSerializableInterface.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report = report,
            expectedErrorMessage =
                "AppFunctionSerializable properties must be one of the following types:\n",
        )
    }

    @Test
    fun testFunctionWithInvalidGenericSerializable_fail() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/invalid/FunctionWithInvalidGenericSerializable.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report = report,
            expectedErrorMessage =
                "AppFunctionSerializable properties must be one of the following types:\n",
        )
    }

    @Test
    fun testFunctionWithInvalidGenericSerializableInterface_fail() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/invalid/FunctionWithInvalidGenericSerializableInterface.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report = report,
            expectedErrorMessage =
                "AppFunctionSerializable properties must be one of the following types:\n",
        )
    }

    @Test
    fun testFunctionWithGenericSerializable_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FunctionWithGenericSerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}FunctionWithGenericSerializable_AppFunctionInventory.kt",
            goldenFileName =
                "inventory/${'$'}FunctionWithGenericSerializable_AppFunctionInventory.KT",
        )
    }

    @Test
    fun testFunctionWithGenericSerializable_genDynamicIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FunctionWithGenericSerializable.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/functionWithGenericSerializable_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeNoArgImpl_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/valid/FakeNoArgImpl.KT", "schema/FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "xml/fakeNoArgImpl_app_function.xml",
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledTrue_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FakeNoArgImpl_IsEnabled_True.KT",
                        "schema/FakeSchemas.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "xml/fakeNoArgImpl_isEnabled_true_app_function.xml",
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledFalse_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FakeNoArgImpl_IsEnabled_False.KT",
                        "schema/FakeSchemas.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "xml/fakeNoArgImpl_isEnabled_false_app_function.xml",
        )
    }

    @Test
    fun testFakeNoArg_freeForm_genLegacyIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FakeNoArg_FreeForm_Function.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions.xml",
            goldenFileName = "xml/fakeNoArg_freeForm_function_app_function.xml",
        )
    }

    @Test
    fun testFakeNoArgImpl_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/valid/FakeNoArgImpl.KT", "schema/FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/fakeNoArgImpl_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledTrue_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FakeNoArgImpl_IsEnabled_True.KT",
                        "schema/FakeSchemas.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/fakeNoArgImpl_isEnabled_true_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeNoArgImp_isEnabledFalse_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FakeNoArgImpl_IsEnabled_False.KT",
                        "schema/FakeSchemas.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/fakeNoArgImpl_isEnabled_false_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeNoArg_freeForm_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FakeNoArg_FreeForm_Function.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/fakeNoArg_freeForm_function_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeFunction_freeForm_detailedKdocAsDescription_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/valid/FakeFreeFormFunctionsWithDetailedKdocs.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/fake_freeForm_with_detailed_kdoc_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeFunction_freeForm_paramKdocAsDescription_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FakeFreeFormFunctionsWithParamKdocs.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/fake_freeForm_with_param_kdocs_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeAllPrimitiveParams_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FakeAllPrimitiveParamsImpl.KT",
                        "schema/FakeSchemas.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/fakeAllPrimitiveParams_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeAllPrimitiveReturns_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FakeAllPrimitiveReturnsImpl.KT",
                        "schema/FakeSchemas.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/fakeAllPrimitiveReturns_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testFakeAllNullablePrimitiveParamsWithDefault_genIndexXmlFile_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/FakeAllNullablePrimitiveParamsWithDefaultValuesImpl.KT",
                        "schema/FakeSchemas.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName =
                "xml/fakeAllNullablePrimitiveParamsWithDefault_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testGenerateInventoryComponentRegistry() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/AllPrimitiveInputFunctions.KT",
                        "functions/valid/DiffPackageFunction.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}Main_InventoryComponentRegistry.kt",
            goldenFileName = "registry/${'$'}Main_InventoryComponentRegistry.KT",
        )
    }

    @Test
    fun testGenerateInvokerComponentRegistry() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/AllPrimitiveInputFunctions.KT",
                        "functions/valid/DiffPackageFunction.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}Main_InvokerComponentRegistry.kt",
            goldenFileName = "registry/${'$'}Main_InvokerComponentRegistry.KT",
        )
    }

    @Test
    fun testGenerateFunctionComponentRegistry() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/AllPrimitiveInputFunctions.KT",
                        "functions/valid/DiffPackageFunction.KT",
                        "functions/valid/FakeFreeFormFunctionsWithDetailedKdocs.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}Main_FunctionComponentRegistry.kt",
            goldenFileName = "registry/${'$'}Main_FunctionComponentRegistry.KT",
        )
    }

    @Test
    fun testGenerateSerializableComponentRegistry() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/valid/FakeFreeFormFunctionsWithDetailedKdocs.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}Main_SerializableComponentRegistry.kt",
            goldenFileName = "registry/${'$'}Main_SerializableComponentRegistry.KT",
        )
    }

    @Test
    fun testGenerateAggregateInventoryImpl() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/AllPrimitiveInputFunctions.KT",
                        "functions/valid/DiffPackageFunction.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}AggregatedAppFunctionInventory_Impl.kt",
            goldenFileName = "inventory/${'$'}AggregatedAppFunctionInventory_Impl.KT",
        )
    }

    @Test
    fun testGenerateAggregateInvokerImpl() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "functions/valid/AllPrimitiveInputFunctions.KT",
                        "functions/valid/DiffPackageFunction.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}AggregatedAppFunctionInvoker_Impl.kt",
            goldenFileName = "invoker/${'$'}AggregatedAppFunctionInvoker_Impl.KT",
        )
    }

    @Test
    fun testGenerateSchemaDefinitionRegistry() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "schema/FakeSchemas.KT",
                        "serializable/valid/DiffPackageSerializable.KT",
                        "schema/DiffPackageSchemas.KT",
                        "serializable/valid/AnotherDiffPackageSerializable.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}Main_SchemaDefinitionComponentRegistry.kt",
            goldenFileName = "registry/${'$'}Main_SchemaDefinitionComponentRegistry.KT",
        )
    }

    @Test
    fun testGenerateSchemaInventory() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("schema/FakeSchemas.KT"),
                processorOptions = mapOf("appfunctions:generateMetadataFromSchema" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}SchemaAppFunctionInventory_Impl.kt",
            goldenFileName = "inventory/${'$'}SchemaAppFunctionInventory_Impl.KT",
        )
    }

    @Test
    fun testSimpleFunctionWithEmptySerializable_genAppFunctionInventory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FunctionWithEmptySerializable.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName =
                "${'$'}FunctionWithEmptySerializable_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}FunctionWithEmptySerializable_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/functionWithEmptySerializable_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testAppFunctionWithOptionalNonNullSerializable_fail() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("functions/invalid/AppFunctionWithOptionalNonNullSerializable.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "Type com.testdata.SerializableData cannot be optional",
        )
    }

    // One Of Serializable Tests

    @Test
    fun oneOfSerializable_oneOfSealedInterface_generatesFactory() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("oneofserializable/OneOfSealedInterface.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report,
            "OneOfSealedInterfaceFactory.kt",
            "oneofserializable/\$OneOfSealedInterfaceFactory.KT",
        )
    }

    @Test
    fun oneOfSerializable_oneOfSealedClass_generatesFactory() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("oneofserializable/OneOfSealedClass.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report,
            "OneOfSealedClassFactory.kt",
            "oneofserializable/\$OneOfSealedClassFactory.KT",
        )
    }

    @Test
    fun oneOfSerializable_nonSerializableSubclasses_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("oneofserializable/NonSerializableSubclasses.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "All subclasses of OneOfSealedInterface should be annotated with @AppFunctionSerializable. Did you forget to annotate OneOfSealedInterface\$ASubclass?",
        )
    }

    @Test
    fun oneOfSerializable_nestedOneOfSerializableWithinSerializable_generatesFactory() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "oneofserializable/OneOfSealedClass.KT",
                        "oneofserializable/OneOfSealedInterface.KT",
                        "oneofserializable/NestedOneOfSerializableWithinSerializable.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report,
            "NestedOneOfSerializableWithinSerializableFactory.kt",
            "oneofserializable/\$NestedOneOfSerializableWithinSerializableFactory.KT",
        )
    }

    @Test
    fun testOneOfSerializableFunctions_genAppFunctionInventoryXml_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "oneofserializable/OneOfSealedClass.KT",
                        "oneofserializable/OneOfSealedInterface.KT",
                        "oneofserializable/OneOfFunctions.KT",
                    ),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}OneOfFunctions_AppFunctionInventory.kt",
            goldenFileName = "oneofserializable/${'$'}OneOfFunctions_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "oneofserializable/oneOfFunctions_app_function_dynamic_schema.xml",
        )
    }

    @Test
    fun testDeprecatedFunction_generatedClass_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/DeprecatedFunction.KT"),
                processorOptions = mapOf("appfunctions:aggregateAppFunctions" to "true"),
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}DeprecatedFunction_AppFunctionInvoker.kt",
            goldenFileName = "invoker/${'$'}DeprecatedFunction_AppFunctionInvoker.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}DeprecatedFunction_AppFunctionInventory.kt",
            goldenFileName = "inventory/${'$'}DeprecatedFunction_AppFunctionInventory.KT",
        )
        compilationTestHelper.assertSuccessWithResourceContent(
            report = report,
            expectGeneratedResourceFileName = "app_functions_v2.xml",
            goldenFileName = "xml/deprecated_app_function_dynamic_schema.xml",
        )
    }
}
