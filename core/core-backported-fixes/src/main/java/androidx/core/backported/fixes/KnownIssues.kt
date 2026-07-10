/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.core.backported.fixes

import android.os.Build

/**
 * List of all known issues reportable by [BackportedFixManager].
 *
 * These are critical issues with fixes that are backported to existing android releases and are
 * reasonable for app developers to guard a code block with []BackportedFixManager.isFixed].
 *
 * Each known issue includes sample usage.
 *
 * The `id` and `alias` of a known issue comes from the list of approved backported fixes in the
 * Android Compatibility Test source directory
 * [cts/backported_fixes/approved](https://cs.android.com/android/platform/superproject/+/android-latest-release:cts/backported_fixes/approved/).
 */
public sealed class KnownIssues {
    public companion object {

        // sort the known issues by alias

        /**
         * **TEST ONLY** known issue that is always fixed on a device.
         *
         * @sample androidx.core.backported.fixes.samples.ki350037023
         */
        @JvmField public val KI_350037023: KnownIssue = KnownIssue(350037023L, 1)

        /**
         * **TEST ONLY** known issue that only applies to robolectric devices
         *
         * @sample androidx.core.backported.fixes.samples.ki372917199
         */
        @JvmField
        public val KI_372917199: KnownIssue =
            KnownIssue(
                372917199L,
                2,
                manuallyTestedFingerprints =
                    setOf(
                        "foo/bar/manually_tested" // The known issue is fixed for this fingerprint.
                    ),
            ) {
                (Build.BRAND.equals("robolectric"))
            }

        /**
         * **TEST ONLY** known issue that is never fixed on a device.
         *
         * @sample androidx.core.backported.fixes.samples.ki350037348
         */
        @JvmField public val KI_350037348: KnownIssue = KnownIssue(350037348L, 3)

        /**
         * Abnormal color tone when capturing `JPEG-R` images on some Pixel devices.
         *
         * Fix by using `JPEG` outputs until this KI is resolved.
         *
         * @sample androidx.core.backported.fixes.samples.ki398591036
         *
         * Full details are at [issue #398591036](https://issuetracker.google.com/issues/398591036).
         */
        @JvmField
        public val KI_398591036: KnownIssue =
            KnownIssue(
                398591036L,
                5,
                manuallyTestedFingerprints =
                    setOf(
                        // Sept Release
                        "google/blazer/blazer:16/BD3A.250721.001.B7/13955164:user/release-keys",
                        "google/caiman/caiman:16/BP3A.250905.014/13873947:user/release-keys",
                        "google/comet/comet:16/BP3A.250905.014/13873947:user/release-keys",
                        "google/frankel/frankel:16/BD3A.250721.001.B7/13955164:user/release-keys",
                        "google/komodo/komodo:16/BP3A.250905.014/13873947:user/release-keys",
                        "google/mustang/mustang:16/BD3A.250721.001.B7/13955164:user/release-keys",
                        "google/tokay/tokay:16/BP3A.250905.014/13873947:user/release-keys",
                        // Oct Release
                        "google/blazer/blazer:16/BD3A.251005.003.W3/14147046:user/release-keys",
                        "google/blazer/blazer:16/BD3A.251005.003.J5/14147083:user/release-keys",
                        "google/caiman/caiman:16/BP3A.251005.004.B1/14042072:user/release-keys",
                        "google/comet/comet:16/BP3A.251005.004.B1/14042072:user/release-keys",
                        "google/frankel/frankel:16/BD3A.251005.003.W3/14147046:user/release-keys",
                        "google/frankel/frankel:16/BD3A.251005.003.J5/14147083:user/release-keys",
                        "google/komodo/komodo:16/BP3A.251005.004.B1/14042072:user/release-keys",
                        "google/mustang/mustang:16/BD3A.251005.003.J5/14147083:user/release-keys",
                        "google/mustang/mustang:16/BD3A.251005.003.W3/14147046:user/release-keys",
                        "google/rango/rango:16/BD3A.251005.003.W3/14147046:user/release-keys",
                        "google/rango/rango:16/BD3A.251005.003.J5/14147083:user/release-keys",
                        "google/tokay/tokay:16/BP3A.251005.004.B1/14042072:user/release-keys",
                        // Nov Release
                        "google/blazer/blazer:16/BD3A.251105.010.E1/14337626:user/release-keys",
                        "google/blazer/blazer:16/BD3A.251105.010.F1/14341671:user/release-keys",
                        "google/blazer/blazer:16/BD3A.251105.010.J3/14341896:user/release-keys",
                        "google/caiman/caiman:16/BP3A.251105.015/14339231:user/release-keys",
                        "google/comet/comet:16/BP3A.251105.015/14339231:user/release-keys",
                        "google/frankel/frankel:16/BD3A.251105.010.E1/14337626:user/release-keys",
                        "google/frankel/frankel:16/BD3A.251105.010.F1/14341671:user/release-keys",
                        "google/frankel/frankel:16/BD3A.251105.010.J3/14341896:user/release-keys",
                        "google/komodo/komodo:16/BP3A.251105.015/14339231:user/release-keys",
                        "google/mustang/mustang:16/BD3A.251105.010.E1/14337626:user/release-keys",
                        "google/mustang/mustang:16/BD3A.251105.010.F1/14341671:user/release-keys",
                        "google/mustang/mustang:16/BD3A.251105.010.J3/14341896:user/release-keys",
                        "google/rango/rango:16/BD3A.251105.010.E1/14337626:user/release-keys",
                        "google/rango/rango:16/BD3A.251105.010.F1/14341671:user/release-keys",
                        "google/rango/rango:16/BD3A.251105.010.J3/14341896:user/release-keys",
                        "google/tokay/tokay:16/BP3A.251105.015/14339231:user/release-keys",
                        // Dec Release
                        "google/blazer/blazer:16/BD4A.251205.006.A1/14402117:user/release-keys",
                        "google/blazer/blazer:16/BD4A.251205.006/14401865:user/release-keys",
                        "google/blazer/blazer:16/BP4A.251205.006.C1/14402245:user/release-keys",
                        "google/caiman/caiman:16/BP4A.251205.006.A1/14402117:user/release-keys",
                        "google/caiman/caiman:16/BP4A.251205.006/14401865:user/release-keys",
                        "google/comet/comet:16/BD4A.251205.006.A1/14402117:user/release-keys",
                        "google/comet/comet:16/BD4A.251205.006/14401865:user/release-keys",
                        "google/frankel/frankel:16/BD4A.251205.006.A1/14402117:user/release-keys",
                        "google/frankel/frankel:16/BD4A.251205.006/14401865:user/release-keys",
                        "google/frankel/frankel:16/BP4A.251205.006.C1/14402245:user/release-keys",
                        "google/komodo/komodo:16/BP4A.251205.006.A1/14402117:user/release-keys",
                        "google/komodo/komodo:16/BP4A.251205.006/14401865:user/release-keys",
                        "google/mustang/mustang:16/BD4A.251205.006.A1/14402117:user/release-keys",
                        "google/mustang/mustang:16/BD4A.251205.006/14401865:user/release-keys",
                        "google/mustang/mustang:16/BP4A.251205.006.C1/14402245:user/release-keys",
                        "google/rango/rango:16/BD4A.251205.006.A1/14402117:user/release-keys",
                        "google/rango/rango:16/BP4A.251205.006.C1/14402245:user/release-keys",
                        "google/rango/rango:16/BD4A.251205.006/14401865:user/release-keys",
                        "google/tokay/tokay:16/BP4A.251205.006.A1/14402117:user/release-keys",
                        "google/tokay/tokay:16/BP4A.251205.006/14401865:user/release-keys",
                    ),
            ) {
                // This known issue only applies to Pixel devices.
                Build.BRAND.equals("google")
            }

        /**
         * Auto Exposure Mode Low Light Boost (LLB) mode
         * [CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata.html#CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY)
         * cannot be enabled for stream use cases such as VIDEO_CALL on Pixel 10 devices.
         *
         * Use
         * [CONTROL_AE_MODE_ON](https://developer.android.com/reference/android/hardware/camera2/CameraMetadata.html#CONTROL_AE_MODE_ON)
         * until this issue is resolved.
         *
         * @sample androidx.core.backported.fixes.samples.ki452390376
         *
         * Full details are at [issue #452390376](https://issuetracker.google.com/issues/452390376).
         */
        @JvmField
        public val KI_452390376: KnownIssue =
            KnownIssue(
                452390376L,
                6,
                precondition = {
                    // This known issue only applies to Pixel 10 but not 10a devices.
                    Build.BRAND.equals("google")
                    setOf("frankel", "blazer", "mustang", "rango").contains(Build.PRODUCT)
                },
            )
    }
}
