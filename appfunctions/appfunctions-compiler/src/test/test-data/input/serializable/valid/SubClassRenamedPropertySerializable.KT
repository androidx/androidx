package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable open class BaseSerializable(open val name: String) {}

@AppFunctionSerializable
class SubClassRenamedPropertySerializable(val anotherString: String) :
    BaseSerializable(anotherString)
