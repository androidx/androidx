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
package androidx.health.data.client.records

import androidx.annotation.RestrictTo
import androidx.health.data.client.metadata.Metadata
import java.time.Instant
import java.time.ZoneOffset

/** Captures what nutrients were consumed as part of a meal or a food item. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class Nutrition(
    /** Biotin in grams. Optional field. Valid range: 0-100. */
    public val biotin: Double = 0.0,
    /** Caffeine in grams. Optional field. Valid range: 0-100. */
    public val caffeine: Double = 0.0,
    /** Calcium in grams. Optional field. Valid range: 0-100. */
    public val calcium: Double = 0.0,
    /** Calories in kilocalories. Optional field. Valid range: 0-100000. */
    public val calories: Double = 0.0,
    /** Calories from fat in kilocalories. Optional field. Valid range: 0-100000. */
    public val caloriesFromFat: Double = 0.0,
    /** Chloride in grams. Optional field. Valid range: 0-100. */
    public val chloride: Double = 0.0,
    /** Cholesterol in grams. Optional field. Valid range: 0-100. */
    public val cholesterol: Double = 0.0,
    /** Chromium in grams. Optional field. Valid range: 0-100. */
    public val chromium: Double = 0.0,
    /** Copper in grams. Optional field. Valid range: 0-100. */
    public val copper: Double = 0.0,
    /** Dietary fiber in grams. Optional field. Valid range: 0-100000. */
    public val dietaryFiber: Double = 0.0,
    /** Folate in grams. Optional field. Valid range: 0-100. */
    public val folate: Double = 0.0,
    /** Folic acid in grams. Optional field. Valid range: 0-100. */
    public val folicAcid: Double = 0.0,
    /** Iodine in grams. Optional field. Valid range: 0-100. */
    public val iodine: Double = 0.0,
    /** Iron in grams. Optional field. Valid range: 0-100. */
    public val iron: Double = 0.0,
    /** Magnesium in grams. Optional field. Valid range: 0-100. */
    public val magnesium: Double = 0.0,
    /** Manganese in grams. Optional field. Valid range: 0-100. */
    public val manganese: Double = 0.0,
    /** Molybdenum in grams. Optional field. Valid range: 0-100. */
    public val molybdenum: Double = 0.0,
    /** Monounsaturated fat in grams. Optional field. Valid range: 0-100000. */
    public val monounsaturatedFat: Double = 0.0,
    /** Niacin in grams. Optional field. Valid range: 0-100. */
    public val niacin: Double = 0.0,
    /** Pantothenic acid in grams. Optional field. Valid range: 0-100. */
    public val pantothenicAcid: Double = 0.0,
    /** Phosphorus in grams. Optional field. Valid range: 0-100. */
    public val phosphorus: Double = 0.0,
    /** Polyunsaturated fat in grams. Optional field. Valid range: 0-100000. */
    public val polyunsaturatedFat: Double = 0.0,
    /** Potassium in grams. Optional field. Valid range: 0-100. */
    public val potassium: Double = 0.0,
    /** Protein in grams. Optional field. Valid range: 0-100000. */
    public val protein: Double = 0.0,
    /** Riboflavin in grams. Optional field. Valid range: 0-100. */
    public val riboflavin: Double = 0.0,
    /** Saturated fat in grams. Optional field. Valid range: 0-100000. */
    public val saturatedFat: Double = 0.0,
    /** Selenium in grams. Optional field. Valid range: 0-100. */
    public val selenium: Double = 0.0,
    /** Sodium in grams. Optional field. Valid range: 0-100. */
    public val sodium: Double = 0.0,
    /** Sugar in grams. Optional field. Valid range: 0-100000. */
    public val sugar: Double = 0.0,
    /** Thiamin in grams. Optional field. Valid range: 0-100. */
    public val thiamin: Double = 0.0,
    /** Total carbohydrate in grams. Optional field. Valid range: 0-100000. */
    public val totalCarbohydrate: Double = 0.0,
    /** Total fat in grams. Optional field. Valid range: 0-100000. */
    public val totalFat: Double = 0.0,
    /** Trans fat in grams. Optional field. Valid range: 0-100000. */
    public val transFat: Double = 0.0,
    /** Unsaturated fat in grams. Optional field. Valid range: 0-100000. */
    public val unsaturatedFat: Double = 0.0,
    /** Vitamin A in grams. Optional field. Valid range: 0-100. */
    public val vitaminA: Double = 0.0,
    /** Vitamin B12 in grams. Optional field. Valid range: 0-100. */
    public val vitaminB12: Double = 0.0,
    /** Vitamin B6 in grams. Optional field. Valid range: 0-100. */
    public val vitaminB6: Double = 0.0,
    /** Vitamin C in grams. Optional field. Valid range: 0-100. */
    public val vitaminC: Double = 0.0,
    /** Vitamin D in grams. Optional field. Valid range: 0-100. */
    public val vitaminD: Double = 0.0,
    /** Vitamin E in grams. Optional field. Valid range: 0-100. */
    public val vitaminE: Double = 0.0,
    /** Vitamin K in grams. Optional field. Valid range: 0-100. */
    public val vitaminK: Double = 0.0,
    /** Zinc in grams. Optional field. Valid range: 0-100. */
    public val zinc: Double = 0.0,
    /**
     * Type of meal related to the nutrients consumed. Optional, enum field. Allowed values:
     * [MealType].
     */
    @property:MealType public val mealType: String? = null,
    override val startTime: Instant,
    override val startZoneOffset: ZoneOffset?,
    override val endTime: Instant,
    override val endZoneOffset: ZoneOffset?,
    override val metadata: Metadata = Metadata.EMPTY,
) : IntervalRecord {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Nutrition) return false

        if (biotin != other.biotin) return false
        if (caffeine != other.caffeine) return false
        if (calcium != other.calcium) return false
        if (calories != other.calories) return false
        if (caloriesFromFat != other.caloriesFromFat) return false
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
        if (mealType != other.mealType) return false
        if (startTime != other.startTime) return false
        if (startZoneOffset != other.startZoneOffset) return false
        if (endTime != other.endTime) return false
        if (endZoneOffset != other.endZoneOffset) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        result = 31 * result + biotin.hashCode()
        result = 31 * result + caffeine.hashCode()
        result = 31 * result + calcium.hashCode()
        result = 31 * result + calories.hashCode()
        result = 31 * result + caloriesFromFat.hashCode()
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
        result = 31 * result + mealType.hashCode()
        result = 31 * result + (startZoneOffset?.hashCode() ?: 0)
        result = 31 * result + endTime.hashCode()
        result = 31 * result + (endZoneOffset?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }
}
