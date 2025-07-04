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

package androidx.privacysandbox.ui.integration.testapp.endtoend

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.privacysandbox.sdkruntime.client.SdkSandboxManagerCompat
import androidx.privacysandbox.ui.integration.sdkproviderutils.IAutomatedTestCallback
import androidx.privacysandbox.ui.integration.sdkproviderutils.ILoadSdkCallback
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AUTOMATED_TEST_CALLBACK
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdFormat
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.AdType
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.FragmentOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.UiFrameworkOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.ZOrderOption
import androidx.privacysandbox.ui.integration.testapp.MainActivity
import androidx.privacysandbox.ui.integration.testapp.fragments.FragmentOptions
import androidx.privacysandbox.ui.integration.testapp.fragments.hidden.BaseHiddenFragment
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Before

abstract class AutomatedEndToEndTest(
    @FragmentOption private val uiFragment: String,
    @UiFrameworkOption private val uiFrameworkOption: String = FragmentOptions.UI_FRAMEWORK_VIEW,
    @AdFormat private val adFormat: String = FragmentOptions.AD_FORMAT_BANNER_AD,
    @MediationOption
    private val mediationOption: String = FragmentOptions.MEDIATION_TYPE_NON_MEDIATED,
    @ZOrderOption private val zOrdering: String = FragmentOptions.Z_ORDER_BELOW,
    @AdType private val adType: String = FragmentOptions.AD_TYPE_NON_WEBVIEW,
) {
    lateinit var scenario: ActivityScenario<MainActivity>
    lateinit var sdkToClientCallback: SdkToClientCallback
    private lateinit var baseHiddenFragment: BaseHiddenFragment

    companion object {
        const val CALLBACK_WAIT_MS = 2000L
        const val UI_CHANGE_WAIT_MS = 2000L

        fun baseParameters(): Collection<Array<String>> {
            val uiFrameworkOptions =
                arrayOf(FragmentOptions.UI_FRAMEWORK_VIEW, FragmentOptions.UI_FRAMEWORK_COMPOSE)
            val mediationOptions =
                arrayOf(
                    FragmentOptions.MEDIATION_TYPE_NON_MEDIATED,
                    FragmentOptions.MEDIATION_TYPE_IN_RUNTIME,
                    FragmentOptions.MEDIATION_TYPE_IN_APP,
                )
            val zOrderings = arrayOf(FragmentOptions.Z_ORDER_BELOW, FragmentOptions.Z_ORDER_ABOVE)
            val testData = mutableListOf<Array<String>>()
            for (uiFrameworkOption in uiFrameworkOptions) {
                for (mediation in mediationOptions) {
                    for (zOrder in zOrderings) {
                        // Testing z-above while SDK run in App is meaningless.
                        if (
                            zOrder == FragmentOptions.Z_ORDER_ABOVE &&
                                mediation == FragmentOptions.MEDIATION_TYPE_IN_APP
                        ) {
                            continue
                        }
                        testData.add(arrayOf(uiFrameworkOption, mediation, zOrder))
                    }
                }
            }
            return testData
        }

        fun customParams(vararg params: Array<String>): Collection<Array<String>> {
            if (params.isEmpty()) {
                return emptyList()
            }

            var testData = params[0].map { arrayOf(it) }

            for (i in 1 until params.size) {
                val nextParam = params[i]
                val newResult = mutableListOf<Array<String>>()

                for (existingCombination in testData) {
                    for (element in nextParam) {
                        newResult.add(existingCombination + element)
                    }
                }
                testData = newResult
            }

            return testData
        }
    }

    @Before
    fun baseSetup() {
        launchTestAppAndWaitForLoadingSdks(
            uiFragment,
            uiFrameworkOption,
            adFormat,
            mediationOption,
            zOrdering,
            adType,
        )
        sdkToClientCallback = SdkToClientCallback()
        val sdkToClientCallbackBundle = Bundle()
        sdkToClientCallbackBundle.putBinder(AUTOMATED_TEST_CALLBACK, sdkToClientCallback)
        scenario.onActivity { activity ->
            baseHiddenFragment = activity.getCurrentFragment() as BaseHiddenFragment
            baseHiddenFragment.loadAd(sdkToClientCallbackBundle)
        }
        assertThat(baseHiddenFragment.ensureUiIsDisplayed(CALLBACK_WAIT_MS)).isTrue()
    }

    @After
    fun baseTearDown() {
        val sdkSandboxManager =
            SdkSandboxManagerCompat.from(ApplicationProvider.getApplicationContext())
        sdkSandboxManager.unloadSdk(MainActivity.SDK_NAME)
        sdkSandboxManager.unloadSdk(MainActivity.MEDIATEE_SDK_NAME)
        scenario.close()
    }

    internal fun launchTestAppAndWaitForLoadingSdks(
        @FragmentOption uiFragment: String,
        @UiFrameworkOption uiFrameworkOption: String,
        @AdFormat adFormat: String,
        @MediationOption mediationOption: String,
        @ZOrderOption zOrdering: String,
        adType: String,
    ) {
        val loadSdkCallback =
            object : ILoadSdkCallback.Stub() {
                val latch = CountDownLatch(1)

                override fun onSdksLoaded() {
                    latch.countDown()
                }
            }
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        val extras = Bundle()
        extras.putString(FragmentOptions.KEY_FRAGMENT, uiFragment)
        extras.putString(FragmentOptions.KEY_UI_FRAMEWORK, uiFrameworkOption)
        extras.putString(FragmentOptions.KEY_AD_FORMAT, adFormat)
        extras.putString(FragmentOptions.KEY_MEDIATION, mediationOption)
        extras.putBoolean(FragmentOptions.KEY_DRAW_VIEWABILITY, false)
        extras.putString(FragmentOptions.KEY_Z_ORDER, zOrdering)
        extras.putString(FragmentOptions.KEY_AD_TYPE, adType)
        extras.putBinder(FragmentOptions.LOAD_SDK_COMPLETE, loadSdkCallback)
        intent.putExtras(extras)

        scenario = ActivityScenario.launch(intent)
        assertThat(loadSdkCallback.latch.await(CALLBACK_WAIT_MS, TimeUnit.MILLISECONDS)).isTrue()
    }

    internal fun getFragment(): BaseHiddenFragment {
        return baseHiddenFragment
    }

    class SdkToClientCallback : IAutomatedTestCallback.Stub() {
        var remoteViewWidth: Int? = null
        var remoteViewHeight: Int? = null
        var remoteViewConfiguration: Configuration? = null
        var dragX: Float? = null
        var dragY: Float? = null
        val resizeLatch = CountDownLatch(1)
        val configLatch = CountDownLatch(1)
        val dragLatch = CountDownLatch(1)
        var isRemoteSession = false

        override fun onResizeOccurred(width: Int, height: Int) {
            remoteViewWidth = width
            remoteViewHeight = height
            resizeLatch.countDown()
        }

        override fun onConfigurationChanged(configuration: Configuration) {
            remoteViewConfiguration = configuration
            configLatch.countDown()
        }

        override fun onGestureFinished(totalChangeInX: Float, totalChangeInY: Float) {
            dragX = totalChangeInX
            dragY = totalChangeInY
            dragLatch.countDown()
        }

        override fun onRemoteSession() {
            isRemoteSession = true
        }
    }
}
