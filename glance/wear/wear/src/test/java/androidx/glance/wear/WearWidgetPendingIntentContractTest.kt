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
import android.os.Bundle
import androidx.compose.remote.core.CoreDocument
import androidx.compose.remote.core.Operation
import androidx.compose.remote.core.RemoteComposeBuffer
import androidx.compose.remote.core.operations.TextData
import androidx.compose.remote.core.operations.layout.Container
import androidx.compose.remote.core.operations.layout.LayoutComponent
import androidx.compose.remote.core.operations.layout.modifiers.HostActionMetadataOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.operations.layout.modifiers.HostNamedActionOperation
import androidx.compose.remote.core.types.IntegerConstant
import androidx.compose.remote.creation.compose.action.pendingIntentAction
import androidx.compose.remote.creation.compose.layout.RemoteBox
import androidx.compose.remote.creation.compose.layout.RemoteColumn
import androidx.compose.remote.creation.compose.modifier.RemoteModifier
import androidx.compose.remote.creation.compose.modifier.clickable
import androidx.compose.runtime.Composable
import androidx.glance.wear.core.ContainerInfo
import androidx.glance.wear.core.WearWidgetParams
import androidx.glance.wear.core.WidgetInstanceId
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import java.io.ByteArrayInputStream
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pins the contract between Glance Wear widgets and Wear widget renderers for
 * `pendingIntentAction`.
 *
 * Renderers are updated independently of apps, so an app built with a newer library must keep
 * producing what existing renderers dispatch. This test reads the captured content the way a
 * renderer does, and deliberately uses literal values instead of library constants, so that any
 * change to the contract fails here:
 * - Each click is a `HOST_NAMED_ACTION` named `"SendPendingIntent"` with an `INT` value.
 * - The value is the key, as a decimal string, of the `PendingIntent` in the `"pending_intents"`
 *   bundle of the extras.
 * - No id actions are written; renderers don't dispatch them.
 */
@RunWith(AndroidJUnit4::class)
class WearWidgetPendingIntentContractTest {
    private val context = getApplicationContext<Context>()

    private val params =
        WearWidgetParams(
            WidgetInstanceId("ns", 1),
            ContainerInfo.CONTAINER_TYPE_SMALL,
            widthDp = 100f,
            heightDp = 100f,
            horizontalPaddingDp = 0f,
            verticalPaddingDp = 0f,
            cornerRadiusDp = 0f,
        )

    @Test
    fun pendingIntentAction_isNamedActionKeyedToBundle() = runTest {
        val pendingIntent = testPendingIntent(requestCode = 1)

        val actions = capture { ClickableBox(pendingIntent) }

        assertThat(actions.sentPendingIntents()).containsExactly(pendingIntent)
    }

    @Test
    fun multiplePendingIntents_eachResolveToTheirOwnPendingIntent() = runTest {
        val first = testPendingIntent(requestCode = 1)
        val second = testPendingIntent(requestCode = 2)

        val actions = capture {
            RemoteColumn {
                ClickableBox(first)
                ClickableBox(second)
            }
        }

        assertThat(actions.sentPendingIntents()).containsExactly(first, second).inOrder()
    }

    @Test
    fun samePendingIntent_isStoredOnce() = runTest {
        val pendingIntent = testPendingIntent(requestCode = 1)

        val actions = capture {
            RemoteColumn {
                ClickableBox(pendingIntent)
                ClickableBox(pendingIntent)
            }
        }

        assertThat(actions.sentPendingIntents()).containsExactly(pendingIntent, pendingIntent)
        assertThat(actions.pendingIntents.keySet()).containsExactly("0")
    }

    @Composable
    private fun ClickableBox(pendingIntent: PendingIntent) {
        RemoteBox(modifier = RemoteModifier.clickable(pendingIntentAction { pendingIntent }))
    }

    private fun testPendingIntent(requestCode: Int): PendingIntent =
        PendingIntent.getActivity(
            context,
            requestCode,
            Intent("test").setPackage(context.packageName),
            PendingIntent.FLAG_IMMUTABLE,
        )

    private suspend fun capture(content: @Composable () -> Unit): CapturedActions {
        val rawContent =
            WearWidgetDocument(background = WearWidgetBrush) { content() }
                .captureRawContent(context, params, isInspectionMode = false)
        val document =
            CoreDocument().apply {
                ByteArrayInputStream(rawContent.rcDocument).use {
                    initFromBuffer(RemoteComposeBuffer.fromInputStream(it))
                }
            }
        return CapturedActions(
            operations = document.allOperations(),
            pendingIntents = rawContent.extras.getBundle("pending_intents")!!,
        )
    }

    /** The click actions of a captured widget, and its pending intents bundle. */
    private class CapturedActions(
        val operations: List<Operation>,
        val pendingIntents: Bundle,
    ) {
        /** Resolves each click to the `PendingIntent` a renderer sends for it. */
        @Suppress("DEPRECATION")
        fun sentPendingIntents(): List<PendingIntent> {
            assertThat(operations.filterIsInstance<HostActionOperation>()).isEmpty()
            assertThat(operations.filterIsInstance<HostActionMetadataOperation>()).isEmpty()

            val texts = operations.filterIsInstance<TextData>().associate { it.mTextId to it.mText }
            val integers =
                operations.filterIsInstance<IntegerConstant>().associate {
                    it.mId to it.intField("mValue")
                }
            return operations.filterIsInstance<HostNamedActionOperation>().map { action ->
                assertThat(texts[action.intField("mTextId")]).isEqualTo("SendPendingIntent")
                assertThat(action.intField("mType")).isEqualTo(INT_TYPE)
                val index = integers[action.intField("mValueId")]
                assertThat(index).isNotNull()
                val pendingIntent = pendingIntents.getParcelable<PendingIntent>("$index")
                assertThat(pendingIntent).isNotNull()
                pendingIntent!!
            }
        }
    }

    private companion object {
        /** `HostNamedActionOperation.INT_TYPE` in the document format. */
        const val INT_TYPE = 1

        fun CoreDocument.allOperations(): List<Operation> {
            val result = mutableListOf<Operation>()
            fun collect(ops: List<Operation>) {
                for (op in ops) {
                    result.add(op)
                    if (op is Container) collect(op.list)
                    if (op is LayoutComponent) collect(op.componentModifiers.list)
                }
            }
            collect(operations)
            return result
        }

        /** Reads a field that the renderer reads but the operation doesn't expose. */
        fun Any.intField(name: String): Int =
            generateSequence<Class<*>>(javaClass) { it.superclass }
                .firstNotNullOf { cls -> cls.declaredFields.firstOrNull { it.name == name } }
                .apply { isAccessible = true }
                .getInt(this)
    }
}
