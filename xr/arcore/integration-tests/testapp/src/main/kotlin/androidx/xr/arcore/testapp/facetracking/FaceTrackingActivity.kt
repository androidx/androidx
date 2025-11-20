/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.xr.arcore.testapp.facetracking

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.xr.arcore.Face
import androidx.xr.arcore.FaceBlendShapeType
import androidx.xr.arcore.FaceConfidenceRegion
import androidx.xr.arcore.testapp.common.BackToMainActivityButton
import androidx.xr.arcore.testapp.common.SessionLifecycleHelper
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.runtime.Config
import androidx.xr.runtime.RequiredCalibrationType
import androidx.xr.runtime.Session
import androidx.xr.runtime.TrackingState
import kotlinx.coroutines.launch

class FaceTrackingActivity : ComponentActivity() {
    private lateinit var session: Session
    private lateinit var sessionHelper: SessionLifecycleHelper
    private lateinit var resultLauncher: ActivityResultLauncher<Intent>

    var currentExpression = Expression.NEUTRAL

    enum class Expression {
        NEUTRAL,
        BLINK,
        SMILE,
        FROWN,
        ANGRY,
        EYEBROW_RAISED,
        WINK,
        MOUTH_OPEN,
        TONGUE_OUT,
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        resultLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode != Activity.RESULT_OK) {
                    Toast.makeText(
                            this,
                            "Returned from calibration with result ${result.resultCode}",
                            Toast.LENGTH_SHORT,
                        )
                        .show()
                } else {
                    sessionHelper.tryCreateSession()
                }
            }

        // Create session and renderers.
        sessionHelper =
            SessionLifecycleHelper(
                this,
                Config(faceTracking = Config.FaceTrackingMode.USER),
                onSessionAvailable = { session ->
                    this.session = session
                    lifecycleScope.launch {
                        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            setContent { MainPanel(session) }
                        }
                    }
                },
                onSessionCalibrationRequired = { calibrationType ->
                    if (
                        calibrationType ==
                            RequiredCalibrationType.REQUIRED_CALIBRATION_TYPE_FACE_TRACKING
                    ) {
                        lifecycleScope.launch {
                            lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                                setContent { CalibrationPanel() }
                            }
                        }
                    } else {
                        throw IllegalStateException(
                            "Unexpected calibration requirement: " + "$calibrationType"
                        )
                    }
                },
            )
        sessionHelper.tryCreateSession()
    }

    @Composable
    private fun CalibrationPanel() {
        Scaffold(
            modifier = Modifier.fillMaxSize().padding(0.dp),
            topBar = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(0.dp).background(color = GoogleYellow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = "Calibration",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier.background(color = Color.White)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
            ) {
                Row { Text("Face Tracker has not been calibrated", fontSize = 30.sp) }
                Row {
                    Button(onClick = { launchCalibrationActivity() }) {
                        Text(text = "Launch calibration", fontSize = 20.sp)
                    }
                }
            }
        }
    }

    @Composable
    private fun MainPanel(session: Session) {
        val face = Face.getUserFace(session)

        var title = intent.getStringExtra("TITLE")
        if (title == null) title = "Face Tracking"

        Scaffold(
            modifier = Modifier.fillMaxSize().padding(0.dp),
            topBar = {
                Row(
                    modifier =
                        Modifier.fillMaxWidth().padding(0.dp).background(color = GoogleYellow),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    BackToMainActivityButton()
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        text = title,
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                    )
                }
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier.background(color = Color.White)
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(innerPadding)
                        .padding(horizontal = 20.dp)
            ) {
                if (face == null) {
                    Text("Face tracking is not supported")
                } else {
                    val faceState = face.state.collectAsState().value
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(20.dp),
                    ) {
                        Text("Face Tracking State: ${faceState.trackingState}")
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(20.dp),
                    ) {
                        Text("Region Confidences: ")
                        Text(
                            "LOWER: ${faceState.getConfidence(FaceConfidenceRegion.FACE_CONFIDENCE_REGION_LOWER)}"
                        )
                        Text(
                            "LEFT_UPPER: ${faceState.getConfidence(FaceConfidenceRegion.FACE_CONFIDENCE_REGION_LEFT_UPPER)}"
                        )
                        Text(
                            "RIGHT_UPPER: ${faceState.getConfidence(FaceConfidenceRegion.FACE_CONFIDENCE_REGION_RIGHT_UPPER)}"
                        )
                    }
                    if (faceState.trackingState == TrackingState.TRACKING) {
                        currentExpression = parseBlendShapesToExpression(faceState)
                        Row {
                            LazyColumn(modifier = Modifier.width(350.dp)) {
                                items(faceState.blendShapes.keys.toList()) { shape ->
                                    Text("$shape = ${faceState.blendShapes[shape]}")
                                }
                            }
                            Column(modifier = Modifier.fillMaxSize()) {
                                Text(currentExpression.toString(), fontSize = 30.sp)
                                Text(text = "${emoteTextMap[currentExpression]}", fontSize = 200.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun launchCalibrationActivity() {
        val packageName =
            "com.google.xr.facetracking.calibration" // Replace with the other app's package name
        val className =
            "com.google.xr.facetracking.calibration.FaceTrackingCalibrationActivity" // Replace with
        // the other
        // app's
        // activity
        // class name

        val intent = Intent().apply { component = ComponentName(packageName, className) }
        try {
            resultLauncher.launch(intent)
        } catch (e: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "Activity not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseBlendShapesToExpression(faceState: Face.State): Expression {
        if (faceState.blendShapes.entries.size != 68) {
            return currentExpression
        }

        // smile
        if (
            faceState.blendShapes[
                    FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_CORNER_PULLER_LEFT]!! >= .3f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_CORNER_PULLER_RIGHT]!! >=
                    .3f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_PRESSOR_LEFT]!! >= .3f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_PRESSOR_RIGHT]!! >= .3f
        ) {
            return Expression.SMILE
        }

        // blink
        if (
            faceState.blendShapes[FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_CLOSED_LEFT]!! ==
                1f &&
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_CLOSED_RIGHT]!! == 1f
        ) {
            return Expression.BLINK
        }

        // frown
        if (
            faceState.blendShapes[
                    FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_CORNER_DEPRESSOR_LEFT]!! >= .5f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_CORNER_DEPRESSOR_RIGHT]!! >=
                    .5f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_STRETCHER_LEFT]!! >= .5f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LIP_STRETCHER_RIGHT]!! >= .5f ||
                faceState.blendShapes[FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_JAW_THRUST]!! >=
                    .5f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_CHIN_RAISER_BOTTOM]!! >= .7f
        ) {
            return Expression.FROWN
        }

        // wink
        if (
            faceState.blendShapes[FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_CLOSED_LEFT]!! >=
                .6f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_EYES_CLOSED_RIGHT]!! >= .6f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LID_TIGHTENER_LEFT]!! >= .6f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_LID_TIGHTENER_RIGHT]!! >= .6f
        ) {
            return Expression.WINK
        }

        // eyebrow(s) raised
        if (
            faceState.blendShapes[
                    FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_OUTER_BROW_RAISER_LEFT]!! >= .5f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_OUTER_BROW_RAISER_RIGHT]!! >=
                    .5f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_INNER_BROW_RAISER_LEFT]!! >= .5f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_INNER_BROW_RAISER_RIGHT]!! >= .5f
        ) {
            return Expression.EYEBROW_RAISED
        }

        // mouth open
        if (faceState.blendShapes[FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_JAW_DROP]!! >= .6f) {
            return Expression.MOUTH_OPEN
        }

        // tongue out
        if (faceState.blendShapes[FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_TONGUE_OUT]!! >= .8f) {
            return Expression.TONGUE_OUT
        }

        // angry
        if (
            faceState.blendShapes[FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_BROW_LOWERER_LEFT]!! >=
                .2f ||
                faceState.blendShapes[
                        FaceBlendShapeType.FACE_BLEND_SHAPE_TYPE_BROW_LOWERER_RIGHT]!! >= .2f
        ) {
            return Expression.ANGRY
        }

        return Expression.NEUTRAL
    }

    companion object {
        val TAG = this::class.simpleName

        val emoteTextMap =
            mapOf(
                Expression.NEUTRAL to "\uD83D\uDE10",
                Expression.BLINK to "\uD83D\uDE11",
                Expression.SMILE to "\uD83D\uDE01",
                Expression.FROWN to "\uD83D\uDE26",
                Expression.ANGRY to "\uD83D\uDE20",
                Expression.EYEBROW_RAISED to "\uD83E\uDD28",
                Expression.WINK to "\uD83D\uDE09",
                Expression.TONGUE_OUT to "\uD83D\uDE1B",
                Expression.MOUTH_OPEN to "\uD83D\uDE2E",
            )
    }
}
