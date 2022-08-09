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

import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregateMetric.AggregationType
import androidx.health.connect.client.aggregate.AggregateMetric.Companion.doubleMetric
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import java.time.Instant
import java.time.ZoneOffset

/** Captures what nutrients were consumed as part of a meal or a food item. */
public class NutritionRecord(
    /** Biotin in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val biotin: Mass? = null,
    /** Caffeine in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val caffeine: Mass? = null,
    /** Calcium in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val calcium: Mass? = null,
    /** Energy in [Energy] unit. Optional field. Valid range: 0-100000 kcal. */
    public val energy: Energy? = null,
    /** Energy from fat in [Energy] unit. Optional field. Valid range: 0-100000 kcal. */
    public val energyFromFat: Energy? = null,
    /** Chloride in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val chloride: Mass? = null,
    /** Cholesterol in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val cholesterol: Mass? = null,
    /** Chromium in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val chromium: Mass? = null,
    /** Copper in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val copper: Mass? = null,
    /** Dietary fiber in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val dietaryFiber: Mass? = null,
    /** Folate in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val folate: Mass? = null,
    /** Folic acid in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val folicAcid: Mass? = null,
    /** Iodine in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val iodine: Mass? = null,
    /** Iron in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val iron: Mass? = null,
    /** Magnesium in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val magnesium: Mass? = null,
    /** Manganese in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val manganese: Mass? = null,
    /** Molybdenum in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val molybdenum: Mass? = null,
    /** Monounsaturated fat in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val monounsaturatedFat: Mass? = null,
    /** Niacin in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val niacin: Mass? = null,
    /** Pantothenic acid in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val pantothenicAcid: Mass? = null,
    /** Phosphorus in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val phosphorus: Mass? = null,
    /** Polyunsaturated fat in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val polyunsaturatedFat: Mass? = null,
    /** Potassium in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val potassium: Mass? = null,
    /** Protein in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val protein: Mass? = null,
    /** Riboflavin in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val riboflavin: Mass? = null,
    /** Saturated fat in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val saturatedFat: Mass? = null,
    /** Selenium in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val selenium: Mass? = null,
    /** Sodium in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val sodium: Mass? = null,
    /** Sugar in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val sugar: Mass? = null,
    /** Thiamin in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val thiamin: Mass? = null,
    /** Total carbohydrate in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val totalCarbohydrate: Mass? = null,
    /** Total fat in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val totalFat: Mass? = null,
    /** Trans fat in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val transFat: Mass? = null,
    /** Unsaturated fat in [Mass] unit. Optional field. Valid range: 0-100000 grams. */
    public val unsaturatedFat: Mass? = null,
    /** Vitamin A in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val vitaminA: Mass? = null,
    /** Vitamin B12 in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val vitaminB12: Mass? = null,
    /** Vitamin B6 in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val vitaminB6: Mass? = null,
    /** Vitamin C in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val vitaminC: Mass? = null,
    /** Vitamin D in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val vitaminD: Mass? = null,
    /** Vitamin E in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val vitaminE: Mass? = null,
    /** Vitamin K in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val vitaminK: Mass? = null,
    /** Zinc in [Mass] unit. Optional field. Valid range: 0-100 grams. */
    public val zinc: Mass? = null,
    /** Name for food or drink, provided by the user. Optional field. */
    public val name: String? = null,
    /**
     * Type of meal related to the nutrients consumed. Optional, enum field. Allowed values:
     * [MealType].
     *
     * @see MealType
     */
    @property:MealTypes public val mealType: String? = null,
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NutritionRecord) return false

        if (biotin != other.biotin) return false
        if (caffeine != other.caffeine) return false
        if (calcium != other.calcium) return false
        if (energy != other.energy) return false
        if (energyFromFat != other.energyFromFat) return false
        if (chloride != other.chloride) return false
        if (cholesterol != other.cholesterol) return false
        if (chromium != other.chromium) return false
        if (copper != other.copper) return false
        if (dietaryFiber != other.dietaryFiber) return false
        if (folate != other.folate) return false
        if (folicAcid != other.folicAcid) return false
        if (iodine != other.iodine) return false
        if (iron != other.iron) return false
        if (magnesium != other.magnesium) return false
        if (manganese != other.manganese) return false
        if (molybdenum != other.molybdenum) return false
        if (monounsaturatedFat != other.monounsaturatedFat) return false
        if (niacin != other.niacin) return false
        if (pantothenicAcid != other.pantothenicAcid) return false
        if (phosphorus != other.phosphorus) return false
        if (polyunsaturatedFat != other.polyunsaturatedFat) return false
        if (potassium != other.potassium) return false
        if (protein != other.protein) return false
        if (riboflavin != other.riboflavin) return false
        if (saturatedFat != other.saturatedFat) return false
        if (selenium != other.selenium) return false
        if (sodium != other.sodium) return false
        if (sugar != other.sugar) return false
        if (thiamin != other.thiamin) return false
        if (totalCarbohydrate != other.totalCarbohydrate) return false
        if (totalFat != other.totalFat) return false
        if (transFat != other.transFat) return false
        if (unsaturatedFat != other.unsaturatedFat) return false
        if (vitaminA != other.vitaminA) return false
        if (vitaminB12 != other.vitaminB12) return false
        if (vitaminB6 != other.vitaminB6) return false
        if (vitaminC != other.vitaminC) return false
        if (vitaminD != other.vitaminD) return false
        if (vitaminE != other.vitaminE) return false
        if (vitaminK != other.vitaminK) return false
        if (zinc != other.zinc) return false
        if (name != other.name) return false
        if (mealType != other.mealType) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    /*
     * Generated by the IDE: Code -> Generate -> "equals() and hashCode()".
     */
    override fun hashCode(): Int {
        var result = biotin.hashCode()
        result = 31 * result + caffeine.hashCode()
        result = 31 * result + calcium.hashCode()
        result = 31 * result + energy.hashCode()
        result = 31 * result + energyFromFat.hashCode()
        result = 31 * result + chloride.hashCode()
        result = 31 * result + cholesterol.hashCode()
        result = 31 * result + chromium.hashCode()
        result = 31 * result + copper.hashCode()
        result = 31 * result + dietaryFiber.hashCode()
        result = 31 * result + folate.hashCode()
        result = 31 * result + folicAcid.hashCode()
        result = 31 * result + iodine.hashCode()
        result = 31 * result + iron.hashCode()
        result = 31 * result + magnesium.hashCode()
        result = 31 * result + manganese.hashCode()
        result = 31 * result + molybdenum.hashCode()
        result = 31 * result + monounsaturatedFat.hashCode()
        result = 31 * result + niacin.hashCode()
        result = 31 * result + pantothenicAcid.hashCode()
        result = 31 * result + phosphorus.hashCode()
        result = 31 * result + polyunsaturatedFat.hashCode()
        result = 31 * result + potassium.hashCode()
        result = 31 * result + protein.hashCode()
        result = 31 * result + riboflavin.hashCode()
        result = 31 * result + saturatedFat.hashCode()
        result = 31 * result + selenium.hashCode()
        result = 31 * result + sodium.hashCode()
        result = 31 * result + sugar.hashCode()
        result = 31 * result + thiamin.hashCode()
        result = 31 * result + totalCarbohydrate.hashCode()
        result = 31 * result + totalFat.hashCode()
        result = 31 * result + transFat.hashCode()
        result = 31 * result + unsaturatedFat.hashCode()
        result = 31 * result + vitaminA.hashCode()
        result = 31 * result + vitaminB12.hashCode()
        result = 31 * result + vitaminB6.hashCode()
        result = 31 * result + vitaminC.hashCode()
        result = 31 * result + vitaminD.hashCode()
        result = 31 * result + vitaminE.hashCode()
        result = 31 * result + vitaminK.hashCode()
        result = 31 * result + zinc.hashCode()
        result = 31 * result + (name?.hashCode() ?: 0)
        result = 31 * result + (mealType?.hashCode() ?: 0)
        result = 31 * result + startTime.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    companion object {
        private const val TYPE_NAME = "Nutrition"

        /**
         * Metric identifier to retrieve the total biotin from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val BIOTIN_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "biotin", Mass::grams)

        /**
         * Metric identifier to retrieve the total caffeine from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CAFFEINE_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "caffeine", Mass::grams)

        /**
         * Metric identifier to retrieve the total calcium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CALCIUM_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "calcium", Mass::grams)

        /**
         * Metric identifier to retrieve the total energy from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val ENERGY_TOTAL: AggregateMetric<Energy> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "calories", Energy::kilocalories)

        /**
         * Metric identifier to retrieve the total energy from fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val ENERGY_FROM_FAT_TOTAL: AggregateMetric<Energy> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "caloriesFromFat", Energy::kilocalories)

        /**
         * Metric identifier to retrieve the total chloride from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CHLORIDE_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "chloride", Mass::grams)

        /**
         * Metric identifier to retrieve the total cholesterol from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CHOLESTEROL_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "cholesterol", Mass::grams)

        /**
         * Metric identifier to retrieve the total chromium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CHROMIUM_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "chromium", Mass::grams)

        /**
         * Metric identifier to retrieve the total copper from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val COPPER_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "copper", Mass::grams)

        /**
         * Metric identifier to retrieve the total dietary fiber from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val DIETARY_FIBER_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "dietaryFiber", Mass::grams)

        /**
         * Metric identifier to retrieve the total folate from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val FOLATE_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "folate", Mass::grams)

        /**
         * Metric identifier to retrieve the total folic acid from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val FOLIC_ACID_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "folicAcid", Mass::grams)

        /**
         * Metric identifier to retrieve the total iodine from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val IODINE_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "iodine", Mass::grams)

        /**
         * Metric identifier to retrieve the total iron from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val IRON_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "iron", Mass::grams)

        /**
         * Metric identifier to retrieve the total magnesium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val MAGNESIUM_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "magnesium", Mass::grams)

        /**
         * Metric identifier to retrieve the total manganese from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val MANGANESE_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "manganese", Mass::grams)

        /**
         * Metric identifier to retrieve the total molybdenum from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val MOLYBDENUM_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "molybdenum", Mass::grams)

        /**
         * Metric identifier to retrieve the total monounsaturated fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val MONOUNSATURATED_FAT_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "monounsaturatedFat", Mass::grams)

        /**
         * Metric identifier to retrieve the total niacin from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val NIACIN_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "niacin", Mass::grams)

        /**
         * Metric identifier to retrieve the total pantothenic acid from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val PANTOTHENIC_ACID_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "pantothenicAcid", Mass::grams)

        /**
         * Metric identifier to retrieve the total phosphorus from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val PHOSPHORUS_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "phosphorus", Mass::grams)

        /**
         * Metric identifier to retrieve the total polyunsaturated fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val POLYUNSATURATED_FAT_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "polyunsaturatedFat", Mass::grams)

        /**
         * Metric identifier to retrieve the total potassium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val POTASSIUM_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "potassium", Mass::grams)

        /**
         * Metric identifier to retrieve the total protein from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val PROTEIN_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "protein", Mass::grams)

        /**
         * Metric identifier to retrieve the total riboflavin from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val RIBOFLAVIN_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "riboflavin", Mass::grams)

        /**
         * Metric identifier to retrieve the total saturated fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SATURATED_FAT_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "saturatedFat", Mass::grams)

        /**
         * Metric identifier to retrieve the total selenium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SELENIUM_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "selenium", Mass::grams)

        /**
         * Metric identifier to retrieve the total sodium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SODIUM_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "sodium", Mass::grams)

        /**
         * Metric identifier to retrieve the total sugar from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SUGAR_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "sugar", Mass::grams)

        /**
         * Metric identifier to retrieve the total thiamin from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val THIAMIN_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "thiamin", Mass::grams)

        /**
         * Metric identifier to retrieve the total total carbohydrate from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val TOTAL_CARBOHYDRATE_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "totalCarbohydrate", Mass::grams)

        /**
         * Metric identifier to retrieve the total total fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val TOTAL_FAT_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "totalFat", Mass::grams)

        /**
         * Metric identifier to retrieve the total trans fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val TRANS_FAT_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "transFat", Mass::grams)

        /**
         * Metric identifier to retrieve the total unsaturated fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val UNSATURATED_FAT_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "unsaturatedFat", Mass::grams)

        /**
         * Metric identifier to retrieve the total vitamin a from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_A_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "vitaminA", Mass::grams)

        /**
         * Metric identifier to retrieve the total vitamin b12 from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_B12_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "vitaminB12", Mass::grams)

        /**
         * Metric identifier to retrieve the total vitamin b6 from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_B6_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "vitaminB6", Mass::grams)

        /**
         * Metric identifier to retrieve the total vitamin c from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_C_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "vitaminC", Mass::grams)

        /**
         * Metric identifier to retrieve the total vitamin d from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_D_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "vitaminD", Mass::grams)

        /**
         * Metric identifier to retrieve the total vitamin e from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_E_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "vitaminE", Mass::grams)

        /**
         * Metric identifier to retrieve the total vitamin k from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_K_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "vitaminK", Mass::grams)

        /**
         * Metric identifier to retrieve the total zinc from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val ZINC_TOTAL: AggregateMetric<Mass> =
            doubleMetric(TYPE_NAME, AggregationType.TOTAL, "zinc", Mass::grams)
    }
}
