package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.testdata.anotherDifferentPackage.AnotherDiffPackageSerializable
import com.testdata.diffPackageSchemas.DiffPackageInputSchemas
import com.testdata.differentPackage.DiffPackageSerializable

@AppFunctionServiceEntryPoint("AppFunctionWithInputFromDifferentPackageService", "app_functions_v2")
abstract class AppFunctionWithInputFromDifferentPackage :
    AppFunctionService(), DiffPackageInputSchemas {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    override fun functionWithDiffPackageInput(
        input: DiffPackageSerializable,
        inputList: List<AnotherDiffPackageSerializable>,
    ) {}
}
