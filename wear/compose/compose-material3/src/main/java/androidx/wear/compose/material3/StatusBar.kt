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

package androidx.wear.compose.material3

import android.content.Context
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowInsets
import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalWithComputedDefaultOf
import androidx.compose.ui.platform.LocalContext
import androidx.wear.utils.WearApiVersionHelper
import com.google.wear.settings.WearSettings

/** Represents the status bar display mode for a screen within [ScreenScaffold]. */
@Immutable
@JvmInline
public value class StatusBarMode internal constructor(internal val value: Int) {
    public companion object {
        /**
         * Inherits the status bar mode from the underlying screen in the screen stack, or falls
         * back to the [AppScaffold] setting if no screen in the stack specifies a mode.
         */
        public val Inherit: StatusBarMode = StatusBarMode(0)

        /** Explicitly enables the system status bar overlay for this screen. */
        public val Enabled: StatusBarMode = StatusBarMode(1)

        /** Explicitly disables the system status bar overlay for this screen. */
        public val Disabled: StatusBarMode = StatusBarMode(2)
    }

    override fun toString(): String =
        when (this) {
            Inherit -> "Inherit"
            Enabled -> "Enabled"
            Disabled -> "Disabled"
            else -> "Unknown"
        }
}

/** [CompositionLocal] to determine if the system status bar is enabled on the device. */
public val LocalStatusBarEnabled: CompositionLocal<Boolean> =
    compositionLocalWithComputedDefaultOf {
        isStatusBarEnabled(LocalContext.currentValue.applicationContext)
    }

private const val TAG = "StatusBar"
// TODO(b/548550572): Replace with WearApiVersionHelper.WEAR_CINNAMON_BUN_2 once wear-core
// releases with it.
private const val WEAR_CINNAMON_BUN_2 = "WEAR_CINNAMON_BUN_2"

/** Checks if the global status bar is enabled for the given application [Context]. */
internal fun isStatusBarEnabled(context: Context): Boolean =
    try {
        WearApiVersionHelper.isApiVersionAtLeast(WEAR_CINNAMON_BUN_2) &&
            WearSettings.isStatusBarEnabled(context)
    } catch (e: Throwable) {
        false
    }

/**
 * Orchestrates calls directly to the view's window insets controller for managing status bar
 * visibility.
 *
 * This class tracks the initial status bar visibility prior to any scaffold-driven modifications
 * (e.g. scroll-away or hiding) and restores that initial state when the scaffold is disposed.
 */
internal interface StatusBarOrchestrator {
    /** Requests the status bar overlay to be displayed. */
    fun show()

    /** Requests the status bar overlay to be hidden. */
    fun hide()

    /**
     * Restores the status bar visibility back to the initial state prior to scaffold modifications.
     */
    fun restore()
}

internal fun StatusBarOrchestrator(view: View): StatusBarOrchestrator =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        StatusBarOrchestratorImpl(view)
    } else {
        NoOpStatusBarOrchestrator
    }

private object NoOpStatusBarOrchestrator : StatusBarOrchestrator {
    override fun show() {}

    override fun hide() {}

    override fun restore() {}
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.R)
private class StatusBarOrchestratorImpl(private val view: View) : StatusBarOrchestrator {
    /**
     * The original system status bar visibility state prior to any scaffold-driven modifications.
     *
     * Captured once from [View.getRootWindowInsets] as soon as root insets become available on the
     * host [view]. This value remains fixed once captured and serves as the baseline target
     * restored during [restore] when the scaffold leaves composition.
     */
    private var initialStatusBarVisibility: Boolean? = null

    /**
     * The target status bar visibility requested by Compose (true for show, false for hide).
     * Applied once the host view is attached and baseline initial status bar visibility is secured.
     */
    private var desiredStatusBarVisibility: Boolean? = null

