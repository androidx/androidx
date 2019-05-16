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

package androidx.viewpager2.integration.testapp

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.integration.testapp.cards.CardViewAdapter
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.MarginPageTransformer

/**
 * Shows examples of [ViewPager2.PageTransformer], e.g. [MarginPageTransformer].
 */
class PageTransformerActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_page_transformer)

        val viewPager: ViewPager2 = findViewById(R.id.view_pager)
        viewPager.adapter = CardViewAdapter()

        OrientationController(viewPager, findViewById(R.id.orientation_spinner)).setUp()
        PageTransformerController(viewPager, findViewById(R.id.transformer_spinner)).setUp()
    }
}
