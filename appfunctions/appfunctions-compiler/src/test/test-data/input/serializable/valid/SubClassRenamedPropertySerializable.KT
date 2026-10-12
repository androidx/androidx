package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable(isDescribedByKDoc = false) open class BaseSerializable(open val name: String) {}

@AppFunctionSerializable(isDescribedByKDoc = false)
class SubClassRenamedPropertySerializable(val anotherString: String) :
    BaseSerializable(anotherString)
