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

package androidx.xr.compose.subspace

import android.content.Intent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.drawable.toDrawable
import androidx.xr.compose.platform.LocalDialogManager
import androidx.xr.compose.platform.LocalSession
import androidx.xr.compose.platform.disposableValueOf
import androidx.xr.compose.platform.getValue
import androidx.xr.compose.subspace.layout.CoreActivityPanelEntity
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.layout.SpatialShape
import androidx.xr.compose.subspace.layout.SubspaceLayout
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.ActivityPanelEntity
import androidx.xr.scenecore.PanelEntity
import androidx.xr.scenecore.scene
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference

/**
 * Creates a [SpatialActivityPanel] that launches and displays an Activity within it.
 *
 * This [SpatialActivityPanel] can render any Activity that the application has permission to invoke
 * as an embedded activity.
 *
 * The panel size is determined solely by the layout constraints and the provided [modifier] it does
 * not resize based on the activity content.
 *
 * @param controller The [SpatialActivityPanelController] used to manage the Activities displayed
 *   within this panel. It provides a mechanism to queue and dispatch [android.content.Intent]s to
 *   the embedded Activity environment.
 * @param modifier SubspaceModifiers to apply to the SpatialPanel. The layout size of the panel will
 *   dictate the viewport size allocated to the embedded Activity.
 * @param shape The shape of this Spatial Panel.
 */
@Composable
@SubspaceComposable
public fun SpatialActivityPanel(
    controller: SpatialActivityPanelController,
    modifier: SubspaceModifier = SubspaceModifier,
    shape: SpatialShape = SpatialPanelDefaults.shape,
) {
    val session = checkNotNull(LocalSession.current) { "session must be initialized" }
    val dialogManager = LocalDialogManager.current
    val density = LocalDensity.current

    val pixelDimensions = IntSize2d(0, 0)
    val pixelDensity = session.scene.virtualPixelDensity

    val activityPanelEntity = remember {
        ActivityPanelEntity.create(
            session,
            pixelDimensions,
            "ActivityPanel-${UUID.randomUUID()}",
            parent = null,
        )
    }

    val corePanelEntity: CoreActivityPanelEntity = remember {
        CoreActivityPanelEntity(pixelDensity, activityPanelEntity).apply { enabled = false }
    }

    SideEffect { corePanelEntity.setShape(shape, density) }

    DisposableEffect(controller, corePanelEntity) {
        controller.setIntentListener { intent -> corePanelEntity.startActivity(intent) }

        onDispose { controller.setIntentListener(null) }
    }

    SubspaceLayout(modifier = modifier, coreEntity = corePanelEntity) { _, constraints ->
        val width = DEFAULT_SIZE_PX.coerceIn(constraints.minWidth, constraints.maxWidth)
        val height = DEFAULT_SIZE_PX.coerceIn(constraints.minHeight, constraints.maxHeight)
        val depth = constraints.minDepth.coerceAtLeast(0)
        layout(width, height, depth) {}
    }

    if (dialogManager.isSpatialDialogActive.value) {
        val localContext = LocalContext.current
        val scrimView =
            remember(localContext) {
                View(localContext).apply {
                    foreground = DEFAULT_SCRIM_ALPHA.toDrawable()
                    setOnClickListener { dialogManager.isSpatialDialogActive.value = false }
                }
            }

        val entityName = "ScrimPanel"
        val scrimPanelEntity by
            remember(scrimView) {
                disposableValueOf(
                    CorePanelEntity(
                            pixelDensity = pixelDensity,
                            entity =
                                PanelEntity.create(
                                    session = session,
                                    view = scrimView,
                                    pixelDimensions =
                                        corePanelEntity.size.run { IntSize2d(width, height) },
                                    name = entityName,
                                    pose = Pose.Identity,
                                    parent = activityPanelEntity,
                                ),
                        )
                        .apply {
                            parent = corePanelEntity
                            poseInMeters = Pose(translation = Vector3(0f, 0f, 0.01f))
                        }
                ) {
                    it.dispose()
                }
            }

        SideEffect {
            scrimPanelEntity.size = corePanelEntity.size
            scrimPanelEntity.setShape(shape, density)
        }
    }
}

/**
 * Creates and remembers a [SpatialActivityPanelController] across recompositions.
 *
 * The provided [initialIntent] is only queued when the controller is initially created. Subsequent
 * recompositions with a different intent will be ignored. To add new intents dynamically after
 * creation, use [SpatialActivityPanelController.startActivity].
 *
 * @param initialIntent The initial [Intent] to queue for execution inside the activity panel.
 * @return A remembered instance of [SpatialActivityPanelController].
 */
@Composable
public fun rememberSpatialActivityPanelController(
    initialIntent: Intent
): SpatialActivityPanelController {
    return remember { SpatialActivityPanelController(initialIntent) }
}

/**
 * A state holder and controller for managing the embedded [android.app.Activity] lifecycle within a
 * [SpatialActivityPanel].
 *
 * This controller acts as a bridge between standard imperative intent dispatching and the
 * declarative Compose XR environment. It maintains a queue of [Intent]s. If the associated
 * [SpatialActivityPanel] is present in the composition and actively listening, incoming Intents are
 * dispatched immediately. If the panel is not yet ready, Intents are queued and will be drained
 * sequentially the moment the panel binds to this controller.
 *
 * This class is thread-safe and safe to call from background threads (e.g. calling [startActivity]
 * concurrently). It uses non-blocking concurrent structures rather than locks to prevent blocking
 * the main recomposition thread.
 *
 * @param initialIntent The initial [Intent] to queue for execution inside the activity panel.
 */
public class SpatialActivityPanelController(initialIntent: Intent) {

    private val pendingIntents = ConcurrentLinkedQueue<Intent>()
    private val onIntentListener = AtomicReference<((Intent) -> Unit)?>()

    init {
        startActivity(initialIntent)
    }

    /**
     * Dispatches an [Intent] to the [SpatialActivityPanel].
     *
     * If the panel is active and bound to this controller, the intent is processed immediately.
     * Otherwise, the intent is placed in a pending queue and will be processed once the panel
     * becomes active.
     *
     * @param intent The [Intent] to launch in the activity panel.
     */
    public fun startActivity(intent: Intent) {
        val listener = onIntentListener.get()
        if (listener != null && pendingIntents.isEmpty()) {
            listener.invoke(intent)
        } else {
            pendingIntents.add(intent)
            val activeListener = onIntentListener.get()
            if (activeListener != null) {
                var pendingIntent = pendingIntents.poll()
                while (pendingIntent != null) {
                    activeListener.invoke(pendingIntent)
                    pendingIntent = pendingIntents.poll()
                }
            }
        }
    }

    /** Internal function to bind the panel to the controller. */
    internal fun setIntentListener(listener: ((Intent) -> Unit)?) {
        onIntentListener.set(listener)
        if (listener != null) {
            var pendingIntent = pendingIntents.poll()
            while (pendingIntent != null) {
                listener.invoke(pendingIntent)
                pendingIntent = pendingIntents.poll()
            }
        }
    }
}
