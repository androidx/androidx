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

package androidx.benchmark.macro

import androidx.test.filters.MediumTest
import org.junit.Test

@MediumTest
class JankCollectionHelperTest {
    @Test
    fun clearGfxInfo_thisProcess() {
        JankCollectionHelper().clearGfxInfo(Packages.TEST)
    }

    @Test
    fun clearGfxInfo_notRunningPackage() {
        // shouldn't fail, clearing a package that isn't running
        // (or in this case, installed) is a noop
        JankCollectionHelper().clearGfxInfo(Packages.MISSING)
    }
}
