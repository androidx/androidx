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

package androidx.camera.integration.uiwidgets.viewpager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.camera.integration.uiwidgets.databinding.ActivityViewpager2Binding
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayoutMediator

/** A activity uses ViewPager2 as container to include {@link CameraFragment} and
 * {@link TextViewFragment} */
class ViewPager2Activity : BaseActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA
        )
        private const val TAG = " ViewPager2Activity"
        private const val REQUEST_CODE_PERMISSIONS = 6
        @VisibleForTesting
        val BLANK_VIEW_ID = View.generateViewId()
        @VisibleForTesting
        val CAMERA_VIEW_ID = View.generateViewId()
    }

    private lateinit var binding: ActivityViewpager2Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate.")

        binding = ActivityViewpager2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (allPermissionsGranted()) {
                setupAdapter()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    REQUIRED_PERMISSIONS,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        } else {
            setupAdapter()
        }
    }

    private fun setupAdapter() {
        binding.viewPager2.adapter = ViewPager2Adapter(this@ViewPager2Activity)
        TabLayoutMediator(binding.tabLayout, binding.viewPager2) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "CAMERA_VIEW"
                    tab.view.id = CAMERA_VIEW_ID
                }
                1 -> {
                    tab.text = "BLANK_VIEW"
                    tab.view.id = BLANK_VIEW_ID
                }
                else -> throw IllegalArgumentException()
            }
        }.attach()
    }

    @Deprecated("Deprecated in ComponentActivity")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                setupAdapter()
            } else {
                Log.e(TAG, "Permissions not granted by the user.")
            }
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    internal class ViewPager2Adapter(fragmentActivity: FragmentActivity) :
        FragmentStateAdapter(fragmentActivity) {

        override fun getItemCount(): Int {
            return 2
        }

        override fun createFragment(position: Int): Fragment = when (position) {
            0 -> CameraFragment.newInstance()
            1 -> TextViewFragment.newInstance()
            else -> throw IllegalArgumentException()
        }
    }
}
