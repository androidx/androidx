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
import android.content.Context
import android.os.Bundle
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
import androidx.activity.invoke
import androidx.activity.prepareCall
import androidx.activity.result.contract.ActivityResultContracts.Dial
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.result.contract.ActivityResultContracts.TakePicture

class MainActivity : ComponentActivity() {

    val requestLocation = prepareCall(RequestPermission(), ACCESS_FINE_LOCATION) { isGranted ->
        toast("Location granted: $isGranted")
    }

    val takePicture = prepareCall(TakePicture()) { bitmap ->
        toast("Got picture: $bitmap")
    }

    val dial = prepareCall(Dial()) { success ->
        toast("Dial success: $success")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView {
            add(::LinearLayout) {
                orientation = VERTICAL

                button("Request location permission") {
                    requestLocation()
                }
                button("Take pic") {
                    takePicture()
                }
                button("Dial 111111111") {
                    dial("111111111")
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