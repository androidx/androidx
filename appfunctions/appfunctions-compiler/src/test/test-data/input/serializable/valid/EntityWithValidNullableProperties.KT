package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
class EntityWithValidNullableProperties(
    val longParam: Long?,
    val doubleParam: Double?,
    val boolParam: Boolean?,
    val stringParam: String?,
    val longArray: LongArray?,
    val doubleArray: DoubleArray?,
    val boolArray: BooleanArray?,
    val stringList: List<String>?,
    val inputSerializable: InputSerializable?,
    val serializableList: List<InputSerializable>?
)
