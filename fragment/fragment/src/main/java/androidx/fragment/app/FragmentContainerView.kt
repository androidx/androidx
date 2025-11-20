/*
 * Copyright 2019 The Android Open Source Project
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

import android.animation.LayoutTransition
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.FrameLayout
import androidx.core.content.withStyledAttributes
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.R

/**
 * FragmentContainerView is a customized Layout designed specifically for Fragments. It extends
 * [FrameLayout], so it can reliably handle Fragment Transactions, and it also has additional
 * features to coordinate with fragment behavior.
 *
 * FragmentContainerView should be used as the container for Fragments, commonly set in the xml
 * layout of an activity, e.g.:
 * ```
 * <androidx.fragment.app.FragmentContainerView
 * xmlns:android="http://schemas.android.com/apk/res/android"
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * android:id="@+id/fragment_container_view"
 * android:layout_width="match_parent"
 * android:layout_height="match_parent">
 * </androidx.fragment.app.FragmentContainerView>
 * ```
 *
 * FragmentContainerView can also be used to add a Fragment by using the `android:name` attribute.
 * FragmentContainerView will perform a one time operation that:
 * 1. Creates a new instance of the Fragment
 * 2. Calls [Fragment.onInflate]
 * 3. Executes a FragmentTransaction to add the Fragment to the appropriate FragmentManager
 *
 * You can optionally include an `android:tag` which allows you to use
 * [FragmentManager.findFragmentByTag] to retrieve the added Fragment.
 *
 * ```
 * <androidx.fragment.app.FragmentContainerView
 * xmlns:android="http://schemas.android.com/apk/res/android"
 * xmlns:app="http://schemas.android.com/apk/res-auto"
 * android:id="@+id/fragment_container_view"
 * android:layout_width="match_parent"
 * android:layout_height="match_parent"
 * android:name="com.example.MyFragment"
 * android:tag="my_tag">
 * </androidx.fragment.app.FragmentContainerView>
 * ```
 *
 * FragmentContainerView should not be used as a replacement for other ViewGroups (FrameLayout,
 * LinearLayout, etc) outside of Fragment use cases.
 *
 * FragmentContainerView will only allow views returned by a Fragment's [Fragment.onCreateView].
 * Attempting to add any other view will result in an [IllegalStateException].
 *
 * Layout animations and transitions are disabled for FragmentContainerView for APIs above 17.
 * Otherwise, Animations should be done through [FragmentTransaction.setCustomAnimations]. If
 * animateLayoutChanges is set to `true` or [setLayoutTransition] is called directly an
 * [UnsupportedOperationException] will be thrown.
 *
 * Fragments using exit animations are drawn before all others for FragmentContainerView. This
 * ensures that exiting Fragments do not appear on top of the view.
 */
public class FragmentContainerView : FrameLayout {
    private val disappearingFragmentChildren: MutableList<View> = mutableListOf()
    private val transitioningFragmentViews: MutableList<View> = mutableListOf()
    private var applyWindowInsetsListener: OnApplyWindowInsetsListener? = null

    // Used to indicate whether the FragmentContainerView should override the default ViewGroup
    // drawing order.
    private var drawDisappearingViewsFirst = true

    public constructor(context: Context) : super(context)

