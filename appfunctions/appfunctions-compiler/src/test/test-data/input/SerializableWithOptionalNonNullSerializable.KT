package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
class NestedSerializable(val intValue: Int)

@AppFunctionSerializable
class SerializableWithOptionalNonNullSerializable(
    val nested: NestedSerializable = NestedSerializable(0)
)