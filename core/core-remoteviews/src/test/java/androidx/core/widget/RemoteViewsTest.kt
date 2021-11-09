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
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.text.SpannableString
import android.text.SpannedString
import android.text.style.UnderlineSpan
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RemoteViews
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider
import androidx.core.remoteviews.test.R
import androidx.test.filters.SdkSuppress
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument
import java.util.Locale
import kotlin.test.assertIs

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(minSdk = 14)
class RemoteViewsTest {
    private val mContext = ApplicationProvider.getApplicationContext<Context>()
    private lateinit var mRemoteViews: RemoteViews
    private lateinit var mView: View
    private lateinit var mTextView: TextView
    private lateinit var mLinearLayout: LinearLayout
    private lateinit var mImageView: ImageView

    @Before
    fun setUp() {
        mRemoteViews = RemoteViews(mContext.packageName, R.layout.remote_views)
        val parent = FrameLayout(mContext)
        mView = mRemoteViews.apply(mContext, parent)
        mTextView = mView.findViewById(R.id.text)
        mLinearLayout = mView.findViewById(R.id.linear_layout)
        mImageView = mView.findViewById(R.id.image)
    }

    @Test
    fun setTextViewError_string() {
        mRemoteViews.setTextViewHint(R.id.text, "Hello world")
        reapplyRemoteViews()
        assertThat(mTextView.hint).isInstanceOf(String::class.java)
        assertThat(mTextView.hint).isEqualTo("Hello world")
    }

    @Test
    fun setTextViewError_spannableString() {
        val error = SpannableString("An error")
        error.setSpan(UnderlineSpan(), 0, error.length, 0)
        mRemoteViews.setTextViewError(R.id.text, error)
        reapplyRemoteViews()
        // Note: SpannableString gets converted to SpannedString when set on the TextView.
        assertThat(mTextView.error).isInstanceOf(SpannedString::class.java)
        assertThat(mTextView.error.toString()).isEqualTo("An error")
    }

    @Test
    fun setTextViewError_null() {
        mRemoteViews.setTextViewError(R.id.text, "Hello error")
        reapplyRemoteViews()
        mRemoteViews.setTextViewError(R.id.text, null)
        reapplyRemoteViews()
        assertThat(mTextView.error).isNull()
    }

    @Config(minSdk = 16)
    @SdkSuppress(minSdkVersion = 16)
    @Test
    fun setTextViewMaxLines() {
        mRemoteViews.setTextViewMaxLines(R.id.text, 7)
        reapplyRemoteViews()
        // setMaxLines was always present, but getMaxLines was added in 16.
        assertThat(mTextView.maxLines).isEqualTo(7)
    }

    @Config(minSdk = 21)
    @SdkSuppress(minSdkVersion = 21)
    @Test
    fun setTextViewHint_res() {
        mRemoteViews.setTextViewHint(R.id.text, R.string.hello_world)
        reapplyRemoteViews()
        testBeforeAndAfterConfigChange(
            value = { findViewById<TextView>(R.id.text).hint.toString() },
            configuration = { setLocale(Locale("es")) },
            before = "Hello world",
            after = "Hola mundo"
        )
    }

    @Config(minSdk = 24)
    @SdkSuppress(minSdkVersion = 24)
    @Test
    fun setViewEnabled() {
        mRemoteViews.setViewEnabled(R.id.text, false)
        reapplyRemoteViews()
        assertThat(mTextView.isEnabled).isFalse()
    }

    @Test
    fun setLinearLayoutWeightSum() {
        mRemoteViews.setLinearLayoutWeightSum(R.id.linear_layout, 4.2f)
        reapplyRemoteViews()
        assertThat(mLinearLayout.weightSum).isEqualTo(4.2f)
    }

    @Test
    fun setViewBackgroundColorResource() {
        mRemoteViews.setViewBackgroundColorResource(R.id.text, R.color.my_color)

        reapplyRemoteViews()

        val background = mTextView.background
        assertIs<ColorDrawable>(background)
        assertThat(background.color).isEqualTo(Color.RED)
    }

    // Note: createConfigurationContext was added in API 17, but only seems to work properly in
    // Robolectric from API 21.
    @RequiresApi(21)
    private fun <T> testBeforeAndAfterConfigChange(
        value: View.() -> T,
        configuration: Configuration.() -> Unit,
        before: T,
        after: T
    ) {
        reapplyRemoteViews()
        assertThat(value(mView)).isEqualTo(before)
        val overrideConfiguration = mContext.resources.configuration.apply(configuration)
        val overrideContext = mContext.createConfigurationContext(overrideConfiguration)
        val overrideView = mRemoteViews.apply(overrideContext, FrameLayout(overrideContext))
        assertThat(value(overrideView)).isEqualTo(after)
    }

    private fun reapplyRemoteViews() {
        mRemoteViews.reapply(mContext, mView)
    }
}
