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

package sample;

import androidx.annotation.experimental.UseExperimental;

@SuppressWarnings({"unused", "WeakerAccess"})
class UseJavaExperimentalFromJava {
    /**
     * Unsafe call into an experimental class.
     */
    int getDateUnsafe() {
        DateProvider dateProvider = new DateProvider();
        return dateProvider.getDate();
    }

    @ExperimentalDateTime
    int getDateExperimental() {
        DateProvider dateProvider = new DateProvider();
        return dateProvider.getDate();
    }

    @UseExperimental(markerClass = ExperimentalDateTime.class)
    int getDateUseExperimental() {
        DateProvider dateProvider = new DateProvider();
        return dateProvider.getDate();
    }

    void displayDate() {
        System.out.println("" + getDateUnsafe());
    }

    // Tests involving multiple experimental markers.

    /**
     * Unsafe call into an experimental class.
     */
    @ExperimentalDateTime
    int getDateExperimentalLocationUnsafe() {
        DateProvider dateProvider = new DateProvider();
        LocationProvider locationProvider = new LocationProvider();
        return dateProvider.getDate() + locationProvider.getLocation();
    }

    @ExperimentalDateTime
    @ExperimentalLocation
    int getDateAndLocationExperimental() {
        DateProvider dateProvider = new DateProvider();
        LocationProvider locationProvider = new LocationProvider();
        return dateProvider.getDate() + locationProvider.getLocation();
    }

    @UseExperimental(markerClass = ExperimentalDateTime.class)
    @ExperimentalLocation
    int getDateUseExperimentalLocationExperimental() {
        DateProvider dateProvider = new DateProvider();
        LocationProvider locationProvider = new LocationProvider();
        return dateProvider.getDate() + locationProvider.getLocation();
    }

    @UseExperimental(markerClass = { ExperimentalDateTime.class, ExperimentalLocation.class })
    int getDateAndLocationUseExperimental() {
        DateProvider dateProvider = new DateProvider();
        LocationProvider locationProvider = new LocationProvider();
        return dateProvider.getDate() + locationProvider.getLocation();
    }
}
