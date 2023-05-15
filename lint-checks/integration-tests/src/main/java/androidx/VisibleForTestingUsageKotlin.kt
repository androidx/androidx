/*
 * Copyright 2021 The Android Open Source Project
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

@file:Suppress("unused")

package androidx

import androidx.annotation.VisibleForTesting
import androidx.annotation.VisibleForTesting.Companion.NONE

@Suppress("RemoveRedundantQualifierName")
class VisibleForTestingUsageKotlin {
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun testMethodPrivate() {}

    @VisibleForTesting(otherwise = VisibleForTesting.Companion.PRIVATE)
    fun testMethodCompanionPrivate() {}

    @VisibleForTesting(VisibleForTesting.PRIVATE)
    fun testMethodValuePrivate() {}

    @VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
    fun testMethodPackagePrivate() {}

    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    fun testMethodProtected() {}

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    fun testMethodPackageNone() {}

    @VisibleForTesting
    fun testMethodDefault() {}

    @get:VisibleForTesting(NONE)
    val testPropertyGet = "test"
}
