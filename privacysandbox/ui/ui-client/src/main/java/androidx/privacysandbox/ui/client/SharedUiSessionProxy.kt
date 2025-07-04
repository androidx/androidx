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

package androidx.privacysandbox.ui.client

import android.annotation.SuppressLint
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SharedUiAdapter

/**
 * Create [SharedUiAdapter.Session] that proxies to [origSession]. Version check on provider version
 * should be made before invoking functions on Session.
 */
@SuppressLint("BanUncheckedReflection") // using reflection on library classes
@ExperimentalFeatures.SharedUiPresentationApi
internal class SharedUiSessionProxy(
    private val uiProviderVersion: Int,
    private val origSession: Any,
) : SharedUiAdapter.Session {
    private val targetClass =
        Class.forName(
                "androidx.privacysandbox.ui.core.SharedUiAdapter\$Session",
                /* initialize = */ false,
                origSession.javaClass.classLoader,
            )
            .also { it.cast(origSession) }

    private val closeMethod = targetClass.getMethod("close")

    override fun close() {
        closeMethod.invoke(origSession)
    }
}
