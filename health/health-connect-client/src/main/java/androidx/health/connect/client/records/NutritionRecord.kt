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
import androidx.health.connect.client.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures what nutrients were consumed as part of a meal or a food item. */
public class NutritionRecord(
    /** Biotin in grams. Optional field. Valid range: 0-100. */
    public val biotinGrams: Double = 0.0,
    /** Caffeine in grams. Optional field. Valid range: 0-100. */
    public val caffeineGrams: Double = 0.0,
    /** Calcium in grams. Optional field. Valid range: 0-100. */
    public val calciumGrams: Double = 0.0,
    /** Calories in kilocalories. Optional field. Valid range: 0-100000. */
    public val kcal: Double = 0.0,
    /** Calories from fat in kilocalories. Optional field. Valid range: 0-100000. */
    public val kcalFromFat: Double = 0.0,
    /** Chloride in grams. Optional field. Valid range: 0-100. */
    public val chlorideGrams: Double = 0.0,
    /** Cholesterol in grams. Optional field. Valid range: 0-100. */
    public val cholesterolGrams: Double = 0.0,
    /** Chromium in grams. Optional field. Valid range: 0-100. */
    public val chromiumGrams: Double = 0.0,
    /** Copper in grams. Optional field. Valid range: 0-100. */
    public val copperGrams: Double = 0.0,
    /** Dietary fiber in grams. Optional field. Valid range: 0-100000. */
    public val dietaryFiberGrams: Double = 0.0,
    /** Folate in grams. Optional field. Valid range: 0-100. */
    public val folateGrams: Double = 0.0,
    /** Folic acid in grams. Optional field. Valid range: 0-100. */
    public val folicAcidGrams: Double = 0.0,
    /** Iodine in grams. Optional field. Valid range: 0-100. */
    public val iodineGrams: Double = 0.0,
    /** Iron in grams. Optional field. Valid range: 0-100. */
    public val ironGrams: Double = 0.0,
    /** Magnesium in grams. Optional field. Valid range: 0-100. */
    public val magnesiumGrams: Double = 0.0,
    /** Manganese in grams. Optional field. Valid range: 0-100. */
    public val manganeseGrams: Double = 0.0,
    /** Molybdenum in grams. Optional field. Valid range: 0-100. */
    public val molybdenumGrams: Double = 0.0,
    /** Monounsaturated fat in grams. Optional field. Valid range: 0-100000. */
    public val monounsaturatedFatGrams: Double = 0.0,
    /** Niacin in grams. Optional field. Valid range: 0-100. */
    public val niacinGrams: Double = 0.0,
    /** Pantothenic acid in grams. Optional field. Valid range: 0-100. */
    public val pantothenicAcidGrams: Double = 0.0,
    /** Phosphorus in grams. Optional field. Valid range: 0-100. */
    public val phosphorusGrams: Double = 0.0,
    /** Polyunsaturated fat in grams. Optional field. Valid range: 0-100000. */
    public val polyunsaturatedFatGrams: Double = 0.0,
    /** Potassium in grams. Optional field. Valid range: 0-100. */
    public val potassiumGrams: Double = 0.0,
    /** Protein in grams. Optional field. Valid range: 0-100000. */
    public val proteinGrams: Double = 0.0,
    /** Riboflavin in grams. Optional field. Valid range: 0-100. */
    public val riboflavinGrams: Double = 0.0,
    /** Saturated fat in grams. Optional field. Valid range: 0-100000. */
    public val saturatedFatGrams: Double = 0.0,
    /** Selenium in grams. Optional field. Valid range: 0-100. */
    public val seleniumGrams: Double = 0.0,
    /** Sodium in grams. Optional field. Valid range: 0-100. */
    public val sodiumGrams: Double = 0.0,
    /** Sugar in grams. Optional field. Valid range: 0-100000. */
    public val sugarGrams: Double = 0.0,
    /** Thiamin in grams. Optional field. Valid range: 0-100. */
    public val thiaminGrams: Double = 0.0,
    /** Total carbohydrate in grams. Optional field. Valid range: 0-100000. */
    public val totalCarbohydrateGrams: Double = 0.0,
    /** Total fat in grams. Optional field. Valid range: 0-100000. */
    public val totalFatGrams: Double = 0.0,
    /** Trans fat in grams. Optional field. Valid range: 0-100000. */
    public val transFatGrams: Double = 0.0,
    /** Unsaturated fat in grams. Optional field. Valid range: 0-100000. */
    public val unsaturatedFatGrams: Double = 0.0,
    /** Vitamin A in grams. Optional field. Valid range: 0-100. */
    public val vitaminAGrams: Double = 0.0,
    /** Vitamin B12 in grams. Optional field. Valid range: 0-100. */
    public val vitaminB12Grams: Double = 0.0,
    /** Vitamin B6 in grams. Optional field. Valid range: 0-100. */
    public val vitaminB6Grams: Double = 0.0,
    /** Vitamin C in grams. Optional field. Valid range: 0-100. */
    public val vitaminCGrams: Double = 0.0,
    /** Vitamin D in grams. Optional field. Valid range: 0-100. */
    public val vitaminDGrams: Double = 0.0,
    /** Vitamin E in grams. Optional field. Valid range: 0-100. */
    public val vitaminEGrams: Double = 0.0,
    /** Vitamin K in grams. Optional field. Valid range: 0-100. */
    public val vitaminKGrams: Double = 0.0,
    /** Zinc in grams. Optional field. Valid range: 0-100. */
    public val zincGrams: Double = 0.0,
    /** Name for food or drink, provided by the user. Optional field. */
    public val name: String? = null,
    /**
     * Type of meal related to the nutrients consumed. Optional, enum field. Allowed values:
     * [MealTypes].
     */
    @property:MealTypes public val mealType: String? = null,
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NutritionRecord) return false

        if (biotinGrams != other.biotinGrams) return false
        if (caffeineGrams != other.caffeineGrams) return false
        if (calciumGrams != other.calciumGrams) return false
        if (kcal != other.kcal) return false
        if (kcalFromFat != other.kcalFromFat) return false
        if (chlorideGrams != other.chlorideGrams) return false
        if (cholesterolGrams != other.cholesterolGrams) return false
        if (chromiumGrams != other.chromiumGrams) return false
        if (copperGrams != other.copperGrams) return false
        if (dietaryFiberGrams != other.dietaryFiberGrams) return false
        if (folateGrams != other.folateGrams) return false
        if (folicAcidGrams != other.folicAcidGrams) return false
        if (iodineGrams != other.iodineGrams) return false
        if (ironGrams != other.ironGrams) return false
        if (magnesiumGrams != other.magnesiumGrams) return false
        if (manganeseGrams != other.manganeseGrams) return false
        if (molybdenumGrams != other.molybdenumGrams) return false
        if (monounsaturatedFatGrams != other.monounsaturatedFatGrams) return false
        if (niacinGrams != other.niacinGrams) return false
        if (pantothenicAcidGrams != other.pantothenicAcidGrams) return false
        if (phosphorusGrams != other.phosphorusGrams) return false
        if (polyunsaturatedFatGrams != other.polyunsaturatedFatGrams) return false
        if (potassiumGrams != other.potassiumGrams) return false
        if (proteinGrams != other.proteinGrams) return false
        if (riboflavinGrams != other.riboflavinGrams) return false
        if (saturatedFatGrams != other.saturatedFatGrams) return false
        if (seleniumGrams != other.seleniumGrams) return false
        if (sodiumGrams != other.sodiumGrams) return false
        if (sugarGrams != other.sugarGrams) return false
        if (thiaminGrams != other.thiaminGrams) return false
        if (totalCarbohydrateGrams != other.totalCarbohydrateGrams) return false
        if (totalFatGrams != other.totalFatGrams) return false
        if (transFatGrams != other.transFatGrams) return false
        if (unsaturatedFatGrams != other.unsaturatedFatGrams) return false
        if (vitaminAGrams != other.vitaminAGrams) return false
        if (vitaminB12Grams != other.vitaminB12Grams) return false
        if (vitaminB6Grams != other.vitaminB6Grams) return false
        if (vitaminCGrams != other.vitaminCGrams) return false
        if (vitaminDGrams != other.vitaminDGrams) return false
        if (vitaminEGrams != other.vitaminEGrams) return false
        if (vitaminKGrams != other.vitaminKGrams) return false
        if (zincGrams != other.zincGrams) return false
        if (mealType != other.mealType) return false
        if (name != other.name) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + biotinGrams.hashCode()
        result = 31 * result + caffeineGrams.hashCode()
        result = 31 * result + calciumGrams.hashCode()
        result = 31 * result + kcal.hashCode()
        result = 31 * result + kcalFromFat.hashCode()
        result = 31 * result + chlorideGrams.hashCode()
        result = 31 * result + cholesterolGrams.hashCode()
        result = 31 * result + chromiumGrams.hashCode()
        result = 31 * result + copperGrams.hashCode()
        result = 31 * result + dietaryFiberGrams.hashCode()
        result = 31 * result + folateGrams.hashCode()
        result = 31 * result + folicAcidGrams.hashCode()
        result = 31 * result + iodineGrams.hashCode()
        result = 31 * result + ironGrams.hashCode()
        result = 31 * result + magnesiumGrams.hashCode()
        result = 31 * result + manganeseGrams.hashCode()
        result = 31 * result + molybdenumGrams.hashCode()
        result = 31 * result + monounsaturatedFatGrams.hashCode()
        result = 31 * result + niacinGrams.hashCode()
        result = 31 * result + pantothenicAcidGrams.hashCode()
        result = 31 * result + phosphorusGrams.hashCode()
        result = 31 * result + polyunsaturatedFatGrams.hashCode()
        result = 31 * result + potassiumGrams.hashCode()
        result = 31 * result + proteinGrams.hashCode()
        result = 31 * result + riboflavinGrams.hashCode()
        result = 31 * result + saturatedFatGrams.hashCode()
        result = 31 * result + seleniumGrams.hashCode()
        result = 31 * result + sodiumGrams.hashCode()
        result = 31 * result + sugarGrams.hashCode()
        result = 31 * result + thiaminGrams.hashCode()
        result = 31 * result + totalCarbohydrateGrams.hashCode()
        result = 31 * result + totalFatGrams.hashCode()
        result = 31 * result + transFatGrams.hashCode()
        result = 31 * result + unsaturatedFatGrams.hashCode()
        result = 31 * result + vitaminAGrams.hashCode()
        result = 31 * result + vitaminB12Grams.hashCode()
        result = 31 * result + vitaminB6Grams.hashCode()
        result = 31 * result + vitaminCGrams.hashCode()
        result = 31 * result + vitaminDGrams.hashCode()
        result = 31 * result + vitaminEGrams.hashCode()
        result = 31 * result + vitaminKGrams.hashCode()
        result = 31 * result + zincGrams.hashCode()
        result = 31 * result + mealType.hashCode()
        result = 31 * result + name.hashCode()
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
        val BIOTIN_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "biotin")

        /**
         * Metric identifier to retrieve the total caffeine from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CAFFEINE_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "caffeine"
            )

        /**
         * Metric identifier to retrieve the total calcium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CALCIUM_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "calcium"
            )

        /**
         * Metric identifier to retrieve the total calories from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CALORIES_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "calories"
            )

        /**
         * Metric identifier to retrieve the total calories from fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CALORIES_FROM_FAT_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "caloriesFromFat"
            )

        /**
         * Metric identifier to retrieve the total chloride from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CHLORIDE_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "chloride"
            )

        /**
         * Metric identifier to retrieve the total cholesterol from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CHOLESTEROL_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "cholesterol"
            )

        /**
         * Metric identifier to retrieve the total chromium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val CHROMIUM_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "chromium"
            )

        /**
         * Metric identifier to retrieve the total copper from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val COPPER_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "copper")

        /**
         * Metric identifier to retrieve the total dietary fiber from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val DIETARY_FIBER_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "dietaryFiber"
            )

        /**
         * Metric identifier to retrieve the total folate from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val FOLATE_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "folate")

        /**
         * Metric identifier to retrieve the total folic acid from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val FOLIC_ACID_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "folicAcid"
            )

        /**
         * Metric identifier to retrieve the total iodine from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val IODINE_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "iodine")

        /**
         * Metric identifier to retrieve the total iron from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val IRON_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "iron")

        /**
         * Metric identifier to retrieve the total magnesium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val MAGNESIUM_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "magnesium"
            )

        /**
         * Metric identifier to retrieve the total manganese from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val MANGANESE_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "manganese"
            )

        /**
         * Metric identifier to retrieve the total molybdenum from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val MOLYBDENUM_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "molybdenum"
            )

        /**
         * Metric identifier to retrieve the total monounsaturated fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val MONOUNSATURATED_FAT_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "monounsaturatedFat"
            )

        /**
         * Metric identifier to retrieve the total niacin from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val NIACIN_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "niacin")

        /**
         * Metric identifier to retrieve the total pantothenic acid from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val PANTOTHENIC_ACID_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "pantothenicAcid"
            )

        /**
         * Metric identifier to retrieve the total phosphorus from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val PHOSPHORUS_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "phosphorus"
            )

        /**
         * Metric identifier to retrieve the total polyunsaturated fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val POLYUNSATURATED_FAT_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "polyunsaturatedFat"
            )

        /**
         * Metric identifier to retrieve the total potassium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val POTASSIUM_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "potassium"
            )

        /**
         * Metric identifier to retrieve the total protein from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val PROTEIN_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "protein"
            )

        /**
         * Metric identifier to retrieve the total riboflavin from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val RIBOFLAVIN_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "riboflavin"
            )

        /**
         * Metric identifier to retrieve the total saturated fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SATURATED_FAT_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "saturatedFat"
            )

        /**
         * Metric identifier to retrieve the total selenium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SELENIUM_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "selenium"
            )

        /**
         * Metric identifier to retrieve the total sodium from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SODIUM_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "sodium")

        /**
         * Metric identifier to retrieve the total sugar from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val SUGAR_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "sugar")

        /**
         * Metric identifier to retrieve the total thiamin from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val THIAMIN_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "thiamin"
            )

        /**
         * Metric identifier to retrieve the total total carbohydrate from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val TOTAL_CARBOHYDRATE_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "totalCarbohydrate"
            )

        /**
         * Metric identifier to retrieve the total total fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val TOTAL_FAT_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "totalFat"
            )

        /**
         * Metric identifier to retrieve the total trans fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val TRANS_FAT_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "transFat"
            )

        /**
         * Metric identifier to retrieve the total unsaturated fat from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val UNSATURATED_FAT_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "unsaturatedFat"
            )

        /**
         * Metric identifier to retrieve the total vitamin a from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_A_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "vitaminA"
            )

        /**
         * Metric identifier to retrieve the total vitamin b12 from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_B12_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "vitaminB12"
            )

        /**
         * Metric identifier to retrieve the total vitamin b6 from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_B6_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "vitaminB6"
            )

        /**
         * Metric identifier to retrieve the total vitamin c from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_C_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "vitaminC"
            )

        /**
         * Metric identifier to retrieve the total vitamin d from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_D_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "vitaminD"
            )

        /**
         * Metric identifier to retrieve the total vitamin e from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_E_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "vitaminE"
            )

        /**
         * Metric identifier to retrieve the total vitamin k from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val VITAMIN_K_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(
                TYPE_NAME,
                AggregateMetric.AggregationType.TOTAL,
                "vitaminK"
            )

        /**
         * Metric identifier to retrieve the total zinc from
         * [androidx.health.connect.client.aggregate.AggregationResult].
         */
        @JvmField
        val ZINC_TOTAL: AggregateMetric<Double> =
            AggregateMetric.doubleMetric(TYPE_NAME, AggregateMetric.AggregationType.TOTAL, "zinc")
    }
}