    /**
     * Listener attached to [view] when initialized before window attachment.
     *
     * Lifecycle & Management:
     * - Registered via [View.addOnAttachStateChangeListener] in `init` if [View.isAttachedToWindow]
     *   is `false`.
     * - When [View.OnAttachStateChangeListener.onViewAttachedToWindow] is invoked, it calls
     *   [attemptCaptureAndApply].
     * - Automatically unregistered and set to `null` once
     *   [isStatusBarBaselineCapturedAndControllerReady] becomes `true` (meaning both initial
     *   baseline status bar visibility is captured and [View.getWindowInsetsController] is bound).
     * - Also explicitly unregistered during [restore] if disposal occurs prior to window
     *   attachment.
     */
    private var onAttachStateChangeListener: View.OnAttachStateChangeListener? = null

    /**
     * Listener attached to [view] to observe layout passes for asynchronous insets and controller
     * availability.
     *
     * Lifecycle & Management:
     * - Registered via [View.addOnLayoutChangeListener] in `init` if
     *   [isStatusBarBaselineCapturedAndControllerReady] is `false`.
     * - Window insets and [View.getWindowInsetsController] handles are often dispatched
     *   asynchronously during initial layout passes right after window attachment.
     * - On every [View.OnLayoutChangeListener.onLayoutChange] callback, it calls
     *   [attemptCaptureAndApply] to re-evaluate baseline capture and apply any pending
     *   [desiredStatusBarVisibility].
     * - Retained across layout passes until [isStatusBarBaselineCapturedAndControllerReady]
     *   evaluates to `true`, ensuring that visibility requests are never lost if
     *   [View.getWindowInsetsController] is `null` on early layout passes.
     * - Automatically unregistered and set to `null` once
     *   [isStatusBarBaselineCapturedAndControllerReady] becomes `true`, or explicitly unregistered
     *   during [restore].
     */
    private var onLayoutChangeListener: View.OnLayoutChangeListener? = null

    /**
     * Determines whether the orchestrator has both captured the initial system baseline status bar
     * visibility and verified that the [View.getWindowInsetsController] is available to apply
     * mutations.
     *
     * View listeners ([onAttachStateChangeListener] and [onLayoutChangeListener]) remain active
     * until this evaluates to `true`, guaranteeing that late controller binding or delayed insets
     * dispatch will not drop visibility updates.
     */
    private val isStatusBarBaselineCapturedAndControllerReady: Boolean
        get() = initialStatusBarVisibility != null && view.windowInsetsController != null

    init {
        // Attempt initial capture and application immediately in case the view is already attached
        // with insets available.
        attemptCaptureAndApply()

        // If initial status bar baseline is not captured OR WindowInsetsController is not ready,
        // attach listeners to observe window attachment and layout passes until both requirements
        // are satisfied.
        if (!isStatusBarBaselineCapturedAndControllerReady) {
            // 1. Observe window attachment if the view is unattached during initial composition.
            if (!view.isAttachedToWindow) {
                val windowAttachListener =
                    object : View.OnAttachStateChangeListener {
                        override fun onViewAttachedToWindow(v: View) {
                            // On window attachment, retry capturing status bar baseline and
                            // applying requested visibility.
                            attemptCaptureAndApply()
                            // Unregister and cleanup listener once initial status bar baseline is
                            // captured and controller is ready.
                            if (isStatusBarBaselineCapturedAndControllerReady) {
                                v.removeOnAttachStateChangeListener(this)
                                onAttachStateChangeListener = null
                            }
                        }

                        override fun onViewDetachedFromWindow(v: View) {}
                    }
                onAttachStateChangeListener = windowAttachListener
                view.addOnAttachStateChangeListener(windowAttachListener)
            }

            // 2. Window insets and WindowInsetsController handles can be dispatched asynchronously
            // during layout passes.
            // Observe layout passes to capture unmodified system status bar insets as soon as they
            // arrive.
            val windowLayoutListener =
                object : View.OnLayoutChangeListener {
                    override fun onLayoutChange(
                        v: View,
                        left: Int,
                        top: Int,
                        right: Int,
                        bottom: Int,
                        oldLeft: Int,
                        oldTop: Int,
                        oldRight: Int,
                        oldBottom: Int,
                    ) {
                        // On layout pass, retry capturing status bar baseline and applying
                        // requested visibility.
                        attemptCaptureAndApply()
                        // Unregister and cleanup listener once initial status bar baseline is
                        // captured and controller is ready.
                        if (isStatusBarBaselineCapturedAndControllerReady) {
                            v.removeOnLayoutChangeListener(this)
                            onLayoutChangeListener = null
                        }
                    }
                }
            onLayoutChangeListener = windowLayoutListener
            view.addOnLayoutChangeListener(windowLayoutListener)
        }
    }

