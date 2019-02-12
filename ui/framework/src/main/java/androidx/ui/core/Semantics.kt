/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.ui.core

import androidx.ui.engine.text.TextDirection
import androidx.ui.core.semantics.SemanticsProperties
import androidx.ui.core.semantics.SemanticsSortKey
import androidx.ui.services.text_editing.TextSelection
import com.google.r4a.Children
import com.google.r4a.Composable
import com.google.r4a.composer

@Composable
fun Semantics(
    @Children children: () -> Unit,
    container: Boolean = false,
    explicitChildNodes: Boolean = false,
    enabled: Boolean = false,
    checked: Boolean = false,
    selected: Boolean = false,
    button: Boolean = false,
    header: Boolean = false,
    textField: Boolean = false,
    focused: Boolean = false,
    inMutuallyExclusiveGroup: Boolean = false,
    obscured: Boolean = false,
    scopesRoute: Boolean = false,
    namesRoute: Boolean = false,
    hidden: Boolean = false,
    label: String? = null,
    value: String? = null,
    increasedValue: String? = null,
    decreasedValue: String? = null,
    hint: String? = null,
    textDirection: TextDirection? = null,
    sortKey: SemanticsSortKey? = null,
    testTag: String? = null,
    onTap: (() -> Unit)? = null,
    onLongPress: (() -> Unit)? = null,
    onScrollLeft: (() -> Unit)? = null,
    onScrollRight: (() -> Unit)? = null,
    onScrollUp: (() -> Unit)? = null,
    onScrollDown: (() -> Unit)? = null,
    onIncrease: (() -> Unit)? = null,
    onDecrease: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onCut: (() -> Unit)? = null,
    onPaste: (() -> Unit)? = null,
    onMoveCursorForwardByCharacter: ((extendSelection: Boolean) -> Unit)? = null,
    onMoveCursorBackwardByCharacter: ((extendSelection: Boolean) -> Unit)? = null,
    onSetSelection: ((selection: TextSelection) -> Unit)? = null,
    onDidGainAccessibilityFocus: (() -> Unit)? = null,
    onDidLoseAccessibilityFocus: (() -> Unit)? = null
) {
    <SemanticsR4ANode
        container=container
        explicitChildNodes=explicitChildNodes
        properties=SemanticsProperties(
            enabled = enabled,
            checked = checked,
            selected = selected,
            button = button,
            header = header,
            textField = textField,
            focused = focused,
            inMutuallyExclusiveGroup = inMutuallyExclusiveGroup,
            obscured = obscured,
            scopesRoute = scopesRoute,
            namesRoute = namesRoute,
            hidden = hidden,
            label = label,
            value = value,
            increasedValue = increasedValue,
            decreasedValue = decreasedValue,
            hint = hint,
            textDirection = textDirection,
            sortKey = sortKey,
            testTag = testTag,
            onTap = onTap,
            onLongPress = onLongPress,
            onScrollLeft = onScrollLeft,
            onScrollRight = onScrollRight,
            onScrollUp = onScrollUp,
            onScrollDown = onScrollDown,
            onIncrease = onIncrease,
            onDecrease = onDecrease,
            onCopy = onCopy,
            onCut = onCut,
            onPaste = onPaste,
            onMoveCursorForwardByCharacter = onMoveCursorForwardByCharacter,
            onMoveCursorBackwardByCharacter = onMoveCursorBackwardByCharacter,
            onDidGainAccessibilityFocus = onDidGainAccessibilityFocus,
            onDidLoseAccessibilityFocus = onDidLoseAccessibilityFocus,
            onSetSelection = onSetSelection
        )>
        <children />
    </SemanticsR4ANode>
}
