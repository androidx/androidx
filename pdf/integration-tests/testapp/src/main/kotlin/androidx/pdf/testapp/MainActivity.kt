/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.pdf.testapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RestrictTo
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.pdf.testapp.databinding.MainActivityBinding
import androidx.pdf.testapp.databinding.ScenarioButtonsBinding
import androidx.pdf.testapp.ui.scenarios.SinglePdfFragment
import androidx.pdf.testapp.ui.scenarios.TabsViewPdfFragment
import com.google.android.material.button.MaterialButton

@SuppressLint("RestrictedApiAndroidX")
@RestrictTo(RestrictTo.Scope.LIBRARY)
class MainActivity : AppCompatActivity() {

    private lateinit var singlePdfButton: MaterialButton
    private lateinit var tabsViewButton: MaterialButton
    private lateinit var fragmentContainer: FrameLayout
    private var pdfViewerFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mainActivity = MainActivityBinding.inflate(layoutInflater)
        setContentView(mainActivity.root)

        val scenarioButtons = ScenarioButtonsBinding.bind(mainActivity.root)

        singlePdfButton = scenarioButtons.singlePdf
        tabsViewButton = scenarioButtons.tabView
        fragmentContainer = mainActivity.pdfInteractionFragmentContainerView

        singlePdfButton.setOnClickListener { loadFragment(SinglePdfFragment()) }
        tabsViewButton.setOnClickListener { loadFragment(TabsViewPdfFragment()) }

        handleWindowInsets()
        handleBackPress()

        // Check if a fragment is currently visible (automatically restored by FragmentManager)
        val currentFragment = supportFragmentManager.findFragmentByTag(PDF_INTERACTION_FRAGMENT_TAG)
        if (currentFragment != null) {
            // If a fragment is restored, hide buttons
            hideButtons()
            // Set Fragment Container Visible
            fragmentContainer.visibility = View.VISIBLE
        }
    }

    private fun handleBackPress() {
        // Handle back button presses
        val callback =
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    // Check if there are any fragments in the back stack
                    if (supportFragmentManager.backStackEntryCount > 0) {
                        // Pop the fragment from the back stack
                        supportFragmentManager.popBackStack()
                        // Show the buttons again
                        showButtons()
                        // Hide PDF_INTERACTION_FRAGMENT
                        fragmentContainer.visibility = View.GONE
                    } else {
                        // If already on the home fragment, exit the app
                        finish()
                    }
                }
            }

        // Add the callback to the OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun loadFragment(fragment: Fragment) {
        // Hide Buttons
        hideButtons()

        val fragmentManager: FragmentManager = supportFragmentManager

        // Replace an existing fragment in a container with an instance of a new fragment
        fragmentManager
            .beginTransaction()
            .replace(
                R.id.pdf_interaction_fragment_container_view,
                fragment,
                PDF_INTERACTION_FRAGMENT_TAG
            )
            .addToBackStack(null)
            .commitAllowingStateLoss()

        fragmentManager.executePendingTransactions()

        // Set Fragment Container Visible
        fragmentContainer.visibility = View.VISIBLE
    }

    private fun showButtons() {
        singlePdfButton.visibility = View.VISIBLE
        tabsViewButton.visibility = View.VISIBLE
    }

    private fun hideButtons() {
        singlePdfButton.visibility = View.GONE
        tabsViewButton.visibility = View.GONE
    }

    private fun handleWindowInsets() {
        val pdfContainerView: View = findViewById(R.id.main_container_view)

        ViewCompat.setOnApplyWindowInsetsListener(pdfContainerView) { view, insets ->
            // Get the insets for the system bars (status bar, navigation bar)
            val systemBarsInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Adjust the padding of the container view to accommodate system windows
            view.setPadding(
                view.paddingLeft,
                systemBarsInsets.top,
                view.paddingRight,
                systemBarsInsets.bottom
            )

            WindowInsetsCompat.CONSUMED
        }
    }

    companion object {
        private const val PDF_INTERACTION_FRAGMENT_TAG = "pdf_interaction_fragment_tag"
    }
}
