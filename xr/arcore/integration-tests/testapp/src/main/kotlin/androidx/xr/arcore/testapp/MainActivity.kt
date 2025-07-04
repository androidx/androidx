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

package androidx.xr.arcore.testapp

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.getString
import androidx.xr.arcore.testapp.common.TestCaseButton
import androidx.xr.arcore.testapp.handtracking.HandTrackingActivity
import androidx.xr.arcore.testapp.helloar.HelloArActivity
import androidx.xr.arcore.testapp.persistentanchors.PersistentAnchorsActivity
import androidx.xr.arcore.testapp.ui.theme.GoogleYellow
import androidx.xr.arcore.testapp.ui.theme.JXRARCoreTestsTheme
import androidx.xr.arcore.testapp.ui.theme.Purple80
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private fun requestUserPermissions(permissions: Array<String>) {
        val permissionsLauncher =
            super.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
                permissions ->
                if (permissions.values.contains(false)) {
                    Toast.makeText(this, "Missing required permissions", Toast.LENGTH_LONG).show()
                    this.finish()
                }
            }
        permissionsLauncher.launch(permissions)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestUserPermissions(
            arrayOf(
                SCENE_UNDERSTANDING_COARSE_PERMISSION,
                HAND_TRACKING_PERMISSION,
                HEAD_TRACKING_PERMISSION,
            )
        )

        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
                JXRARCoreTestsTheme {
                    val scrollBehavior =
                        TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        topBar = { TopBar(scrollBehavior) },
                        bottomBar = { BottomBar() },
                    ) { innerPadding ->
                        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                            Row(modifier = Modifier.fillMaxWidth().weight(0.2f)) { BuildDetails() }
                            Row(modifier = Modifier.fillMaxWidth().weight(0.8f)) { TestCases() }
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TopBar(scrollBehavior: TopAppBarScrollBehavior) {
        val context = LocalContext.current

        CenterAlignedTopAppBar(
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = GoogleYellow,
                    titleContentColor = Color.Black,
                ),
            title = {
                Text(
                    "JXR ARCore Tests",
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 32.sp,
                )
            },
            navigationIcon = {
                IconButton(onClick = { (context as Activity).finish() }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close the app",
                        Modifier.size(36.dp),
                        tint = Color.Black,
                    )
                }
            },
            scrollBehavior = scrollBehavior,
        )
    }

    @Composable
    private fun TestCases() {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(10.dp).verticalScroll(rememberScrollState())) {
                TestCaseColumnRowItem(R.string.plane_tracking) { startTest<HelloArActivity>(it) }
                TestCaseColumnRowItem(R.string.session_lifecycle) { startTest<HelloArActivity>(it) }
                TestCaseColumnRowItem(R.string.hit_test) { startTest<HelloArActivity>(it) }
                TestCaseColumnRowItem(R.string.device_tracking_test) {
                    startTest<PersistentAnchorsActivity>(it)
                }
                TestCaseColumnRowItem(R.string.persisting_anchor) {
                    startTest<PersistentAnchorsActivity>(it)
                }
                TestCaseColumnRowItem(R.string.loading_anchors) {
                    startTest<PersistentAnchorsActivity>(it)
                }
                TestCaseColumnRowItem(R.string.fov_main_panel) {
                    startTest<PersistentAnchorsActivity>(it)
                }
                TestCaseColumnRowItem(R.string.hand_tracking) {
                    startTest<HandTrackingActivity>(it)
                }
            }
        }
    }

    @Composable
    private fun BottomBar() {
        Box {
            BottomAppBar(
                actions = {},
                contentColor = Color.White,
                containerColor = GoogleYellow,
                tonalElevation = 5.dp,
            )
        }
    }

    @Composable
    private fun BuildDetails() {
        Box(modifier = Modifier.fillMaxWidth().background(color = Color.DarkGray)) {
            Column(modifier = Modifier.padding(10.dp)) {
                val buildDate =
                    SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH).format(Build.TIME)
                BuildInfoRowItem("Build Fingerprint: ", Build.FINGERPRINT)
                BuildInfoRowItem("Build Device: ", Build.DEVICE)
                BuildInfoRowItem("Build Date: ", buildDate)
            }
        }
    }

    @Composable
    private fun BuildInfoRowItem(label: String, value: String) {
        Row {
            Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(value, modifier = Modifier.weight(3f))
        }
    }

    @Composable
    private fun TestCaseColumnRowItem(resId: Int, onClick: (String) -> Unit) {
        val label = getString(resId)
        TestCaseRowItem(label) { onClick(label) }
        Spacer(modifier = Modifier.height(1.dp).background(Purple80).fillMaxWidth())
    }

    @Composable
    private fun TestCaseRowItem(label: String, onClick: () -> Unit) {
        Row(modifier = Modifier.padding(5.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                label,
                modifier = Modifier.weight(3.5f),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Left,
            )
            Box(modifier = Modifier.weight(1.5f)) { TestCaseButton("Run Test", onClick) }
        }
    }

    private inline fun <reified T> startTest(label: String) {
        val intent = Intent(this@MainActivity, T::class.java)
        intent.putExtra("TITLE", label)
        startActivity(intent)
    }

    private inline fun <reified T> createIntent(): Intent = Intent(this@MainActivity, T::class.java)

    companion object {
        const val SCENE_UNDERSTANDING_COARSE_PERMISSION =
            "android.permission.SCENE_UNDERSTANDING_COARSE"
        const val HAND_TRACKING_PERMISSION = "android.permission.HAND_TRACKING"
        const val HEAD_TRACKING_PERMISSION = "android.permission.HEAD_TRACKING"
    }
}
