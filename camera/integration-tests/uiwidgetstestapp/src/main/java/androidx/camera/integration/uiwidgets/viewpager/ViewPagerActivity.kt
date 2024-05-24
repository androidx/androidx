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
@file:Suppress("DEPRECATION")

package androidx.camera.integration.uiwidgets.viewpager

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.camera.integration.uiwidgets.databinding.ActivityViewpagerBinding
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter

/**
 * An activity uses ViewPager as a container to include {@link CameraFragment} and {@link
 * TextViewFragment}. The main usage difference between ViewPager2 and ViewPager is the viewpager
 * adapter. ViewPager2 adapter is from {@link FragmentStateAdapter} and ViewPager adapter is the
 * deprecated (@link FragmentStatePagerAdapter}.
 */
class ViewPagerActivity : BaseActivity() {

    companion object {
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private const val TAG = " ViewPagerActivity"
        private const val REQUEST_CODE_PERMISSIONS = 8

        @VisibleForTesting const val CAMERA_FRAGMENT_TAB_TITLE = "CAMERA_VIEW"

        @VisibleForTesting const val BLANK_FRAGMENT_TAB_TITLE = "BLANK_VIEW"
    }

    private lateinit var binding: ActivityViewpagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate.")

        binding = ActivityViewpagerBinding.inflate(layoutInflater)
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

    /**
     * There are 2 common adapters for ViewPager, they are deprecated (@link
     * FragmentStatePagerAdapter} and (@link FragmentPagerAdapter}
     */
    private fun setupAdapter() {
        Log.d(TAG, "Setup ViewPagerAdapter. ")
        binding.viewPager.adapter = ViewPagerAdapter(supportFragmentManager)
        binding.tabLayout.setupWithViewPager(binding.viewPager)
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
            if (
                ContextCompat.checkSelfPermission(this, permission) !=
                    PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
        }
        return true
    }

    internal class ViewPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

        override fun getCount(): Int {
            return 2
        }

        override fun getItem(position: Int): Fragment =
            when (position) {
                0 -> CameraFragment.newInstance()
                1 -> TextViewFragment.newInstance()
                else -> throw IllegalArgumentException()
            }

        override fun getPageTitle(position: Int) =
            when (position) {
                0 -> CAMERA_FRAGMENT_TAB_TITLE
                1 -> BLANK_FRAGMENT_TAB_TITLE
                else -> throw IllegalArgumentException()
            }
    }
}
