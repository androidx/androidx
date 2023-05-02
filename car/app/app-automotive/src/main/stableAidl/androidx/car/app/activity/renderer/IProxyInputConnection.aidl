/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.car.app.activity.renderer;

import android.os.Bundle;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import androidx.car.app.serialization.Bundleable;

/**
 * Proxies the {@link InputConnection} method invocations from {@link CarAppActivity} across a
 * binder interface to the host renderer.
 *
 * @hide
 */
interface IProxyInputConnection {
    /** Proxies a call to {@link InputConnection#getTextBeforeCursor}. */
    CharSequence getTextBeforeCursor(int length, int flags) = 1;

    /** Proxies a call to {@link InputConnection#getTextAfterCursor}. */
    CharSequence getTextAfterCursor(int length, int flags) = 2;

    /** Proxies a call to {@link InputConnection#getSelectedText}. */
    CharSequence getSelectedText(int flags) = 3;

    /** Proxies a call to {@link InputConnection#getCursorCapsMode}. */
    int getCursorCapsMode(int reqModes) = 4;

    /** Proxies a call to {@link InputConnection#deleteSurroundingText}. */
    boolean deleteSurroundingText(int beforeLength, int afterLength) = 5;

    /** Proxies a call to {@link InputConnection#setComposingText}. */
    boolean setComposingText(CharSequence text, int newCursorPosition) = 6;

    /** Proxies a call to {@link InputConnection#setComposingRegion}. */
    boolean setComposingRegion(int start, int end) = 7;

    /** Proxies a call to {@link InputConnection#finishComposingText}. */
    boolean finishComposingText() = 8;

    /** Proxies a call to {@link InputConnection#commitText}. */
    boolean commitText(CharSequence text, int newCursorPosition) = 9;

    /** Proxies a call to {@link InputConnection#setSelection}. */
    boolean setSelection(int start, int end) = 10;

    /** Proxies a call to {@link InputConnection#performEditorAction}. */
    boolean performEditorAction(int editorAction) = 11;

    /** Proxies a call to {@link InputConnection#performContextMenuAction}. */
    boolean performContextMenuAction(int id) = 12;

    /** Proxies a call to {@link InputConnection#beginBatchEdit}. */
    boolean beginBatchEdit() = 13;

    /** Proxies a call to {@link InputConnection#endBatchEdit}. */
    boolean endBatchEdit() = 14;

    /** Proxies a call to {@link InputConnection#sendKeyEvent}. */
    boolean sendKeyEvent(in KeyEvent event) = 15;

    /** Proxies a call to {@link InputConnection#clearMetaKeyStates}. */
    boolean clearMetaKeyStates(int states) = 16;

    /** Proxies a call to {@link InputConnection#reportFullscreenMode}. */
    boolean reportFullscreenMode(boolean enabled) = 17;

    /** Proxies a call to {@link InputConnection#performPrivateCommand}. */
    boolean performPrivateCommand(String action, in Bundle data) = 18;

    /** Proxies a call to {@link InputConnection#requestCursorUpdates}. */
    boolean requestCursorUpdates(int cursorUpdateMode) = 19;

    /** Proxies a call to {@link InputConnection#commitCorrection}. */
    boolean commitCorrection(in CorrectionInfo correctionInfo) = 20;

    /** Proxies a call to {@link InputConnection#commitCompletion}. */
    boolean commitCompletion(in CompletionInfo text) = 21;

    /** Proxies a call to {@link InputConnection#getExtractedText}. */
    ExtractedText getExtractedText(in ExtractedTextRequest request, int flags) = 22;

    /** Proxies a call to {@link InputConnection#closeConnection}. */
    void closeConnection() = 23;

    /** Returns the {@link EditorInfo} associated with the input connection. */
    EditorInfo getEditorInfo() = 24;

    /**
     * Proxies a call to {@link InputConnection#getSurroundingText}.
     * Note that this returns a {@link Bundleable} that wraps a {@link SurroundingText} since the
     * latter is only available on Android S+. Note that this returns {@code null} on Android R- or
     * when an exception is thrown.
     */
    Bundleable getSurroundingText(int beforeLength, int afterLength, int flags) = 25;
}
