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

package androidx.preference

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.activity.OnBackPressedCallback
import androidx.activity.OnBackPressedDispatcherOwner
import androidx.annotation.CallSuper
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.fragment.app.commit
import androidx.slidingpanelayout.widget.SlidingPaneLayout

/**
 * [PreferenceHeaderFragmentCompat] implements a two-pane fragment for preferences. The list
 * pane is a container of preference headers. Tapping on a preference header swaps out the fragment
 * shown in the detail pane. Subclasses are expected to implement [onCreatePreferenceHeader] to
 * provide your own [PreferenceFragmentCompat] in the list pane. The preference header hierarchy
 * is defined by either providing an XML resource or build in code through
 * [PreferenceFragmentCompat]. In both cases, users need to use a [PreferenceScreen] as the root
 * component in the hierarchy.
 *
 * Usage:
 *
 * ```
 * class TwoPanePreference : PreferenceHeaderFragmentCompat() {
 *     override fun onCreatePreferenceHeader(): PreferenceFragmentCompat {
 *         return PreferenceHeader()
 *     }
 * }
 * ```
 *
 * [PreferenceHeaderFragmentCompat] handles the fragment transaction when users defines a
 * fragment or intent associated with the preference header. By default, the initial state fragment
 * for the detail pane is set to the associated fragment that first found in preference
 * headers. You can override [onCreateInitialDetailFragment] to provide the custom empty state
 * fragment for the detail pane.
 */
