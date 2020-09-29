/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.integration.uiwidgets

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.integration.uiwidgets.rotations.LockedOrientationActivity
import androidx.camera.integration.uiwidgets.rotations.OrientationConfigChangesOverriddenActivity
import androidx.camera.integration.uiwidgets.rotations.UnlockedOrientationActivity
import androidx.camera.integration.uiwidgets.viewpager.ViewPager2Activity
import androidx.camera.integration.uiwidgets.viewpager.ViewPagerActivity
import kotlinx.android.synthetic.main.activity_main.rotationConfigChanges
import kotlinx.android.synthetic.main.activity_main.rotationLocked
import kotlinx.android.synthetic.main.activity_main.rotationUnlocked
import kotlinx.android.synthetic.main.activity_main.viewpager
import kotlinx.android.synthetic.main.activity_main.viewpager2

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        rotationUnlocked.setOnClickListener { launch(UnlockedOrientationActivity::class.java) }
        rotationLocked.setOnClickListener { launch(LockedOrientationActivity::class.java) }
        rotationConfigChanges.setOnClickListener {
            launch(OrientationConfigChangesOverriddenActivity::class.java)
        }
        viewpager.setOnClickListener { launch(ViewPagerActivity::class.java) }
        viewpager2.setOnClickListener { launch(ViewPager2Activity::class.java) }
    }

    private fun <A : AppCompatActivity> launch(activityClass: Class<A>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }
}