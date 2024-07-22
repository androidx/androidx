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

import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.pdf.models.PdfDocumentRemote;
import androidx.pdf.service.PdfDocumentRemoteProto;
import androidx.pdf.util.ThreadUtils;

/**
 * Performs one command on the PDF Document asynchronously, like rendering a page's bitmap or
 * searching text.
 * This base class handles the execution and basic error handling. Subclasses should focus on
 * performing the command (in {@link #doInBackground} and handling its result
 * (in {@link #doCallback}).
 *
 * {@link AbstractPdfTask}:
 * <ul>
 * <li>Handle various error cases during {@link #doInBackground} by logging and canceling the task
 * <li>Don't do anything if cancelled or if reference to the listener is lost.
 * <li>Log when started, stopped or cancelled, and log how much time is taken.
 * <li>Has a Priority that controls when it is executed.
 * </ul>
 * <p>
 * Error handling: All these conditions will print a log and cancel the task, thus skipping the
 * {@link #doCallback} call:
 * <ul>
 * <li>No {@link PdfDocumentRemote} instance available (see comment on {@link #doInBackground}).
 * <li>A {@link RemoteException} raised in {@link #doInBackground}
 * <li>A null result returned from {@link #doInBackground}
 * </ul>
 * <p>
 * All AbstractPDF tasks for a single PDF document should be run by calling
 * {@link PdfTaskExecutor#schedule} on a single {@code PdfTaskExecutor}
 * instance - this will ensure they are run on a single thread and that
 * priority is honored.
 *
 * @param <T> The result from {@link #doInBackground}. Should not be Void, because a null result is
 *            considered an error and will cancel the task.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
abstract class AbstractPdfTask<T> {

    /** Subclasses should return their log tag in here. */
    protected abstract String getLogTag();

    private final PdfLoader mPdfLoader;
    public final Priority mPriority;
    private boolean mReportError = false;
    private boolean mIsCancelled = false;

    AbstractPdfTask(PdfLoader pdfLoader, Priority priority) {
        this.mPdfLoader = pdfLoader;
        this.mPriority = priority;
    }

    /**
     * Hook used to get the {@link PdfDocumentRemote} to work with. This implementation will only
     * return a fully-loaded PdfDocument, and null while it's still initializing.
     * <p>
     * A pdfClient PdfDocument is single threaded, so call this only from findPdfAndDoInBackground,
     * which is run for one task at a time on a dedicated PDF thread. Do not call this from
     * doCallback or cleanUp or anything else that is run on the UI thread.
     */
    @Nullable
    protected PdfDocumentRemote getPdfDocument() {
        return mPdfLoader.getLoadedPdfDocument(getLogTag());
    }

    protected final T findPdfAndDoInBackground() {
        T result;
        try {
            PdfDocumentRemote pdfDocument = getPdfDocument();
            result =
                    pdfDocument != null
                            ? doInBackground(new PdfDocumentRemoteProto(pdfDocument))
                            : doInBackground();
            if (result != null) {
                return result;
            }
        } catch (RemoteException e) {
            mReportError = true;
        } catch (RuntimeException rx) {
            mReportError = true;
            throw rx;
        } finally {
            mPdfLoader.releasePdfDocument();
        }
        cancel();
        if (mReportError) {
            // The service is crashed, no need to ddos-it with requests that will fail anyway.
            mPdfLoader.cancelAll();
        }
        return null;
    }

    /**
     * Implementations must override - do the work in the background thread.
     *
     * @param pdf The connected {@link PdfDocumentRemote} to the PDF.
     * @return The result of the command. Returning @code{null} cancels the task.
     */
    protected abstract T doInBackground(PdfDocumentRemoteProto pdf) throws RemoteException;

    /**
     * An alternative to the method above, called when no {@link PdfDocumentRemote} is available,
     * which may be 2 cases:
     * <ul>
     * <li>The service is not yet bound,
     * <li>The service has crashed, and there isn't much to do
     * </ul>
     *
     * This implementation assumes the latter, and returns @code{null} in order to cancel the task.
     * It also tries to restart the service for subsequent tasks to have better luck.
     *
     * @return The result of the command. Returning @code{null} cancels the task.
     */
    protected T doInBackground() {
        mPdfLoader.reconnect();
        return null;
    }

    protected void onPostExecute(T result) {
        if (isCancelled()) {
            // If a task is cancelled, don't use its results - this means we will avoid
            // updating objects that have already been cleaned up.
            return;
        }
        doCallback(mPdfLoader.getCallbacks(), result);
        cleanup();
    }

    /**
     * Implementations must override - the background operation was successful: the appropriate
     * callback should be called.
     *
     * @param callbacks the {@link PdfLoaderCallbacks} instance, guaranteed non-null
     * @param result    the result of the command, guaranteed non-null.
     */
    protected abstract void doCallback(PdfLoaderCallbacks callbacks, T result);

    /**
     * Hook used to report a generic error (such as service crashed) to the user.
     * By default, does nothing.
     *
     * @param callbacks the {@link PdfLoaderCallbacks} instance, guaranteed non-null
     */
    protected void reportError(PdfLoaderCallbacks callbacks) {

    }

    /**
     * The same idea as a finally block: called whether the task is cancelled or not, after
     * everything else, and on the UI thread.
     */
    protected abstract void cleanup();

    public boolean isCancelled() {
        return mIsCancelled;
    }

    public void cancel() {
        if (!mIsCancelled) {
            mIsCancelled = true;
            ThreadUtils.runOnUiThread(() -> onCancelled());
        }
    }

    private void onCancelled() {
        if (mReportError) {
            reportError(mPdfLoader.getCallbacks());
        }
        cleanup();
    }
}
