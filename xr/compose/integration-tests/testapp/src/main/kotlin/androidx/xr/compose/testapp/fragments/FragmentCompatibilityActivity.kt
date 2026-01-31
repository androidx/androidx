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

package androidx.xr.compose.testapp.fragments

import android.os.Bundle
import androidx.fragment.app.FragmentActivity

class FragmentCompatibilityActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (savedInstanceState == null) {
            val simpleSpatialFragmentBundle = Bundle().apply { putFloat("x_offset", -300f) }
            val simpleSpatialFragment =
                SimpleSpatialFragment().apply { arguments = simpleSpatialFragmentBundle }
            val simpleTextFragmentBundle =
                Bundle().apply {
                    putFloat("x_offset", 200f)
                    putString("text", "This is SecondFragment to see multi fragment view")
                }
            val simpleTextFragment =
                SimpleTextFragment().apply { arguments = simpleTextFragmentBundle }

            supportFragmentManager
                .beginTransaction()
                .add(android.R.id.content, simpleSpatialFragment, "fragment1")
                .add(android.R.id.content, simpleTextFragment, "fragment2")
                .commit()
        }
    }

    fun showVideoPlayerFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, VideoPlayerFragment())
            .addToBackStack(null)
            .commit()
    }

    fun showMainPanelFragment() {
        supportFragmentManager
            .beginTransaction()
            .replace(android.R.id.content, MainPanelFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun finish() {
        // Workaround for session - fragment lifecycle issue. Need further investigation b/463762377
        val fragment = supportFragmentManager.findFragmentById(android.R.id.content)
        if (fragment != null) {
            supportFragmentManager.beginTransaction().remove(fragment).commit()
            supportFragmentManager.executePendingTransactions()
        }

        super.finish()
    }
}
