package com.testdata

import androidx.appfunctions.AppFunctionSchemaCapability
import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSchemaCapability
public interface AppFunctionOpenable {
    public val stringToOpen: String
}

public interface OpenableResponse : AppFunctionOpenable {
    override val stringToOpen: String
}

@AppFunctionSerializable(isDescribedByKDoc = false)
open class LongBaseSerializable(
    open val age: Long,
    override val stringToOpen: String,
) : OpenableResponse {}

@AppFunctionSerializable(isDescribedByKDoc = false)
class DerivedSerializable(
    override val stringToOpen: String,
    override val age: Long,
) : LongBaseSerializable(age, stringToOpen)
