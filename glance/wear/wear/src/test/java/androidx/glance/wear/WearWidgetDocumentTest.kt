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

package androidx.glance.wear

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId
import androidx.glance.wear.parcel.WearWidgetCapture
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WearWidgetDocumentTest {

    @Test
    fun captureRawContent_inInspectionMode_emptyPendingIntents() = runTest {
        val context = getApplicationContext<Context>()
        val params =
            WearWidgetParams(
                WidgetInstanceId("ns", 1),
                ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 100f,
                heightDp = 100f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 0f,
            )
        val document =
            WearWidgetDocument(background = WearWidgetBrush) {
                // This should return CombinedAction() in inspection mode
                RemoteBox(
                    modifier =
                        RemoteModifier.clickable(
                            pendingIntentAction { context ->
                                PendingIntent.getActivity(
                                    context,
                                    0,
                                    Intent(),
                                    PendingIntent.FLAG_IMMUTABLE,
                                )
                            }
                        )
                )
            }

        val rawContent = document.captureRawContent(context, params, isInspectionMode = true)

        val pendingIntentsBundle = rawContent.extras.getBundle(WearWidgetCapture.PENDING_INTENT_KEY)
        assertThat(pendingIntentsBundle).isNotNull()
        assertThat(pendingIntentsBundle!!.isEmpty).isTrue()
    }

    @Test
    fun captureRawContent_notInInspectionMode_hasPendingIntents() = runTest {
        val context = getApplicationContext<Context>()
        val params =
            WearWidgetParams(
                WidgetInstanceId("ns", 1),
                ContainerInfo.CONTAINER_TYPE_SMALL,
                widthDp = 100f,
                heightDp = 100f,
                horizontalPaddingDp = 0f,
                verticalPaddingDp = 0f,
                cornerRadiusDp = 0f,
            )
        val document =
            WearWidgetDocument(background = WearWidgetBrush) {
                // This should return PendingIntentAction in normal mode
                RemoteBox(
                    modifier =
                        RemoteModifier.clickable(
                            pendingIntentAction { context ->
                                PendingIntent.getActivity(
                                    context,
                                    0,
                                    Intent(),
                                    PendingIntent.FLAG_IMMUTABLE,
                                )
                            }
                        )
                )
            }

        val rawContent = document.captureRawContent(context, params, isInspectionMode = false)

        val pendingIntentsBundle = rawContent.extras.getBundle(WearWidgetCapture.PENDING_INTENT_KEY)
        assertThat(pendingIntentsBundle).isNotNull()
        assertThat(pendingIntentsBundle!!.isEmpty).isFalse()
    }
}
