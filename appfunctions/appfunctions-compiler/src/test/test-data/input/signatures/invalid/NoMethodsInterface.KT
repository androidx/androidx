package com.test

import androidx.appfunctions.AppFunctionSignature
import androidx.appfunctions.metadata.AppFunctionMetadata

@AppFunctionSignature(
    scope = AppFunctionMetadata.SCOPE_GLOBAL,
    appFunctionXmlFileName = "app_level_app_functions",
)
interface NoMethodsInterface {}
