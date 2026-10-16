package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable
import com.testdata.diffPackageSchemas.DiffPackageOutputSchemas
import com.testdata.differentPackage.DiffPackageSerializable

@AppFunctionServiceEntryPoint("AppFunctionWithOutputFromDifferentPackageService", "app_functions_v2")
abstract class AppFunctionWithOutputFromDifferentPackage :
    AppFunctionService(), DiffPackageOutputSchemas {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun functionWithDiffPackageOutput(): DiffPackageSerializable {
        return DiffPackageSerializable(2L)
    }

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun functionWithAnotherDiffPackageOutput(): List<AnotherDiffPackageSerializable> {
        return listOf(AnotherDiffPackageSerializable("Test String"))
    }
}
