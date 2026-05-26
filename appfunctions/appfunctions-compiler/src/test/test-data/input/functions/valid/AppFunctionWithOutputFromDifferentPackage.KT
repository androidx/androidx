package com.testdata

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable
import com.testdata.diffPackageSchemas.DiffPackageOutputSchemas
import com.testdata.differentPackage.DiffPackageSerializable

class AppFunctionWithOutputFromDifferentPackage : DiffPackageOutputSchemas {
    @AppFunction
    override fun functionWithDiffPackageOutput(
        appFunctionContext: AppFunctionContext,
    ): DiffPackageSerializable {
        return DiffPackageSerializable(2L)
    }

    @AppFunction
    override fun functionWithAnotherDiffPackageOutput(
        appFunctionContext: AppFunctionContext,
    ): List<AnotherDiffPackageSerializable> {
        return listOf(AnotherDiffPackageSerializable("Test String"))
    }
}
