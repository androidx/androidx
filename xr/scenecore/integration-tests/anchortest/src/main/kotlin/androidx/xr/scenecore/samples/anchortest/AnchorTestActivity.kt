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

package androidx.xr.scenecore.samples.anchortest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.xr.runtime.Config
import androidx.xr.runtime.Config.PlaneTrackingMode
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.runtime.math.FloatSize2d
import androidx.xr.runtime.math.Pose
import androidx.xr.scenecore.AnchorEntity
import androidx.xr.scenecore.GltfModel
import androidx.xr.scenecore.GltfModelEntity
import androidx.xr.scenecore.PlaneOrientation
import androidx.xr.scenecore.PlaneSemanticType
import java.nio.file.Paths
import kotlinx.coroutines.launch

class AnchorTestActivity : AppCompatActivity() {
    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.anchortest_activity)
        session.configure(Config(planeTracking = PlaneTrackingMode.HORIZONTAL_AND_VERTICAL))
        // Create a transform widget model and assign it to an Anchor
        lifecycleScope.launch {
            val transformWidgetModel =
                GltfModel.create(session, Paths.get("models", "xyzArrows.glb"))
            setupAnchorUi(transformWidgetModel)
        }
    }

    @Suppress("UNUSED_VARIABLE")
    fun setupAnchorUi(transformWidgetModel: GltfModel) {
        val anchoredTransformWidgetEntity =
            GltfModelEntity.create(session, transformWidgetModel, Pose.Identity)
        val anchor =
            AnchorEntity.create(
                session,
                FloatSize2d(0.1f, 0.1f),
                PlaneOrientation.ANY,
                PlaneSemanticType.ANY,
            )
        anchor.addChild(anchoredTransformWidgetEntity)
        anchoredTransformWidgetEntity.setPose(Pose.Identity)
    }
}