abstract class PreferenceHeaderFragmentCompat :
    Fragment(),
    PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
    private var onBackPressedCallback: OnBackPressedCallback? = null

    /**
     * Return the [SlidingPaneLayout] this fragment is currently controlling.
     *
     * @throws IllegalStateException if the SlidingPaneLayout has not been created by [onCreateView]
     */
    val slidingPaneLayout: SlidingPaneLayout
        get() = requireView() as SlidingPaneLayout

    @CallSuper
    override fun onPreferenceStartFragment(
        caller: PreferenceFragmentCompat,
        pref: Preference
    ): Boolean {
        if (caller.id == R.id.preferences_header) {
            // Opens the preference header.
            openPreference(pref)
            return true
        }
        if (caller.id == R.id.preferences_detail) {
            // Opens an preference in detail pane.
            val frag = childFragmentManager.fragmentFactory.instantiate(
                requireContext().classLoader,
                pref.fragment!!
            )
            frag.arguments = pref.extras

            childFragmentManager.commit {
                setReorderingAllowed(true)
                replace(R.id.preferences_detail, frag)
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
                addToBackStack(null)
            }
            return true
        }
        return false
    }

    private class InnerOnBackPressedCallback(
        private val caller: PreferenceHeaderFragmentCompat
    ) :
        OnBackPressedCallback(true),
        SlidingPaneLayout.PanelSlideListener {

        init {
            caller.slidingPaneLayout.addPanelSlideListener(this)
        }

        override fun handleOnBackPressed() {
            caller.slidingPaneLayout.closePane()
        }

        override fun onPanelSlide(panel: View, slideOffset: Float) {}

        override fun onPanelOpened(panel: View) {
            // Intercept the system back button when the detail pane becomes visible.
            isEnabled = true
        }

        override fun onPanelClosed(panel: View) {
            // Disable intercepting the system back button when the user returns to the list pane.
            isEnabled = false
        }
    }

    @CallSuper
    override fun onAttach(context: Context) {
        super.onAttach(context)
        parentFragmentManager.commit {
            setPrimaryNavigationFragment(this@PreferenceHeaderFragmentCompat)
        }
    }

    @CallSuper
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val slidingPaneLayout = buildContentView(inflater)
        // Now create the header fragment
        val existingHeaderFragment = childFragmentManager.findFragmentById(
            R.id.preferences_header
        )
        if (existingHeaderFragment == null) {
            onCreatePreferenceHeader().also { newHeaderFragment ->
                childFragmentManager.commit {
                    setReorderingAllowed(true)
                    add(R.id.preferences_header, newHeaderFragment)
                }
            }
        }
        slidingPaneLayout.lockMode = SlidingPaneLayout.LOCK_MODE_LOCKED
        return slidingPaneLayout
    }

    private fun buildContentView(inflater: LayoutInflater): SlidingPaneLayout {
        val slidingPaneLayout = SlidingPaneLayout(inflater.context).apply {
            id = R.id.preferences_sliding_pane_layout
        }
        // Add Preference Header Pane
        val headerContainer = FragmentContainerView(inflater.context).apply {
            id = R.id.preferences_header
        }
        val headerLayoutParams = SlidingPaneLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.preferences_header_width),
            MATCH_PARENT
        ).apply {
            weight = resources.getInteger(R.integer.preferences_header_pane_weight).toFloat()
        }
        slidingPaneLayout.addView(
            headerContainer,
            headerLayoutParams
        )

        // Add Preference Detail Pane
        val detailContainer = FragmentContainerView(inflater.context).apply {
            id = R.id.preferences_detail
        }
        val detailLayoutParams = SlidingPaneLayout.LayoutParams(
            resources.getDimensionPixelSize(R.dimen.preferences_detail_width),
            MATCH_PARENT
        ).apply {
            weight = resources.getInteger(R.integer.preferences_detail_pane_weight).toFloat()
        }
        slidingPaneLayout.addView(
            detailContainer,
            detailLayoutParams
        )
        return slidingPaneLayout
    }

    /**
     * Called to supply the preference header for this fragment. The subclasses are expected
     * to call [setPreferenceScreen(PreferenceScreen)] either directly or via helper methods
     * such as [setPreferenceFromResource(int)] to set headers.
     */
    abstract fun onCreatePreferenceHeader(): PreferenceFragmentCompat

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onBackPressedCallback = InnerOnBackPressedCallback(this)
        slidingPaneLayout.doOnLayout {
            onBackPressedCallback!!.isEnabled =
                slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen
        }
        childFragmentManager.addOnBackStackChangedListener {
            onBackPressedCallback!!.isEnabled = childFragmentManager.backStackEntryCount == 0
        }
        val onBackPressedDispatcherOwner = requireContext() as? OnBackPressedDispatcherOwner
        onBackPressedDispatcherOwner?.let {
            it.onBackPressedDispatcher.addCallback(
                viewLifecycleOwner,
                onBackPressedCallback!!
            )
        }
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        if (savedInstanceState == null) {
            onCreateInitialDetailFragment()?.let {
                childFragmentManager.commit {
                    setReorderingAllowed(true)
                    replace(R.id.preferences_detail, it)
                }
            }
        }
    }

    /**
     * Override this method to set initial detail fragment that to be shown. The default
     * implementation returns the first preference that has a fragment defined on
     * it.
     *
     * @return Fragment The first fragment that found in the list of preference headers.
     */
    open fun onCreateInitialDetailFragment(): Fragment? {
        val headerFragment = childFragmentManager.findFragmentById(R.id.preferences_header)
            as PreferenceFragmentCompat
        if (headerFragment.preferenceScreen.preferenceCount <= 0) {
            return null
        }
        for (index in 0 until headerFragment.preferenceScreen.preferenceCount) {
            val header = headerFragment.preferenceScreen.getPreference(index)
            if (header.fragment == null) {
                continue
            }
            val fragment = header.fragment?.let {
                childFragmentManager.fragmentFactory.instantiate(
                    requireContext().classLoader,
                    it
                )
            }
            return fragment
        }
        return null
    }

    /**
     * Swaps out the fragment that associated with preference header. If associated fragment is
     * unspecified, open the preference with the given intent instead.
     *
     * @param header The preference header that selected
     */
    fun openPreference(header: Preference) {
        if (header.fragment == null) {
            openPreference(header.intent)
            return
        }
        val fragment = header.fragment?.let {
            childFragmentManager.fragmentFactory.instantiate(
                requireContext().classLoader,
                it
            )
        }

        fragment?.apply {
            arguments = header.extras
        }

        // Clear back stack
        if (childFragmentManager.backStackEntryCount > 0) {
            val entry = childFragmentManager.getBackStackEntryAt(0)
            childFragmentManager.popBackStack(entry.id, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        }

        childFragmentManager.commit {
            setReorderingAllowed(true)
            replace(R.id.preferences_detail, fragment!!)
            if (slidingPaneLayout.isOpen) {
                setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            }
            slidingPaneLayout.openPane()
        }
    }

    /**
     * Open preference with the given intent
     *
     * @param intent The intent that associated with preference header
     */
    private fun openPreference(intent: Intent?) {
        if (intent == null) return
        // TODO: Change to use WindowManager ActivityView API
        startActivity(intent)
    }
}