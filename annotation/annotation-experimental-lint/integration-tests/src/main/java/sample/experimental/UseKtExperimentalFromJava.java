/*
 * Copyright 2020 The Android Open Source Project
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

package sample.experimental;

@SuppressWarnings({"unused", "WeakerAccess"})
class UseKtExperimentalFromJava {
    /**
     * Unsafe call into an experimental class.
     */
    int getDateUnsafe() {
        sample.experimental.DateProviderKt dateProvider = new sample.experimental.DateProviderKt();
        return dateProvider.getDate();
    }

    @ExperimentalDateTimeKt
    int getDateExperimental() {
        sample.experimental.DateProviderKt dateProvider = new sample.experimental.DateProviderKt();
        return dateProvider.getDate();
    }

    @SuppressWarnings("deprecation")
    @androidx.annotation.experimental.UseExperimental(markerClass = ExperimentalDateTimeKt.class)
    int getDateUseExperimental() {
        sample.experimental.DateProviderKt dateProvider = new sample.experimental.DateProviderKt();
        return dateProvider.getDate();
    }

    void displayDate() {
        System.out.println("" + getDateUnsafe());
    }

    // Tests involving multiple experimental markers.

    /**
     * Unsafe call into an experimental class.
     */
    @ExperimentalDateTimeKt
    int getDateExperimentalLocationUnsafe() {
        sample.experimental.DateProviderKt dateProvider = new sample.experimental.DateProviderKt();
        LocationProviderKt locationProvider = new LocationProviderKt();
        return dateProvider.getDate() + locationProvider.getLocation();
    }

    @ExperimentalDateTimeKt
    @ExperimentalLocationKt
    int getDateAndLocationExperimental() {
        sample.experimental.DateProviderKt dateProvider = new sample.experimental.DateProviderKt();
        LocationProviderKt locationProvider = new LocationProviderKt();
        return dateProvider.getDate() + locationProvider.getLocation();
    }

    @SuppressWarnings("deprecation")
    @androidx.annotation.experimental.UseExperimental(markerClass = ExperimentalDateTimeKt.class)
    @ExperimentalLocationKt
    int getDateUseExperimentalLocationExperimental() {
        sample.experimental.DateProviderKt dateProvider = new sample.experimental.DateProviderKt();
        LocationProviderKt locationProvider = new LocationProviderKt();
        return dateProvider.getDate() + locationProvider.getLocation();
    }

    @SuppressWarnings("deprecation")
    @androidx.annotation.experimental.UseExperimental(markerClass = {ExperimentalDateTimeKt.class,
            ExperimentalLocationKt.class})
    int getDateAndLocationUseExperimental() {
        sample.experimental.DateProviderKt dateProvider = new DateProviderKt();
        LocationProviderKt locationProvider = new LocationProviderKt();
        return dateProvider.getDate() + locationProvider.getLocation();
    }

    /**
     * Regression test for issue reported in b/140637106, which passes here but fails in Studio.
     */
    void regressionTestStaticUsage() {
        TimeProviderKt.getTimeStatically();
        TimeProviderKt.Companion.getTimeStatically();
    }

    /**
     * Regression test for issue reported in b/140637106, which passes here but fails in Studio.
     */
    void regressionTestInlineUsage() {
        new TimeProviderKt().getTime();
        new TimeProviderKt().getTimeJava();
    }

    @SuppressWarnings("deprecation")
    @androidx.annotation.experimental.UseExperimental(markerClass = ExperimentalDateTimeKt.class)
    static class FancyDateProvider extends DateProviderKt {}
}
