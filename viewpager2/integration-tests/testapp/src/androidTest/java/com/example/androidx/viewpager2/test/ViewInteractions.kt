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

package com.example.androidx.viewpager2.test

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.isDisplayingAtLeast
import androidx.test.espresso.matcher.ViewMatchers.withChild
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import org.hamcrest.CoreMatchers.allOf

fun onViewPager(): ViewInteraction {
    return onView(isAssignableFrom(ViewPager2::class.java))
}

fun onCurrentPage(): ViewInteraction {
    return onView(allOf(
        withParent(withParent(isAssignableFrom(ViewPager2::class.java))),
        isDisplayingAtLeast(50)
    ))
}

fun onTab(withText: String): ViewInteraction {
    return onView(
        allOf(
            isDescendantOfA(isAssignableFrom(TabLayout::class.java)),
            withChild(withText(withText))
        )
    )
}
