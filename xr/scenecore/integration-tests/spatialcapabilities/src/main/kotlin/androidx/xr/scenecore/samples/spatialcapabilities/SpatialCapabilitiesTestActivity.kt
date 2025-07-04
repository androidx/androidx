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

package androidx.xr.scenecore.samples.spatialcapabilities

import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.xr.runtime.Session
import androidx.xr.runtime.SessionCreateSuccess
import androidx.xr.scenecore.SpatialCapabilities
import androidx.xr.scenecore.scene
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SpatialCapabilitiesTestActivity : AppCompatActivity() {

    private val session by lazy { (Session.create(this) as SessionCreateSuccess).session }
    private val debugTextView by lazy { findViewById<TextView>(R.id.debug_text_area) }
    private val debugTextString: StringBuilder = StringBuilder()
    private val formatter = SimpleDateFormat("HH:mm:ss", Locale.US)

    private var isFsm = true // launch in FSM

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.spatialcapabilities_activity)

        // TODO: b/361132526 - Cannot call hasCapability until session has had a moment to
        // initialize
        Handler(mainLooper).postDelayed({ logCapabilities() }, 500)

        findViewById<Button>(R.id.toggle_fsm_hsm).setOnClickListener { _ ->
            if (isFsm) {
                session.scene.requestHomeSpaceMode()
                isFsm = false
                debugTextString.append("Toggled to HSM\n")
            } else {
                session.scene.requestFullSpaceMode()
                isFsm = true
                debugTextString.append("Toggled to FSM\n")
            }
            debugTextView.text = debugTextString.toString()
        }

        findViewById<Button>(R.id.log_capabilities).setOnClickListener { _ -> logCapabilities() }

        session.scene.addSpatialCapabilitiesChangedListener() { _ ->
            debugTextString.append("Capabilities changed event received.\n")
            logCapabilities()
        }

        session.scene.activitySpace.addOnBoundsChangedListener { bounds ->
            debugTextString.append(
                "Bounds Changed event received: w=${bounds.width}, h=${bounds.height}, d=${bounds.depth}\n"
            )
            isFsm = bounds.width == Float.POSITIVE_INFINITY
        }
    }

    private fun logCapabilities() {
        val capsStr: StringBuilder = StringBuilder()
        val caps = session.scene.spatialCapabilities
        capsStr.append(capToStr("UI", caps, SpatialCapabilities.SPATIAL_CAPABILITY_UI))
        capsStr.append(capToStr("3D", caps, SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT))
        capsStr.append(
            capToStr("PT", caps, SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL)
        )
        capsStr.append(
            capToStr("Env", caps, SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT)
        )
        capsStr.append(
            capToStr("Audio", caps, SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO)
        )
        capsStr.append(
            capToStr("Embed Activity", caps, SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY)
        )
        val timestamp = formatter.format(Calendar.getInstance().time)
        debugTextString.append("${timestamp}: ${capsStr}\n")
        debugTextView.text = debugTextString.toString()
    }

    private fun capToStr(
        name: String,
        spatialCapabilities: SpatialCapabilities,
        capability: Int,
    ): String {
        val status: String = if (spatialCapabilities.hasCapability(capability)) "Y" else "N"
        return "${name}: ${status}  \t"
    }
}
