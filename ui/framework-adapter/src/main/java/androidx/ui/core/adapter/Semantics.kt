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
package androidx.ui.core.adapter

// Ignore that the IDEA cannot resolve this.
import androidx.ui.core.Semantics
import com.google.r4a.Children
import com.google.r4a.Composable

/**
 * There is an IR bug that allows only 22 or fewer parameters.
 * Uncomment the lines that you need and maybe comment out ones that are unnecessary,
 * depending on the number of args. You'll see this error in your compile
 * if you encounter this bug:
 *     Caused by: java.lang.ClassCastException: org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl cannot be cast to org.jetbrains.kotlin.ir.declarations.IrClass
 *     at org.jetbrains.kotlin.ir.util.IrUtilsKt.getParentAsClass(IrUtils.kt:370)
 *     at org.jetbrains.kotlin.backend.jvm.lower.CallableReferenceLowering$lower$1.visitCall(CallableReferenceLowering.kt:84)
 */
@Composable
@Suppress("PLUGIN_ERROR")
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
//    value: String? = null,
//    increasedValue: String? = null,
//    decreasedValue: String? = null,
//    hint: String? = null,
//    textDirection: TextDirection? = null,
//    sortKey: SemanticsSortKey? = null,
    testTag: String? = null,
    onTap: (() -> Unit)? = null
//    onLongPress: (() -> Unit)? = null,
//    onScrollLeft: (() -> Unit)? = null,
//    onScrollRight: (() -> Unit)? = null,
//    onScrollUp: (() -> Unit)? = null,
//    onScrollDown: (() -> Unit)? = null
//    onIncrease: (() -> Unit)? = null,
//    onDecrease: (() -> Unit)? = null,
//    onCopy: (() -> Unit)? = null,
//    onCut: (() -> Unit)? = null,
//    onPaste: (() -> Unit)? = null,
//    onMoveCursorForwardByCharacter: ((extendSelection: Boolean) -> Unit)? = null,
//    onMoveCursorBackwardByCharacter: ((extendSelection: Boolean) -> Unit)? = null,
//    onSetSelection: ((selection: TextSelection) -> Unit)? = null,
//    onDidGainAccessibilityFocus: (() -> Unit)? = null,
//    onDidLoseAccessibilityFocus: (() -> Unit)? = null
) {
    Semantics(
        children,
        container,
        explicitChildNodes,
        enabled,
        checked,
        selected,
        button,
        header,
        textField,
        focused,
        inMutuallyExclusiveGroup,
        obscured,
        scopesRoute,
        namesRoute,
        hidden,
        label,
        null, // value,
        null, // increasedValue,
        null, // decreasedValue,
        null, // hint,
        null, // textDirection,
        null, // sortKey,
        testTag, // testTag,
        onTap, // onTap,
        null, // onLongPress,
        null, // onScrollLeft,
        null, // onScrollRight,
        null, // onScrollUp,
        null, // onScrollDown,
        null, // onIncrease,
        null, // onDecrease,
        null, // onCopy,
        null, // onCut,
        null, // onPaste,
        null, // onMoveCursorForwardByCharacter,
        null, // onMoveCursorBackwardByCharacter,
        null, // onSetSelection,
        null, // onDidGainAccessibilityFocus,
        null // onDidLoseAccessibilityFocus
        )
}