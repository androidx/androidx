/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.navigation.compose

import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHostControllerTest.TestVM
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlin.reflect.typeOf
import kotlinx.serialization.Serializable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavHostControllerAndroidTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNavigateKClassCustomArgsSavedStateHandle() {
        @Serializable
        class CustomType(val nestedArg: Int) : Parcelable {
            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {}
        }

        val navType =
            object : NavType<CustomType>(false) {
                override fun put(bundle: Bundle, key: String, value: CustomType) {
                    bundle.putString(key, value.nestedArg.toString())
                }

                override fun get(bundle: Bundle, key: String): CustomType =
                    CustomType(nestedArg = bundle.getString(key)!!.toInt())

                override fun parseValue(value: String): CustomType = CustomType(value.toInt())

                override fun serializeAsValue(value: CustomType) = value.nestedArg.toString()
            }

        @Serializable class TestClass(val arg: CustomType)

        val typeMap = mapOf(typeOf<CustomType>() to navType)
        lateinit var vm: TestVM
        lateinit var navController: NavHostController
        composeTestRule.setContent {
            navController = rememberNavController()

            NavHost(navController, startDestination = "first") {
                composable("first") {}
                composable<TestClass>(typeMap) {
                    vm =
                        viewModel<TestVM> {
                            val handle = createSavedStateHandle()
                            TestVM(handle)
                        }
                }
            }
        }
        composeTestRule.runOnUiThread { navController.navigate(TestClass(CustomType(12))) {} }
        composeTestRule.runOnIdle {
            assertThat(navController.currentDestination?.hasRoute<TestClass>()).isTrue()
            assertThat(vm.handle.toRoute<TestClass>(typeMap).arg.nestedArg).isEqualTo(12)
        }
    }
}