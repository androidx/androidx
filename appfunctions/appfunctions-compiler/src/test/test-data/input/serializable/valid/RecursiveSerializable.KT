package com.testdata

import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
class RecursiveSerializable(val nested: RecursiveSerializable?)