/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appcompat.app

import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AppCompatDialogTest {

    @Test
    fun testViewTreeLifecycleOwner() {
        withUse(ActivityScenario.launch(AppCompatActivity::class.java)) {
            lateinit var view: View
            val dialog = withActivity {
                view = View(this)
                AppCompatDialog(this)
            }
            dialog.setContentView(view)

            val lifecycle = dialog.lifecycle
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.INITIALIZED)

            onActivity {
                dialog.show()
            }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.RESUMED)

            val viewOwner = dialog.window?.decorView?.findViewTreeLifecycleOwner()!!
            assertThat(viewOwner).isEqualTo(dialog)

            onActivity {
                dialog.dismiss()
            }
            assertThat(lifecycle.currentState).isEqualTo(Lifecycle.State.DESTROYED)

            assertWithMessage("A new Lifecycle object should be created after destruction")
                .that(dialog.lifecycle)
                .isNotSameInstanceAs(lifecycle)
        }
    }
}
