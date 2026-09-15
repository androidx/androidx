package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable
import com.testdata.diffPackageSchemas.DiffPackageInputSchemas
import com.testdata.differentPackage.DiffPackageSerializable

class AppFunctionWithInputFromDifferentPackage : DiffPackageInputSchemas {
    @AppFunctionDeclaration
    override fun functionWithDiffPackageInput(
        appFunctionContext: AppFunctionContext,
        input: DiffPackageSerializable,
        inputList: List<AnotherDiffPackageSerializable>,
    ) {}
}
