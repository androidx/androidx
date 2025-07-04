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

package androidx.xr.scenecore.samples.fieldofviewvisibility

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.HeadTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.IntSize2d
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.SpatialVisibility
import androidx.xr.scenecore.samples.commontestview.DebugTextLinearView
import androidx.xr.scenecore.scene
import java.util.function.Consumer

class FieldOfViewVisibilityActivity : AppCompatActivity() {

    private val TAG = "FieldOfViewVisibility"

    private val mSession by lazy { (Session.create(this) as SessionCreateSuccess).session }
    private lateinit var mGltfManager: GltfManager
    private lateinit var mSurfaceEntityManager: SurfaceEntityManager
    private lateinit var mSpatialEnvironmentManager: SpatialEnvironmentManager
    private lateinit var mHeadLockedUIManager: HeadLockedUIManager

    private lateinit var mPanelEntityManager: PanelEntityManager
    private lateinit var mPerceivedResolutionManager: PerceivedResolutionManager
    private lateinit var mHeadLockedPanelView: DebugTextLinearView
    private var mSpatialVisibility by mutableStateOf(SpatialVisibility(SpatialVisibility.UNKNOWN))
    private var mPerceivedResolution by mutableStateOf(IntSize2d(0, 0))
    private val mPerceivedResolutionListener: Consumer<IntSize2d> = Consumer {
        mPerceivedResolution = it
        Log.i(TAG, "Perceived Resolution listener called $mPerceivedResolution")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mActivity = this
        mSession.configure(Config(headTracking = HeadTrackingMode.LAST_KNOWN))

        // Set the main panel size and make the main panel movable.
        mSession.scene.mainPanelEntity.sizeInPixels = IntSize2d(width = 1500, height = 1400)
        val movableComponent = MovableComponent.createSystemMovable(mSession, scaleInZ = false)
        val unused = mSession.scene.mainPanelEntity.addComponent(movableComponent)

        // Create the UI component managers.
        mSpatialEnvironmentManager = SpatialEnvironmentManager(mSession)
        mSurfaceEntityManager = SurfaceEntityManager(mSession)
        mGltfManager = GltfManager(mSession, this.lifecycleScope)
        mPanelEntityManager = PanelEntityManager(mSession)
        mPerceivedResolutionManager =
            PerceivedResolutionManager(mSession, mSurfaceEntityManager, mPanelEntityManager)

        // Create the headlocked panel
        mHeadLockedPanelView = DebugTextLinearView(context = this)
        mHeadLockedPanelView.setName("Spatial Visibility")
        mHeadLockedPanelView.setLine("State", "UNKNOWN")
        this.mHeadLockedUIManager = HeadLockedUIManager(mSession, mHeadLockedPanelView)

        mSession.scene.setSpatialVisibilityChangedListener { visibility: SpatialVisibility ->
            mSpatialVisibility = visibility
            Log.i(TAG, "Spatial visibility changed listener called $visibility")
            mHeadLockedPanelView.setLine("State", "$visibility")
        }
        mSession.scene.addPerceivedResolutionChangedListener(mPerceivedResolutionListener)

        setContent { MainPanelContent(mSession, mActivity) }
    }

    override fun onDestroy() {
        super.onDestroy()
        mSession.scene.clearSpatialVisibilityChangedListener()
        mSession.scene.removePerceivedResolutionChangedListener(mPerceivedResolutionListener)
    }

    @Composable
    fun MainPanelContent(session: Session, activity: Activity) {
        LaunchedEffect(Unit) {
            activity.setContentView(
                GetMainPanelViewUsingCompose(activity = activity, session = session)
            )
        }
    }

    private fun GetMainPanelViewUsingCompose(activity: Activity, session: Session): View {
        val view =
            ComposeView(activity.applicationContext).apply {
                setViewCompositionStrategy(
                    ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
                )
                setContent { MainPanelSettings(session) }
            }
        view.setViewTreeLifecycleOwner(activity as LifecycleOwner)
        view.setViewTreeViewModelStoreOwner(activity as ViewModelStoreOwner)
        view.setViewTreeSavedStateRegistryOwner(activity as SavedStateRegistryOwner)
        return view
    }

    @Composable
    fun MainPanelSettings(session: Session) {
        Column(verticalArrangement = Arrangement.Top) {
            Text(
                modifier = Modifier.padding(15.dp),
                text = "SpatialVisibility: $mSpatialVisibility",
            )
            Text(
                modifier = Modifier.padding(15.dp),
                text = "Perceived Resolution (HSM): $mPerceivedResolution",
            )

            Text(
                modifier = Modifier.padding(15.dp),
                text =
                    "To turn on bounding boxes, use these ADB Commands:\n" +
                        "adb root\n" +
                        "adb shell setprop persist.spaceflinger.fov.visualize_bounds 1\n" +
                        "adb shell setprop persist.ix.sysui.editor_enabled 1\n" +
                        "adb reboot",
            )

            mHeadLockedUIManager.HeadLockedUISettings()
            HorizontalDivider(Modifier.padding(15.dp), 1.dp, Color.Black)

            mSpatialEnvironmentManager.SpatialEnvironmentSettings()
            HorizontalDivider(Modifier.padding(15.dp), 1.dp, Color.Black)

            mSurfaceEntityManager.SurfaceEntitySettings()
            HorizontalDivider(Modifier.padding(15.dp), 1.dp, Color.Black)

            mGltfManager.GltfEntitySettings()
            HorizontalDivider(Modifier.padding(15.dp), 1.dp, Color.Black)

            mPanelEntityManager.PanelEntitySettings()
            HorizontalDivider(Modifier.padding(15.dp), 1.dp, Color.Black)

            mPerceivedResolutionManager.PerceivedResolutionSettings()
            HorizontalDivider(Modifier.padding(15.dp), 1.dp, Color.Black)

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { session.scene.requestFullSpaceMode() }) {
                    Text(text = "Request FSM", fontSize = 15.sp)
                }
                Button(onClick = { session.scene.requestHomeSpaceMode() }) {
                    Text(text = "Request HSM", fontSize = 15.sp)
                }
            }
        }
    }
}
