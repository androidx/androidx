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

package androidx.pdf.viewer.loader;

import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import androidx.annotation.RestrictTo;
import androidx.pdf.service.PdfDocumentRemoteProto;

import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;

/** AbstractWriteTask writes to the given {@link FileOutputStream}, and closes it. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class AbstractWriteTask extends AbstractPdfTask<Boolean> {
    private final FileOutputStream mFileOutputStream;

    AbstractWriteTask(PdfLoader loader, FileOutputStream fileOutputStream, Priority priority) {
        super(loader, priority);
        this.mFileOutputStream = fileOutputStream;
    }

    /**
     * Implementors must override this to do something useful with the stream.
     *
     * @param pdfDocument the open PdfDocument to read or manipulate.
     * @param pfd         the open, writeable {@link ParcelFileDescriptor}
     * @return true on success
     */
    abstract boolean execute(PdfDocumentRemoteProto pdfDocument, ParcelFileDescriptor pfd)
            throws RemoteException;

    @Override
    protected Boolean doInBackground(PdfDocumentRemoteProto pdfDocument) throws RemoteException {
        FileDescriptor fileDescriptor = null;
        try {
            fileDescriptor = mFileOutputStream.getFD();
            if (fileDescriptor == null) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        ParcelFileDescriptor parcelFileDescriptor = null;
        try {
            parcelFileDescriptor = ParcelFileDescriptor.dup(fileDescriptor);
            if (parcelFileDescriptor == null) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        final boolean result = execute(pdfDocument, parcelFileDescriptor);

        try {
            parcelFileDescriptor.close();
        } catch (IOException e) {
            // TODO: Handle exceptions by sending back error codes
        }

        try {
            mFileOutputStream.close();
        } catch (IOException e) {
            // TODO: Handle exceptions by sending back error codes
        }

        return result;
    }

    @Override
    protected void cleanup() {
    }
}
