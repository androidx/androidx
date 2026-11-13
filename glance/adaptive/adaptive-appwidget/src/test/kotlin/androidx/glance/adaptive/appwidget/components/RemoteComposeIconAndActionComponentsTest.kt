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

package androidx.glance.adaptive.appwidget.components

import android.content.Context
import android.os.Bundle
import androidx.compose.remote.core.operations.layout.modifiers.HostActionOperation
import androidx.compose.remote.core.semantics.AccessibleComponent
import androidx.compose.remote.core.semantics.CoreSemantics
import androidx.compose.ui.unit.dp
import androidx.glance.adaptive.appwidget.components.icon.Content
import androidx.glance.adaptive.appwidget.components.icon.IconDefaults
import androidx.glance.adaptive.appwidget.components.iconbutton.Content
import androidx.glance.adaptive.core.action.GlanceAdaptiveActionCallback
import androidx.glance.adaptive.core.action.GlanceWidgetAction
import androidx.glance.adaptive.core.slots.Icon
import androidx.glance.adaptive.core.slots.IconButton
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Suppress("RestrictedApiAndroidX")
@Config(sdk = [37], qualifiers = "xhdpi")
@RunWith(RobolectricTestRunner::class)
class RemoteComposeIconAndActionComponentsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun icon_bareAndContainedCircle_recordsExpectedSizeAndSemantics() = runTest {
        val bareDecorative =
            captureAndRender(context, widthDp = 32, heightDp = 32) {
                Icon(icon = TEST_VECTOR_GLYPH, contentDescription = null, contained = false)
                    .Content(24.dp)
            }
        val bareBox = bareDecorative.firstLayoutChild()
        assertThat(bareBox.width).isWithin(1f).of(24.dpToPx(context))
        assertThat(bareBox.height).isWithin(1f).of(24.dpToPx(context))
        assertThat(bareBox.layoutChildren()).isEmpty()
        assertThat(bareDecorative.operationsOfType<CoreSemantics>()).isEmpty()

        val containedBrand =
            captureAndRender(context, widthDp = 56, heightDp = 56) {
                Icon(icon = TEST_VECTOR_GLYPH, contentDescription = "Home", contained = true)
                    .Content(56.dp)
            }
        val circleBox = containedBrand.firstLayoutChild()
        assertThat(circleBox.width).isWithin(1f).of(56.dpToPx(context))
        assertThat(circleBox.height).isWithin(1f).of(56.dpToPx(context))
        val innerGlyph = circleBox.layoutChildren().single()
        assertThat(innerGlyph.width)
            .isWithin(2f)
            .of((56f * IconDefaults.ContainedGlyphRatio) * context.resources.displayMetrics.density)

        val semantics = containedBrand.operationsOfType<CoreSemantics>().single()
        assertThat(containedBrand.textContext.getText(semantics.contentDescriptionId))
            .isEqualTo("Home")
        assertThat(semantics.role).isEqualTo(AccessibleComponent.Role.IMAGE)
        // Both surfaceContainerHighest (circle) and onSecondaryContainer (glyph tint) are exposed
        // at the layout container level for host theme updates.
        assertThat(checkNotNull(containedBrand.document.themedColors)).hasSize(2)
    }

    @Test
    fun iconButton_circleAndPillShapes_handleDeeplinkNamedCallbackAndInertCallback() = runTest {
        val deeplink = GlanceWidgetAction.LaunchDeeplink("app://lights")
        val circle =
            captureAndRender(context, widthDp = 64, heightDp = 64) {
                IconButton(
                        icon = TEST_VECTOR_GLYPH,
                        contentDescription = "Lights",
                        action = deeplink,
                    )
                    .Content(
                        taps = RemoteComposeActions(),
                        diameter = 48.dp,
                        glyphSize = 24.dp,
                    )
            }
        val circleBox = circle.firstLayoutChild()
        assertThat(circleBox.width).isWithin(1f).of(48.dpToPx(context))
        assertThat(circleBox.height).isWithin(1f).of(48.dpToPx(context))
        assertThat(circle.captured.pendingIntents.size).isEqualTo(1)
        assertThat(shadowOf(circle.captured.pendingIntents[0]!!).savedIntent.dataString)
            .isEqualTo("app://lights")
        val hostAction = circle.operationsOfType<HostActionOperation>().single()
        assertThat(hostAction.actionId).isEqualTo(FIRST_PENDING_INTENT_ACTION_ID)
        val tappableSemantics = circle.operationsOfType<CoreSemantics>().single()
        assertThat(circle.textContext.getText(tappableSemantics.contentDescriptionId))
            .isEqualTo("Lights")
        assertThat(tappableSemantics.role).isEqualTo(AccessibleComponent.Role.BUTTON)

        val callback = GlanceWidgetAction.Callback(NoOpCallback::class.java)
        val unnamedCallback =
            captureAndRender(context, widthDp = 72, heightDp = 64) {
                IconButton(
                        icon = TEST_VECTOR_GLYPH,
                        contentDescription = "Refresh",
                        action = callback,
                    )
                    .Content(
                        taps = RemoteComposeActions(widgetName = null),
                        diameter = 56.dp,
                        glyphSize = 30.dp,
                        width = 64.dp,
                    )
            }
        val pillBox = unnamedCallback.firstLayoutChild()
        assertThat(pillBox.width).isWithin(1f).of(64.dpToPx(context))
        assertThat(pillBox.height).isWithin(1f).of(56.dpToPx(context))
        // Without a widgetName, a Callback action is inert: no PendingIntent, no HostAction, and no
        // Button role announced to accessibility services.
        assertThat(unnamedCallback.captured.pendingIntents.size).isEqualTo(0)
        assertThat(unnamedCallback.operationsOfType<HostActionOperation>()).isEmpty()
        val inertSemantics = unnamedCallback.operationsOfType<CoreSemantics>().single()
        assertThat(inertSemantics.role).isEqualTo(AccessibleComponent.Role.UNKNOWN)

        val namedCallback =
            captureAndRender(context, widthDp = 72, heightDp = 64) {
                IconButton(
                        icon = TEST_VECTOR_GLYPH,
                        contentDescription = "Refresh",
                        action = callback,
                    )
                    .Content(
                        taps = RemoteComposeActions(widgetName = "track_steps"),
                        diameter = 56.dp,
                        glyphSize = 30.dp,
                        width = 64.dp,
                    )
            }
        assertThat(namedCallback.captured.pendingIntents.size).isEqualTo(1)
        assertThat(namedCallback.operationsOfType<HostActionOperation>().single().actionId)
            .isEqualTo(FIRST_PENDING_INTENT_ACTION_ID)
        assertThat(namedCallback.operationsOfType<CoreSemantics>().single().role)
            .isEqualTo(AccessibleComponent.Role.BUTTON)
    }

    private class NoOpCallback : GlanceAdaptiveActionCallback {
        override suspend fun onAction(
            context: Context,
            widgetName: String,
            widgetId: String?,
            parameters: Bundle,
        ) {}
    }

    private companion object {
        const val FIRST_PENDING_INTENT_ACTION_ID = 0x7E000000
    }
}
