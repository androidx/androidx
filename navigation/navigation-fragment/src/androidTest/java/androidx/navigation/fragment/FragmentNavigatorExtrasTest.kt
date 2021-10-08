/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.navigation.fragment

import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentNavigatorExtrasTest {

    @Test
    fun testAddSharedElement() {
        val view = View(InstrumentationRegistry.getInstrumentation().targetContext)
        val extras = FragmentNavigator.Extras.Builder()
            .addSharedElement(view, "test")
            .build()
        val sharedElements = extras.sharedElements
        assertWithMessage("Should be one shared element")
            .that(sharedElements.size)
            .isEqualTo(1)
        val name = sharedElements[view]
        assertWithMessage("Shared element should exist in the map")
            .that(name)
            .isNotNull()
        assertWithMessage("Shared element's name should match")
            .that(name)
            .isEqualTo("test")
    }

    @Test
    fun testAddSharedElements() {
        val map = mapOf(
            View(InstrumentationRegistry.getInstrumentation().targetContext) to "test1",
            View(InstrumentationRegistry.getInstrumentation().targetContext) to "test2"
        )
        val extras = FragmentNavigator.Extras.Builder()
            .addSharedElements(map)
            .build()
        val sharedElements = extras.sharedElements
        assertWithMessage("Should be ${map.size} shared elements")
            .that(sharedElements.size)
            .isEqualTo(map.size)
        map.forEach { (view, expected) ->
            val name = sharedElements[view]
            assertWithMessage("Shared element should exist in the map")
                .that(name)
                .isNotNull()
            assertWithMessage("Shared element's name should match")
                .that(name)
                .isEqualTo(expected)
        }
    }

    @Test
    fun testFragmentNavigatorExtras() {
        val view1 = View(InstrumentationRegistry.getInstrumentation().targetContext)
        val view2 = View(InstrumentationRegistry.getInstrumentation().targetContext)
        val extras = FragmentNavigatorExtras(view1 to "test1", view2 to "test2")
        val sharedElements = extras.sharedElements

        assertWithMessage("Should be 2 shared elements")
            .that(sharedElements.size)
            .isEqualTo(2)
        val name1 = sharedElements[view1]
        assertWithMessage("Shared element should exist in the map")
            .that(name1)
            .isNotNull()
        assertWithMessage("Shared element's name should match")
            .that(name1)
            .isEqualTo("test1")
        val name2 = sharedElements[view2]
        assertWithMessage("Shared element should exist in the map")
            .that(name2)
            .isNotNull()
        assertWithMessage("Shared element's name should match")
            .that(name2)
            .isEqualTo("test2")
    }
}
