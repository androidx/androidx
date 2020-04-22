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

package androidx.fragment.app

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDialog
import androidx.fragment.test.R
import androidx.test.core.app.ActivityScenario
import androidx.test.filters.LargeTest
import androidx.testutils.withActivity
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

// the Layout used to inflate the view
sealed class InflatedViewLayout {
    abstract fun getLayoutId(): Int
    abstract fun getChildFragment(f: Fragment): Fragment?
    override fun toString(): String = this.javaClass.simpleName
}
// When to inflate the view
sealed class InflateViewLocation {
    override fun toString(): String = this.javaClass.simpleName
}

object UseFragmentContainerView : InflatedViewLayout() {
    override fun getLayoutId() = R.layout.inflated_fragment_container_view
    override fun getChildFragment(f: Fragment) =
        f.childFragmentManager.findFragmentByTag("fragment1")
}

object UseFragmentTag : InflatedViewLayout() {
    override fun getLayoutId() = R.layout.nested_inflated_fragment_parent
    override fun getChildFragment(f: Fragment) =
        f.childFragmentManager.findFragmentById(R.id.child_fragment)
}

object OnCreateDialog : InflateViewLocation()
object OnCreateView : InflateViewLocation()

@LargeTest
@RunWith(Parameterized::class)
class DialogFragmentInflatedChildTest(
    private val inflatedView: InflatedViewLayout,
    private val inflateLocation: InflateViewLocation
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "inflatedView={0}, inflateLocation={1}")
        fun data() = mutableListOf<Array<Any>>().apply {
            arrayOf(
                UseFragmentContainerView,
                UseFragmentTag
            ).forEach { inflatedViewLayout ->
                add(arrayOf(inflatedViewLayout, OnCreateDialog))
                add(arrayOf(inflatedViewLayout, OnCreateView))
            }
        }
    }

    @Test
    fun testInflatedChildDialogFragment() {
        with(ActivityScenario.launch(SimpleContainerActivity::class.java)) {
            val dialogFragment = TestInflatedChildDialogFragment.newInstance(
                false, inflatedView.getLayoutId(), inflateLocation is OnCreateDialog)

            withActivity {
                supportFragmentManager.beginTransaction()
                    .add(dialogFragment, "dialog")
                    .commitNow()
            }

            val child = inflatedView.getChildFragment(dialogFragment)
            assertWithMessage("Inflated child fragment should not be null")
                .that(child).isNotNull()

            recreate()

            val recreatedDialogFragment = withActivity {
                supportFragmentManager.findFragmentByTag("dialog") as DialogFragment
            }
            val recreatedChild = inflatedView.getChildFragment(recreatedDialogFragment)
            assertWithMessage("Inflated child fragment should not be null")
                .that(recreatedChild).isNotNull()
        }
    }

    @Test
    fun testInflatedChildAppCompatDialogFragment() {
        with(ActivityScenario.launch(TestAppCompatActivity::class.java)) {
            val dialogFragment = TestInflatedChildDialogFragment.newInstance(
                true, inflatedView.getLayoutId(), inflateLocation is OnCreateDialog)

            withActivity {
                supportFragmentManager.beginTransaction()
                    .add(dialogFragment, "dialog")
                    .commitNow()
            }

            val child = inflatedView.getChildFragment(dialogFragment)
            assertWithMessage("Inflated child fragment should not be null")
                .that(child).isNotNull()

            recreate()

            val recreatedDialogFragment = withActivity {
                supportFragmentManager.findFragmentByTag("dialog") as DialogFragment
            }
            val recreatedChild = inflatedView.getChildFragment(recreatedDialogFragment)
            assertWithMessage("Inflated child fragment should not be null")
                .that(recreatedChild).isNotNull()
        }
    }
}

// This Activity has an AppCompatTheme
class TestAppCompatActivity : AppCompatActivity(R.layout.simple_container)

class TestInflatedChildDialogFragment : DialogFragment() {

    companion object {
        private const val USE_APP_COMPAT_KEY = "USE_APP_COMPAT"
        private const val LAYOUT_ID_KEY = "LAYOUT_ID"
        private const val ON_CREATE_DIALOG_KEY = "ON_CREATE_DIALOG"

        fun newInstance(
            useAppCompat: Boolean,
            layoutId: Int,
            onCreateDialog: Boolean
        ) = TestInflatedChildDialogFragment().apply {
            arguments = Bundle().apply {
                putBoolean(USE_APP_COMPAT_KEY, useAppCompat)
                putInt(LAYOUT_ID_KEY, layoutId)
                putBoolean(ON_CREATE_DIALOG_KEY, onCreateDialog)
            }
        }
    }

    private val useAppCompat get() = requireArguments().getBoolean(USE_APP_COMPAT_KEY)
    private val layoutId get() = requireArguments().getInt(LAYOUT_ID_KEY, 0)
    private val onCreateDialog get() = requireArguments().getBoolean(ON_CREATE_DIALOG_KEY)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = if (!onCreateDialog) inflater.inflate(layoutId, container, false) else null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog =
            if (useAppCompat) AppCompatDialog(requireContext()) else Dialog(requireContext())
        if (onCreateDialog) {
            dialog.setContentView(layoutInflater.inflate(layoutId, null))
        }
        return dialog
    }
}
