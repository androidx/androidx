/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.appcompat.app

import android.app.Application
import android.view.View
import android.widget.TextView
import androidx.appcompat.R
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(RobolectricTestRunner::class)
@DoNotInstrument
@Config(maxSdk = 28)
class AppCompatDelegateContentTest {

    @Test
    fun testSetContentViewWhenWindowContentViewIsNull() {
        val application = ApplicationProvider.getApplicationContext<Application>()
        application.setTheme(R.style.Theme_AppCompat_Light_NoActionBar)

        val controller = Robolectric.buildActivity(AppCompatActivity::class.java)
        val activity = controller.get()

        // Remove android.R.id.content from window decor view before subdecor is installed
        val windowContentView = activity.window.findViewById<View>(android.R.id.content)
        windowContentView?.id = View.NO_ID

        // setContentView should complete without throwing NullPointerException
        activity.setContentView(TextView(activity))
    }
}
