/*
 * Copyright (C) 2024 The Android Open Source Project
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
package androidx.compose.remote.creation.modifiers;

import androidx.annotation.RestrictTo;
import androidx.compose.remote.core.semantics.AccessibilityModifier;
import androidx.compose.remote.creation.RemoteComposeWriter;

import org.jspecify.annotations.NonNull;

/** Semantics modifier, including */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class SemanticsModifier implements RecordingModifier.Element {

    @NonNull
    AccessibilityModifier mSemantics;

    public SemanticsModifier(@NonNull AccessibilityModifier semantics) {
        this.mSemantics = semantics;
    }

    public @NonNull AccessibilityModifier getSemantics() {
        return mSemantics;
    }

    @Override
    public void write(@NonNull RemoteComposeWriter writer) {
        writer.getBuffer().getBuffer().start(mSemantics.getOpCode());
        mSemantics.write(writer.getBuffer().getBuffer());
//      TODO change to use writer
//        writer.addSemanticsModifier(
//                mSemantics.mContentDescriptionId,
//                (byte) ((mSemantics.mRole != null) ? mSemantics.mRole.ordinal() : -1),
//                mSemantics.mTextId,
//                mSemantics.mStateDescriptionId,
//                (byte) mSemantics.mMode.ordinal(),
//                mSemantics.mEnabled,
//                mSemantics.mClickable);

    }
}
