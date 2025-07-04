package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
open class NestedBaseSerializable(
    open val stringBaseName: String,
    open val nestedBase: NestedBaseSerializable?,
) {}

@AppFunctionSerializable
open class NonNestedChildSerializable(
    override val stringBaseName: String,
    override val nestedBase: NestedBaseSerializable?,
    open val longBaseValue: Long,
) : NestedBaseSerializable(stringBaseName, nestedBase) {}

@AppFunctionSerializable
class NestedDerivedSerializable(
    override val stringBaseName: String,
    override val nestedBase: NestedBaseSerializable?,
    override val longBaseValue: Long,
    val nestedDerived: NestedDerivedSerializable?,
) : NonNestedChildSerializable(stringBaseName, nestedBase, longBaseValue)
