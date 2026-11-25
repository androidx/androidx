/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.core.view

import android.app.Activity
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import java.lang.ref.ReferenceQueue
import java.lang.ref.WeakReference
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.robolectric.util.ReflectionHelpers.ClassParameter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, Config.TARGET_SDK])
class ViewTreeObserverListenersTest {
    private lateinit var decorView: View
    private lateinit var content: ViewGroup
    private lateinit var owner: TestLifecycleOwner

    /** Number of times the listener under test was called. */
    private var calls = 0

    @Before
    fun setUp() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        content = FrameLayout(activity)
        activity.setContentView(content)
        decorView = activity.window.decorView
        shadowOf(Looper.getMainLooper()).idle()
        assertThat(content.isAttachedToWindow).isTrue()

        owner = TestLifecycleOwner()
    }

    // Lifecycle scope

    @Test
    fun lifecycleScope_removesListenerWhenDestroyed() {
        val registration = decorView.addOnGlobalLayoutListener(owner) { calls++ }
        assertThat(owner.registry.observerCount).isEqualTo(1)

        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)

        owner.destroy()
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
        assertThat(owner.registry.observerCount).isEqualTo(0)
        registration.remove() // Does nothing.
    }

    @Test
    fun lifecycleScope_alreadyDestroyed_doesNotAddListener() {
        owner.destroy()

        val registration = decorView.addOnGlobalLayoutListener(owner) { calls++ }
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(0)
        assertThat(owner.registry.observerCount).isEqualTo(0)
        registration.remove() // Does nothing.
    }

    @Test
    fun lifecycleScope_viewDetachedBeforeDestroy_removesListenerFromWindow() {
        val view = View(content.context)
        content.addView(view)
        view.addOnGlobalLayoutListener(owner) { calls++ }

        // The window's observer keeps the listener of a view that was detached. Looking up the
        // observer of the detached view at this point would return a new, empty observer.
        content.removeView(view)
        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)

        owner.destroy()
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun lifecycleScope_viewAttachedAfterAdd_removesListenerFromWindow() {
        val view = View(content.context)
        val floatingObserver = view.viewTreeObserver
        view.addOnGlobalLayoutListener(owner) { calls++ }

        // Attaching merges the view's floating observer into the window's observer and kills it.
        content.addView(view)
        assertThat(floatingObserver.isAlive).isFalse()
        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)

        owner.destroy()
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun lifecycleScope_viewAttachedAndDetachedAfterAdd_removesListenerFromWindow() {
        val view = View(content.context)
        view.addOnGlobalLayoutListener(owner) { calls++ }

        // The listener moves to the window's observer on attach and stays there after detach.
        content.addView(view)
        content.removeView(view)
        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)

        owner.destroy()
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun lifecycleScope_destroyedWhileViewIsBeingAttached_removesListenerFromWindow() {
        val view = View(content.context)
        // Runs during the attach, after the floating observer was merged and killed, and before
        // any attach listener that was added later.
        view.doOnAttach { owner.destroy() }
        view.addOnGlobalLayoutListener(owner) { calls++ }

        content.addView(view)
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun lifecycleScope_viewNeverAttached_removesListener() {
        val view = View(content.context)
        view.addOnGlobalLayoutListener(owner) { calls++ }
        view.viewTreeObserver.dispatchOnGlobalLayout()
        assertThat(calls).isEqualTo(1)

        owner.destroy()
        view.viewTreeObserver.dispatchOnGlobalLayout()
        assertThat(calls).isEqualTo(1)

        // Nothing is left to merge into the window.
        content.addView(view)
        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun lifecycleScope_sameListenerAddedTwice_removesBoth() {
        val listener = ViewTreeObserver.OnGlobalLayoutListener { calls++ }
        decorView.addOnGlobalLayoutListener(owner, listener)
        decorView.addOnGlobalLayoutListener(owner, listener)

        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(2)

        owner.destroy()
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(2)
        assertThat(owner.registry.observerCount).isEqualTo(0)
    }

    // View scope

    @Test
    fun viewScope_removesListenerWhenDetached() {
        val scope = View(content.context)
        content.addView(scope)
        decorView.addOnGlobalLayoutListener(scope) { calls++ }

        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)

        content.removeView(scope)
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun viewScope_reattached_doesNotAddListenerBack() {
        val scope = View(content.context)
        content.addView(scope)
        decorView.addOnGlobalLayoutListener(scope) { calls++ }

        content.removeView(scope)
        content.addView(scope)
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun viewScope_attachedAfterAdd_removesListenerWhenDetached() {
        val scope = View(content.context)
        decorView.addOnGlobalLayoutListener(scope) { calls++ }

        content.addView(scope)
        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)

        content.removeView(scope)
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun viewScope_sameViewAttachedAfterAdd_removesListenerFromWindow() {
        val view = View(content.context)
        view.addOnGlobalLayoutListener(view) { calls++ }

        content.addView(view)
        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)

        content.removeView(view)
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
    }

    // Registration

    @Test
    fun remove_removesListenerAndStopsObservingScope() {
        val registration = decorView.addOnGlobalLayoutListener(owner) { calls++ }
        dispatchGlobalLayout()
        assertThat(calls).isEqualTo(1)

        registration.remove()
        registration.remove() // Does nothing.
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
        assertThat(owner.registry.observerCount).isEqualTo(0)
        owner.destroy() // Does nothing.
    }

    @Test
    fun remove_removesOnlyItsOwnListener() {
        val listener = ViewTreeObserver.OnGlobalLayoutListener { calls++ }
        val first = decorView.addOnGlobalLayoutListener(owner, listener)
        decorView.addOnGlobalLayoutListener(owner, listener)

        first.remove()
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
        assertThat(owner.registry.observerCount).isEqualTo(1)
    }

    @Test
    fun remove_calledFromListener_removesListener() {
        lateinit var registration: ViewTreeObserverRegistration
        registration =
            decorView.addOnGlobalLayoutListener(owner) {
                calls++
                registration.remove()
            }

        dispatchGlobalLayout()
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun remove_viewNeverAttached_removesListener() {
        val view = View(content.context)
        val registration = view.addOnGlobalLayoutListener(owner) { calls++ }

        registration.remove()
        content.addView(view)
        dispatchGlobalLayout()

        assertThat(calls).isEqualTo(0)
    }

    @Test
    fun registration_heldInFragmentFieldAfterDestroy_doesNotRetainViewOrLifecycle() {
        val fragment = FakeFragment()
        val (fragmentViewTarget, lifecycleTarget) = attachAndDestroyFragmentView(fragment)

        assertWithMessage("Dead registration retained destroyed fragmentView")
            .that(fragmentViewTarget.awaitGc())
            .isTrue()
        assertWithMessage("Dead registration retained destroyed viewLifecycleOwner.lifecycle")
            .that(lifecycleTarget.awaitGc())
            .isTrue()
        assertThat(fragment.layoutRegistration).isNotNull()
    }

    @Test
    fun registration_heldInFieldAfterViewDetach_doesNotRetainTargetOrScopeView() {
        val fragment = FakeFragment()
        val (targetViewTarget, scopeViewTarget) = attachAndDetachViewScoped(fragment)

        assertWithMessage("Dead registration retained targetView")
            .that(targetViewTarget.awaitGc())
            .isTrue()
        assertWithMessage("Dead registration retained scopeView")
            .that(scopeViewTarget.awaitGc())
            .isTrue()
        assertThat(fragment.layoutRegistration).isNotNull()
    }

    // Every listener type

    @Test
    fun everyListenerType_lifecycleScope_removesListenerWhenDestroyed() {
        for (type in LISTENER_TYPES) {
            val owner = TestLifecycleOwner()
            var calls = 0
            type.addWithLifecycleOwner(decorView, owner) { calls++ }

            type.dispatch(decorView.viewTreeObserver, decorView)
            assertWithMessage(type.name).that(calls).isEqualTo(1)

            owner.destroy()
            type.dispatch(decorView.viewTreeObserver, decorView)

            assertWithMessage(type.name).that(calls).isEqualTo(1)
            assertWithMessage(type.name).that(owner.registry.observerCount).isEqualTo(0)
        }
    }

    @Test
    fun everyListenerType_viewScope_removesListenerWhenDetached() {
        for (type in LISTENER_TYPES) {
            val scope = View(content.context)
            content.addView(scope)
            var calls = 0
            type.addWithView(decorView, scope) { calls++ }

            type.dispatch(decorView.viewTreeObserver, decorView)
            assertWithMessage(type.name).that(calls).isEqualTo(1)

            content.removeView(scope)
            type.dispatch(decorView.viewTreeObserver, decorView)

            assertWithMessage(type.name).that(calls).isEqualTo(1)
        }
    }

    @Test
    fun everyListenerType_remove_removesListener() {
        for (type in LISTENER_TYPES) {
            var calls = 0
            val registration = type.addWithLifecycleOwner(decorView, owner) { calls++ }

            registration.remove()
            type.dispatch(decorView.viewTreeObserver, decorView)

            assertWithMessage(type.name).that(calls).isEqualTo(0)
        }
    }

    private fun dispatchGlobalLayout() = decorView.viewTreeObserver.dispatchOnGlobalLayout()

    private fun attachAndDestroyFragmentView(
        fragment: FakeFragment
    ): Pair<GcTarget<View>, GcTarget<Lifecycle>> {
        val fragmentView = View(content.context)
        content.addView(fragmentView)
        val viewLifecycleOwner = TestLifecycleOwner()

        fragment.startObserving(fragmentView, viewLifecycleOwner)
        dispatchGlobalLayout()
        assertThat(fragment.overlayUpdates).isEqualTo(1)

        // Simulate Fragment moving to the back stack (onDestroyView):
        // viewLifecycleOwner is destroyed and fragmentView is detached, while `fragment`
        // (and its `layoutRegistration` field) remains alive.
        viewLifecycleOwner.destroy()
        content.removeView(fragmentView)
        dispatchGlobalLayout()
        assertThat(fragment.overlayUpdates).isEqualTo(1)

        return GcTarget(fragmentView) to GcTarget(viewLifecycleOwner.registry)
    }

    private fun attachAndDetachViewScoped(
        fragment: FakeFragment
    ): Pair<GcTarget<View>, GcTarget<View>> {
        val targetView = View(content.context)
        val scopeView = View(content.context)
        content.addView(targetView)
        content.addView(scopeView)

        fragment.startObservingWithViewScope(targetView, scopeView)
        dispatchGlobalLayout()
        assertThat(fragment.overlayUpdates).isEqualTo(1)

        content.removeView(scopeView)
        content.removeView(targetView)
        dispatchGlobalLayout()
        assertThat(fragment.overlayUpdates).isEqualTo(1)

        return GcTarget(targetView) to GcTarget(scopeView)
    }

    private class GcTarget<T>(referent: T) {
        private val queue = ReferenceQueue<T>()
        private val ref = WeakReference(referent, queue)

        fun awaitGc(maxAttempts: Int = 20): Boolean {
            repeat(maxAttempts) {
                if (ref.get() == null) return true
                System.gc()
                if (queue.remove(50) != null || ref.get() == null) return true
            }
            return ref.get() == null
        }
    }

    /** Mirrors a Fragment kept on the back stack after onDestroyView(). */
    private class FakeFragment {
        var layoutRegistration: ViewTreeObserverRegistration? = null
        var overlayUpdates = 0

        fun startObserving(fragmentView: View, viewLifecycleOwner: LifecycleOwner) {
            layoutRegistration?.remove()
            layoutRegistration =
                fragmentView.addOnGlobalLayoutListener(viewLifecycleOwner) {
                    updateOverlayPosition()
                }
        }

        fun startObservingWithViewScope(targetView: View, scopeView: View) {
            layoutRegistration?.remove()
            layoutRegistration =
                targetView.addOnGlobalLayoutListener(scopeView) { updateOverlayPosition() }
        }

        private fun updateOverlayPosition() {
            overlayUpdates++
        }
    }

    private class TestLifecycleOwner : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }

        override val lifecycle: Lifecycle
            get() = registry

        fun destroy() {
            registry.currentState = Lifecycle.State.DESTROYED
        }
    }

    /** One of the listener types of [ViewTreeObserver], with both ways to add it with a scope. */
    private class ListenerType(
        val name: String,
        val addWithLifecycleOwner:
            View.(scope: LifecycleOwner, onCall: () -> Unit) -> ViewTreeObserverRegistration,
        val addWithView: View.(scope: View, onCall: () -> Unit) -> ViewTreeObserverRegistration,
        /** Makes [ViewTreeObserver] call its listeners of this type. */
        val dispatch: ViewTreeObserver.(view: View) -> Unit,
    )

    private companion object {
        val LISTENER_TYPES =
            listOf(
                ListenerType(
                    name = "OnGlobalLayoutListener",
                    addWithLifecycleOwner = { scope, onCall ->
                        addOnGlobalLayoutListener(scope) { onCall() }
                    },
                    addWithView = { scope, onCall ->
                        addOnGlobalLayoutListener(scope) { onCall() }
                    },
                    dispatch = { dispatchOnGlobalLayout() },
                ),
                ListenerType(
                    name = "OnPreDrawListener",
                    addWithLifecycleOwner = { scope, onCall ->
                        addOnPreDrawListener(scope) {
                            onCall()
                            true
                        }
                    },
                    addWithView = { scope, onCall ->
                        addOnPreDrawListener(scope) {
                            onCall()
                            true
                        }
                    },
                    dispatch = { dispatchOnPreDraw() },
                ),
                ListenerType(
                    name = "OnDrawListener",
                    addWithLifecycleOwner = { scope, onCall ->
                        addOnDrawListener(scope) { onCall() }
                    },
                    addWithView = { scope, onCall -> addOnDrawListener(scope) { onCall() } },
                    dispatch = { dispatchOnDraw() },
                ),
                ListenerType(
                    name = "OnScrollChangedListener",
                    addWithLifecycleOwner = { scope, onCall ->
                        addOnScrollChangedListener(scope) { onCall() }
                    },
                    addWithView = { scope, onCall ->
                        addOnScrollChangedListener(scope) { onCall() }
                    },
                    dispatch = { callHidden("dispatchOnScrollChanged") },
                ),
                ListenerType(
                    name = "OnGlobalFocusChangeListener",
                    addWithLifecycleOwner = { scope, onCall ->
                        addOnGlobalFocusChangeListener(scope) { _, _ -> onCall() }
                    },
                    addWithView = { scope, onCall ->
                        addOnGlobalFocusChangeListener(scope) { _, _ -> onCall() }
                    },
                    dispatch = { view ->
                        callHidden(
                            "dispatchOnGlobalFocusChange",
                            ClassParameter.from(View::class.java, view),
                            ClassParameter.from(View::class.java, view),
                        )
                    },
                ),
                ListenerType(
                    name = "OnTouchModeChangeListener",
                    addWithLifecycleOwner = { scope, onCall ->
                        addOnTouchModeChangeListener(scope) { onCall() }
                    },
                    addWithView = { scope, onCall ->
                        addOnTouchModeChangeListener(scope) { onCall() }
                    },
                    dispatch = {
                        callHidden(
                            "dispatchOnTouchModeChanged",
                            ClassParameter.from(Boolean::class.java, true),
                        )
                    },
                ),
                ListenerType(
                    name = "OnWindowAttachListener",
                    addWithLifecycleOwner = { scope, onCall ->
                        addOnWindowAttachListener(scope, windowAttachListener(onCall))
                    },
                    addWithView = { scope, onCall ->
                        addOnWindowAttachListener(scope, windowAttachListener(onCall))
                    },
                    dispatch = {
                        callHidden(
                            "dispatchOnWindowAttachedChange",
                            ClassParameter.from(Boolean::class.java, true),
                        )
                    },
                ),
                ListenerType(
                    name = "OnWindowFocusChangeListener",
                    addWithLifecycleOwner = { scope, onCall ->
                        addOnWindowFocusChangeListener(scope) { onCall() }
                    },
                    addWithView = { scope, onCall ->
                        addOnWindowFocusChangeListener(scope) { onCall() }
                    },
                    dispatch = {
                        callHidden(
                            "dispatchOnWindowFocusChange",
                            ClassParameter.from(Boolean::class.java, true),
                        )
                    },
                ),
            )

        fun windowAttachListener(onCall: () -> Unit) =
            object : ViewTreeObserver.OnWindowAttachListener {
                override fun onWindowAttached() = onCall()

                override fun onWindowDetached() {}
            }

        /** Calls a dispatch method of [ViewTreeObserver] that is not part of the SDK. */
        fun ViewTreeObserver.callHidden(method: String, vararg parameters: ClassParameter<*>) {
            ReflectionHelpers.callInstanceMethod<Any?>(this, method, *parameters)
        }
    }
}
