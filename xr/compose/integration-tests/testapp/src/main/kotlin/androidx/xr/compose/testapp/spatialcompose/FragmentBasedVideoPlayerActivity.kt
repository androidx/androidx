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

package androidx.xr.compose.testapp.spatialcompose

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.xr.compose.testapp.R
import java.io.File

/**
 * For quickly playing a video in SpatialExternalSurface from a fragment, without configurations.
 * Requires adb pushing video assets with matching file paths.
 */
class FragmentBasedVideoPlayerActivity : AppCompatActivity() {
    private val TAG = "FragmentBasedVideoPlayerActivity"
    private val drmVideoUri =
        Environment.getExternalStorageDirectory().path + "/Download/sdr_singleview_protected.mp4"
    private val regularVideoUri =
        Environment.getExternalStorageDirectory().path + "/Download/vid_bigbuckbunny.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkFile(drmVideoUri)
        checkFile(regularVideoUri)

        setContentView(R.layout.activity_fragment_layout)

        // This check is crucial: only add the fragment on the *first* creation.
        // On screen rotation, the system automatically restores the fragment.
        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.content_container, VideoPlayerFragment::class.java, null)
                .commit()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun checkFile(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) {
            Log.e(TAG, "$filePath does not exist. Did you adb push the asset?")
            Toast.makeText(
                    this@FragmentBasedVideoPlayerActivity,
                    "$filePath does not exist. Did you adb push the asset?",
                    Toast.LENGTH_LONG,
                )
                .show()
            finish()
        }
    }
}
