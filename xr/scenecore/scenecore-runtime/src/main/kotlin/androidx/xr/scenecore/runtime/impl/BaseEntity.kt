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

package androidx.xr.scenecore.runtime.impl

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.runtime.Component
import androidx.xr.scenecore.runtime.Entity
import androidx.xr.scenecore.runtime.Space
import androidx.xr.scenecore.runtime.SpaceValue
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max
import kotlin.math.min

/** Implementation of a subset of core Entity functionality. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
// TODO(b/452961674): Review RestrictTo annotations in SceneCore.
public abstract class BaseEntity public constructor(private var _context: Context?) :
    BaseScenePose(), Entity {

    private val _children = ArrayList<Entity>()
    private val _componentList = ArrayList<Component>()
    private var _parent: AtomicReference<BaseEntity?> = AtomicReference<BaseEntity?>(null)
    private var _pose = Pose()
    private var _scale = Vector3(1.0f, 1.0f, 1.0f)
    private var _alpha = 1.0f
    private var _hidden = false
    private var accessibilityLayout: ViewGroup? = null

    protected fun addChildInternal(child: Entity) {
        synchronized(_children) {
            if (child in _children) {
                throw IllegalStateException("Trying to add child who is already a child.")
            }
            _children.add(child)
        }
    }

    protected fun removeChildInternal(child: Entity) {
        synchronized(_children) {
            if (child !in _children) {
                throw IllegalStateException("Trying to remove child who is not a child.")
            }
            _children.remove(child)
        }
    }

    private fun getAccessibilityView(): View {
        val activity =
            activity
                ?: throw IllegalStateException(
                    "Activity is not set and unable to create accessibility view"
                )

        if (accessibilityLayout == null) {
            val mainLayout = activity.window.decorView as ViewGroup
            accessibilityLayout =
                FrameLayout(activity).apply { layoutParams = FrameLayout.LayoutParams(1, 1) }
            mainLayout.addView(accessibilityLayout)
        }

        // There should be only one child as per this design
        accessibilityLayout?.let { layout ->
            if (layout.childCount > 0) {
                return layout.getChildAt(0)
            }
            // If no view exists create one
            val view =
                View(activity).apply {
                    isFocusable = true
                    isFocusableInTouchMode = true
                }
            layout.addView(view)
            return view
        }
        throw IllegalStateException("Accessibility layout is null unexpectedly")
    }

    private fun destroyAccessibilityView() {
        activity?.let {
            if (accessibilityLayout != null) {
                val mainLayout = it.window.decorView as ViewGroup
                mainLayout.removeView(accessibilityLayout)
                accessibilityLayout = null
            }
        }
    }

    protected val context: Context?
        get() = _context

    protected val activity: Activity?
        get() = _context as? Activity

    override fun addChild(child: Entity) {
        child.parent = this
    }

    override fun addChildren(children: List<Entity>) {
        for (child in children) {
            child.parent = this
        }
    }

    override var parent: Entity?
        get() = _parent.get()
        set(value) {
            if (value != null && value !is BaseEntity) {
                throw IllegalStateException("Cannot set non-BaseEntity as a parent of a BaseEntity")
            }

            val oldParent: BaseEntity? = _parent.getAndSet(value)
            oldParent?.removeChildInternal(this)
            value?.addChildInternal(this)
        }

    override val children: List<Entity>
        get() {
            synchronized(_children) {
                // Returns a new copy of the list to avoid ConcurrentModificationException during
                // external iteration.
                return ArrayList<Entity>(_children)
            }
        }

    override var contentDescription: CharSequence
        get() {
            if (accessibilityLayout != null) {
                try {
                    val view = getAccessibilityView()
                    return view.contentDescription ?: ""
                } catch (e: IllegalStateException) {
                    // Accessibility view is not set.
                }
            }
            return ""
        }
        set(value) {
            if (value.isEmpty()) {
                if (accessibilityLayout != null) {
                    destroyAccessibilityView()
                }
                return
            }
            val view = getAccessibilityView()
            view.contentDescription = value
        }

    override fun getPose(@SpaceValue relativeTo: Int): Pose {
        return _pose
    }

    override fun setPose(pose: Pose, @SpaceValue relativeTo: Int) {
        _pose = pose
    }

    override val activitySpacePose: Pose
        get() {
            // Any parentless "space" entities (such as the root and anchor entities) are expected
            // to override this method non-recursively so that this error is never thrown.
            val parent =
                _parent.get()
                    ?: throw IllegalStateException(
                        "Cannot get pose in ActivitySpace with a null parent"
                    )
            return parent.activitySpacePose.compose(
                Pose(_pose.translation.scale(parent.activitySpaceScale), _pose.rotation)
            )
        }

    override fun getScale(@SpaceValue relativeTo: Int): Vector3 {
        return when (relativeTo) {
            Space.PARENT -> _scale
            Space.ACTIVITY -> activitySpaceScale
            Space.REAL_WORLD -> worldSpaceScale
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setScale(scale: Vector3, @SpaceValue relativeTo: Int) {
        val parent = _parent.get()
        when (relativeTo) {
            Space.PARENT -> _scale = scale
            Space.ACTIVITY -> {
                if (parent == null) {
                    throw IllegalStateException(
                        "Cannot set scale relative to ActivitySpace with a null parent"
                    )
                }
                _scale = scale.scale(parent.activitySpaceScale.inverse())
            }
            Space.REAL_WORLD -> {
                if (parent == null) {
                    throw IllegalStateException(
                        "Cannot set scale relative to WorldSpace with a null parent"
                    )
                }
                _scale = scale.scale(parent.worldSpaceScale.inverse())
            }
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    protected fun setScaleInternal(scale: Vector3) {
        _scale = scale
    }

    override fun getAlpha(@SpaceValue relativeTo: Int): Float {
        return when (relativeTo) {
            Space.PARENT -> _alpha
            Space.ACTIVITY,
            Space.REAL_WORLD -> getActivitySpaceAlpha()
            else -> throw IllegalArgumentException("Unsupported relativeTo value: $relativeTo")
        }
    }

    override fun setAlpha(alpha: Float) {
        _alpha = max(0.0f, min(1.0f, alpha))
    }

    private fun getActivitySpaceAlpha(): Float {
        val parent = _parent.get() ?: return _alpha
        return parent.getActivitySpaceAlpha() * _alpha
    }

    override val worldSpaceScale: Vector3
        get() {
            val parent =
                _parent.get()
                    ?: throw IllegalStateException(
                        "Cannot get scale in WorldSpace with a null parent"
                    )
            return parent.worldSpaceScale.scale(_scale)
        }

    override val activitySpaceScale: Vector3
        get() {
            val parent =
                _parent.get()
                    ?: throw IllegalStateException(
                        "Cannot get scale in ActivitySpace with a null parent"
                    )
            return parent.activitySpaceScale.scale(_scale)
        }

    override fun isHidden(includeParents: Boolean): Boolean {
        val parent = _parent.get()
        if (!includeParents || parent == null) {
            return _hidden
        }
        return _hidden || parent.isHidden(true)
    }

    override fun setHidden(hidden: Boolean) {
        _hidden = hidden
    }

    override fun dispose() {
        destroyAccessibilityView()
        _context = null
    }

    override fun addComponent(component: Component): Boolean {
        if (component.onAttach(this)) {
            synchronized(_componentList) { _componentList.add(component) }
            return true
        }
        return false
    }

    override fun <T : Component> getComponentsOfType(type: Class<out T>): List<T> {
        synchronized(_componentList) {
            return _componentList.filterIsInstance(type)
        }
    }

    override fun getComponents(): List<Component> {
        synchronized(_componentList) {
            // Returns a new copy of the list to avoid ConcurrentModificationException during
            // external iteration.
            return ArrayList<Component>(_componentList)
        }
    }

    override fun removeComponent(component: Component) {
        synchronized(_componentList) {
            component.takeIf { _componentList.remove(it) }?.onDetach(this)
        }
    }

    override fun removeAllComponents() {
        synchronized(_componentList) {
            _componentList.forEach { it.onDetach(this) }
            _componentList.clear()
        }
    }
}
