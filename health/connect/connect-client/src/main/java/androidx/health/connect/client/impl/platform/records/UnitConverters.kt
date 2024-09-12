/*
 * Copyright 2023 The Android Open Source Project
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

@file:RestrictTo(RestrictTo.Scope.LIBRARY)
@file:RequiresApi(api = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

package androidx.health.connect.client.impl.platform.records

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresExtension
import androidx.annotation.RestrictTo
import androidx.health.connect.client.units.BloodGlucose
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Length
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Percentage
import androidx.health.connect.client.units.Power
import androidx.health.connect.client.units.Pressure
import androidx.health.connect.client.units.Temperature
import androidx.health.connect.client.units.TemperatureDelta
import androidx.health.connect.client.units.Velocity
import androidx.health.connect.client.units.Volume

internal fun BloodGlucose.toPlatformBloodGlucose(): PlatformBloodGlucose {
    return PlatformBloodGlucose.fromMillimolesPerLiter(inMillimolesPerLiter)
}

internal fun Energy.toPlatformEnergy(): PlatformEnergy {
    return PlatformEnergy.fromCalories(inCalories)
}

internal fun Length.toPlatformLength(): PlatformLength {
    return PlatformLength.fromMeters(inMeters)
}

internal fun Mass.toPlatformMass(): PlatformMass {
    return PlatformMass.fromGrams(inGrams)
}

internal fun Percentage.toPlatformPercentage(): PlatformPercentage {
    return PlatformPercentage.fromValue(value)
}

internal fun Power.toPlatformPower(): PlatformPower {
    return PlatformPower.fromWatts(inWatts)
}

internal fun Pressure.toPlatformPressure(): PlatformPressure {
    return PlatformPressure.fromMillimetersOfMercury(inMillimetersOfMercury)
}

internal fun Temperature.toPlatformTemperature(): PlatformTemperature {
    return PlatformTemperature.fromCelsius(inCelsius)
}

@SuppressLint("NewApi") // Guarded by sdk extension check
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
internal fun TemperatureDelta.toPlatformTemperatureDelta(): PlatformTemperatureDelta {
    return PlatformTemperatureDelta.fromCelsius(inCelsius)
}

internal fun Velocity.toPlatformVelocity(): PlatformVelocity {
    return PlatformVelocity.fromMetersPerSecond(inMetersPerSecond)
}

internal fun Volume.toPlatformVolume(): PlatformVolume {
    return PlatformVolume.fromLiters(inLiters)
}

internal fun PlatformBloodGlucose.toSdkBloodGlucose(): BloodGlucose {
    return BloodGlucose.millimolesPerLiter(inMillimolesPerLiter)
}

internal fun PlatformEnergy.toNonDefaultSdkEnergy() =
    takeIf { inCalories != Double.MIN_VALUE }?.toSdkEnergy()

internal fun PlatformEnergy.toSdkEnergy(): Energy {
    return Energy.calories(inCalories)
}

internal fun PlatformLength.toSdkLength(): Length {
    return Length.meters(inMeters)
}

internal fun PlatformMass.toNonDefaultSdkMass() =
    takeIf { inGrams != Double.MIN_VALUE }?.toSdkMass()

internal fun PlatformMass.toSdkMass(): Mass {
    return Mass.grams(inGrams)
}

internal fun PlatformPercentage.toSdkPercentage(): Percentage {
    return Percentage(value)
}

internal fun PlatformPower.toSdkPower(): Power {
    return Power.watts(inWatts)
}

internal fun PlatformPressure.toSdkPressure(): Pressure {
    return Pressure.millimetersOfMercury(inMillimetersOfMercury)
}

internal fun PlatformTemperature.toSdkTemperature(): Temperature {
    return Temperature.celsius(inCelsius)
}

@SuppressLint("NewApi") // Guarded by sdk extension check
@RequiresExtension(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, 13)
internal fun PlatformTemperatureDelta.toSdkTemperatureDelta(): TemperatureDelta {
    return TemperatureDelta.celsius(inCelsius)
}

internal fun PlatformVelocity.toSdkVelocity(): Velocity {
    return Velocity.metersPerSecond(inMetersPerSecond)
}

internal fun PlatformVolume.toSdkVolume(): Volume {
    return Volume.liters(inLiters)
}
