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

package androidx.xr.scenecore.testapp.handtracking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.xr.arcore.Hand
import androidx.xr.arcore.Trackable
import androidx.xr.runtime.Config
import androidx.xr.runtime.HandTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import androidx.xr.scenecore.Entity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.MovableComponent
import androidx.xr.scenecore.scene
import androidx.xr.scenecore.testapp.R
import androidx.xr.scenecore.testapp.common.managers.SessionManager
import kotlinx.coroutines.launch

class HandTrackingTest : AppCompatActivity() {

    private var session: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hand_tracking_test)

        session =
            SessionManager(this).createSession()?.also {
                it.configure(Config(handTracking = HandTrackingMode.BOTH))
                it.scene.keyEntity = it.scene.mainPanelEntity
            }

        // Toolbar action
        findViewById<Toolbar>(R.id.top_app_bar_activity_panel).also {
            setSupportActionBar(it)
            it.setNavigationOnClickListener { finish() }
        }

        // Set up tracking for both hands
        val currentSession = session ?: return
        setupHandTracking(Hand.left(currentSession) as Trackable<Hand.State>)
        setupHandTracking(Hand.right(currentSession) as Trackable<Hand.State>)
    }

    /** Creates an entity that tracks a given hand and attaches a visual model to it. */
    private fun setupHandTracking(hand: Trackable<Hand.State>) {
        val session = this.session ?: return

        // Create a parent entity that will be moved by the perception data stream.
        val handTrackerEntity = Entity.create(session = session, parent = session.scene.keyEntity)

        // Create a movable component that follows the hand's palm.
        val movable = MovableComponent.createTrackingMovable(session = session, trackable = hand)
        handTrackerEntity.addComponent(movable)

        // Asynchronously load and attach a 3D model to the tracking entity.
        lifecycleScope.launch {
            val model = GltfModel.create(session, "models/xyzArrows.glb".toUri())
            val gltfModelEntity =
                GltfModelEntity.create(
                    session,
                    model,
                    Pose(Vector3(0f, -0.05f, 0f)), // Minor offset for visibility
                    handTrackerEntity,
                )
            gltfModelEntity.setScale(0.1f)
        }
    }
}
