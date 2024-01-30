/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.health.connect.client.records

import androidx.annotation.IntDef
import androidx.annotation.RestrictTo
import androidx.health.connect.client.records.BloodGlucoseRecord.SpecimenSource
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.BloodGlucose
import java.time.Instant
import java.time.ZoneOffset

/**
 * Captures the concentration of glucose in the blood. Each record represents a single instantaneous
 * blood glucose reading.
 */
public class BloodGlucoseRecord(
    override val time: Instant,
    override val zoneOffset: ZoneOffset?,
    /**
     * Blood glucose level or concentration. Required field. Valid range: 0-50 mmol/L.
     *
     * @see BloodGlucose
     */
    public val level: BloodGlucose,
    /**
     * Type of body fluid used to measure the blood glucose. Optional, enum field. Allowed values:
     * [SpecimenSource].
     *
     * @see SpecimenSource
     */
    @property:SpecimenSources public val specimenSource: Int = SPECIMEN_SOURCE_UNKNOWN,
    /**
     * Type of meal related to the blood glucose measurement. Optional, enum field. Allowed values:
     * [MealType].
     *
     * @see MealType
     */
    @property:MealTypes public val mealType: Int = MealType.MEAL_TYPE_UNKNOWN,
    /**
     * Relationship of the meal to the blood glucose measurement. Optional, enum field. Allowed
     * values: [RelationToMeal].
     *
     * @see RelationToMeal
     */
    @property:RelationToMeals public val relationToMeal: Int = RELATION_TO_MEAL_UNKNOWN,
    override val metadata: Metadata = Metadata.EMPTY,
) : InstantaneousRecord {

    init {
        level.requireNotLess(other = level.zero(), name = "level")
        level.requireNotMore(other = MAX_BLOOD_GLUCOSE_LEVEL, name = "level")
    }

    /**
     * List of supported blood glucose specimen sources (type of body fluid used to measure the
     * blood glucose).
     */
    internal object SpecimenSource {
        const val INTERSTITIAL_FLUID = "interstitial_fluid"
        const val CAPILLARY_BLOOD = "capillary_blood"
        const val PLASMA = "plasma"
        const val SERUM = "serum"
        const val TEARS = "tears"
        const val WHOLE_BLOOD = "whole_blood"
    }

    /** Temporal relationship of measurement time to a meal. */
    internal object RelationToMeal {
        const val GENERAL = "general"
        const val FASTING = "fasting"
        const val BEFORE_MEAL = "before_meal"
        const val AFTER_MEAL = "after_meal"
    }

    companion object {
        private val MAX_BLOOD_GLUCOSE_LEVEL = BloodGlucose.millimolesPerLiter(50.0)

        const val RELATION_TO_MEAL_UNKNOWN = 0
        const val RELATION_TO_MEAL_GENERAL = 1
        const val RELATION_TO_MEAL_FASTING = 2
        const val RELATION_TO_MEAL_BEFORE_MEAL = 3
        const val RELATION_TO_MEAL_AFTER_MEAL = 4

        const val SPECIMEN_SOURCE_UNKNOWN = 0
        const val SPECIMEN_SOURCE_INTERSTITIAL_FLUID = 1
        const val SPECIMEN_SOURCE_CAPILLARY_BLOOD = 2
        const val SPECIMEN_SOURCE_PLASMA = 3
        const val SPECIMEN_SOURCE_SERUM = 4
        const val SPECIMEN_SOURCE_TEARS = 5
        const val SPECIMEN_SOURCE_WHOLE_BLOOD = 6

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val RELATION_TO_MEAL_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                RelationToMeal.GENERAL to RELATION_TO_MEAL_GENERAL,
                RelationToMeal.AFTER_MEAL to RELATION_TO_MEAL_AFTER_MEAL,
                RelationToMeal.FASTING to RELATION_TO_MEAL_FASTING,
                RelationToMeal.BEFORE_MEAL to RELATION_TO_MEAL_BEFORE_MEAL
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val RELATION_TO_MEAL_INT_TO_STRING_MAP = RELATION_TO_MEAL_STRING_TO_INT_MAP.reverse()

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val SPECIMEN_SOURCE_STRING_TO_INT_MAP: Map<String, Int> =
            mapOf(
                SpecimenSource.INTERSTITIAL_FLUID to SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                SpecimenSource.CAPILLARY_BLOOD to SPECIMEN_SOURCE_CAPILLARY_BLOOD,
                SpecimenSource.PLASMA to SPECIMEN_SOURCE_PLASMA,
                SpecimenSource.TEARS to SPECIMEN_SOURCE_TEARS,
                SpecimenSource.WHOLE_BLOOD to SPECIMEN_SOURCE_WHOLE_BLOOD,
                SpecimenSource.SERUM to SPECIMEN_SOURCE_SERUM
            )

        @RestrictTo(RestrictTo.Scope.LIBRARY)
        @JvmField
        val SPECIMEN_SOURCE_INT_TO_STRING_MAP = SPECIMEN_SOURCE_STRING_TO_INT_MAP.reverse()
    }

    /**
     * List of supported blood glucose specimen sources (type of body fluid used to measure the
     * blood glucose).
     *
     */
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                SPECIMEN_SOURCE_CAPILLARY_BLOOD,
                SPECIMEN_SOURCE_PLASMA,
                SPECIMEN_SOURCE_SERUM,
                SPECIMEN_SOURCE_TEARS,
                SPECIMEN_SOURCE_WHOLE_BLOOD,
            ]
    )
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    annotation class SpecimenSources

    /**
     * Temporal relationship of measurement time to a meal.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(
        value =
            [
                RELATION_TO_MEAL_GENERAL,
                RELATION_TO_MEAL_FASTING,
                RELATION_TO_MEAL_BEFORE_MEAL,
                RELATION_TO_MEAL_AFTER_MEAL,
            ]
    )
    annotation class RelationToMeals

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as BloodGlucoseRecord

        if (time != other.time) return false
        if (zoneOffset != other.zoneOffset) return false
        if (level != other.level) return false
        if (specimenSource != other.specimenSource) return false
        if (mealType != other.mealType) return false
        if (relationToMeal != other.relationToMeal) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = time.hashCode()
        result = 31 * result + (zoneOffset?.hashCode() ?: 0)
        result = 31 * result + level.hashCode()
        result = 31 * result + specimenSource
        result = 31 * result + mealType
        result = 31 * result + relationToMeal
        result = 31 * result + metadata.hashCode()
        return result
    }
}
