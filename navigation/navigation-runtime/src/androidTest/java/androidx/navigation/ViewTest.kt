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

package androidx.navigation

import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith

@SmallTest
@RunWith(AndroidJUnit4::class)
class ViewTest {

    @Test fun findNavController() {
        val view = View(ApplicationProvider.getApplicationContext() as android.content.Context)
        val navController =
            NavController(ApplicationProvider.getApplicationContext() as android.content.Context)
        Navigation.setViewNavController(view, navController)

        val foundNavController = view.findNavController()
        assertWithMessage("View should have NavController set")
            .that(foundNavController).isSameInstanceAs(navController)
    }

    @Test fun findNavControllerNull() {
        val view = View(ApplicationProvider.getApplicationContext() as android.content.Context)
        try {
            view.findNavController()
            fail(
                "findNavController should throw IllegalStateException if a NavController" +
                    " was not set"
            )
        } catch (e: IllegalStateException) {
            // Expected
        }
    }
}
