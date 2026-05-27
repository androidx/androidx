/*
 * Copyright 2024 The Android Open Source Project
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
@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.layout

import android.os.Build
import android.view.View
import android.view.View.OnAttachStateChangeListener
import androidx.collection.MutableIntObjectMap
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.R
import androidx.compose.ui.util.fastForEach
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsAnimationCompat
import androidx.core.view.WindowInsetsAnimationCompat.BoundsCompat
import androidx.core.view.WindowInsetsCompat

internal interface WindowInsetsRulerProvider {
    val insetsProvider: WindowInsetsRulersProvider
}

/**
 * A listener for WindowInsets changes. This updates the [currentInsets] values whenever values
 * change and allows access to [findAnimation] and [findAnimationPositions] to be used for
 * WindowInsetsAnimation access.
 */
internal class WindowInsetsWatcher(val view: View) :
    WindowInsetsAnimationCompat.Callback(DISPATCH_MODE_CONTINUE_ON_SUBTREE),
    Runnable,
    OnApplyWindowInsetsListener,
    OnAttachStateChangeListener {
    /**
     * When [android.view.WindowInsetsController.controlWindowInsetsAnimation] is called, the
     * [onApplyWindowInsets] is called after [onPrepare] with the target size. We don't want to
     * report the target size, we want to always report the current size, so we must ignore those
     * calls. However, the animation may be canceled before it progresses. On R, it won't make any
     * callbacks, so we have to figure out whether the [onApplyWindowInsets] is from a canceled
     * animation or if it is from the controlled animation. When [prepared] is `true` on R, we post
     * a callback to set the [onApplyWindowInsets] insets value.
     */
    private var prepared = false

    /** `true` if there is an animation in progress. */
    private var runningAnimationMask = 0

    private var savedInsets: WindowInsetsCompat? = null

    var currentInsets by mutableStateOf<WindowInsetsCompat?>(null)

    // The ongoing animations. All values are added so that we don't have to watch the map itself.
    private val animations = MutableIntObjectMap<MutableState<WindowInsetsAnimationCompat?>>(8)

    private val animationPositions = MutableIntObjectMap<MutableState<AnimationPositions?>>(8)

    fun findAnimation(type: Int): State<WindowInsetsAnimationCompat?> = mutableAnimation(type)

    fun findAnimationPositions(type: Int): State<AnimationPositions?> =
        mutableAnimationPositions(type)

    private fun mutableAnimation(type: Int) =
        animations[type]
            ?: mutableStateOf<WindowInsetsAnimationCompat?>(null).also { animations[type] = it }

    private fun mutableAnimationPositions(type: Int) =
        animationPositions[type]
            ?: mutableStateOf<AnimationPositions?>(null).also { animationPositions[type] = it }

    override fun onPrepare(animation: WindowInsetsAnimationCompat) {
        prepared = true
        super.onPrepare(animation)
    }

    override fun onStart(
        animation: WindowInsetsAnimationCompat,
        bounds: BoundsCompat,
    ): BoundsCompat {
        val insets = savedInsets
        prepared = false
        savedInsets = null

        if (animation.durationMillis > 0L && insets != null) {
            val type = animation.typeMask
            val current = currentInsets?.getInsets(type)
            val target = insets.getInsets(type)
            if (target != current && current != null) {
                runningAnimationMask = runningAnimationMask or type
                mutableAnimation(type).value = animation
                mutableAnimationPositions(type).value = AnimationPositions(current, target)
                Snapshot.sendApplyNotifications()
            }
        }

        return super.onStart(animation, bounds)
    }

    override fun onProgress(
        insets: WindowInsetsCompat,
        runningAnimations: MutableList<WindowInsetsAnimationCompat>,
    ): WindowInsetsCompat {
        runningAnimations.fastForEach { animation ->
            val type = animation.typeMask
            if (runningAnimationMask and type != 0) {
                mutableAnimation(type).value = animation
            }
        }
        updateInsets(insets)
        return insets
    }

    override fun onEnd(animation: WindowInsetsAnimationCompat) {
        prepared = false
        val type = animation.typeMask
        mutableAnimation(type).value = null
        mutableAnimationPositions(type).value = null
        runningAnimationMask = runningAnimationMask and type.inv()
        savedInsets = null
        Snapshot.sendApplyNotifications()
        super.onEnd(animation)
    }

    override fun onApplyWindowInsets(view: View, insets: WindowInsetsCompat): WindowInsetsCompat {
        // Keep track of the most recent insets we've seen, to ensure onEnd will always use the
        // most recently acquired insets
        if (prepared) {
            savedInsets = insets // save for onStart()

            // There may be no callback on R if the animation is canceled after onPrepare(),
            // so we won't know if the onPrepare() was canceled or if this is an
            // onApplyWindowInsets() after the cancellation. We'll just post the value
            // and if it is still preparing then we just use the value.
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.R) {
                view.post(this)
            }
        } else if (runningAnimationMask == 0) {
            // If an animation is running, rely on onProgress() to update the insets
            // On APIs less than 30 where the IME animation is backported, this avoids reporting
            // the final insets for a frame while the animation is running.
            updateInsets(insets)
        }
        return insets
    }

    private fun updateInsets(insets: WindowInsetsCompat) {
        if (currentInsets == null) {
            val imeType = WindowInsetsCompat.Type.ime()
            val none = Insets.NONE
            // if we're setting insets with no values, we treat this as not setting any insets
            val hasValue =
                AllWindowInsetsTypes.any { type ->
                    val inset =
                        if (type == imeType) {
                            insets.getInsets(type)
                        } else {
                            insets.getInsetsIgnoringVisibility(type)
                        }
                    inset != none
                }
            if (!hasValue) {
                return
            }
        }
        currentInsets = insets
        Snapshot.sendApplyNotifications()
    }

    /**
     * On [R], we don't receive the [onEnd] call when an animation is canceled, so we post the value
     * received in [onApplyWindowInsets] immediately after [onPrepare]. If [onProgress] or [onEnd]
     * is received before the runnable executes then the value won't be used. Otherwise, the
     * [onApplyWindowInsets] value will be used. It may have a janky frame, but it is the best we
     * can do.
     */
    override fun run() {
        if (prepared) {
            runningAnimationMask = 0
            prepared = false
            savedInsets?.let {
                updateInsets(it)
                savedInsets = null
            }
        }
    }

    override fun onViewAttachedToWindow(view: View) {
        // Until merging the foundation layout implementation and this implementation, we'll
        // listen on the ComposeView containing the AndroidComposeView so that there isn't
        // a collision
        val listenerView = view.parent as? View ?: view
        ViewCompat.setOnApplyWindowInsetsListener(listenerView, this)
        ViewCompat.setWindowInsetsAnimationCallback(listenerView, this)
    }

    override fun onViewDetachedFromWindow(view: View) {
        // Until merging the foundation layout implementation and this implementation, we'll
        // listen on the ComposeView containing the AndroidComposeView so that there isn't
        // a collision
        val listenerView = view.parent as? View ?: view
        ViewCompat.setOnApplyWindowInsetsListener(listenerView, null)
        ViewCompat.setWindowInsetsAnimationCallback(listenerView, null)
    }

    class AnimationPositions(val source: Insets, val target: Insets)

    companion object {
        val AllWindowInsetsTypes =
            arrayOf(
                WindowInsetsCompat.Type.ime(),
                WindowInsetsCompat.Type.tappableElement(),
                WindowInsetsCompat.Type.captionBar(),
                WindowInsetsCompat.Type.statusBars(),
                WindowInsetsCompat.Type.displayCutout(),
                WindowInsetsCompat.Type.systemGestures(),
                WindowInsetsCompat.Type.navigationBars(),
                WindowInsetsCompat.Type.mandatorySystemGestures(),
            )
    }
}
