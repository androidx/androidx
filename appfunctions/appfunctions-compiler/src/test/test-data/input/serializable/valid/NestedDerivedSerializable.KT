package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable(isDescribedByKDoc = false)
open class NestedBaseSerializable(
    open val stringBaseName: String,
    open val nestedBase: NestedBaseSerializable?,
) {}

@AppFunctionSerializable(isDescribedByKDoc = false)
open class NonNestedChildSerializable(
    override val stringBaseName: String,
    override val nestedBase: NestedBaseSerializable?,
    open val longBaseValue: Long,
) : NestedBaseSerializable(stringBaseName, nestedBase) {}

@AppFunctionSerializable(isDescribedByKDoc = false)
class NestedDerivedSerializable(
    override val stringBaseName: String,
    override val nestedBase: NestedBaseSerializable?,
    override val longBaseValue: Long,
    val nestedDerived: NestedDerivedSerializable?,
) : NonNestedChildSerializable(stringBaseName, nestedBase, longBaseValue)
