/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.privacysandbox.ui.integration.testapp

import android.os.Bundle
import android.os.ext.SdkExtensions
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var sdkSandboxManager: SdkSandboxManagerCompat
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var currentFragment: BaseFragment

    // TODO(b/257429573): Remove this line once fixed.
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerLayout = findViewById(R.id.drawer)
        navigationView = findViewById(R.id.navigation_view)

        sdkSandboxManager = SdkSandboxManagerCompat.from(applicationContext)

        Log.i(TAG, "Loading SDK")
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val loadedSdks = sdkSandboxManager.getSandboxedSdks()
                val loadedSdk = loadedSdks.firstOrNull { it.getSdkInfo()?.name == SDK_NAME }
                if (loadedSdk == null) {
                    sdkSandboxManager.loadSdk(SDK_NAME, Bundle())
                    sdkSandboxManager.loadSdk(MEDIATEE_SDK_NAME, Bundle())
                    sdkSandboxManager.registerAppOwnedSdkSandboxInterface(
                        AppOwnedSdkSandboxInterfaceCompat(
                            MEDIATEE_SDK_NAME,
                            /*version=*/ 0,
                            AppOwnedMediateeSdkApi(applicationContext)
                        )
                    )
                }
                switchContentFragment(MainFragment(), "Main CUJ")
                initializeOptionsButton()
                initializeDrawer()
            } catch (e: LoadSdkCompatException) {
                Log.i(
                    TAG, "loadSdk failed with errorCode: " + e.loadSdkErrorCode +
                        " and errorMsg: " + e.message
                )
            }
        }
    }

    private fun initializeOptionsButton() {
        val button: Button = findViewById(R.id.toggle_drawer_button)
        button.setOnClickListener {
            if (drawerLayout.isOpen) {
                drawerLayout.closeDrawers()
            } else {
                currentFragment.handleDrawerStateChange(true)
                drawerLayout.open()
            }
        }
    }

    private fun initializeDrawer() {
        drawerLayout.addDrawerListener(object : DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            }

            override fun onDrawerOpened(drawerView: View) {
                // we handle this in the button onClick instead
            }

            override fun onDrawerClosed(drawerView: View) {
                currentFragment.handleDrawerStateChange(false)
            }

            override fun onDrawerStateChanged(newState: Int) {
            }
        })
        navigationView.setNavigationItemSelectedListener {
            val itemId = it.itemId
            when (itemId) {
                R.id.item_main -> switchContentFragment(MainFragment(), it.title)
                R.id.item_pooling_container -> switchContentFragment(PoolingContainerFragment(),
                    it.title)
                R.id.item_sandbox_death -> switchContentFragment(SandboxDeathFragment(), it.title)
                else -> {
                    Log.e(TAG, "Invalid fragment option")
                    true
                }
            }
        }
    }

    private fun switchContentFragment(fragment: BaseFragment, title: CharSequence?): Boolean {
        drawerLayout.closeDrawers()
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_fragment_container, fragment).commit()
        currentFragment = fragment
        title?.let {
            runOnUiThread {
                setTitle(it)
            }
        }
        return true
    }

    companion object {
        private const val TAG = "TestSandboxClient"

        /**
         * Name of the SDK to be loaded.
         */
        private const val SDK_NAME = "androidx.privacysandbox.ui.integration.testsdkprovider"
        private const val MEDIATEE_SDK_NAME =
            "androidx.privacysandbox.ui.integration.mediateesdkprovider"
    }
}
