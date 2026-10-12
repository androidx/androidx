package com.testdata.differentPackage

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable(isDescribedByKDoc = false)
class DiffPackageSerializable (val longParam: Long)