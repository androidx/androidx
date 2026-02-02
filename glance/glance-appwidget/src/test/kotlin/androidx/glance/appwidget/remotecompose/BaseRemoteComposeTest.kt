/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.glance.appwidget.remotecompose

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import androidx.glance.appwidget.configurationContext
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.TestScope
import org.junit.Before
import org.robolectric.annotation.Config

/** TODO: refactor this to use composition rather than inheritance. b/460234115 */
@SuppressLint("NewApi") // TODO
@Config(minSdk = 35)
abstract class BaseRemoteComposeTest {
    protected lateinit var fakeCoroutineScope: TestScope
    protected val context = ApplicationProvider.getApplicationContext<Context>()
    protected val lightContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_NO }
    protected val darkContext = configurationContext { uiMode = Configuration.UI_MODE_NIGHT_YES }

    protected val floatMarginOfError = .00001f

    @Before
    fun setUp() {
        fakeCoroutineScope = TestScope()
    }
}
