package com.testdata

import androidx.appfunctions.AppFunctionSerializable
import com.testdata.differentPackage.DiffPackageSerializable

@AppFunctionSerializable(isDescribedByKDoc = false)
class EntityWithDiffPackageSerializableProperty(
    val inputSerializable: DiffPackageSerializable,
    val serializableList: List<DiffPackageSerializable>
)
