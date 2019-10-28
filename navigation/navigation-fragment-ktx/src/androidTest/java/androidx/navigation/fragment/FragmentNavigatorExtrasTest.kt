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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock

@LargeTest
@RunWith(AndroidJUnit4::class)
class FragmentNavigatorExtrasTest {

    @Test
    fun testFragmentNavigatorExtras() {
        val view1 = mock(View::class.java)
        val view2 = mock(View::class.java)
        val extras = FragmentNavigatorExtras(view1 to "test1", view2 to "test2")
        val sharedElements = extras.sharedElements

        assertEquals("Should be 2 shared elements", 2, sharedElements.size)
        val name1 = sharedElements[view1]
        assertNotNull("Shared element should exist in the map", name1)
        assertEquals("Shared element's name should match", "test1", name1)
        val name2 = sharedElements[view2]
        assertNotNull("Shared element should exist in the map", name2)
        assertEquals("Shared element's name should match", "test2", name2)
    }
}
