/*
 * Copyright (C) 2018 The Android Open Source Project
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

package androidx.viewpager2.widget.swipe

import android.os.Bundle
import androidx.testutils.RecreatedActivity
import androidx.viewpager2.LocaleTestUtils
import androidx.viewpager2.test.R

class TestActivity : RecreatedActivity() {
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.hasExtra(EXTRA_LANGUAGE) == true) {
            LocaleTestUtils(this).setLocale(intent.getStringExtra(EXTRA_LANGUAGE))
        }

        setContentView(R.layout.activity_test_layout)

        /** hacky way of configuring this instance from test code */
        onCreateCallback(this)
    }

    companion object {
        var onCreateCallback: ((TestActivity) -> Unit) = { }
        const val EXTRA_LANGUAGE = "language"
    }
}
