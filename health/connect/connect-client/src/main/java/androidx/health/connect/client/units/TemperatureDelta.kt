/*
 * Copyright 2024 The Android Open Source Project
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
 * Represents a unit of TemperatureDelta difference. Supported units:
 * - Celsius - see [TemperatureDelta.celsius], [Double.celsius]
 * - Fahrenheit - see [TemperatureDelta.fahrenheit], [Double.fahrenheit]
 */
class TemperatureDelta
private constructor(
    private val value: Double,
    private val type: Type,
) : Comparable<TemperatureDelta> {

    /** Returns the TemperatureDelta in Celsius degrees. */
    @get:JvmName("getCelsius")
    val inCelsius: Double
        get() =
            when (type) {
                Type.CELSIUS -> value
                Type.FAHRENHEIT -> value / 1.8
            }

    /** Returns the TemperatureDelta in Fahrenheit degrees. */
    @get:JvmName("getFahrenheit")
    val inFahrenheit: Double
        get() =
            when (type) {
                Type.CELSIUS -> value * 1.8
                Type.FAHRENHEIT -> value
            }

    override fun compareTo(other: TemperatureDelta): Int =
        if (type == other.type) {
            value.compareTo(other.value)
        } else {
            inCelsius.compareTo(other.inCelsius)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TemperatureDelta) return false

        if (type == other.type) {
            return value == other.value
        }

        return inCelsius == other.inCelsius
    }

    override fun hashCode(): Int = inCelsius.hashCode()

    override fun toString(): String = "$value ${type.title}"

    companion object {
        /** Creates [TemperatureDelta] with the specified value in Celsius degrees. */
        @JvmStatic
        fun celsius(value: Double): TemperatureDelta = TemperatureDelta(value, Type.CELSIUS)

        /** Creates [TemperatureDelta] with the specified value in Fahrenheit degrees. */
        @JvmStatic
        fun fahrenheit(value: Double): TemperatureDelta = TemperatureDelta(value, Type.FAHRENHEIT)
    }

    private enum class Type {
        CELSIUS {
            override val title: String = "Celsius"
        },
        FAHRENHEIT {
            override val title: String = "Fahrenheit"
        };

        abstract val title: String
    }
}
