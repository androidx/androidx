package com.testdata

import androidx.appfunctions.AppFunctionSerializable
import com.testdata.differentPackage.DiffPackageSerializable

@AppFunctionSerializable
class EntityWithDiffPackageSerializableProperty(
    val inputSerializable: DiffPackageSerializable,
    val serializableList: List<DiffPackageSerializable>
)
