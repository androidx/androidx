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

package androidx.navigation.testing

import android.os.Bundle
import android.support.test.filters.SmallTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class TestNavigatorTest {

    @Test
    fun backStack() {
        val testNavigator = TestNavigator()
        val destination = testNavigator.createDestination()
        val args = Bundle()
        testNavigator.navigate(destination, args, null)
        assertEquals("TestNavigator back stack size is 1 after navigate",
                1,
                testNavigator.backStack.size)
        val (foundDestination, foundArgs) = testNavigator.backStack.last()
        assertEquals("last() returns last destination navigated to",
                destination, foundDestination)
        assertEquals("last() returns arguments Bundle",
                args, foundArgs)
    }
}
