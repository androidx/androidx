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

import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.MediationOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.UiFrameworkOption
import androidx.privacysandbox.ui.integration.sdkproviderutils.SdkApiConstants.Companion.ZOrderOption
import androidx.privacysandbox.ui.integration.testapp.fragments.FragmentOptions
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject
import androidx.test.uiautomator.UiObjectNotFoundException
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Enclosed::class)
class UserInteractionTests() {

    @RunWith(Parameterized::class)
    @LargeTest
    class ClickableRemoteView(
        @UiFrameworkOption private val uiFrameworkOption: String,
        @MediationOption private val mediationOption: String,
        @ZOrderOption private val zOrdering: String,
    ) :
        AbstractUserInteractionInScrollTest(
            uiFrameworkOption,
            mediationOption,
            zOrdering,
            adType = FragmentOptions.AD_TYPE_NON_WEBVIEW,
        ) {
        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "uiFrameworkOption={0}, mediationOption={1}, zOrdering={2}"
            )
            fun parameters(): Collection<Array<String>> {
                return baseParameters()
            }
        }

        @Test
        fun clickOnRemoteViewTest() {
            clickOnContentView()

            ensureThatChromeStarted()

            pressBack()
        }
    }

    @RunWith(Parameterized::class)
    @LargeTest
    class ClientAppNotAllowedToScroll(
        @UiFrameworkOption private val uiFrameworkOption: String,
        @MediationOption private val mediationOption: String,
        @ZOrderOption private val zOrdering: String,
    ) :
        AbstractUserInteractionInScrollTest(
            uiFrameworkOption = uiFrameworkOption,
            mediationOption = mediationOption,
            zOrdering = zOrdering,
            adType = FragmentOptions.AD_TYPE_SCROLL_VIEW_APP_CAN_NOT_SCROLL,
        ) {

        companion object {
            @JvmStatic
            @Parameterized.Parameters(
                name = "uiFrameworkOption={0}, mediationOption={1}, zOrdering={2}"
            )
            fun parameters(): Collection<Array<String>> {
                return baseParameters()
            }
        }

        @Test
        fun contentViewConsumesScroll() {
            val scrollViewLastItemIdBefore = getLastVisibleItemId()

            scrollContentViewDown()

            assertThat(sdkToClientCallback.dragLatch.await(CALLBACK_WAIT_MS, TimeUnit.MILLISECONDS))
                .isTrue()
            assertThat(sdkToClientCallback.dragX).isEqualTo(0)
            assertThat(sdkToClientCallback.dragY).isGreaterThan(0)
            assertThat(getLastVisibleItemId()).isEqualTo(scrollViewLastItemIdBefore)
        }
    }

    @RunWith(Enclosed::class)
    class ClientAppAllowedToScroll() {

        // TODO(b/374270009): This might change after this bug gets fixed.
        @RunWith(Parameterized::class)
        @LargeTest
        class ContentInAboveMode(
            @UiFrameworkOption private val uiFrameworkOption: String,
            @MediationOption private val mediationOption: String,
        ) :
            AbstractUserInteractionInScrollTest(
                uiFrameworkOption = uiFrameworkOption,
                mediationOption = mediationOption,
                zOrdering = FragmentOptions.Z_ORDER_ABOVE,
                adType = FragmentOptions.AD_TYPE_SCROLL_VIEW,
            ) {

            @Before
            fun before() {
                // Z-Above mode is only meaningful in case that SDK is loaded in a remote process.
                assumeTrue(sdkToClientCallback.isRemoteSession)
            }

            companion object {
                @JvmStatic
                @Parameterized.Parameters(name = "uiFrameworkOption={0}, mediationOption={1}")
                fun parameters(): Collection<Array<String>> {
                    return customParams(
                        arrayOf(
                            FragmentOptions.UI_FRAMEWORK_VIEW
                            // TODO(b/419824125): Enable this option.
                            // FragmentOptions.UI_FRAMEWORK_COMPOSE
                        ),
                        arrayOf(
                            // In-App Mediated is not tested as z-level above is not meaningful
                            // in this mode.
                            FragmentOptions.MEDIATION_TYPE_NON_MEDIATED,
                            FragmentOptions.MEDIATION_TYPE_IN_RUNTIME,
                        ),
                    )
                }
            }

            @Test
            fun clientAppCanNotScroll() {
                val scrollViewLastItemIdBefore = getLastVisibleItemId()

                scrollContentViewDown()

                assertThat(
                        sdkToClientCallback.dragLatch.await(CALLBACK_WAIT_MS, TimeUnit.MILLISECONDS)
                    )
                    .isTrue()
                assertThat(getLastVisibleItemId()).isEqualTo(scrollViewLastItemIdBefore)
            }
        }

        @RunWith(Parameterized::class)
        @LargeTest
        class ContentInBelowMode(
            @UiFrameworkOption private val uiFrameworkOption: String,
            @MediationOption private val mediationOption: String,
        ) :
            AbstractUserInteractionInScrollTest(
                uiFrameworkOption = uiFrameworkOption,
                mediationOption = mediationOption,
                zOrdering = FragmentOptions.Z_ORDER_BELOW,
                adType = FragmentOptions.AD_TYPE_SCROLL_VIEW,
            ) {

            companion object {
                @JvmStatic
                @Parameterized.Parameters(name = "uiFrameworkOption={0}, mediationOption={1}")
                fun parameters(): Collection<Array<String>> {
                    return customParams(
                        arrayOf(
                            FragmentOptions.UI_FRAMEWORK_VIEW,
                            FragmentOptions.UI_FRAMEWORK_COMPOSE,
                        ),
                        arrayOf(
                            FragmentOptions.MEDIATION_TYPE_NON_MEDIATED,
                            FragmentOptions.MEDIATION_TYPE_IN_APP,
                            FragmentOptions.MEDIATION_TYPE_IN_RUNTIME,
                        ),
                    )
                }
            }

            @Test
            fun clientAppCanScroll() {
                val scrollViewLastItemIdBefore = getLastVisibleItemId()

                scrollContentViewDown()

                assertThat(
                        sdkToClientCallback.dragLatch.await(CALLBACK_WAIT_MS, TimeUnit.MILLISECONDS)
                    )
                    .isFalse()
                assertThat(getLastVisibleItemId()).isGreaterThan(scrollViewLastItemIdBefore)
            }
        }
    }
}

