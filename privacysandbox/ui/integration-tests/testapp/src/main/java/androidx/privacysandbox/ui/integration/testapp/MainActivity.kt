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

package androidx.privacysandbox.ui.integration.testapp

import android.os.Build
import android.os.Bundle
import android.os.RemoteException
import android.os.ext.SdkExtensions
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresExtension
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.sdkruntime.client.SdkSandboxProcessDeathCallbackCompat
import androidx.privacysandbox.sdkruntime.core.LoadSdkCompatException
import androidx.privacysandbox.ui.integration.inappmediateeadaptersdk.InAppMediateeAdapter
import androidx.privacysandbox.ui.integration.sdkproviderutils.ILoadSdkCallback
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.testapp.fragments.BaseFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.FragmentOptions
import androidx.privacysandbox.ui.integration.testapp.fragments.compose.FullscreenSetupComposeFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.compose.FullscreenSetupFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.compose.LazyListFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.compose.ResizeComposeFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.compose.ScrollComposeFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.hidden.BaseHiddenFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.views.PoolingContainerFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.views.ResizeFragment
import androidx.privacysandbox.ui.integration.testapp.fragments.views.ScrollFragment
import androidx.privacysandbox.ui.integration.testapp.util.DisabledItemsArrayAdapter
import androidx.privacysandbox.ui.integration.testsdkprovider.ISdkApi
import androidx.privacysandbox.ui.integration.testsdkprovider.ISdkApiFactory
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
    private lateinit var composeToggleButton: SwitchMaterial
    private lateinit var mediationDropDownMenu: Spinner
    private lateinit var adTypeDropDownMenu: Spinner
    private lateinit var adFormatDropDownMenu: Spinner
    private lateinit var titleBar: TextView
    private lateinit var sdkApi: ISdkApi

    fun getCurrentFragment(): BaseFragment {
        return currentFragment
    }

    @SdkApiConstants.Companion.AdFormat
    private val adFormat
        get() =
            if (::adFormatDropDownMenu.isInitialized) adFormatDropDownMenu.selectedItemPosition
            else SdkApiConstants.Companion.AdFormat.Companion.BANNER_AD

    @SdkApiConstants.Companion.AdType
    private val adType
        get() =
            if (::adTypeDropDownMenu.isInitialized) adTypeDropDownMenu.selectedItemPosition
            else SdkApiConstants.Companion.AdType.Companion.BASIC_NON_WEBVIEW

    @SdkApiConstants.Companion.MediationOption
    private val mediationOption
        get() =
            if (::mediationDropDownMenu.isInitialized) mediationDropDownMenu.selectedItemPosition
            else SdkApiConstants.Companion.MediationOption.Companion.NON_MEDIATED

    private val drawViewabilityLayer
        get() = viewabilityToggleButton.isChecked

    private var useCompose = false
    private var selectedCUJMenuId = R.id.item_resize

    // TODO(b/257429573): Remove this line once fixed.
    @RequiresExtension(extension = SdkExtensions.AD_SERVICES, version = 5)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        titleBar = findViewById(R.id.title_bar)
        drawerLayout = findViewById(R.id.drawer)
        navigationView = findViewById(R.id.navigation_view)
        zOrderToggleButton = findViewById(R.id.zorder_above_switch)
        composeToggleButton = findViewById(R.id.compose_switch)
        viewabilityToggleButton = findViewById(R.id.display_viewability_switch)
        triggerSandboxDeathButton = findViewById(R.id.trigger_sandbox_death)
        mediationDropDownMenu = findViewById(R.id.mediation_dropdown_menu)
        adTypeDropDownMenu = findViewById(R.id.ad_type_dropdown_menu)
        adFormatDropDownMenu = findViewById(R.id.ad_format_dropdown_menu)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // there is no sandbox to kill on T-
            triggerSandboxDeathButton.visibility = View.GONE
        } else {
            triggerSandboxDeathButton.setOnClickListener {
                triggerSandboxDeath()
                setAllControlsEnabled(false)
            }
        }

        sdkSandboxManager = SdkSandboxManagerCompat.Companion.from(applicationContext)
        sdkSandboxManager.addSdkSandboxProcessDeathCallback(Runnable::run, DeathCallbackImpl())
        Log.i(TAG, "Loading SDK")
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val loadedSdks = sdkSandboxManager.getSandboxedSdks()
                var loadedSdk = loadedSdks.firstOrNull { it.getSdkInfo()?.name == SDK_NAME }
                if (loadedSdk == null) {
                    loadedSdk = sdkSandboxManager.loadSdk(SDK_NAME, Bundle())
                    sdkSandboxManager.loadSdk(MEDIATEE_SDK_NAME, Bundle())
                    sdkApi =
                        ISdkApiFactory.wrapToISdkApi(
                            checkNotNull(loadedSdk.getInterface()) { "Cannot find Sdk Service!" }
                        )
                    // Register in-app mediatee adapter with Mediator.
                    sdkApi.registerInAppMediateeAdapter(InAppMediateeAdapter(applicationContext))
                }

                // TODO(b/337793172): Replace with a default fragment
                val extras = intent.extras
                if (extras != null) {
                    val fragmentOptions = FragmentOptions.Companion.createFromIntentExtras(extras)
                    val fragment = fragmentOptions.getFragment()
                    fragment.handleOptionsFromIntent(fragmentOptions)
                    switchContentFragment(fragment, "Automated CUJ")
                    val loadSdkCallback =
                        extras.getBinder(FragmentOptions.Companion.LOAD_SDK_COMPLETE)
                    if (loadSdkCallback != null) {
                        (loadSdkCallback as ILoadSdkCallback).onSdksLoaded()
                    }
                } else {
                    switchContentFragment(ResizeFragment(), "Resize CUJ")
                }

                setWindowsInsetsListener()
                initializeOptionsButton()
                initializeDrawer()
            } catch (e: LoadSdkCompatException) {
                Log.i(
                    TAG,
                    "loadSdk failed with errorCode: " +
                        e.loadSdkErrorCode +
                        " and errorMsg: " +
                        e.message,
                )
            }
        }
        initializeToggles()
    }

    private inner class DeathCallbackImpl : SdkSandboxProcessDeathCallbackCompat {
        override fun onSdkSandboxDied() {
            runOnUiThread {
                Log.i(TAG, "Sandbox died")
                // The if condition makes sure that the toast is not displayed in automated tests.
                if (currentFragment !is BaseHiddenFragment) {
                    Toast.makeText(applicationContext, "Sandbox died", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setWindowsInsetsListener() {
        val params = titleBar.layoutParams as ViewGroup.MarginLayoutParams
        ViewCompat.setOnApplyWindowInsetsListener(titleBar) { view, insets ->
            val systemWindowInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            params.topMargin = systemWindowInsets.top
            view.layoutParams = params

            insets
        }
    }

    /** Kill the sandbox process */
    private fun triggerSandboxDeath() {
        try {
            currentFragment.getSdkApi().triggerProcessDeath()
        } catch (ignored: RemoteException) {
            // avoids a crash when clicking the "trigger sandbox death" button twice
        }
    }

    private fun initializeToggles() {
        initializeViewabilityToggleButton()
        initializeAdFormatDropDown()
        initializeMediationDropDown()
        initializeAdTypeDropDown()
        initializeZOrderToggleButton()
        initializeComposeToggleButton()
    }

    private fun initializeViewabilityToggleButton() {
        viewabilityToggleButton.setOnCheckedChangeListener { _, _ -> loadAllAds() }
    }

    private fun initializeAdFormatDropDown() {
        adFormatDropDownMenu.apply {
            adapter =
                DisabledItemsArrayAdapter(
                    applicationContext,
                    resources.getStringArray(R.array.ad_format_menu_array),
                ) { position: Int ->
                    val isSupported = isSupportedOptionsCombination(position, mediationOption)
                    when (position) {
                        SdkApiConstants.Companion.AdFormat.Companion.BANNER_AD -> isSupported
                        SdkApiConstants.Companion.AdFormat.Companion.NATIVE_AD ->
                            isSupported && supportsNativeAd(currentFragment)

                        else -> false
                    }
                }
            onItemSelectedListener = OnItemSelectedListener()
        }
    }

    private fun initializeMediationDropDown() {
        mediationDropDownMenu.apply {
            // Supply the mediation_option array to the mediationDropDownMenu spinner.
            adapter =
                DisabledItemsArrayAdapter(
                    applicationContext,
                    resources.getStringArray(R.array.mediation_dropdown_menu_array),
                ) { position: Int ->
                    isSupportedOptionsCombination(adFormat, position)
                }
            onItemSelectedListener = OnItemSelectedListener()
        }
    }

    private fun initializeAdTypeDropDown() {
        adTypeDropDownMenu.apply {
            adapter =
                DisabledItemsArrayAdapter(
                    applicationContext,
                    resources.getStringArray(R.array.ad_type_dropdown_menu_array),
                ) { _: Int ->
                    isSupportedOptionsCombination(adFormat, mediationOption)
                }
            onItemSelectedListener = OnItemSelectedListener()
        }
    }

    private fun initializeZOrderToggleButton() {
        zOrderToggleButton.setOnCheckedChangeListener { _, isChecked ->
            BaseFragment.Companion.isZOrderAboveToggleChecked = isChecked
        }
    }

    private fun initializeComposeToggleButton() {
        composeToggleButton.setOnCheckedChangeListener { _, isChecked ->
            useCompose = isChecked
            selectCuj(navigationView.menu.findItem(selectedCUJMenuId))
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
            object : DrawerLayout.DrawerListener {
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
            selectCuj(it)
            selectedCUJMenuId = it.itemId
            true
        }
    }

    private fun selectCuj(menuItem: MenuItem) {
        when (menuItem.itemId) {
            R.id.item_resize ->
                if (useCompose) {
                    switchContentFragment(
                        ResizeComposeFragment(),
                        "${menuItem.title} ${getString(R.string.compose)}",
                    )
                } else {
                    switchContentFragment(ResizeFragment(), menuItem.title)
                }
            R.id.item_scroll ->
                if (useCompose) {
                    switchContentFragment(
                        ScrollComposeFragment(),
                        "${menuItem.title} ${getString(R.string.compose)}",
                    )
                } else {
                    switchContentFragment(ScrollFragment(), menuItem.title)
                }
            R.id.item_pooling_container ->
                if (useCompose) {
                    switchContentFragment(
                        LazyListFragment(),
                        "${menuItem.title} ${getString(R.string.compose)}",
                    )
                } else {
                    switchContentFragment(PoolingContainerFragment(), menuItem.title)
                }
            R.id.item_fullscreen ->
                if (useCompose) {
                    switchContentFragment(
                        FullscreenSetupComposeFragment(),
                        getString(R.string.fullscreen_compose_cuj),
                    )
                } else {
                    switchContentFragment(FullscreenSetupFragment(), menuItem.title)
                }
            else -> {
                Log.e(TAG, "Invalid fragment option")
            }
        }
    }

    private fun isSupportedOptionsCombination(
        @AdFormat adFormat: Int,
        @MediationOption mediationOption: Int,
    ): Boolean {
        when (adFormat) {
            SdkApiConstants.Companion.AdFormat.Companion.BANNER_AD -> return true
            SdkApiConstants.Companion.AdFormat.Companion.NATIVE_AD -> {
                when (mediationOption) {
                    SdkApiConstants.Companion.MediationOption.Companion.NON_MEDIATED,
                    SdkApiConstants.Companion.MediationOption.Companion.IN_APP_MEDIATEE,
                    SdkApiConstants.Companion.MediationOption.Companion.SDK_RUNTIME_MEDIATEE ->
                        return true
                    SdkApiConstants.Companion.MediationOption.Companion
                        .SDK_RUNTIME_MEDIATEE_WITH_OVERLAY,
                    SdkApiConstants.Companion.MediationOption.Companion.REFRESHABLE_MEDIATION ->
                        return false
                }
            }
        }
        return false
    }

    // TODO(b/3300859): remove once all non-fullscreen fragments are supported for native.
    private fun supportsNativeAd(fragment: BaseFragment): Boolean =
        fragment is ResizeFragment || fragment is PoolingContainerFragment

    private fun updateDrawerOptions() {
        setAllControlsEnabled(true)
        if (adFormat == SdkApiConstants.Companion.AdFormat.Companion.NATIVE_AD) {
            runOnUiThread { navigationView.menu.findItem(R.id.item_scroll).isEnabled = false }
            viewabilityToggleButton.isEnabled = false
            composeToggleButton.isEnabled = false
        }
    }

    private fun setAllControlsEnabled(isEnabled: Boolean) {
        runOnUiThread { navigationView.menu.forEach { it.isEnabled = isEnabled } }
        adFormatDropDownMenu.isEnabled = isEnabled
        mediationDropDownMenu.isEnabled = isEnabled
        adTypeDropDownMenu.isEnabled = isEnabled
        viewabilityToggleButton.isEnabled = isEnabled
        zOrderToggleButton.isEnabled = isEnabled
        composeToggleButton.isEnabled = isEnabled
    }

    private fun switchContentFragment(fragment: BaseFragment, title: CharSequence?): Boolean {
        updateDrawerOptions()
        drawerLayout.closeDrawers()
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.content_fragment_container, fragment)
            .commit()
        currentFragment = fragment
        title?.let { runOnUiThread { titleBar.text = title } }
        return true
    }

    /** Loads all ads in the current fragment. */
    private fun loadAllAds() {
        currentFragment.handleLoadAdFromDrawer(
            adFormat,
            adType,
            mediationOption,
            drawViewabilityLayer,
        )
    }

    private inner class OnItemSelectedListener : AdapterView.OnItemSelectedListener {
        var isCalledOnStartingApp = true

        override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
            if (isCalledOnStartingApp) {
                isCalledOnStartingApp = false
                return
            }
            updateDrawerOptions()
            loadAllAds()
        }

        override fun onNothingSelected(parent: AdapterView<*>?) {}
    }

    companion object {
        private const val TAG = "TestSandboxClient"

        /** Name of the SDK to be loaded. */
        const val SDK_NAME = "androidx.privacysandbox.ui.integration.testsdkproviderwrapper"
        const val MEDIATEE_SDK_NAME =
            "androidx.privacysandbox.ui.integration.mediateesdkproviderwrapper"
    }
}
