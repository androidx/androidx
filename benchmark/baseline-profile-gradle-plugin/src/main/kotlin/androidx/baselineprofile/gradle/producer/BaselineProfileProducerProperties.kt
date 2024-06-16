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

package androidx.baselineprofile.gradle.producer

import androidx.baselineprofile.gradle.utils.INSTRUMENTATION_ARG_TARGET_PACKAGE_NAME

private const val PROP_PREFIX_ANDROID_TEST_INSTRUMENTATION_RUNNER_ARG =
    "android.testInstrumentationRunnerArguments."

/**
 * This property determines whether the test task should actually run to generate the baseline
 * profile artifacts. When this property is set, the test tasks will be disabled and baseline
 * profile won't be generated. This property is useful for Github CI, as default ubuntu runners
 * cannot run android emulators.
 *
 * An example of how to configure baseline profile generation on Github CI is in the project
 * NowInAndroid:
 * https://github.com/android/nowinandroid/blob/main/.github/workflows/AndroidCIWithGmd.yaml#L42
 * It's necessary to set `runs-on: macos-12` and properties -Dorg.gradle.workers.max=1
 * -Pandroid.testoptions.manageddevices.emulator.gpu="swiftshader_indirect.
 */
internal const val PROP_SKIP_GENERATION = "androidx.baselineprofile.skipgeneration"

/**
 * This property determines whether the baselineProfile dsl specification for `managedDevices` and
 * `useConnectedDevices` is respected. When this property is set to to true only connected devices
 * are used and managed devices are ignored.
 */
internal const val PROP_FORCE_ONLY_CONNECTED_DEVICES =
    "androidx.baselineprofile.forceonlyconnecteddevices"

/**
 * This property determines whether the testInstrumentationRunnerArguments
 * `androidx.benchmark.enabledRules` is set depending on which variants the tests are invoked on.
 * When this flag IS NOT SPECIFIED, the plugin injects the `enabledRules` argument in order to
 * enable tests with a specific rule, depending on the variant. For example, when running tests on a
 * `nonMinified` build type such as `connectedNonMinifiedReleaseAndroidTest`, the plugin injects
 * `android.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=baselineprofile`.
 * When this flag IS SPECIFIED, the plugin will not inject any `enabledRules` argument and all the
 * tests, independent from the rule applied, will be run.
 */
internal const val PROP_DONT_DISABLE_RULES = "androidx.baselineprofile.dontdisablerules"

/**
 * This property specifies the target package name of the app to start when running the baseline
 * profile generator. Normally this is passed by the baseline profile gradle plugin and it's
 * specific per variant. It's possible to override this property manually directly specifying the
 * test instrumentation runner argument.
 */
internal const val PROP_SEND_TARGET_PACKAGE_NAME =
    "$PROP_PREFIX_ANDROID_TEST_INSTRUMENTATION_RUNNER_ARG$INSTRUMENTATION_ARG_TARGET_PACKAGE_NAME"
