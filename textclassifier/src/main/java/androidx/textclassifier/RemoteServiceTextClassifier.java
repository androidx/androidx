/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.textclassifier;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.annotation.WorkerThread;
import androidx.core.util.Preconditions;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Handles text classification requests by binding to a given {@link TextClassifierService}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public final class RemoteServiceTextClassifier extends TextClassifier {
    private static final String TAG = TextClassifier.DEFAULT_LOG_TAG;
    private static final int REQUEST_TIMEOUT_SECOND = 2;

    @NonNull
    private final TextClassificationSessionId mSessionId = new TextClassificationSessionId();
    @NonNull
    private final Context mContext;
    @NonNull
    private final TextClassifier mFallBack = TextClassifier.NO_OP;
    @NonNull
    private final ServiceManager mServiceManager;

    public RemoteServiceTextClassifier(
            @NonNull Context context,
            @NonNull TextClassificationContext textClassificationContext,
            @NonNull ComponentName serviceComponent) {
        super(textClassificationContext);
        mContext = Preconditions.checkNotNull(context);
        mServiceManager = new ServiceManager(mContext, serviceComponent);
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextSelection suggestSelection(@NonNull final TextSelection.Request request) {
        return new TextSelectionRequestProcessor(request).process();
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextClassification classifyText(@NonNull final TextClassification.Request request) {
        return new TextClassificationRequestProcessor(request).process();
    }

    /** @inheritDoc */
    @NonNull
    @WorkerThread
    @Override
    public TextLinks generateLinks(@NonNull final TextLinks.Request request) {
        return new TextLinksRequestProcessor(request).process();
    }

    /** @inheritDoc */
    @WorkerThread
    @Override
    public void onSelectionEvent(@NonNull final SelectionEvent event) {
        new SelectionEventProcessor(event).process();
    }

    private final class TextSelectionRequestProcessor
            extends RequestProcessor<TextSelection.Request, TextSelection> {

        TextSelectionRequestProcessor(@NonNull TextSelection.Request request) {
            super(request, "suggestSelection");
        }

        @Override
        void processRequest(ITextClassifierService service)
                throws RemoteException {
            service.onSuggestSelection(
                    mSessionId.toBundle(), request.toBundle(), createCallback());
        }

        @Override
        TextSelection fallback() {
            return mFallBack.suggestSelection(request);
        }

        private ITextSelectionCallback createCallback() {
            return new ITextSelectionCallback.Stub() {
                @Override
                public void onSuccess(Bundle selection) throws RemoteException {
                    responseReceiver.onSuccess(TextSelection.createFromBundle(selection));
                }

                @Override
                public void onFailure() throws RemoteException {
                    responseReceiver.onFailure();
                }
            };
        }
    }

    private final class TextClassificationRequestProcessor
            extends RequestProcessor<TextClassification.Request, TextClassification> {

        TextClassificationRequestProcessor(@NonNull TextClassification.Request request) {
            super(request, "classifyText");
        }

        @Override
        void processRequest(ITextClassifierService service) throws RemoteException {
            service.onClassifyText(mSessionId.toBundle(), request.toBundle(), createCallback());
        }

        @Override
        TextClassification fallback() {
            return mFallBack.classifyText(request);
        }

        private ITextClassificationCallback createCallback() {
            return new ITextClassificationCallback.Stub() {
                @Override
                public void onSuccess(Bundle classification) throws RemoteException {
                    responseReceiver.onSuccess(TextClassification.createFromBundle(classification));
                }

                @Override
                public void onFailure() throws RemoteException {
                    responseReceiver.onFailure();
                }
            };
        }
    }

    private final class TextLinksRequestProcessor extends
            RequestProcessor<TextLinks.Request, TextLinks> {

        TextLinksRequestProcessor(@NonNull TextLinks.Request request) {
            super(request, "generateLinks");
        }

        @Override
        void processRequest(ITextClassifierService service)
                throws RemoteException {
            service.onGenerateLinks(mSessionId.toBundle(), request.toBundle(), createCallback());
        }

        @Override
        TextLinks fallback() {
            return mFallBack.generateLinks(request);
        }

        private ITextLinksCallback createCallback() {
            return new ITextLinksCallback.Stub() {

                @Override
                public void onSuccess(Bundle links) throws RemoteException {
                    responseReceiver.onSuccess(TextLinks.createFromBundle(links));
                }

                @Override
                public void onFailure() throws RemoteException {
                    responseReceiver.onFailure();
                }
            };
        }
    }

    private final class SelectionEventProcessor extends RequestProcessor<SelectionEvent, Boolean> {

        private SelectionEventProcessor(SelectionEvent selectionEvent) {
            super(selectionEvent, "onSelectionEvent");
        }

        @Override
        void processRequest(ITextClassifierService service) throws RemoteException {
            service.onSelectionEvent(mSessionId.toBundle(), request.toBundle());
        }

        @Override
        Boolean fallback() {
            // Never happen.
            return true;
        }
    }

    private abstract class RequestProcessor<RequestT, ResultT> {
        @NonNull
        public final RequestT request;
        @NonNull
        public final String requestName;
        @NonNull
        public final ResponseReceiver<ResultT> responseReceiver;

        private RequestProcessor(
                @NonNull RequestT request, @NonNull String requestName) {
            this.request = request;
            this.requestName = requestName;
            this.responseReceiver = new ResponseReceiver<>();
        }

        @NonNull
        final ResultT process() {
            Preconditions.checkNotNull(request);
            ensureNotOnMainThread();

            ITextClassifierService service = mServiceManager.bindAndAwait();
            if (service == null) {
                Log.w(TAG, "failed to bind the service: " + requestName);
                return fallback();
            }
            try {
                ResultT result = null;
                try {
                    processRequest(service);
                    result = responseReceiver.get();
                } catch (RemoteException | TimeoutException | InterruptedException e) {
                    Log.e(TAG, "Failure in " + requestName, e);
                }
                if (result == null) {
                    Log.w(TAG, "Fallback: " + requestName);
                    return fallback();
                }
                return result;
            } finally {
                mServiceManager.scheduleUnbind();
            }
        }

        abstract void processRequest(ITextClassifierService service) throws RemoteException;

        @NonNull
        abstract ResultT fallback();
    }

    private static final class ResponseReceiver<ResultT> {

        private final CountDownLatch mLatch = new CountDownLatch(1);
        private volatile ResultT mResponse;

        public void onSuccess(ResultT response) {
            mResponse = response;
            mLatch.countDown();
        }

        public void onFailure() {
            mLatch.countDown();
        }

        @Nullable
        @WorkerThread
        public ResultT get() throws InterruptedException, TimeoutException {
            boolean timeout = !mLatch.await(REQUEST_TIMEOUT_SECOND, TimeUnit.SECONDS);
            if (timeout) {
                throw new TimeoutException("Timeout when waiting for response");
            }
            return mResponse;
        }
    }
}
