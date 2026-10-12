package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable(isDescribedByKDoc = false)
class NestedSerializable(val intValue: Int)

@AppFunctionSerializable(isDescribedByKDoc = false)
class SerializableWithOptionalNonNullSerializable(
    val nested: NestedSerializable = NestedSerializable(0)
)