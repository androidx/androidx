/*
 * Copyright 2019 The Android Open Source Project
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

@file:Suppress("deprecation")

package sample.experimental

import androidx.annotation.experimental.UseExperimental

@Suppress("unused", "MemberVisibilityCanBePrivate")
class UseJavaExperimentalFromKt {
    /**
     * Unsafe call into an experimental class.
     */
    fun getDateUnsafe(): Int {
        val dateProvider = DateProvider()
        return dateProvider.date
    }

    @ExperimentalDateTime
    fun getDateExperimental(): Int {
        val dateProvider = DateProvider()
        return dateProvider.date
    }

    @UseExperimental(ExperimentalDateTime::class)
    fun getDateUseExperimental(): Int {
        val dateProvider = DateProvider()
        return dateProvider.date
    }

    fun displayDate() {
        println("" + getDateUnsafe())
    }

    // Tests involving multiple experimental markers.

    /**
     * Unsafe call into an experimental class.
     */
    @ExperimentalDateTime
    fun getDateExperimentalLocationUnsafe(): Int {
        val dateProvider = DateProvider()
        val locationProvider = LocationProvider()
        return dateProvider.date + locationProvider.location
    }

    @ExperimentalDateTime
    @ExperimentalLocation
    fun getDateAndLocationExperimental(): Int {
        val dateProvider = DateProvider()
        val locationProvider = LocationProvider()
        return dateProvider.date + locationProvider.location
    }

    @UseExperimental(ExperimentalDateTime::class)
    @ExperimentalLocation
    fun getDateUseExperimentalLocationExperimental(): Int {
        val dateProvider = DateProvider()
        val locationProvider = LocationProvider()
        return dateProvider.date + locationProvider.location
    }

    @UseExperimental(ExperimentalDateTime::class, ExperimentalLocation::class)
    fun getDateAndLocationUseExperimental(): Int {
        val dateProvider = DateProvider()
        val locationProvider = LocationProvider()
        return dateProvider.date + locationProvider.location
    }
}
