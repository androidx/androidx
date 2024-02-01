/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.activity.result

import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class ActivityResultCallerTest {
    @Test
    fun getContractTest() {
        with(ActivityScenario.launch(EmptyContentActivity::class.java)) {
            val contract = withActivity { contract }
            val javaLauncher = withActivity { javaLauncher }
            val kotlinLauncher = withActivity { kotlinLauncher }

            assertThat(javaLauncher.contract).isSameInstanceAs(contract)
            assertThat(kotlinLauncher.contract).isNotSameInstanceAs(contract)
        }
    }
}

class EmptyContentActivity : ComponentActivity() {
    val contract = StartActivityForResult()
    val javaLauncher = registerForActivityResult(contract) { }
    val kotlinLauncher = registerForActivityResult(contract, Intent()) { }
}