/**
 * When extending this class, the client app must show a vertical scrollable list with first item to
 * be either SandboxedSdkView or SandboxedSdkUi with an established Session.
 */
open class AbstractUserInteractionInScrollTest(
    @UiFrameworkOption private val uiFrameworkOption: String = FragmentOptions.UI_FRAMEWORK_VIEW,
    @MediationOption
    private val mediationOption: String = FragmentOptions.MEDIATION_TYPE_NON_MEDIATED,
    @ZOrderOption private val zOrdering: String = FragmentOptions.Z_ORDER_BELOW,
    @SdkApiConstants.Companion.AdType
    private val adType: String = FragmentOptions.AD_TYPE_NON_WEBVIEW,
) :
    AutomatedEndToEndTest(
        FragmentOptions.FRAGMENT_SCROLL_HIDDEN,
        uiFrameworkOption,
        FragmentOptions.AD_FORMAT_BANNER_AD,
        mediationOption,
        zOrdering,
        adType,
    ) {
    companion object {
        const val MAX_ITEM_IN_LIST = 20
        const val CHROME_PACKAGE_NAME = "com.android.chrome"
    }

    private lateinit var device: UiDevice
    private lateinit var uiScrollable: UiScrollable
    private lateinit var contentViewUiObject: UiObject

    @Before
    fun setup() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        uiScrollable = UiScrollable(UiSelector().scrollable(true))
        contentViewUiObject =
            uiScrollable.getChild(UiSelector().index(0)).getChild(UiSelector().index(0))
    }

    fun getLastVisibleItemId(): Int {
        var lastVisibleItemId = -1
        var lasVisibleItem: UiObject? = null
        for (i in 1..MAX_ITEM_IN_LIST) {
            try {
                lasVisibleItem =
                    uiScrollable.getChildByText(
                        UiSelector().className("android.widget.TextView"),
                        "ClientItem $i",
                        false,
                    )
                lastVisibleItemId = i
            } catch (_: UiObjectNotFoundException) {}
        }

        assertThat(lasVisibleItem).isNotNull()
        assertThat(lastVisibleItemId).isGreaterThan(-1)

        return lastVisibleItemId
    }

    fun scrollContentViewDown() {
        val contentViewCenterX = contentViewUiObject.bounds.centerX()
        val contentViewCenterY = contentViewUiObject.bounds.centerY()

        val scrollYAmount = contentViewUiObject.bounds.height() / 2
        val steps = 10
        device.swipe(
            contentViewCenterX,
            contentViewCenterY,
            contentViewCenterX,
            contentViewCenterY - scrollYAmount,
            steps,
        )
    }

    fun clickOnContentView() {
        device.click(contentViewUiObject.bounds.centerX(), contentViewUiObject.bounds.centerY())
    }

    fun ensureThatChromeStarted() {
        assertThat(
                device.wait(
                    Until.hasObject(By.pkg(CHROME_PACKAGE_NAME).depth(0)),
                    UI_CHANGE_WAIT_MS,
                )
            )
            .isTrue()
    }

    fun pressBack() {
        device.pressBack()
    }
}
