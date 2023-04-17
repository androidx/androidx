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

package androidx.camera.testing

import androidx.annotation.RequiresApi
import androidx.test.filters.CustomFilter

/**
 * Indicates that a specific host side test should not be run against an emulator.
 *
 * It will be executed only if the test is running against a physical android device.
 *
 * This annotation is a temporary replacement of androidx.test.filters.RequiresDevice which is
 * deprecated. The detection conditions of emulator should be the same.
 *
 * Note: androidx.test.filters.RequiresDevice can be used on both "method" and "class" but this
 * annotation depends on [androidx.test.filters.CustomFilter] which can only be used on test
 * "method".
 */
@RequiresApi(21) // TODO(b/200306659): Remove and replace with annotation on package-info.java
@CustomFilter(filterClass = RequiresDeviceFilter::class)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class RequiresDevice