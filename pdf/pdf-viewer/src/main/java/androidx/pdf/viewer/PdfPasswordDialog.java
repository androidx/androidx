/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.pdf.viewer;

import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.viewer.password.PasswordDialog;

/**
 * This instance requires a {@link #getTargetFragment} to be set to give back the typed password.
 * Currently, this target Fragment must be a {@link PdfViewer}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("deprecation")
public class PdfPasswordDialog extends PasswordDialog {

    @Nullable
    public PasswordDialogEventsListener mListener;

    public void setListener(@NonNull PasswordDialogEventsListener listener) {
        mListener = listener;
    }

    @Override
    public void sendPassword(@NonNull EditText textField) {
        if (mListener != null) {
            mListener.onPasswordTextChange(textField.getText().toString());
        }
    }

    @Override
    public void showErrorOnDialogCancel() {
        if (mListener != null) {
            mListener.onDialogCancelled();
        }
    }

    public interface PasswordDialogEventsListener {
        /**
         * Callback to pass the password to the fragment.
         */
        void onPasswordTextChange(@NonNull String password);

        /**
         * Callback to handle the cancellation of this dialog.
         */
        void onDialogCancelled();
    }
}