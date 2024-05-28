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

package androidx.fragment.app

import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@MediumTest
class ContentViewTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testContentViewWithInflatedFragment() {
        // Test basic lifecycle when the fragment view is inflated with <fragment> tag
        withUse(ActivityScenario.launch(ContentViewActivity::class.java)) {
            val fm = withActivity { supportFragmentManager }
            val f1 = fm.findFragmentById(R.id.inflated_fragment) as StrictViewFragment

            assertWithMessage("onDestroyView was called when the fragment was added")
                .that(f1.onDestroyViewCalled)
                .isFalse()

            fm.beginTransaction().remove(f1).commit()
            executePendingTransactions(fm)

            assertWithMessage("onDestroyView was not called when the fragment was removed")
                .that(f1.onDestroyViewCalled)
                .isTrue()
        }
    }
}

class ContentViewActivity : FragmentActivity(R.layout.activity_inflated_fragment)
