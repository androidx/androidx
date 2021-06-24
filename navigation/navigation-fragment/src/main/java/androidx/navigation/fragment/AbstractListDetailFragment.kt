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

package androidx.navigation.fragment

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.annotation.CallSuper
import androidx.core.content.res.use
import androidx.core.view.doOnLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.slidingpanelayout.widget.SlidingPaneLayout

/**
 * A fragment supports adaptive two-pane layout. The first child is a list pane, which could be a
 * content list or browser, and the second child is NavHostFragment which controls to navigate
 * between different detail views.
 *
 * Implementation of the fragment should override this class and implement
 * [AbstractListDetailFragment.onCreateListPaneView] to supply custom view for the list pane. The
 * fragment provides default [NavHostFragment] with a NavGraph ID passed in the fragment, and it can
 * be overridden by [AbstractListDetailFragment.onCreateDetailPaneNavHostFragment] and provide
 * custom NavHostFragment.
 */
abstract class AbstractListDetailFragment : Fragment(R.layout.abstract_list_detail_fragment) {
    private var onBackPressedCallback: OnBackPressedCallback? = null
    private var detailPaneNavHostFragment: NavHostFragment? = null
    private var graphId = 0

    private class InnerOnBackPressedCallback(
        private val slidingPaneLayout: SlidingPaneLayout
    ) :
        OnBackPressedCallback(true),
        SlidingPaneLayout.PanelSlideListener {

        init {
            slidingPaneLayout.addPanelSlideListener(this)
        }

        override fun handleOnBackPressed() {
            slidingPaneLayout.closePane()
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

    /**
     * Return the [SlidingPaneLayout] this fragment is currently controlling.
     *
     * @throws IllegalStateException if the SlidingPaneLayout has not been created by [onCreateView]
     */
    fun requireSlidingPaneLayout(): SlidingPaneLayout {
        return requireView() as SlidingPaneLayout
    }

    /**
     * Return the [NavHostFragment] this fragment uses
     *
     * @throws IllegalStateException if the NavHostFragment has not been created by
     * {@link #onCreateView}.
     */
    fun requireDetailPaneNavHostFragment(): NavHostFragment {
        if (detailPaneNavHostFragment == null)
            throw IllegalStateException("Fragment $this was called before onCreateView().")
        return detailPaneNavHostFragment as NavHostFragment
    }

    @CallSuper
    override fun onInflate(
        context: Context,
        attrs: AttributeSet,
        savedInstanceState: Bundle?
    ) {
        super.onInflate(context, attrs, savedInstanceState)
        context.obtainStyledAttributes(
            attrs,
            androidx.navigation.R.styleable.NavHost
        ).use { navHost ->
            val graphId = navHost.getResourceId(
                androidx.navigation.R.styleable.NavHost_navGraph, 0
            )
            if (graphId != 0) {
                this.graphId = graphId
            }
        }
    }

    /**
     * Create the view for the fragment. This method provides two callbacks to instantiate a
     * list pane view and a NavHostFragment to control navigation between different detail views.
     *
     * @param inflater The [LayoutInflater] that used to inflate the fragment's views.
     * @param container The parent view that the fragment's UI should be attached to.
     * @param savedInstanceState The previous saved state of the fragment.
     *
     * @return Return the view for the fragment's UI
     *
     * @see onCreateListPaneView
     * @see onCreateDetailPaneNavHostFragment
     */
    @CallSuper
    final override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (savedInstanceState != null) {
            graphId = savedInstanceState.getInt(NavHostFragment.KEY_GRAPH_ID)
        }
        val slidingPaneLayout = super.onCreateView(inflater, container, savedInstanceState) as
            SlidingPaneLayout
        val listContainer = slidingPaneLayout.findViewById<FrameLayout>(R.id.list_container)
        val listPaneView = onCreateListPaneView(inflater, listContainer, savedInstanceState)
        if (listPaneView.parent != listContainer) {
            listContainer.addView(listPaneView)
        }
        val existingNavHostFragment = childFragmentManager.findFragmentById(R.id.detail_nav_host)
        detailPaneNavHostFragment = if (existingNavHostFragment != null) {
            existingNavHostFragment as NavHostFragment
        } else {
            onCreateDetailPaneNavHostFragment().also { newNavHostFragment ->
                childFragmentManager
                    .commit {
                        setReorderingAllowed(true)
                        add(R.id.detail_nav_host, newNavHostFragment)
                    }
            }
        }
        onBackPressedCallback = InnerOnBackPressedCallback(slidingPaneLayout)
        slidingPaneLayout.doOnLayout {
            onBackPressedCallback!!.isEnabled =
                slidingPaneLayout.isSlideable && slidingPaneLayout.isOpen
        }
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            onBackPressedCallback!!
        )
        return slidingPaneLayout
    }

    /**
     * Provide a list pane view for the fragment. Called when creating the view of the fragment.
     *
     * @param inflater The [LayoutInflater] that used to inflate the list pane view.
     * @param container The parent view of the list pane view. The parent view can be used to
     * generate the LayoutParams of the view.
     * @param savedInstanceState The previous saved state of the fragment.
     *
     * @return Return the list pane view for the fragment.
     */
    abstract fun onCreateListPaneView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View

    /**
     * Return an alternative [NavHostFragment] to swap the default NavHostFragment in the
     * fragment. This method get called when creating the view of the fragment.
     */
    open fun onCreateDetailPaneNavHostFragment(): NavHostFragment {
        if (graphId != 0) {
            return NavHostFragment.create(graphId)
        }
        return NavHostFragment()
    }

    /**
     * This method provides a callback [onListPaneViewCreated] after the view hierarchy has
     * been completely created.
     *
     * @param view The view returned by [onCreateView]
     * @param savedInstanceState The previous saved state of the fragment.
     *
     * @see onListPaneViewCreated
     */
    @CallSuper
    final override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listContainer =
            requireSlidingPaneLayout().findViewById<FrameLayout>(R.id.list_container)
        val listPaneView = listContainer.getChildAt(0)
        onListPaneViewCreated(listPaneView, savedInstanceState)
    }

    /**
     * Provides list pane view created in the fragment. Called when the fragment's [onViewCreated]
     * get called.
     *
     * @param view The list pane view created by [onCreateListPaneView] and added to view hierarchy
     * @param savedInstanceState The previous saved state of the fragment.
     */
    open fun onListPaneViewCreated(view: View, savedInstanceState: Bundle?) {}

    @CallSuper
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        onBackPressedCallback!!.isEnabled = requireSlidingPaneLayout().isSlideable &&
            requireSlidingPaneLayout().isOpen
    }

    @CallSuper
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (graphId != 0) {
            outState.putInt(NavHostFragment.KEY_GRAPH_ID, graphId)
        }
    }
}