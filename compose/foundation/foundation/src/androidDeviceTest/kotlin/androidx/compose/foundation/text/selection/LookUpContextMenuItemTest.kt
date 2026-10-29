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

package androidx.compose.foundation.text.selection

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.collection.longObjectMapOf
import androidx.compose.foundation.text.LookUpText
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.internal.TextLayoutState
import androidx.compose.foundation.text.input.internal.TransformedTextFieldState
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.input.internal.selection.canShowLookUpMenuItem
import androidx.compose.foundation.text.input.internal.selection.lookUp
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import com.google.common.truth.Truth.assertThat
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@SmallTest
@RunWith(AndroidJUnit4::class)
class LookUpContextMenuItemTest {
    private val mockPackageManager = mock<PackageManager>()
    private val mockContext =
        mock<Context>().also {
            whenever(it.packageManager).thenReturn(mockPackageManager)
        }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    fun textFieldSelectionState_canShowLookUpMenuItem_whenIntentResolves() {
        val state = createTextFieldSelectionState(TextRange(0, 5))

        setLookUpActivityResolvable(true)
        assertThat(state.canShowLookUpMenuItem(mockContext)).isTrue()

        setLookUpActivityResolvable(false)
        assertThat(state.canShowLookUpMenuItem(mockContext)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    fun textFieldSelectionState_canShowLookUpMenuItem_collapsedSelection() {
        val state = createTextFieldSelectionState(TextRange(0, 0))
        setLookUpActivityResolvable(true)

        assertThat(state.canShowLookUpMenuItem(mockContext)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    fun textFieldSelectionState_canShowLookUpMenuItem_blankSelection() {
        val state = createTextFieldSelectionState(TextRange(5, 6))
        setLookUpActivityResolvable(true)

        assertThat(state.canShowLookUpMenuItem(mockContext)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    fun textFieldSelectionState_canShowLookUpMenuItem_password() {
        val state = createTextFieldSelectionState(TextRange(0, 5), isPassword = true)
        setLookUpActivityResolvable(true)

        assertThat(state.canShowLookUpMenuItem(mockContext)).isFalse()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 36)
    fun textFieldSelectionState_canShowLookUpMenuItem_beforeApi37() {
        val state = createTextFieldSelectionState(TextRange(0, 5))

        withLookUpAlwaysResolvable {
            assertThat(state.canShowLookUpMenuItem(mockContext)).isFalse()
        }
    }

    @Test
    fun textFieldSelectionState_lookUp_startsActivity() {
        val state = createTextFieldSelectionState(TextRange(0, 5))

        state.lookUp(mockContext)

        assertLookUpIntentStarted(expectedText = "Hello")
    }

    @Test
    fun textFieldSelectionState_lookUp_blankSelection_doesNotStartActivity() {
        val state = createTextFieldSelectionState(TextRange(5, 6))

        state.lookUp(mockContext)

        verify(mockContext, never()).startActivity(any())
    }

    @Test
    fun textFieldSelectionState_lookUp_activityNotFound_doesNotCrash() {
        val state = createTextFieldSelectionState(TextRange(0, 5))
        doThrow(ActivityNotFoundException()).whenever(mockContext).startActivity(any())

        state.lookUp(mockContext)

        verify(mockContext).startActivity(any())
    }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    fun selectionManager_canShowLookUpMenuItem_whenIntentResolves() {
        val manager = createSelectionManager(TextRange(0, 5))

        setLookUpActivityResolvable(true)
        assertThat(manager.canShowLookUpMenuItem(mockContext)).isTrue()

        setLookUpActivityResolvable(false)
        assertThat(manager.canShowLookUpMenuItem(mockContext)).isFalse()
    }

    @Test
    @SdkSuppress(minSdkVersion = 37)
    fun selectionManager_canShowLookUpMenuItem_noSelection() {
        val manager = createSelectionManager(selection = null)
        setLookUpActivityResolvable(true)

        assertThat(manager.canShowLookUpMenuItem(mockContext)).isFalse()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 36)
    fun selectionManager_canShowLookUpMenuItem_beforeApi37() {
        val manager = createSelectionManager(TextRange(0, 5))

        withLookUpAlwaysResolvable {
            assertThat(manager.canShowLookUpMenuItem(mockContext)).isFalse()
        }
    }

    @Test
    fun selectionManager_lookUp_startsActivity() {
        val manager = createSelectionManager(TextRange(0, 5))

        manager.lookUp(mockContext)

        assertLookUpIntentStarted(expectedText = "Hello")
    }

    private fun setLookUpActivityResolvable(resolvable: Boolean) {
        val resolveInfo =
            if (resolvable) {
                ResolveInfo().apply {
                    activityInfo =
                        ActivityInfo().apply {
                            applicationInfo =
                                ApplicationInfo().apply { packageName = "com.example" }
                            name = "LookUpActivity"
                        }
                }
            } else {
                null
            }
        whenever(mockPackageManager.resolveActivity(any(), any<Int>())).thenReturn(resolveInfo)
    }

    private fun withLookUpAlwaysResolvable(block: () -> Unit) {
        val oldResolver = LookUpText.canResolveLookUpActivity
        LookUpText.canResolveLookUpActivity = { _, _ -> true }
        try {
            block()
        } finally {
            LookUpText.canResolveLookUpActivity = oldResolver
        }
    }

    private fun assertLookUpIntentStarted(expectedText: String) {
        val intentCaptor = argumentCaptor<Intent>()
        verify(mockContext).startActivity(intentCaptor.capture())
        val intent = intentCaptor.firstValue
        assertThat(intent.action).isEqualTo(LookUpText.ACTION_LOOK_UP_TEXT)
        assertThat(intent.type).isEqualTo("text/plain")
        assertThat(intent.getStringExtra(Intent.EXTRA_TEXT)).isEqualTo(expectedText)
    }

    private fun createTextFieldSelectionState(
        selection: TextRange,
        isPassword: Boolean = false,
    ): TextFieldSelectionState =
        TextFieldSelectionState(
            textFieldState =
                TransformedTextFieldState(
                    textFieldState = TextFieldState(TEXT, initialSelection = selection),
                    inputTransformation = null,
                    codepointTransformation = null,
                    outputTransformation = null,
                ),
            textLayoutState = TextLayoutState(),
            density = Density(1f),
            enabled = true,
            readOnly = false,
            isPassword = isPassword,
            toolbarRequester = mock(),
            coroutineScope = CoroutineScope(EmptyCoroutineContext),
            platformSelectionBehaviors = null,
            clipboard = mock(),
        )

    private fun createSelectionManager(selection: TextRange?): SelectionManager {
        val registrar = SelectionRegistrarImpl()
        val manager = SelectionManager(registrar)

        val mockCoordinates = mock<LayoutCoordinates>()
        whenever(mockCoordinates.isAttached).thenReturn(true)
        manager.containerLayoutCoordinates = mockCoordinates

        if (selection != null) {
            val selectableId = 1L
            val mockSelectable = mock<Selectable>()
            whenever(mockSelectable.selectableId).thenReturn(selectableId)
            whenever(mockSelectable.getText()).thenReturn(AnnotatedString(TEXT))

            registrar.subscribe(mockSelectable)
            registrar.sorted = true // bypass sorting

            val fakeSelection =
                Selection(
                    start =
                        Selection.AnchorInfo(
                            ResolvedTextDirection.Ltr,
                            selection.min,
                            selectableId,
                        ),
                    end =
                        Selection.AnchorInfo(
                            ResolvedTextDirection.Ltr,
                            selection.max,
                            selectableId,
                        ),
                )
            registrar.subselections = longObjectMapOf(selectableId, fakeSelection)
            manager.selection = fakeSelection
        }
        return manager
    }

    private companion object {
        const val TEXT = "Hello World"
    }
}
