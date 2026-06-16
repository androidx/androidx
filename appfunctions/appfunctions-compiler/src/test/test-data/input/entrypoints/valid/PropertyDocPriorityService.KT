package com.testdata

import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionSerializable

@AppFunctionServiceEntryPoint("MyPropertyDocPriorityService", "property_doc_priority.xml")
abstract class PropertyDocPriorityService: AppFunctionService() {
    @AppFunction(isDescribedByKDoc = true)
    fun propertyKdocParameters(
        all: AllKDocAvailable,
        onlyClass: OnlyClassLevelKDoc,
        param: OnlyParamKDoc,
        mixed: MixedSources
    ) {}
}

/**
 * AllKDocAvailable KDoc.
 *
 * @property property Property tag KDoc.
 * @param property Param tag KDoc.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AllKDocAvailable(
    /** Directly apply KDoc */
    val property: String
)

/**
 * OnlyClassLevelKDoc KDoc.
 *
 * @property property Property tag KDoc.
 * @param property Param tag KDoc.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class OnlyClassLevelKDoc(
    val property: String
)

/**
 * OnlyParamKDoc KDoc.
 *
 * @param property Param tag KDoc.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class OnlyParamKDoc(
    val property: String
)

/**
 * Test case for multiple properties with different documentation sources.
 *
 * @property prop1 Class property description.
 * @param prop2 Class param description.
 */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class MixedSources(
    /** Direct description. */
    val prop1: String,
    val prop2: String,
    val prop3: String
)