/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.core.widget

import android.Manifest
import android.app.UiAutomation
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
import android.appwidget.AppWidgetManager.OPTION_APPWIDGET_SIZES
import android.content.Context
import android.os.Build
import android.text.Editable
import android.text.TextWatcher
import android.util.SizeF
import android.view.ViewTreeObserver
import android.widget.RemoteViews
import android.widget.TextView
import androidx.core.remoteviews.test.R
import androidx.core.util.SizeFCompat
import androidx.core.util.component1
import androidx.core.util.component2
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.filters.MediumTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.fail
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@SdkSuppress(minSdkVersion = 29)
@MediumTest
class AppWidgetManagerDeviceTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private val mPackageName = mContext.packageName
    private val mAppWidgetManager = AppWidgetManager.getInstance(mContext)

    @Rule
    @JvmField
    public val mActivityTestRule: ActivityScenarioRule<AppWidgetHostTestActivity> =
        ActivityScenarioRule(AppWidgetHostTestActivity::class.java)

    private lateinit var mRemoteViews: RemoteViews
    private lateinit var mHostView: AppWidgetHostView
    private var mAppWidgetId = 0

    private val mUiAutomation: UiAutomation
        get() = InstrumentationRegistry.getInstrumentation().uiAutomation

    private val mTextView: TextView
        get() = mHostView.getChildAt(0) as TextView

    @Before
    public fun setUp() {
        mUiAutomation.adoptShellPermissionIdentity(Manifest.permission.BIND_APPWIDGET)

        mActivityTestRule.scenario.onActivity { activity ->
            mHostView = activity.bindAppWidget()
        }

        mAppWidgetId = mHostView.appWidgetId
        mRemoteViews = RemoteViews(mPackageName, R.layout.remote_views_text)
        mAppWidgetManager.updateAppWidget(mAppWidgetId, mRemoteViews)

        // Wait until the remote views has been added to the host view.
        observeDrawUntil { mHostView.childCount > 0 }
    }

    @After
    public fun tearDown() {
        mUiAutomation.dropShellPermissionIdentity()
    }

    @Test
    public fun exact_shouldUseActualWidgetSize() {
        mAppWidgetManager.updateAppWidget(mAppWidgetId) { (widthDp, heightDp) ->
            RemoteViews(mPackageName, R.layout.remote_views_text).apply {
                setTextViewText(R.id.text, "$widthDp x $heightDp")
            }
        }

        val (width, height) = getSingleWidgetSize()
        observeTextUntilEquals("$width x $height")
    }

    @Test
    public fun responsive_shouldUseBestFittingProvidedWidgetSizes() {
        val (width, height) = getSingleWidgetSize()
        mAppWidgetManager.updateAppWidget(
            mAppWidgetId,
            listOf(width - 2 x height - 2, width + 2 x height + 2)
        ) { (widthDp, heightDp) ->
            RemoteViews(mPackageName, R.layout.remote_views_text).apply {
                setTextViewText(R.id.text, "$widthDp x $heightDp")
            }
        }

        observeTextUntilEquals("${width - 2} x ${height - 2}")
    }

    private fun observeTextUntilEquals(expectedText: String) {
        if (mTextView.text.toString() == expectedText) return

        val latch = CountDownLatch(1)
        val textWatcher = object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

            override fun onTextChanged(text: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (text.toString() == expectedText) latch.countDown()
            }

            override fun afterTextChanged(text: Editable?) {}
        }

        mActivityTestRule.scenario.onActivity {
            mTextView.addTextChangedListener(textWatcher)
        }

        val countedDown = latch.await(5, TimeUnit.SECONDS)

        mActivityTestRule.scenario.onActivity {
            mTextView.removeTextChangedListener(textWatcher)
        }

        if (!countedDown && mTextView.text.toString() != expectedText) {
            fail("Expected text to be \"$expectedText\" within 5 seconds")
        }
    }

    private fun observeDrawUntil(test: () -> Boolean) {
        val latch = CountDownLatch(1)
        val onDrawListener = ViewTreeObserver.OnDrawListener {
            if (test()) latch.countDown()
        }

        mActivityTestRule.scenario.onActivity {
            mHostView.viewTreeObserver.addOnDrawListener(onDrawListener)
        }

        val countedDown = latch.await(5, TimeUnit.SECONDS)

        mActivityTestRule.scenario.onActivity {
            mHostView.viewTreeObserver.removeOnDrawListener(onDrawListener)
        }

        if (!countedDown && !test()) {
            fail("Expected condition to be met within 5 seconds")
        }
    }

    @Suppress("DEPRECATION")
    private fun getSingleWidgetSize(): SizeFCompat {
        val options = mAppWidgetManager.getAppWidgetOptions(mAppWidgetId)
        return if (Build.VERSION.SDK_INT >= 31) {
            options
                .getParcelableArrayList<SizeF>(OPTION_APPWIDGET_SIZES)!!
                .single()
                .let { it.width x it.height }
        } else {
            val minWidth = options.getInt(OPTION_APPWIDGET_MIN_WIDTH)
            val maxWidth = options.getInt(OPTION_APPWIDGET_MAX_WIDTH)
            val minHeight = options.getInt(OPTION_APPWIDGET_MIN_HEIGHT)
            val maxHeight = options.getInt(OPTION_APPWIDGET_MAX_HEIGHT)
            assertThat(minWidth).isEqualTo(maxWidth)
            assertThat(minHeight).isEqualTo(maxHeight)
            minWidth x minHeight
        }
    }

    private infix fun Number.x(other: Number) = SizeFCompat(this.toFloat(), other.toFloat())
}
