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

import static androidx.car.app.utils.LogTags.TAG;

import static java.util.Objects.requireNonNull;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.CorrectionInfo;
import android.view.inputmethod.ExtractedText;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnectionWrapper;
import android.view.inputmethod.SurroundingText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.activity.ServiceDispatcher;
import androidx.car.app.activity.renderer.IProxyInputConnection;
import androidx.car.app.serialization.Bundleable;
import androidx.car.app.serialization.BundlerException;

/** Proxies input connection calls to the provided {@link IProxyInputConnection}. */
final class RemoteProxyInputConnection extends InputConnectionWrapper {
    private final ServiceDispatcher mServiceDispatcher;
    private final IProxyInputConnection mProxyInputConnection;

    RemoteProxyInputConnection(@NonNull ServiceDispatcher serviceDispatcher,
            @NonNull IProxyInputConnection proxyInputConnection) {
        super(null, true);
        mServiceDispatcher = serviceDispatcher;
        mProxyInputConnection = proxyInputConnection;
    }

    @Nullable
    @Override
    public CharSequence getTextBeforeCursor(int n, int flags) {
        return mServiceDispatcher.fetch("getTextBeforeCursor", null, () ->
                mProxyInputConnection.getTextBeforeCursor(n, flags));
    }

    @Nullable
    @Override
    public CharSequence getTextAfterCursor(int n, int flags) {
        return mServiceDispatcher.fetch("getTextAfterCursor", null, () ->
                mProxyInputConnection.getTextAfterCursor(n, flags));
    }

    @Nullable
    @Override
    public CharSequence getSelectedText(int flags) {
        return mServiceDispatcher.fetch("getSelectedText", null, () ->
                mProxyInputConnection.getSelectedText(flags));
    }

    @Override
    public int getCursorCapsMode(int reqModes) {
        Integer res = mServiceDispatcher.fetch("getCursorCapsMode", 0, () ->
                mProxyInputConnection.getCursorCapsMode(reqModes));
        return res != null ? res : 0;
    }

    @Nullable
    @Override
    public ExtractedText getExtractedText(@NonNull ExtractedTextRequest request, int flags) {
        requireNonNull(request);
        return mServiceDispatcher.fetch("getExtractedText", null, () ->
                mProxyInputConnection.getExtractedText(request, flags));
    }

    @Override
    public boolean deleteSurroundingText(int beforeLength, int afterLength) {
        Boolean success = mServiceDispatcher.fetch("deleteSurroundingText", false, () ->
                mProxyInputConnection.deleteSurroundingText(beforeLength, afterLength));
        return success != null ? success : false;
    }

    @Override
    public boolean setComposingText(@NonNull CharSequence text, int newCursorPosition) {
        requireNonNull(text);
        Boolean success = mServiceDispatcher.fetch("setComposingText", false, () ->
                mProxyInputConnection.setComposingText(text, newCursorPosition));
        return success != null ? success : false;
    }

    @Override
    public boolean setComposingRegion(int start, int end) {
        Boolean success = mServiceDispatcher.fetch("setComposingRegion", false, () ->
                mProxyInputConnection.setComposingRegion(start, end));
        return success != null ? success : false;
    }

    @Override
    public boolean finishComposingText() {
        Boolean success = mServiceDispatcher.fetch("finishComposingText", false,
                mProxyInputConnection::finishComposingText);
        return success != null ? success : false;
    }

    @Override
    public boolean commitText(@NonNull CharSequence text, int newCursorPosition) {
        requireNonNull(text);
        Boolean success = mServiceDispatcher.fetch("commitText", false, () ->
                mProxyInputConnection.commitText(text, newCursorPosition));
        return success != null ? success : false;
    }

    @Override
    public boolean commitCompletion(@NonNull CompletionInfo text) {
        requireNonNull(text);
        Boolean success = mServiceDispatcher.fetch("commitCompletion", false, () ->
                mProxyInputConnection.commitCompletion(text));
        return success != null ? success : false;
    }

    @Override
    public boolean commitCorrection(@NonNull CorrectionInfo correctionInfo) {
        requireNonNull(correctionInfo);
        Boolean success = mServiceDispatcher.fetch("commitCorrection", false, () ->
                mProxyInputConnection.commitCorrection(correctionInfo));
        return success != null ? success : false;
    }

    @Override
    public boolean setSelection(int start, int end) {
        Boolean success = mServiceDispatcher.fetch("setSelection", false, () ->
                mProxyInputConnection.setSelection(start, end));
        return success != null ? success : false;
    }

    @Override
    public boolean performEditorAction(int editorAction) {
        Boolean success = mServiceDispatcher.fetch("performEditorAction", false, () ->
                mProxyInputConnection.performEditorAction(editorAction));
        return success != null ? success : false;
    }

    @Override
    public boolean performContextMenuAction(int id) {
        Boolean success = mServiceDispatcher.fetch("performContextMenuAction", false, () ->
                mProxyInputConnection.performContextMenuAction(id));
        return success != null ? success : false;
    }

    @Override
    public boolean beginBatchEdit() {
        Boolean success = mServiceDispatcher.fetch("beginBatchEdit", false,
                mProxyInputConnection::beginBatchEdit);
        return success != null ? success : false;
    }

    @Override
    public boolean endBatchEdit() {
        Boolean success = mServiceDispatcher.fetch("endBatchEdit", false,
                mProxyInputConnection::endBatchEdit);
        return success != null ? success : false;
    }

    @Override
    public boolean sendKeyEvent(@NonNull KeyEvent event) {
        requireNonNull(event);
        Boolean success = mServiceDispatcher.fetch("sendKeyEvent", false, () ->
                mProxyInputConnection.sendKeyEvent(event));
        return success != null ? success : false;
    }

    @Override
    public boolean clearMetaKeyStates(int states) {
        Boolean success = mServiceDispatcher.fetch("clearMetaKeyStates", false, () ->
                mProxyInputConnection.clearMetaKeyStates(states));
        return success != null ? success : false;
    }

    @Override
    public boolean reportFullscreenMode(boolean enabled) {
        Boolean success = mServiceDispatcher.fetch("requestFullscreenMode", false, () ->
                mProxyInputConnection.reportFullscreenMode(enabled));
        return success != null ? success : false;
    }

    @Override
    public boolean performPrivateCommand(@NonNull String action, @NonNull Bundle data) {
        requireNonNull(action);
        requireNonNull(data);
        Boolean success = mServiceDispatcher.fetch("performPrivateCommand", false, () ->
                mProxyInputConnection.performPrivateCommand(action, data));
        return success != null ? success : false;
    }

    @Override
    public boolean requestCursorUpdates(int cursorUpdateMode) {
        Boolean success = mServiceDispatcher.fetch("requestCursorUpdates", false, () ->
                mProxyInputConnection.requestCursorUpdates(cursorUpdateMode));
        return success != null ? success : false;
    }

    @Override
    public void closeConnection() {
        mServiceDispatcher.dispatch("closeConnection", mProxyInputConnection::closeConnection);
    }

    @Nullable
    @Override
    public SurroundingText getSurroundingText(int beforeLength, int afterLength, int flags) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return mServiceDispatcher.fetch("getSurroundingText", null, () -> {
                try {
                    Bundleable bundleable = mProxyInputConnection.getSurroundingText(beforeLength,
                            afterLength, flags);
                    return bundleable == null ? null : (SurroundingText) bundleable.get();
                } catch (BundlerException e) {
                    Log.e(TAG, "Cannot get surrounding text", e);
                    return null;
                }
            });
        }
        return null;
    }

    @Nullable
    @Override
    public Handler getHandler() {
        return null;
    }
}
