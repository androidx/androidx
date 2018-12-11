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

import android.os.Bundle
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import org.junit.Test

@SmallTest
class NavDestinationAndroidTest {
    @Test
    fun addInDefaultArgs() {
        val destination = NoOpNavigator().createDestination()
        val stringArgument = NavArgument.Builder()
            .setType(NavType.StringType)
            .setDefaultValue("aaa")
            .build()
        val intArgument = NavArgument.Builder()
            .setType(NavType.IntType)
            .setDefaultValue(123)
            .build()
        destination.addArgument("stringArg", stringArgument)
        destination.addArgument("intArg", intArgument)

        val bundle = destination.addInDefaultArgs(Bundle().apply {
            putString("stringArg", "bbb")
        })
        assertThat(bundle?.getString("stringArg")).isEqualTo("bbb")
        assertThat(bundle?.getInt("intArg")).isEqualTo(123)
    }

    @Test(expected = IllegalArgumentException::class)
    fun addInDefaultArgsWrong() {
        val destination = NoOpNavigator().createDestination()
        val stringArgument = NavArgument.Builder()
            .setType(NavType.StringType)
            .setDefaultValue("aaa")
            .build()
        val intArgument = NavArgument.Builder()
            .setType(NavType.IntType)
            .setDefaultValue(123)
            .build()
        destination.addArgument("stringArg", stringArgument)
        destination.addArgument("intArg", intArgument)

        destination.addInDefaultArgs(Bundle().apply {
            putInt("stringArg", 123)
        })
    }
}
