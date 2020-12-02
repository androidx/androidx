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

package androidx.benchmark.macro
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import com.android.helpers.JankCollectionHelper
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class JankCollectionHelperTest {
    // Setting a minSdkVersion of 27 because JankHelper fails with an error on API 26.
    // Needs a fix in JankHelper and re-import of prebults.
    // https://android-build.googleplex.com/builds/tests/view?invocationId=I00300005943166534&redirect=http://sponge2/025964b6-d278-44a7-805c-56d8010935a8
    @Test
    @SdkSuppress(minSdkVersion = 27)
    fun trivialTest() {
        JankCollectionHelper()
    }
}
