package com.testdata

import androidx.appfunctions.AppFunctionSerializable

/**
 * Serializable defined in a separate library module.
 *
 * @property libraryProperty Library property fallback.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class MultiModuleLibrarySerializable(
    val libraryProperty: String
)
