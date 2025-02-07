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

package androidx.xr.arcore.apps.whitebox

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.xr.arcore.apps.whitebox.handtracking.HandTrackingActivity as HandTrackingActivity
import androidx.xr.arcore.apps.whitebox.helloar.HelloArActivity as HelloArActivity
import androidx.xr.arcore.apps.whitebox.persistentanchors.PersistentAnchorsActivity as PersistentAnchorsActivity
import java.text.SimpleDateFormat
import java.util.Locale

/** Entrypoint for testing various ARCore for Android XR functionalities. */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent { WhiteboxHomeScreen() }
    }
}

@Composable
fun WhiteboxHomeScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = Color.White) {
        Column() {
            Text(
                "AR Whitebox Test Application",
                modifier = Modifier.padding(20.dp),
                fontSize = 30.sp,
                color = Color.Black,
            )
            VersionInfoCard()
            WhiteboxSessionMenu()
        }
    }
}

@Composable
fun VersionInfoCard() {
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Build Fingerprint: ${Build.FINGERPRINT}")
            Text(
                "Date: ${SimpleDateFormat("dd MMMM yyyy, HH:mm:ss", Locale.ENGLISH).format(Build.TIME)}"
            )
            Text("CL Number: N/A")
        }
    }
}

@Composable
fun WhiteboxSessionMenu() {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Test Activity List",
            modifier = Modifier.padding(20.dp),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
        )
        HorizontalDivider()
        TextButton(
            onClick = { context.startActivity(Intent(context, HelloArActivity::class.java)) }
        ) {
            Text("Hello AR")
        }
        TextButton(
            onClick = {
                context.startActivity(Intent(context, PersistentAnchorsActivity::class.java))
            }
        ) {
            Text("Persistent Anchors")
        }
        TextButton(
            onClick = { context.startActivity(Intent(context, HandTrackingActivity::class.java)) }
        ) {
            Text("Hand Tracking")
        }
    }
}
