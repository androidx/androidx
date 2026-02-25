package com.testdata.diffPackageSchemas

import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionSchemaDefinition
import com.testdata.differentPackage.DiffPackageSerializable
import com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable

private const val SCHEMA_CATEGORY = "diff_package_schemas_category"

@AppFunctionSchemaDefinition(name = "diffPackageInputSchemas", version = 1, category = SCHEMA_CATEGORY)
interface DiffPackageInputSchemas {
    fun functionWithDiffPackageInput(
        appFunctionContext: AppFunctionContext,
        input: DiffPackageSerializable,
        inputList: List<AnotherDiffPackageSerializable>,
    )
}

@AppFunctionSchemaDefinition(name = "diffPackageOutputSchemas", version = 1, category = SCHEMA_CATEGORY)
interface DiffPackageOutputSchemas {
    fun functionWithDiffPackageOutput(
        appFunctionContext: AppFunctionContext,
    ): DiffPackageSerializable

    fun functionWithAnotherDiffPackageOutput(
        appFunctionContext: AppFunctionContext,
    ) : List<AnotherDiffPackageSerializable>
}