    override fun show() {
        // Store requested visibility (show) and attempt to apply it to the status bar.
        desiredStatusBarVisibility = true
        attemptCaptureAndApply()
    }

    override fun hide() {
        // Store requested visibility (hide) and attempt to apply it to the status bar.
        desiredStatusBarVisibility = false
        attemptCaptureAndApply()
    }

    /**
     * Restores the status bar visibility back to the initial system state captured prior to any
     * scaffold modifications.
     *
     * Lifecycle & Cleanup Operations:
     * 1. **Listener Cleanup**: Immediately unregisters any active [onAttachStateChangeListener] and
     *    [onLayoutChangeListener] from [view] to prevent memory leaks and prevent asynchronous view
     *    callbacks from executing after disposal.
     * 2. **Baseline State Restoration**: Reads [initialStatusBarVisibility], which holds the
     *    unmodified system status bar state captured when the view first received window insets. If
     *    [initialStatusBarVisibility] was never captured (e.g. if the view was never attached to a
     *    window before disposal), no status bar mutation is attempted.
     * 3. **Window Mutation & Exception Handling**: Calls [android.view.WindowInsetsController.show]
     *    or [android.view.WindowInsetsController.hide] on [View.getWindowInsetsController] to
     *    revert window state. Runtime exceptions thrown by framework or OEM controller
     *    implementations are safely caught and logged to prevent app crashes during disposal.
     */
    override fun restore() {
        // Step 1: Immediately unregister and clear active view listeners to prevent memory leaks or
        // late execution after disposal.
        onAttachStateChangeListener?.let {
            view.removeOnAttachStateChangeListener(it)
            onAttachStateChangeListener = null
        }
        onLayoutChangeListener?.let {
            view.removeOnLayoutChangeListener(it)
            onLayoutChangeListener = null
        }

        // Step 2: Read initial system status bar visibility baseline. If baseline was never
        // captured, exit safely without mutation.
        val visible = initialStatusBarVisibility ?: return

        // Step 3: Revert system status bar visibility back to its original baseline state.
        try {
            if (visible) {
                view.windowInsetsController?.show(WindowInsets.Type.statusBars())
            } else {
                view.windowInsetsController?.hide(WindowInsets.Type.statusBars())
            }
        } catch (e: Exception) {
            // Swallowing exceptions here as WindowInsetsController is a newer API whose
            // platform implementations across OEM hardware may throw unexpected runtime
            // exceptions. We avoid crashing the app due to framework controller issues.
            Log.w(TAG, "Failed to restore status bar", e)
        }
    }

    /**
     * Safely attempts to secure the initial system baseline state, and only if successful, applies
     * the current desired visibility state requested by Compose.
     */
    private fun attemptCaptureAndApply() {
        // Step 1: Secure initial system status bar baseline visibility before performing any window
        // modifications.
        if (initialStatusBarVisibility == null) {
            val insets = view.rootWindowInsets
            if (insets != null) {
                initialStatusBarVisibility = insets.isVisible(WindowInsets.Type.statusBars())
            }
        }

        // Step 2: Only mutate status bar visibility if baseline is captured and
        // WindowInsetsController is available.
        if (initialStatusBarVisibility != null) {
            val targetVisibility = desiredStatusBarVisibility ?: return
            val controller = view.windowInsetsController ?: return

            try {
                if (targetVisibility) {
                    controller.show(WindowInsets.Type.statusBars())
                } else {
                    controller.hide(WindowInsets.Type.statusBars())
                }
            } catch (e: Exception) {
                // Swallowing exceptions here as WindowInsetsController is a newer API whose
                // platform implementations across OEM hardware may throw unexpected runtime
                // exceptions. We avoid crashing the app due to framework controller issues.
                Log.w(TAG, "Failed to change status bar visibility", e)
            }
        }
    }
}
