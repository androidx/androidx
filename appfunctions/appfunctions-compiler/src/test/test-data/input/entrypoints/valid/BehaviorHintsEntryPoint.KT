package com.testdata

import androidx.appfunctions.AppFunctionBehaviorHints
import androidx.appfunctions.AppFunctionDeclaration
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint

@AppFunctionServiceEntryPoint("MyBehaviorHintsService", "my_behavior_hints_service")
abstract class BehaviorHintsEntryPoint : AppFunctionService() {
    @AppFunctionDeclaration(isDescribedByKDoc = false)
    @AppFunctionBehaviorHints(
        isReadOnly = true,
        isDestructive = true,
        isIdempotent = false,
        isOpenWorld = true,
    )
    internal fun readOnlyFunction() {}

    @AppFunctionDeclaration(isDescribedByKDoc = false)
    @AppFunctionBehaviorHints(
        isReadOnly = false,
        isDestructive = true,
        isIdempotent = true,
        isOpenWorld = false,
    )
    internal fun destructiveIdempotentFunction() {}
}
