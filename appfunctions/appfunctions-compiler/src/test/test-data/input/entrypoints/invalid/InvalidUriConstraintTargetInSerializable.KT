package com.testdata

import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionUriValueConstraint

@AppFunctionServiceEntryPoint("InvalidUriSerializableService", "invalid_uri_serializable_service")
abstract class InvalidUriConstraintTargetInSerializable : AppFunctionService() {
    @AppFunctionDeclaration
    fun functionWithInvalidSerializable(
        param: InvalidSerializableUriTarget
    ) {}
}

@AppFunctionSerializable
data class InvalidSerializableUriTarget(
    @AppFunctionUriValueConstraint
    val invalidProp: String
)
