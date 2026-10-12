package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionContext
import com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable
import com.testdata.diffPackageSchemas.DiffPackageOutputSchemas
import com.testdata.differentPackage.DiffPackageSerializable

class AppFunctionWithOutputFromDifferentPackage : DiffPackageOutputSchemas {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun functionWithDiffPackageOutput(
        appFunctionContext: AppFunctionContext,
    ): DiffPackageSerializable {
        return DiffPackageSerializable(2L)
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun functionWithAnotherDiffPackageOutput(
        appFunctionContext: AppFunctionContext,
    ): List<AnotherDiffPackageSerializable> {
        return listOf(AnotherDiffPackageSerializable("Test String"))
    }
}
