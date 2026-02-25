package com.testdata

import androidx.appfunctions.AppFunctionSchemaCapability
import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSchemaCapability
public interface AppFunctionOpenable {
    public val stringToOpen: String
}

@AppFunctionSerializable
class SubClassRenamedCapabilityProperty(val anotherString: String) : AppFunctionOpenable
