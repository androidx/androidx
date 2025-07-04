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

package androidx.privacysandbox.tools.integration.testsdk

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.privacysandbox.tools.PrivacySandboxInterface
import androidx.privacysandbox.tools.PrivacySandboxService
import androidx.privacysandbox.tools.PrivacySandboxValue
import androidx.privacysandbox.ui.core.ExperimentalFeatures
import androidx.privacysandbox.ui.core.SandboxedUiAdapter
import androidx.privacysandbox.ui.core.SessionData
import androidx.privacysandbox.ui.core.SharedUiAdapter
import java.util.concurrent.Executor

@PrivacySandboxService
interface MySdk {
    suspend fun doSum(x: Int, y: Int): Int

    suspend fun getTextViewAd(): TextViewAd

    suspend fun getAdapterForTextViewAd(): SandboxedUiAdapter

    suspend fun getNativeAdData(): NativeAdData
}

@PrivacySandboxInterface interface TextViewAd : SandboxedUiAdapter

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
@PrivacySandboxInterface
interface NativeAd : SharedUiAdapter

@PrivacySandboxValue
data class NativeAdData(
    val nativeAd: NativeAd,
    val headerText: String,
    val remoteUiAdapter: TextViewAd,
) {
    companion object {
        const val TEXT_VIEW_ASSET_ID = "text-view"
        const val REMOTE_UI_ASSET_ID = "remote-ui"
    }
}

class MySdkImpl(private val context: Context) : MySdk {
    override suspend fun doSum(x: Int, y: Int): Int {
        return x + y
    }

    override suspend fun getTextViewAd(): TextViewAd {
        return TextViewAdImpl()
    }

    override suspend fun getAdapterForTextViewAd(): SandboxedUiAdapter {
        return TextViewAdImpl()
    }

    override suspend fun getNativeAdData(): NativeAdData {
        return NativeAdData(
            nativeAd = NativeAdImpl(),
            headerText = "Text from SDK",
            remoteUiAdapter = TextViewAdImpl(),
        )
    }
}

class TextViewAdImpl : TextViewAd {
    override fun openSession(
        context: Context,
        sessionData: SessionData,
        initialWidth: Int,
        initialHeight: Int,
        isZOrderOnTop: Boolean,
        clientExecutor: Executor,
        client: SandboxedUiAdapter.SessionClient,
    ) {
        val view = TextView(context)
        view.text = "foo bar baz"
        clientExecutor.execute { client.onSessionOpened(TextViewAdSession(view)) }
    }

    inner class TextViewAdSession(override val view: View) : SandboxedUiAdapter.Session {
        override fun close() {}

        override fun notifyConfigurationChanged(configuration: Configuration) {}

        override fun notifyUiChanged(uiContainerInfo: Bundle) {}

        override fun notifySessionRendered(supportedSignalOptions: Set<String>) {}

        override val signalOptions: Set<String> = setOf()

        override fun notifyResized(width: Int, height: Int) {}

        override fun notifyZOrderChanged(isZOrderOnTop: Boolean) {}
    }
}

@SuppressLint("NullAnnotationGroup")
@OptIn(ExperimentalFeatures.SharedUiPresentationApi::class)
class NativeAdImpl : NativeAd {
    override fun openSession(clientExecutor: Executor, client: SharedUiAdapter.SessionClient) {
        clientExecutor.execute { client.onSessionOpened(NativeAdSession()) }
    }

    inner class NativeAdSession : SharedUiAdapter.Session {
        override fun close() {}
    }
}
