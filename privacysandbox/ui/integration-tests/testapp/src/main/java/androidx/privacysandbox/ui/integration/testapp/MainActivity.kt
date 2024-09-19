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

import android.os.Build
import android.os.Bundle
import android.os.ext.SdkExtensions
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.drawerlayout.widget.DrawerLayout.DrawerListener
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.client.SdkSandboxProcessDeathCallbackCompat
import androidx.privacysandbox.sdkruntime.core.AppOwnedSdkSandboxInterfaceCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.ui.integration.sdkproviderutils.MediateeSdkApiImpl
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import com.google.android.material.navigation.NavigationView
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var sdkSandboxManager: SdkSandboxManagerCompat
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var currentFragment: BaseFragment
    private lateinit var triggerSandboxDeathButton: Button
    private lateinit var zOrderToggleButton: SwitchMaterial
    private lateinit var viewabilityToggleButton: SwitchMaterial
    private lateinit var mediationDropDownMenu: Spinner
    private lateinit var adTypeDropDownMenu: Spinner

    @AdType private var adType = AdType.BASIC_NON_WEBVIEW

    @MediationOption private var mediationOption = MediationOption.NON_MEDIATED
    private var drawViewabilityLayer = false

    // TODO(b/257429573): Remove this line once fixed.
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawerLayout = findViewById(R.id.drawer)
        navigationView = findViewById(R.id.navigation_view)
        zOrderToggleButton = findViewById(R.id.zorder_below_switch)
        viewabilityToggleButton = findViewById(R.id.display_viewability_switch)
        triggerSandboxDeathButton = findViewById(R.id.trigger_sandbox_death)
        mediationDropDownMenu = findViewById(R.id.mediation_dropdown_menu)
        adTypeDropDownMenu = findViewById(R.id.ad_type_dropdown_menu)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // there is no sandbox to kill on T-
            triggerSandboxDeathButton.visibility = View.GONE
        } else {
            triggerSandboxDeathButton.setOnClickListener {
                triggerSandboxDeath()
                disableAllControls()
            }
        }

        sdkSandboxManager = SdkSandboxManagerCompat.from(applicationContext)
        sdkSandboxManager.addSdkSandboxProcessDeathCallback(Runnable::run, DeathCallbackImpl())
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
                            MediateeSdkApiImpl(applicationContext)
                        )
                    )
                }

                // TODO(b/337793172): Replace with a default fragment
                switchContentFragment(ResizeFragment(), "Resize CUJ")

                initializeOptionsButton()
                initializeDrawer()
            } catch (e: LoadSdkCompatException) {
                Log.i(
                    TAG,
                    "loadSdk failed with errorCode: " +
                        e.loadSdkErrorCode +
                        " and errorMsg: " +
                        e.message
                )
            }
        }
        initializeToggles()
    }

    override fun onDestroy() {
        super.onDestroy()
        sdkSandboxManager.unregisterAppOwnedSdkSandboxInterface(MEDIATEE_SDK_NAME)
    }

    private inner class DeathCallbackImpl : SdkSandboxProcessDeathCallbackCompat {
        override fun onSdkSandboxDied() {
            runOnUiThread {
                Log.i(TAG, "Sandbox died")
                Toast.makeText(applicationContext, "Sandbox died", Toast.LENGTH_LONG).show()
            }
        }
    }

    /** Kill the sandbox process */
    private fun triggerSandboxDeath() {
        currentFragment.getSdkApi().triggerProcessDeath()
    }

    private fun initializeToggles() {
        initializeViewabilityToggleButton()
        initializeMediationDropDown()
        initializeAdTypeDropDown()
        initializeZOrderToggleButton()
    }

    private fun disableAllControls() {
        mediationDropDownMenu.isEnabled = false
        adTypeDropDownMenu.isEnabled = false
        viewabilityToggleButton.isEnabled = false
        zOrderToggleButton.isEnabled = false
    }

    private fun enableAllControls() {
        mediationDropDownMenu.isEnabled = true
        adTypeDropDownMenu.isEnabled = true
        viewabilityToggleButton.isEnabled = true
        zOrderToggleButton.isEnabled = true
    }

    private fun initializeViewabilityToggleButton() {
        viewabilityToggleButton.setOnCheckedChangeListener { _, isChecked ->
            drawViewabilityLayer = isChecked
            loadAllAds()
        }
    }

    private fun initializeMediationDropDown() {
        // Supply the mediation_option array to the mediationDropDownMenu spinner.
        ArrayAdapter.createFromResource(
                applicationContext,
                R.array.mediation_dropdown_menu_array,
                android.R.layout.simple_spinner_item
            )
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                mediationDropDownMenu.adapter = adapter
            }

        mediationDropDownMenu.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                var isCalledOnStartingApp = true

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    selectedMediationOptionId: Long
                ) {
                    if (isCalledOnStartingApp) {
                        isCalledOnStartingApp = false
                        return
                    }

                    mediationOption = selectedMediationOptionId.toInt()
                    loadAllAds()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun initializeAdTypeDropDown() {
        ArrayAdapter.createFromResource(
                applicationContext,
                R.array.ad_type_dropdown_menu_array,
                android.R.layout.simple_spinner_item
            )
            .also { adapter ->
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                adTypeDropDownMenu.adapter = adapter
            }

        adTypeDropDownMenu.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                var isCalledOnStartingApp = true

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    selectedAdOptionId: Long
                ) {
                    if (isCalledOnStartingApp) {
                        isCalledOnStartingApp = false
                        return
                    }
                    adType =
                        when (position) {
                            0 -> AdType.BASIC_NON_WEBVIEW
                            1 -> AdType.BASIC_WEBVIEW
                            2 -> AdType.WEBVIEW_FROM_LOCAL_ASSETS
                            3 -> AdType.NON_WEBVIEW_VIDEO
                            else -> AdType.BASIC_NON_WEBVIEW
                        }
                    loadAllAds()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
    }

    private fun initializeZOrderToggleButton() {
        zOrderToggleButton.setOnCheckedChangeListener { _, isChecked ->
            BaseFragment.isZOrderOnTop = !isChecked
        }
    }

    private fun initializeOptionsButton() {
        val button: Button = findViewById(R.id.toggle_drawer_button)
        button.setOnClickListener {
            if (drawerLayout.isOpen) {
                drawerLayout.closeDrawers()
            } else {
                drawerLayout.open()
            }
        }
    }

    private fun initializeDrawer() {
        drawerLayout.addDrawerListener(
            object : DrawerListener {
                private var isDrawerOpen = false

                override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                    if (!isDrawerOpen) {
                        isDrawerOpen = true
                        currentFragment.handleDrawerStateChange(isDrawerOpen = true)
                    } else if (slideOffset == 0f) {
                        isDrawerOpen = false
                        currentFragment.handleDrawerStateChange(isDrawerOpen = false)
                    }
                }

                override fun onDrawerOpened(drawerView: View) {}

                override fun onDrawerClosed(drawerView: View) {}

                override fun onDrawerStateChanged(newState: Int) {}
            }
        )
        navigationView.setNavigationItemSelectedListener {
            val itemId = it.itemId
            when (itemId) {
                R.id.item_resize -> switchContentFragment(ResizeFragment(), it.title)
                R.id.item_scroll -> switchContentFragment(ScrollFragment(), it.title)
                R.id.item_pooling_container ->
                    switchContentFragment(PoolingContainerFragment(), it.title)
                else -> {
                    Log.e(TAG, "Invalid fragment option")
                    true
                }
            }
        }
    }

    private fun switchContentFragment(fragment: BaseFragment, title: CharSequence?): Boolean {
        enableAllControls()
        drawerLayout.closeDrawers()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_fragment_container, fragment)
            .commit()
        currentFragment = fragment
        title?.let { runOnUiThread { setTitle(it) } }
        return true
    }

    /** Loads all ads in the current fragment. */
    private fun loadAllAds() {
        currentFragment.handleLoadAdFromDrawer(adType, mediationOption, drawViewabilityLayer)
    }

    companion object {
        private const val TAG = "TestSandboxClient"

        /** Name of the SDK to be loaded. */
        private const val SDK_NAME = "androidx.privacysandbox.ui.integration.testsdkprovider"
        private const val MEDIATEE_SDK_NAME =
            "androidx.privacysandbox.ui.integration.mediateesdkprovider"
    }
}
