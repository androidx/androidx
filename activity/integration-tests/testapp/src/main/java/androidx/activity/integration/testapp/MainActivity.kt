/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.activity.integration.testapp

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.ViewManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.LinearLayout.VERTICAL
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.CaptureVideo
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.activity.result.contract.ActivityResultContracts.TakePicturePreview
import androidx.activity.result.launch
import androidx.activity.result.registerForActivityResult
import androidx.core.content.FileProvider
import java.io.File

class MainActivity : ComponentActivity() {

    val requestLocation = registerForActivityResult(
        RequestPermission(), ACCESS_FINE_LOCATION
    ) { isGranted ->
        toast("Location granted: $isGranted")
    }

    val takePicturePreview = registerForActivityResult(TakePicturePreview()) { bitmap ->
        toast("Got picture: $bitmap")
    }

    val takePicture = registerForActivityResult(TakePicture()) { success ->
        toast("Got picture: $success")
    }

    val captureVideo = registerForActivityResult(CaptureVideo()) { success ->
        toast("Got video: $success")
    }

    val getContent = registerForActivityResult(GetContent()) { uri ->
        toast("Got image: $uri")
    }

    lateinit var openDocuments: ActivityResultLauncher<Array<String>>

    val intentSender = registerForActivityResult(
        ActivityResultContracts
            .StartIntentSenderForResult()
    ) {
        toast("Received intent sender callback")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            openDocuments = registerForActivityResult(OpenMultipleDocuments()) { uris ->
                var docs = ""
                uris.forEach {
                    docs += "uri: $it \n"
                }
                toast("Got documents: $docs")
            }
        }

        setContentView {
            add(::LinearLayout) {
                orientation = VERTICAL

                button("Request location permission") {
                    requestLocation.launch()
                }
                button("Get picture thumbnail") {
                    takePicturePreview.launch()
                }
                button("Take pic") {
                    val file = File(filesDir, "image")
                    val uri = FileProvider.getUriForFile(this@MainActivity, packageName, file)
                    takePicture.launch(uri)
                }
                button("Capture video") {
                    val file = File(filesDir, "video")
                    val uri = FileProvider.getUriForFile(this@MainActivity, packageName, file)
                    captureVideo.launch(uri)
                }
                button("Pick an image") {
                    getContent.launch("image/*")
                }
                if (android.os.Build.VERSION.SDK_INT >= 19) {
                    button("Open documents") {
                        openDocuments.launch(arrayOf("*/*"))
                    }
                }
                button("Start IntentSender") {
                    val request = IntentSenderRequest.Builder(
                        PendingIntent.getActivity(
                            context,
                            0, Intent(MediaStore.ACTION_IMAGE_CAPTURE), 0
                        ).intentSender
                    )
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK, 1)
                        .build()
                    intentSender.launch(request)
                }
            }
        }
    }
}

fun Context.toast(msg: String) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
}

inline fun Activity.setContentView(ui: ViewManager.() -> Unit) =
    ActivityViewManager(this).apply(ui)

class ActivityViewManager(val activity: Activity) : ViewManager {
    override fun addView(p0: View?, p1: ViewGroup.LayoutParams?) {
        activity.setContentView(p0)
    }

    override fun updateViewLayout(p0: View?, p1: ViewGroup.LayoutParams?) {
        TODO("not implemented")
    }

    override fun removeView(p0: View?) {
        TODO("not implemented")
    }
}
val ViewManager.context get() = when (this) {
    is View -> context
    is ActivityViewManager -> activity
    else -> TODO()
}

fun <VM : ViewManager, V : View> VM.add(construct: (Context) -> V, init: V.() -> Unit) {
    construct(context).apply(init).also {
        addView(it, ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
    }
}

fun ViewManager.button(txt: String, listener: (View) -> Unit) {
    add(::Button) {
        text = txt
        setOnClickListener(listener)
    }
}