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

package androidx.wear.watchface.ui

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.wearable.watchface.Constants
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RestrictTo
import androidx.annotation.RestrictTo.Scope.LIBRARY
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.wear.complications.ComplicationHelperActivity
import androidx.wear.complications.data.ComplicationType
import androidx.wear.watchface.style.UserStyle
import androidx.wear.watchface.style.UserStyleSchema

/** @hide */
@RestrictTo(LIBRARY)
internal interface FragmentController {
    /** Show the [ConfigFragment] which lets the user select what they want to configure. */
    fun showConfigFragment()

    /**
     * Show the [ComplicationConfigFragment] which lets the user select the complication
     * they want to configure.
     */
    fun showComplicationConfigSelectionFragment()

    /** Show the [StyleConfigFragment] which lets the user configure the watch face style. */
    fun showStyleConfigFragment(
        settingId: String,
        styleSchema: UserStyleSchema,
        userStyle: UserStyle
    )

    /** Lets the user configure the complication provider for a single complication slot. */
    fun showComplicationConfig(
        complicationId: Int,
        vararg supportedComplicationDataTypes: Int
    )
}

/**
 * Config activity for the watch face, which supports complication and provider selection, as well
 * as userStyle configuration.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@TargetApi(16)
@SuppressWarnings("ForbiddenSuperClass")
public // Not intended to be composable.
class WatchFaceConfigActivity : FragmentActivity() {
    internal lateinit var watchFaceConfigDelegate: WatchFaceConfigDelegate
        private set

    internal lateinit var styleSchema: UserStyleSchema
        private set

    internal lateinit var watchFaceComponentName: ComponentName
        private set

    internal lateinit var fragmentController: FragmentController

    internal var backgroundComplicationId: Int? = null
        private set

    public companion object {
        private const val TAG = "WatchFaceConfigActivity"

        private val sComponentNameToIWatchFaceConfig =
            HashMap<ComponentName, WatchFaceConfigDelegate>()

        /** @hide */
        @SuppressWarnings("SyntheticAccessor")
        @JvmStatic
        public fun registerWatchFace(
            componentName: ComponentName,
            watchFaceConfigDelegate: WatchFaceConfigDelegate
        ) {
            sComponentNameToIWatchFaceConfig[componentName] = watchFaceConfigDelegate
        }

        @SuppressWarnings("SyntheticAccessor")
        @JvmStatic
        public fun unregisterWatchFace(componentName: ComponentName) {
            sComponentNameToIWatchFaceConfig.remove(componentName)
        }

        @SuppressWarnings("SyntheticAccessor")
        @JvmStatic
        internal fun getIWatchFaceConfig(componentName: ComponentName): WatchFaceConfigDelegate? {
            return sComponentNameToIWatchFaceConfig[componentName]
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val context = this as Context
        val componentName: ComponentName =
            intent.getParcelableExtra(Constants.EXTRA_WATCH_FACE_COMPONENT) ?: return

        init(
            componentName,
            object : FragmentController {
                @SuppressLint("SyntheticAccessor")
                override fun showConfigFragment() {
                    showFragment(ConfigFragment())
                }

                @SuppressLint("SyntheticAccessor")
                override fun showComplicationConfigSelectionFragment() {
                    showFragment(ComplicationConfigFragment())
                }

                @SuppressLint("SyntheticAccessor")
                override fun showStyleConfigFragment(
                    settingId: String,
                    styleSchema: UserStyleSchema,
                    userStyle: UserStyle
                ) {
                    showFragment(
                        StyleConfigFragment.newInstance(settingId, styleSchema, userStyle)
                    )
                }

                /**
                 * Displays a config screen which allows the user to select the data source for the
                 * complication.
                 */
                @SuppressWarnings("deprecation")
                override fun showComplicationConfig(
                    complicationId: Int,
                    vararg supportedComplicationDataTypes: Int
                ) {
                    startActivityForResult(
                        ComplicationHelperActivity.createProviderChooserHelperIntent(
                            context,
                            watchFaceComponentName,
                            complicationId,
                            supportedComplicationDataTypes
                        ),
                        Constants.PROVIDER_CHOOSER_REQUEST_CODE
                    )
                }
            }
        )
    }

    private fun focusCurrentFragment() {
        val curFragment = supportFragmentManager.findFragmentById(android.R.id.content)
        if (curFragment != null) {
            curFragment.view?.importantForAccessibility =
                View.IMPORTANT_FOR_ACCESSIBILITY_YES
        }

        // Clear focus on the old fragment that is behind the new one, and announce the new title.
        // This prevents the Talkback linear navigation from selecting elements that are behind the
        // new fragment.
        window.decorView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)
    }

    private fun showFragment(fragment: Fragment) {
        val curFragment = supportFragmentManager.findFragmentById(android.R.id.content)
        curFragment?.view?.importantForAccessibility =
            View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        // The new fragment will have its importance set by OnBackStackChangedListener.
        supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment)
            .addToBackStack(null)
            .commit()
    }

    @VisibleForTesting
    internal fun init(
        watchFaceComponentName: ComponentName,
        fragmentController: FragmentController
    ) {
        this.watchFaceComponentName = watchFaceComponentName
        this.fragmentController = fragmentController

        watchFaceConfigDelegate = getIWatchFaceConfig(watchFaceComponentName) ?: run {
            Log.e(TAG, "Unknown watchFace $watchFaceComponentName")
            return
        }

        supportFragmentManager
            .addOnBackStackChangedListener {
                if (supportFragmentManager.backStackEntryCount == 0) {
                    finish()
                } else {
                    focusCurrentFragment()
                }
            }

        styleSchema = UserStyleSchema(watchFaceConfigDelegate.getUserStyleSchema())

        backgroundComplicationId = watchFaceConfigDelegate.getBackgroundComplicationId()

        var topLevelOptionCount = styleSchema.userStyleSettings.size
        val hasBackgroundComplication = backgroundComplicationId != null
        if (hasBackgroundComplication) {
            topLevelOptionCount++
        }
        val numComplications = watchFaceConfigDelegate.getComplicationsMap().size
        val hasNonBackgroundComplication =
            numComplications > (if (hasBackgroundComplication) 1 else 0)
        if (hasNonBackgroundComplication) {
            topLevelOptionCount++
        }

        when {
            // More than one top level option, so show a fragment which lets the user choose what
            // they want to configure.
            topLevelOptionCount > 1 -> fragmentController.showConfigFragment()

            // For a single complication go directly to the provider selector.
            numComplications == 1 -> {
                val onlyComplication = watchFaceConfigDelegate.getComplicationsMap().values.first()
                fragmentController.showComplicationConfig(
                    onlyComplication.id,
                    *ComplicationType.toWireTypes(onlyComplication.supportedTypes)
                )
            }

            // For multiple complications select the complication to configure first.
            numComplications > 1 -> fragmentController.showComplicationConfigSelectionFragment()

            // For a single style, go select the option.
            styleSchema.userStyleSettings.size == 1 -> {
                // There should only be a single userStyle setting if we get here.
                val onlyStyleSetting = styleSchema.userStyleSettings.first()
                fragmentController.showStyleConfigFragment(
                    onlyStyleSetting.id,
                    styleSchema,
                    UserStyle(
                        watchFaceConfigDelegate.getUserStyle(),
                        styleSchema
                    )
                )
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        finish()
    }
}
