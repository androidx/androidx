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

package androidx.compose.ui.window

import android.os.Binder
import android.view.WindowManager
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PopupWindowTokenTest {

    @Test
    fun resolveWindowToken_providedTokenSpecified_returnsProvidedToken() {
        val providedToken = Binder()
        val appToken = Binder()
        val rootParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL
                token = Binder()
            }

        val result = resolveWindowToken(providedToken, rootParams, appToken)

        assertThat(result).isEqualTo(providedToken)
    }

    @Test
    fun resolveWindowToken_rootIsSubWindow_returnsRootToken() {
        val rootToken = Binder()
        val appToken = Binder()
        val rootParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL
                token = rootToken
            }

        val result = resolveWindowToken(null, rootParams, appToken)

        assertThat(result).isEqualTo(rootToken)
    }

    @Test
    fun resolveWindowToken_rootIsFirstSubWindowBoundary_returnsRootToken() {
        val rootToken = Binder()
        val appToken = Binder()
        val rootParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.FIRST_SUB_WINDOW
                token = rootToken
            }

        val result = resolveWindowToken(null, rootParams, appToken)

        assertThat(result).isEqualTo(rootToken)
    }

    @Test
    fun resolveWindowToken_rootIsLastSubWindowBoundary_returnsRootToken() {
        val rootToken = Binder()
        val appToken = Binder()
        val rootParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.LAST_SUB_WINDOW
                token = rootToken
            }

        val result = resolveWindowToken(null, rootParams, appToken)

        assertThat(result).isEqualTo(rootToken)
    }

    @Test
    fun resolveWindowToken_rootIsNormalWindow_returnsAppToken() {
        val rootToken = Binder()
        val appToken = Binder()
        val rootParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.TYPE_BASE_APPLICATION
                token = rootToken
            }

        val result = resolveWindowToken(null, rootParams, appToken)

        assertThat(result).isEqualTo(appToken)
    }

    @Test
    fun resolveWindowToken_rootIsBelowSubWindowBoundary_returnsAppToken() {
        val rootToken = Binder()
        val appToken = Binder()
        val rootParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.FIRST_SUB_WINDOW - 1
                token = rootToken
            }

        val result = resolveWindowToken(null, rootParams, appToken)

        assertThat(result).isEqualTo(appToken)
    }

    @Test
    fun resolveWindowToken_rootIsAboveSubWindowBoundary_returnsAppToken() {
        val rootToken = Binder()
        val appToken = Binder()
        val rootParams =
            WindowManager.LayoutParams().apply {
                type = WindowManager.LayoutParams.LAST_SUB_WINDOW + 1
                token = rootToken
            }

        val result = resolveWindowToken(null, rootParams, appToken)

        assertThat(result).isEqualTo(appToken)
    }

    @Test
    fun resolveWindowToken_rootParamsNull_returnsAppToken() {
        val appToken = Binder()

        val result = resolveWindowToken(null, null, appToken)

        assertThat(result).isEqualTo(appToken)
    }
}
