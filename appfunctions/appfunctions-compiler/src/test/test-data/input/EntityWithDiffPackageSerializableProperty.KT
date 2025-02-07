package com.testdata

import androidx.appfunctions.AppFunctionSerializable
import com.testdata.differentPackage.DiffPackageInputSerializable

@AppFunctionSerializable
class EntityWithDiffPackageSerializableProperty(
    val inputSerializable: DiffPackageInputSerializable,
    val serializableList: List<DiffPackageInputSerializable>
)
