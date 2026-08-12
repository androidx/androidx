package com.testdata

import android.net.Uri
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionSerializable
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunctionStringValueConstraint
import androidx.appfunctions.AppFunctionUriValueConstraint

@AppFunctionServiceEntryPoint("ValidUriService", "valid_uri_service")
abstract class ValidUriConstraintService : AppFunctionService() {
    @AppFunction
    fun functionWithContentUri(
        @AppFunctionUriValueConstraint(allowedSchemes = ["content"]) fileUri: Uri
    ) {}

    @AppFunction
    fun functionWithMultiSchemeUri(
        @AppFunctionUriValueConstraint(allowedSchemes = ["content", "file"]) multiUri: Uri
    ) {}

    @AppFunction
    fun functionWithUnconstrainedUri(
        plainUri: Uri
    ) {}

    @AppFunction
    fun functionWithStringConstraintNoEnumValues(
        @AppFunctionStringValueConstraint(pattern = "^[a-z]+$", format = "custom") stringParam: String
    ) {}

    @AppFunction
    fun functionWithSerializableValueConstraints(
        param: SerializableWithValueConstraints
    ) {}

    @AppFunction
    @AppFunctionUriValueConstraint(allowedSchemes = ["content"])
    fun functionWithUriReturn(): Uri {
        return Uri.EMPTY
    }
}

@AppFunctionSerializable
data class SerializableWithValueConstraints(
    @AppFunctionUriValueConstraint(allowedSchemes = ["content"])
    val contentUri: Uri,
    @AppFunctionStringValueConstraint(pattern = "^[0-9]+$", format = "numeric")
    val numericString: String,
)
