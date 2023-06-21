/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.health.connect.client.units

/**
 * Represents a unit of energy. Supported units:
 * - calories - see [Energy.calories], [Double.calories]
 * - kilocalories - see [Energy.kilocalories], [Double.kilocalories]
 * - joules - see [Energy.joules], [Double.joules]
 * - kilojoules - see [Energy.kilojoules], [Double.kilojoules]
 */
class Energy
private constructor(
    private val value: Double,
    private val type: Type,
) : Comparable<Energy> {

    /** Returns the energy in calories. */
    @get:JvmName("getCalories")
    val inCalories: Double
        get() = value * type.caloriesPerUnit

    /** Returns the energy in kilocalories. */
    @get:JvmName("getKilocalories")
    val inKilocalories: Double
        get() = get(type = Type.KILOCALORIES)

    /** Returns the energy in joules. */
    @get:JvmName("getJoules")
    val inJoules: Double
        get() = get(type = Type.JOULES)

    /** Returns the energy in kilojoules. */
    @get:JvmName("getKilojoules")
    val inKilojoules: Double
        get() = get(type = Type.KILOJOULES)

    private fun get(type: Type): Double =
        if (this.type == type) value else inCalories / type.caloriesPerUnit

    /** Returns zero [Energy] of the same [Type]. */
    internal fun zero(): Energy = ZEROS.getValue(type)

    override fun compareTo(other: Energy): Int =
        if (type == other.type) {
            value.compareTo(other.value)
        } else {
            inCalories.compareTo(other.inCalories)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Energy) return false

        if (type == other.type) {
            return value == other.value
        }

        return inCalories == other.inCalories
    }

    override fun hashCode(): Int = inCalories.hashCode()

    override fun toString(): String = "$value ${type.title}"

    companion object {
        private val ZEROS = Type.values().associateWith { Energy(value = 0.0, type = it) }

        /** Creates [Energy] with the specified value in calories. */
        @JvmStatic fun calories(value: Double): Energy = Energy(value, Type.CALORIES)

        /** Creates [Energy] with the specified value in kilocalories. */
        @JvmStatic fun kilocalories(value: Double): Energy = Energy(value, Type.KILOCALORIES)

        /** Creates [Energy] with the specified value in joules. */
        @JvmStatic fun joules(value: Double): Energy = Energy(value, Type.JOULES)

        /** Creates [Energy] with the specified value in kilojoules. */
        @JvmStatic fun kilojoules(value: Double): Energy = Energy(value, Type.KILOJOULES)
    }

    private enum class Type {
        CALORIES {
            override val caloriesPerUnit: Double = 1.0
            override val title: String = "cal"
        },
        KILOCALORIES {
            override val caloriesPerUnit: Double = 1000.0
            override val title: String = "kcal"
        },
        JOULES {
            override val caloriesPerUnit: Double = 0.2390057361
            override val title: String = "J"
        },
        KILOJOULES {
            override val caloriesPerUnit: Double = 239.0057361
            override val title: String = "kJ"
        };

        abstract val caloriesPerUnit: Double
        abstract val title: String
    }
}

/** Creates [Energy] with the specified value in calories. */
@get:JvmSynthetic
val Double.calories: Energy
    get() = Energy.calories(value = this)

/** Creates [Energy] with the specified value in calories. */
@get:JvmSynthetic
val Long.calories: Energy
    get() = toDouble().calories

/** Creates [Energy] with the specified value in calories. */
@get:JvmSynthetic
val Float.calories: Energy
    get() = toDouble().calories

/** Creates [Energy] with the specified value in calories. */
@get:JvmSynthetic
val Int.calories: Energy
    get() = toDouble().calories

/** Creates [Energy] with the specified value in kilocalories. */
@get:JvmSynthetic
val Double.kilocalories: Energy
    get() = Energy.kilocalories(value = this)

/** Creates [Energy] with the specified value in kilocalories. */
@get:JvmSynthetic
val Long.kilocalories: Energy
    get() = toDouble().kilocalories

/** Creates [Energy] with the specified value in kilocalories. */
@get:JvmSynthetic
val Float.kilocalories: Energy
    get() = toDouble().kilocalories

/** Creates [Energy] with the specified value in kilocalories. */
@get:JvmSynthetic
val Int.kilocalories: Energy
    get() = toDouble().kilocalories

/** Creates [Energy] with the specified value in joules. */
@get:JvmSynthetic
val Double.joules: Energy
    get() = Energy.joules(value = this)

/** Creates [Energy] with the specified value in joules. */
@get:JvmSynthetic
val Long.joules: Energy
    get() = toDouble().joules

/** Creates [Energy] with the specified value in joules. */
@get:JvmSynthetic
val Float.joules: Energy
    get() = toDouble().joules

/** Creates [Energy] with the specified value in joules. */
@get:JvmSynthetic
val Int.joules: Energy
    get() = toDouble().joules

/** Creates [Energy] with the specified value in kilojoules. */
@get:JvmSynthetic
val Double.kilojoules: Energy
    get() = Energy.kilojoules(value = this)

/** Creates [Energy] with the specified value in kilojoules. */
@get:JvmSynthetic
val Long.kilojoules: Energy
    get() = toDouble().kilojoules

/** Creates [Energy] with the specified value in kilojoules. */
@get:JvmSynthetic
val Float.kilojoules: Energy
    get() = toDouble().kilojoules

/** Creates [Energy] with the specified value in kilojoules. */
@get:JvmSynthetic
val Int.kilojoules: Energy
    get() = toDouble().kilojoules
