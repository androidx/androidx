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

package androidx.preference.tests

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.preference.DialogPreference
import androidx.preference.Preference
import androidx.preference.PreferenceDialogFragmentCompat
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.test.R
import androidx.preference.tests.helpers.PreferenceTestHelperActivity
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@LargeTest
@RunWith(Parameterized::class)
class PreferenceDialogFragmentCompatTest(private val xmlLayoutId: Int) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "xmlLayoutId={0}")
        fun data(): Array<Int> {
            return arrayOf(
                R.xml.test_fragment_container_dialog_preference,
                R.xml.test_fragment_tag_dialog_preference
            )
        }
    }

    @Suppress("DEPRECATION")
    @get:Rule
    val activityTestRule = androidx.test.rule.ActivityTestRule(
        PreferenceTestHelperActivity::class.java
    )

    @Test
    fun testInflatedChildDialogFragment() {
        val fm = activityTestRule.activity.supportFragmentManager
        lateinit var fragment: InflatedFragment

        activityTestRule.runOnUiThread {
            fragment = InflatedFragment(xmlLayoutId)

            fm.beginTransaction()
                .add(fragment, null)
                .commitNow()
        }

        // Show the dialog
        fragment.preferenceManager.showDialog(fragment.preferenceManager.findPreference("key")!!)

        // Assert on UI thread so we wait for the dialog to show
        activityTestRule.runOnUiThread {
            assertWithMessage("The inflated child fragment should not be null")
                .that(fragment.dialogFragment.childFragmentManager.findFragmentByTag("fragment1"))
                .isNotNull()
        }
    }
}

class InflatedFragment(val xmlLayoutId: Int) : PreferenceFragmentCompat() {

    lateinit var dialogFragment: DialogFragment
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(xmlLayoutId, rootKey)
    }

    @Suppress("DEPRECATION")
    override fun onDisplayPreferenceDialog(preference: Preference) {
        dialogFragment = DialogFragment(preference.key)
            .also { it.setTargetFragment(this, 0) }
            .also { it.show(parentFragmentManager, null) }
    }
}

class NestedFragment : Fragment(R.layout.simple_layout)

class TestFragmentContainerViewDialogPreference(context: Context, attrs: AttributeSet?) :
    DialogPreference(context, attrs) {
    init { dialogLayoutResource = R.layout.inflated_fragment_container_view }
}

class TestFragmentTagDialogPreference(context: Context, attrs: AttributeSet?) :
    DialogPreference(context, attrs) {
    init { dialogLayoutResource = R.layout.inflated_fragment_tag }
}

class DialogFragment(val key: String) : PreferenceDialogFragmentCompat() {
    init { arguments = Bundle(1).apply { putString(ARG_KEY, key) } }
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.simple_layout, container, false)
    }
    override fun onDialogClosed(positiveResult: Boolean) {}
}