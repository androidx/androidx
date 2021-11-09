/*
 * Copyright (C) 2017 The Android Open Source Project
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

import androidx.annotation.IdRes
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class NavActionTest {

    companion object {
        @IdRes
        private const val DESTINATION_ID = 1
    }

    @Test
    fun createAction() {
        val action = NavAction(DESTINATION_ID)

        assertThat(action.destinationId).isEqualTo(DESTINATION_ID)
    }

    @Test
    fun createActionWithNullNavOptions() {
        val action = NavAction(DESTINATION_ID, null)

        assertThat(action.destinationId).isEqualTo(DESTINATION_ID)
        assertThat(action.navOptions).isNull()
    }

    @Test
    fun setNavOptions() {
        val action = NavAction(DESTINATION_ID)
        val navOptions = NavOptions.Builder().build()
        action.navOptions = navOptions

        assertThat(action.navOptions).isEqualTo(navOptions)
    }
}
