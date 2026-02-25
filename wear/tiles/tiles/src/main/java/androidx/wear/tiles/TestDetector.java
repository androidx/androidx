/*
 * Copyright 2026 The Android Open Source Project
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
package androidx.wear.tiles;

/**
 * Helper class to detect if the code is running in a test environment in order to avoid using
 * background executors in {@link SysUiTileUpdateRequester}.
 */
class TestDetector {
    @SuppressWarnings("NonFinalStaticField")
    private static Boolean sIsRobolectricCache = null;

    @SuppressWarnings("NonFinalStaticField")
    private static Boolean sIsAndroidTestCache = null;

    /** Checks if it's a Robolectric environment. */
    static boolean isRobolectricUnitTest() {
        if (sIsRobolectricCache == null) {
            try {
                Class.forName("org.robolectric.Robolectric");
                sIsRobolectricCache = true;
            } catch (ClassNotFoundException e) {
                sIsRobolectricCache = false;
            }
        }
        return sIsRobolectricCache;
    }

    /** Checks if it's an AndroidX Test (Instrumentation) environment. */
    public static boolean isAndroidInstrumentationTest() {
        if (sIsAndroidTestCache == null) {
            try {
                Class.forName("androidx.test.platform.app.InstrumentationRegistry");
                sIsAndroidTestCache = true;
            } catch (ClassNotFoundException e) {
                try {
                    // Older instrumentation runners might have this
                    Class.forName("android.test.InstrumentationTestCase");
                    sIsAndroidTestCache = true;
                } catch (ClassNotFoundException e2) {
                    sIsAndroidTestCache = false;
                }
            }
        }
        return sIsAndroidTestCache;
    }

    /**
     * Returns whether the code is likely running within a Robolectric or AndroidX Instrumentation
     * test.
     */
    static boolean isRunningInTest() {
        return isRobolectricUnitTest() || isAndroidInstrumentationTest();
    }

    private TestDetector() {}
}
