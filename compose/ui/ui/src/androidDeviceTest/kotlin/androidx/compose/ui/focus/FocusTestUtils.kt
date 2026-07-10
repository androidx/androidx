/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.focus

import android.app.Instrumentation
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.view.View
import android.widget.LinearLayout
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.google.common.truth.IterableSubject

/**
 * This function adds a parent composable which has size.
 * [View.requestFocus()][android.view.View.requestFocus] will not take focus if the view has no
 * size.
 *
 * @param extraItemForInitialFocus Includes an extra item that takes focus initially. This is useful
 *   in cases where we need tests that could be affected by initial focus. Eg. When there is only
 *   one focusable item and we clear focus, that item could end up being focused on again by the
 *   initial focus logic.
 */
internal fun ComposeContentTestRule.setFocusableContent(
    extraItemForInitialFocus: Boolean = true,
    content: @Composable () -> Unit,
) {
    setContent {
        if (extraItemForInitialFocus) {
            Row {
                Box(modifier = Modifier.requiredSize(10.dp, 10.dp).focusTarget())
                Box(modifier = Modifier.requiredSize(100.dp, 100.dp)) { content() }
            }
        } else {
            Box(modifier = Modifier.requiredSize(100.dp, 100.dp)) { content() }
        }
    }
}

/**
 * This is a composable that makes it easier to create focusable boxes with a specific offset and
 * dimensions.
 */
@Composable
internal fun FocusableBox(
    isFocused: MutableState<Boolean>,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
    focusRequester: FocusRequester? = null,
    deactivated: Boolean = false,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Layout(
        content = content,
        modifier =
            modifier
                .offset { IntOffset(x, y) }
                .focusRequester(focusRequester ?: remember { FocusRequester() })
                .onFocusChanged { isFocused.value = it.isFocused }
                .focusProperties { canFocus = !deactivated }
                .focusTarget(),
        measurePolicy =
            remember(width, height) {
                MeasurePolicy { measurableList, constraint ->
                    layout(width, height) {
                        measurableList.forEach {
                            val placeable = it.measure(constraint)
                            placeable.placeRelative(0, 0)
                        }
                    }
                }
            },
    )
}

/**
 * Asserts that the elements appear in the specified order.
 *
 * Consider using this helper function instead of
 * [containsExactlyElementsIn][IterableSubject.containsExactlyElementsIn] or
 * [containsExactly][IterableSubject.containsExactly] as it also asserts that the elements are in
 * the specified order.
 */
fun IterableSubject.isExactly(vararg expected: Any?) {
    return containsExactlyElementsIn(expected).inOrder()
}

fun FocusableView(context: Context): View {
    return LinearLayout(context).apply {
        minimumHeight = 50
        minimumWidth = 50
        isFocusable = true
        isFocusableInTouchMode = true
    }
}

@Composable
fun FocusableComponent(tag: String? = null, modifier: Modifier = Modifier) {
    Box(modifier.then(if (tag != null) Modifier.testTag(tag) else Modifier).size(50.dp).focusable())
}

fun Instrumentation.setInTouchModeCompat(touchMode: Boolean) {
    if (touchMode) {
        setInTouchMode(true)
    } else {
        // setInTouchMode(false) is flaky, so we press a key to put the system in non-touch mode.
        sendKeyDownUpSync(Key.Grave.nativeKeyCode)
    }
}

// TODO(b/267253920): Add a compose test API to set/reset InputMode.
fun Instrumentation.resetInTouchModeCompat() {
    if (SDK_INT < 33) setInTouchMode(true) else resetInTouchMode()
}
