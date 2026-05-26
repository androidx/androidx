package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable
import com.testdata.diffPackageSchemas.DiffPackageInputSchemas
import com.testdata.differentPackage.DiffPackageSerializable

class AppFunctionWithInputFromDifferentPackage : DiffPackageInputSchemas {
    @AppFunction
    override fun functionWithDiffPackageInput(
        appFunctionContext: AppFunctionContext,
        input: DiffPackageSerializable,
        inputList: List<AnotherDiffPackageSerializable>,
    ) {}
}
