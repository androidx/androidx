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

package androidx.fragment.app

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.test.EmptyFragmentTestActivity
import androidx.fragment.test.R
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import androidx.savedstate.findViewTreeSavedStateRegistryOwner
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.testutils.withUse
import com.google.common.truth.Truth.assertWithMessage
import leakcanary.DetectLeaksAfterTestSuccess
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class DialogFragmentViewTreeTest {

    @get:Rule val rule = DetectLeaksAfterTestSuccess()

    @Test
    fun testDialogFragmentViewTree() {
        withUse(ActivityScenario.launch(EmptyFragmentTestActivity::class.java)) {
            val dialogFragment = TestDialogFragment()

            onActivity { dialogFragment.showNow(it.supportFragmentManager, null) }

            val decorView =
                dialogFragment.requireDialog().window?.decorView ?: error("no decor view available")

            assertWithMessage("DialogFragment dialog should have a ViewTreeLifecycleOwner")
                .that(decorView.findViewTreeLifecycleOwner())
                .isNotNull()
            assertWithMessage("DialogFragment dialog should have a ViewTreeViewModelStoreOwner")
                .that(decorView.findViewTreeViewModelStoreOwner())
                .isNotNull()
            assertWithMessage("DialogFragment dialog should have a ViewTreeSavedStateRegistryOwner")
                .that(decorView.findViewTreeSavedStateRegistryOwner())
                .isNotNull()
        }
    }

    class TestDialogFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            val view = layoutInflater.inflate(R.layout.with_edit_text, null, false)
            return AlertDialog.Builder(context)
                .setTitle("Test")
                .setMessage("Message")
                .setView(view)
                .setPositiveButton("Button", null)
                .create()
        }
    }
}
