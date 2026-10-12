package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable(isDescribedByKDoc = false)
class RecursiveSerializable(val nested: RecursiveSerializable?)