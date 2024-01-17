/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build

/**
 * These lint checks are normally a warning (or lower), but in AndroidX we ignore warnings in Lint.
 * We want these errors to be reported, so they'll be promoted from a warning to an error in modules
 * that use the [AndroidXComposeImplPlugin].
 */
internal val ComposeLintWarningIdsToTreatAsErrors =
    listOf(
        "ComposableNaming",
        "ComposableLambdaParameterNaming",
        "ComposableLambdaParameterPosition",
        "CompositionLocalNaming",
        "ComposableModifierFactory",
        "AutoboxingStateCreation",
        "AutoboxingStateValueProperty",
        "InvalidColorHexValue",
        "MissingColorAlphaChannel",
        "ModifierFactoryReturnType",
        "ModifierFactoryExtensionFunction",
        "ModifierNodeInspectableProperties",
        "ModifierParameter",
        "MutableCollectionMutableState",
        "OpaqueUnitKey",
        "UnnecessaryComposedModifier",
        "FrequentlyChangedStateReadInComposition",
        "ReturnFromAwaitPointerEventScope",
        "UseOfNonLambdaOffsetOverload",
        "MultipleAwaitPointerEventScopes",
    )
