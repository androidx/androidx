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
                proxySourceFileNames =
                    listOf(
                        "androidx/appfunctions/internal/serializableproxies/AppFunctionLocalDateTime.KT",
                        "androidx/appfunctions/internal/serializableproxies/AppFunctionUri.KT",
                    ),
                symbolProcessorProviders = listOf(AppFunctionSerializableProcessor.Provider()),
            )
    }

    // TODO(b/392587953): break down test by parameter types (e.g. EntityWithPrimitive,
    //  EntityWithNullablePrimitive) when all types are supported.
    @Test
    fun testProcessor_validProperties_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "serializable/valid/EntityWithValidProperties.KT",
                        "serializable/valid/InputSerializable.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$EntityWithValidPropertiesFactory.kt",
            goldenFileName = "factory/\$EntityWithValidPropertiesFactory.KT",
        )
    }

    @Test
    fun testProcessor_validNullableProperties_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "serializable/valid/EntityWithValidNullableProperties.KT",
                        "serializable/valid/InputSerializable.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$EntityWithValidNullablePropertiesFactory.kt",
            goldenFileName = "factory/\$EntityWithValidNullablePropertiesFactory.KT",
        )
    }

    @Test
    fun testProcessor_validInheritedProperties_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/valid/DerivedSerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$DerivedSerializableFactory.kt",
            goldenFileName = "factory/\$DerivedSerializableFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$LongBaseSerializableFactory.kt",
            goldenFileName = "factory/\$LongBaseSerializableFactory.KT",
        )
    }

    @Test
    fun testProcessor_validNestedInheritedProperties_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/valid/NestedDerivedSerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$NestedDerivedSerializableFactory.kt",
            goldenFileName = "factory/\$NestedDerivedSerializableFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$NestedBaseSerializableFactory.kt",
            goldenFileName = "factory/\$NestedBaseSerializableFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$NonNestedChildSerializableFactory.kt",
            goldenFileName = "factory/\$NonNestedChildSerializableFactory.KT",
        )
    }

    @Test
    fun testProcessor_badlyInheritedSerializableProperties_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("serializable/valid/SubClassRenamedPropertySerializable.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "All parameters in @AppFunctionSerializable supertypes must be present in subtype",
        )
    }

    @Test
    fun testProcessor_badlyInheritedCapabilityProperties_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/valid/SubClassRenamedCapabilityProperty.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "All Properties in @AppFunctionSchemaCapability supertypes must be present in subtype",
        )
    }

    @Test
    fun testProcessor_differentPackageSerializableProperty_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf(
                        "serializable/valid/EntityWithDiffPackageSerializableProperty.KT",
                        "serializable/valid/DiffPackageSerializable.KT",
                    )
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$EntityWithDiffPackageSerializablePropertyFactory.kt",
            goldenFileName = "factory/\$EntityWithDiffPackageSerializablePropertyFactory.KT",
        )
    }

    @Test
    fun testProcessor_recursiveSerializable_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/valid/RecursiveSerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$RecursiveSerializableFactory.kt",
            goldenFileName = "factory/\$RecursiveSerializableFactory.KT",
        )
    }

    @Test
    fun testProcessor_nonPropertyParameter_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/invalid/EntityWithNonPropertyParameter.KT")
            )
        compilationTestHelper.assertErrorWithMessage(
            report,
            "All parameters in @AppFunctionSerializable primary constructor must have getters",
        )
    }

    @Test
    fun testProcessor_invalidPropertyType_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/invalid/EntityWithInvalidParameterType.KT")
            )
        compilationTestHelper.assertErrorWithMessage(
            report,
            "AppFunctionSerializable properties must be one of the following types:\n",
        )
    }

    @Test
    fun testProcessor_invalidPropertyListType_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("serializable/invalid/EntityWithInvalidListParameterType.KT")
            )
        compilationTestHelper.assertErrorWithMessage(
            report,
            "AppFunctionSerializable properties must be one of the following types:\n",
        )
    }

    @Test
    fun testProcessor_validAppFunctionSerializableFactory_succeeds() {
        val report = compilationTestHelper.compileAll(sourceFileNames = listOf())
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$LocalDateTimeFactory.kt",
            goldenFileName = "factory/\$LocalDateTimeFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$UriFactory.kt",
            goldenFileName = "factory/\$UriFactory.KT",
        )
    }

    @Test
    fun testProcessor_validSerializableWithProxyProperties_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/valid/SerializableWithProxyType.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$SerializableWithProxyTypeFactory.kt",
            goldenFileName = "factory/\$SerializableWithProxyTypeFactory.KT",
        )
    }

    @Test
    fun testProcessor_serializableProxyMissingToMethod_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/invalid/SerializableProxyMissingToMethod.KT")
            )
        compilationTestHelper.assertErrorWithMessage(
            report,
            "Class must have exactly one member function: toLocalDateTime",
        )
    }

    @Test
    fun testProcessor_serializableProxyMissingFromMethod_fails() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("serializable/invalid/SerializableProxyMissingFromMethod.KT")
            )
        compilationTestHelper.assertErrorWithMessage(
            report,
            "Companion Class must have exactly one member function: fromLocalDateTime",
        )
    }

    @Test
    fun testProcessor_genericFactory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FunctionWithGenericSerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}SetFieldFactory.kt",
            goldenFileName = "factory/${'$'}SetFieldFactory.KT",
        )
    }

    @Test
    fun testProcessor_genericSerializableFieldFactory_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FunctionWithGenericSerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}UpdateNoteParamsFactory.kt",
            goldenFileName = "factory/${'$'}UpdateNoteParamsFactory.KT",
        )
    }

    @Test
    fun testProcessor_serializableWithEmptyConstructor_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("functions/valid/FunctionWithEmptySerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}EmptyFactory.kt",
            goldenFileName = "factory/${'$'}EmptyFactory.KT",
        )
    }

    @Test
    fun testProcessor_multiLevelSerializable_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/valid/MultiLevelSerializable.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}MyNoteFactory.kt",
            goldenFileName = "factory/${'$'}MyNoteFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}ResponseFactory.kt",
            goldenFileName = "factory/${'$'}ResponseFactory.KT",
        )
    }

    @Test
    fun testProcessor_nestedClasses_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("serializable/valid/NestedSerializablesWithSimilarNames.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$SimpleNoteFactory.kt",
            goldenFileName = "factory/\$SimpleNoteFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$SimpleNote\$SimpleAttachmentFactory.kt",
            goldenFileName = "factory/\$SimpleNote\$SimpleAttachmentFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$CreateSimpleNoteParamsFactory.kt",
            goldenFileName = "factory/\$CreateSimpleNoteParamsFactory.KT",
        )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$SimpleMessageFactory.kt",
            goldenFileName = "factory/\$SimpleMessageFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$SimpleMessage\$SimpleAttachmentFactory.kt",
            goldenFileName = "factory/\$SimpleMessage\$SimpleAttachmentFactory.KT",
        )
        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$UpdateSimpleMessageParamsFactory.kt",
            goldenFileName = "factory/\$UpdateSimpleMessageParamsFactory.KT",
        )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "\$ContainsBothAttachmentsFactory.kt",
            goldenFileName = "factory/\$ContainsBothAttachmentsFactory.KT",
        )
    }

    @Test
    fun testProcessor_serializableWithDefaultValue_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/valid/SerializableWithDefaultValue.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report = report,
            expectGeneratedSourceFileName = "${'$'}SerializableWithDefaultValueFactory.kt",
            goldenFileName = "factory/${'$'}SerializableWithDefaultValueFactory.KT",
        )
    }

    @Test
    fun testProcessor_serializableWithOptionalNonNullSerializable_fail() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames =
                    listOf("serializable/invalid/SerializableWithOptionalNonNullSerializable.KT")
            )

        compilationTestHelper.assertErrorWithMessage(
            report,
            "Type com.testdata.NestedSerializable cannot be optional",
        )
    }

    @Test
    fun testProcessor_serializableWithParcelables_success() {
        val report =
            compilationTestHelper.compileAll(
                sourceFileNames = listOf("serializable/valid/SerializableWithParcelables.KT")
            )

        compilationTestHelper.assertSuccessWithSourceContent(
            report,
            "\$SerializableWithParcelablesFactory.kt",
            "factory/\$SerializableWithParcelablesFactory.KT",
        )
    }
}