    /**
     * Do not call this constructor directly. Doing so will result in an
     * [UnsupportedOperationException].
     */
    @JvmOverloads
    public constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int = 0,
    ) : super(context, attrs, defStyleAttr) {
        if (attrs != null) {
            var name = attrs.classAttribute
            var attribute = "class"
            context.withStyledAttributes(attrs, R.styleable.FragmentContainerView) {
                if (name == null) {
                    name = getString(R.styleable.FragmentContainerView_android_name)
                    attribute = "android:name"
                }
            }
            if (name != null && !isInEditMode) {
                throw UnsupportedOperationException(
                    "FragmentContainerView must be within a FragmentActivity to use $attribute" +
                        "=\"$name\""
                )
            }
        }
    }

    internal constructor(
        context: Context,
        attrs: AttributeSet,
        fm: FragmentManager,
    ) : super(context, attrs) {
        var name = attrs.classAttribute
        var tag: String? = null
        context.withStyledAttributes(attrs, R.styleable.FragmentContainerView) {
            if (name == null) {
                name = getString(R.styleable.FragmentContainerView_android_name)
            }
            tag = getString(R.styleable.FragmentContainerView_android_tag)
        }
        val id = id
        val existingFragment: Fragment? = fm.findFragmentById(id)
        // If there is a name and there is no existing fragment,
        // we should add an inflated Fragment to the view.
        if (name != null && existingFragment == null) {
            if (id == View.NO_ID) {
                val tagMessage = if (tag != null) " with tag $tag" else ""
                throw IllegalStateException(
                    "FragmentContainerView must have an android:id to add Fragment $name$tagMessage"
                )
            }
            val containerFragment: Fragment =
                fm.fragmentFactory.instantiate(context.classLoader, name)
            containerFragment.mFragmentId = id
            containerFragment.mContainerId = id
            containerFragment.mTag = tag
            containerFragment.mFragmentManager = fm
            containerFragment.mHost = fm.host
            containerFragment.onInflate(context, attrs, null)
            fm.beginTransaction()
                .setReorderingAllowed(true)
                .add(this, containerFragment, tag)
                .commitNowAllowingStateLoss()
        }
        fm.onContainerAvailable(this)
    }

    /**
     * When called, this method throws a [UnsupportedOperationException] on APIs above 17. On APIs
     * 17 and below, it calls [FrameLayout.setLayoutTransition]. This can be called either
     * explicitly, or implicitly by setting animateLayoutChanges to `true`.
     *
     * View animations and transitions are disabled for FragmentContainerView for APIs above 17. Use
     * [FragmentTransaction.setCustomAnimations] and [FragmentTransaction.setTransition].
     *
     * @param transition The LayoutTransition object that will animated changes in layout. A value
     *   of `null` means no transition will run on layout changes.
     * @attr ref android.R.styleable#ViewGroup_animateLayoutChanges
     */
    public override fun setLayoutTransition(transition: LayoutTransition?) {
        throw UnsupportedOperationException(
            "FragmentContainerView does not support Layout Transitions or " +
                "animateLayoutChanges=\"true\"."
        )
    }

    public override fun setOnApplyWindowInsetsListener(listener: OnApplyWindowInsetsListener?) {
        applyWindowInsetsListener = listener
    }

    public override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets = insets

    /**
     * {@inheritDoc}
     *
     * The sys ui flags must be set to enable extending the layout into the window insets.
     */
    public override fun dispatchApplyWindowInsets(insets: WindowInsets): WindowInsets {
        val insetsCompat = WindowInsetsCompat.toWindowInsetsCompat(insets)
        val dispatchInsets =
            if (applyWindowInsetsListener != null) {
                WindowInsetsCompat.toWindowInsetsCompat(
                    applyWindowInsetsListener!!.onApplyWindowInsets(this, insets)
                )
            } else {
                ViewCompat.onApplyWindowInsets(this, insetsCompat)
            }
        if (!dispatchInsets.isConsumed) {
            for (i in 0 until childCount) {
                ViewCompat.dispatchApplyWindowInsets(getChildAt(i), dispatchInsets)
            }
        }
        return insets
    }

    protected override fun dispatchDraw(canvas: Canvas) {
        if (drawDisappearingViewsFirst) {
            disappearingFragmentChildren.forEach { child ->
                super.drawChild(canvas, child, drawingTime)
            }
        }
        super.dispatchDraw(canvas)
    }

    protected override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
        if (drawDisappearingViewsFirst && disappearingFragmentChildren.isNotEmpty()) {
            // If the child is disappearing, we have already drawn it so skip.
            if (disappearingFragmentChildren.contains(child)) {
                return false
            }
        }
        return super.drawChild(canvas, child, drawingTime)
    }

    public override fun startViewTransition(view: View) {
        if (view.parent === this) {
            transitioningFragmentViews.add(view)
        }
        super.startViewTransition(view)
    }

    public override fun endViewTransition(view: View) {
        transitioningFragmentViews.remove(view)
        if (disappearingFragmentChildren.remove(view)) {
            drawDisappearingViewsFirst = true
        }
        super.endViewTransition(view)
    }

    // Used to indicate the container should change the default drawing order.
    @JvmName("setDrawDisappearingViewsLast")
    internal fun setDrawDisappearingViewsLast(drawDisappearingViewsFirst: Boolean) {
        this.drawDisappearingViewsFirst = drawDisappearingViewsFirst
    }

    /**
     * FragmentContainerView will only allow views returned by a Fragment's [Fragment.onCreateView].
     * Attempting to add any other view will result in an [IllegalStateException].
     *
     * {@inheritDoc}
     */
    public override fun addView(child: View, index: Int, params: ViewGroup.LayoutParams?) {
        checkNotNull(FragmentManager.getViewFragment(child)) {
            ("Views added to a FragmentContainerView must be associated with a Fragment. View " +
                "$child is not associated with a Fragment.")
        }
        super.addView(child, index, params)
    }

    public override fun removeViewAt(index: Int) {
        val view = getChildAt(index)
        addDisappearingFragmentView(view)
        super.removeViewAt(index)
    }

    public override fun removeViewInLayout(view: View) {
        addDisappearingFragmentView(view)
        super.removeViewInLayout(view)
    }

    public override fun removeView(view: View) {
        addDisappearingFragmentView(view)
        super.removeView(view)
    }

    public override fun removeViews(start: Int, count: Int) {
        for (i in start until start + count) {
            val view = getChildAt(i)
            addDisappearingFragmentView(view)
        }
        super.removeViews(start, count)
    }

    public override fun removeViewsInLayout(start: Int, count: Int) {
        for (i in start until start + count) {
            val view = getChildAt(i)
            addDisappearingFragmentView(view)
        }
        super.removeViewsInLayout(start, count)
    }

    public override fun removeAllViewsInLayout() {
        for (i in childCount - 1 downTo 0) {
            val view = getChildAt(i)
            addDisappearingFragmentView(view)
        }
        super.removeAllViewsInLayout()
    }

    /**
     * This method adds a [View] to the list of disappearing views only if it meets the proper
     * conditions to be considered a disappearing view.
     *
     * @param v [View] that might be added to list of disappearing views
     */
    private fun addDisappearingFragmentView(v: View) {
        if (transitioningFragmentViews.contains(v)) {
            disappearingFragmentChildren.add(v)
        }
    }

    /**
     * This method grabs the [Fragment] whose view was most recently added to the container. This
     * may used as an alternative to calling [FragmentManager.findFragmentById] and passing in the
     * [FragmentContainerView]'s id.
     *
     * @return The fragment if any exist, null otherwise.
     */
    @Suppress("UNCHECKED_CAST") // a ClassCastException is automatically thrown if the given type
    // of F is wrong
    public fun <F : Fragment?> getFragment(): F =
        FragmentManager.findFragmentManager(this).findFragmentById(this.id) as F
}
