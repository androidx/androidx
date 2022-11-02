/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.emoji2.emojipicker

import android.app.Activity
import android.content.Context
import android.os.Bundle
import androidx.core.view.isVisible
import androidx.emoji2.emojipicker.test.R
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

class EmojiPickerViewTestActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.inflation_test)
    }
}

@LargeTest
@RunWith(AndroidJUnit4::class)
class EmojiPickerViewTest {
    private lateinit var mContext: Context

    @get:Rule
    val mActivityTestRule = ActivityScenarioRule(
        EmojiPickerViewTestActivity::class.java
    )

    @Before
    fun setUp() {
        mContext = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun testCustomEmojiPickerView_rendered() {
        mActivityTestRule.scenario.onActivity {
            val mEmojiPickerView = it.findViewById<EmojiPickerView>(R.id.emojiPickerTest)
            assert(mEmojiPickerView.isVisible)
            assertEquals(mEmojiPickerView.emojiGridColumns, 10)
        }
    }
}
