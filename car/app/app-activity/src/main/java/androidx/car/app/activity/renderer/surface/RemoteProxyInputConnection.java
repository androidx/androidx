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

package androidx.car.app.activity.renderer.surface;

import static androidx.car.app.activity.LogTags.TAG;

import static java.util.Objects.requireNonNull;

import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnectionWrapper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.activity.renderer.IProxyInputConnection;

/** Proxies input connection calls to the provided {@link IProxyInputConnection}. */
final class RemoteProxyInputConnection extends InputConnectionWrapper {
    private final IProxyInputConnection mProxyInputConnection;

    RemoteProxyInputConnection(@NonNull IProxyInputConnection proxyInputConnection) {
        super(null, true);
        mProxyInputConnection = proxyInputConnection;
    }

    @Nullable
    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        CharSequence text;
        try {
            text = mProxyInputConnection.getTextBeforeCursor(n, flags);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            text = null;
        }
        return text;
    }

    @Nullable
    @Override
    public CharSequence getTextAfterCursor(int n, int flags) {
        CharSequence text;
        try {
            text = mProxyInputConnection.getTextAfterCursor(n, flags);
        } catch (RemoteException e) {
            Log.w(TAG, "Remote connection lost", e);
            text = null;
        }

        return text;
    }

    @Nullable
    @Override
    public CharSequence getSelectedText(int flags) {
        CharSequence text;
        try {
            text = mProxyInputConnection.getSelectedText(flags);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            text = null;
        }

        return text;
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        int text;
        try {
            text = mProxyInputConnection.getCursorCapsMode(reqModes);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            text = 0;
        }

        return text;
    }

    @Nullable
    @Override
    public ExtractedText getExtractedText(@NonNull ExtractedTextRequest request, int flags) {
        requireNonNull(request);
        ExtractedText text;
        try {
            text = mProxyInputConnection.getExtractedText(request, flags);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            text = null;
        }

        return text;
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        boolean success;
        try {
            success = mProxyInputConnection.deleteSurroundingText(beforeLength, afterLength);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean setComposingText(@NonNull CharSequence text, int newCursorPosition) {
        requireNonNull(text);
        boolean success;
        try {
            success = mProxyInputConnection.setComposingText(text, newCursorPosition);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        boolean success;
        try {
            success = mProxyInputConnection.setComposingRegion(start, end);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean finishComposingText() {
        boolean success;
        try {
            success = mProxyInputConnection.finishComposingText();
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean commitText(@NonNull CharSequence text, int newCursorPosition) {
        requireNonNull(text);
        boolean success;
        try {
            success = mProxyInputConnection.commitText(text, newCursorPosition);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean commitCompletion(@NonNull CompletionInfo text) {
        requireNonNull(text);
        boolean success;
        try {
            success = mProxyInputConnection.commitCompletion(text);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean commitCorrection(@NonNull CorrectionInfo correctionInfo) {
        requireNonNull(correctionInfo);
        boolean success;
        try {
            success = mProxyInputConnection.commitCorrection(correctionInfo);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean setSelection(int start, int end) {
        boolean success;
        try {
            success = mProxyInputConnection.setSelection(start, end);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean performEditorAction(int editorAction) {
        boolean success;
        try {
            success = mProxyInputConnection.performEditorAction(editorAction);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        boolean success;
        try {
            success = mProxyInputConnection.performContextMenuAction(id);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean beginBatchEdit() {
        boolean success;
        try {
            success = mProxyInputConnection.beginBatchEdit();
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean endBatchEdit() {
        boolean success;
        try {
            success = mProxyInputConnection.endBatchEdit();
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean sendKeyEvent(@NonNull KeyEvent event) {
        requireNonNull(event);
        boolean success;
        try {
            success = mProxyInputConnection.sendKeyEvent(event);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        boolean success;
        try {
            success = mProxyInputConnection.clearMetaKeyStates(states);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        boolean success;
        try {
            success = mProxyInputConnection.reportFullscreenMode(enabled);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean performPrivateCommand(@NonNull String action, @NonNull Bundle data) {
        requireNonNull(action);
        requireNonNull(data);
        boolean success;
        try {
            success = mProxyInputConnection.performPrivateCommand(action, data);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        boolean success;
        try {
            success = mProxyInputConnection.requestCursorUpdates(cursorUpdateMode);
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
            success = false;
        }

        return success;
    }

    @Override
    public void closeConnection() {
        try {
            mProxyInputConnection.closeConnection();
        } catch (RemoteException e) {
            Log.e(TAG, "Remote connection lost", e);
        }
    }

    @Nullable
    @Override
    public Handler getHandler() {
        return null;
    }
}
