/*
 * Copyright (C) 2018 The Android Open Source Project
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

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;

/**
 * Abstract base class for the TextClassifier service.
 *
 * <p>A TextClassifier service provides text classification related features.
 *
 * <p>See: {@link TextClassifier}.
 * See: {@link TextClassificationManager}.
 *
 * <p>To provide a TextClassifierService from your app, include the following in the manifest:
 *
 * <pre>
 * {@literal
 * <service android:name=".YourTextClassifierService" android:exported="true">
 *     <intent-filter>
 *         <action android:name="androidx.textclassifier.TextClassifierService" />
 *     </intent-filter>
 * </service>}</pre>
 *
 * @see TextClassifier
 */
public abstract class TextClassifierService extends Service {

    private static final String LOG_TAG = "TextClassifierService";

    /**
     * The {@link Intent} that must be declared as handled by the service.
     */
    public static final String SERVICE_INTERFACE =
            "androidx.textclassifier.TextClassifierService";

    private final ITextClassifierService.Stub mBinder = new ITextClassifierService.Stub() {

        // TODO(b/72533911): Implement cancellation signal
        @NonNull private final CancellationSignal mCancellationSignal = new CancellationSignal();

        /** {@inheritDoc} */
        @Override
        public void onSuggestSelection(
                @Nullable Bundle sessionId,
                @NonNull Bundle request,
                @NonNull final ITextSelectionCallback callback)
                throws RemoteException {
            Preconditions.checkNotNull(request);
            Preconditions.checkNotNull(callback);

            TextClassifierService.this.onSuggestSelection(
                    createTextClassificationSessionIdFromBundle(sessionId),
                    TextSelection.Request.createFromBundle(request),
                    mCancellationSignal,
                    new Callback<TextSelection>() {
                        @Override
                        public void onSuccess(TextSelection result) {
                            try {
                                callback.onSuccess(result.toBundle());
                            } catch (RemoteException e) {
                                Log.d(LOG_TAG, "Error calling callback");
                            }
                        }

                        @Override
                        public void onFailure(CharSequence error) {
                            try {
                                if (callback.asBinder().isBinderAlive()) {
                                    callback.onFailure();
                                }
                            } catch (RemoteException e) {
                                Log.d(LOG_TAG, "Error calling callback");
                            }
                        }
                    });
        }

        /** {@inheritDoc} */
        @Override
        public void onClassifyText(
                @Nullable Bundle sessionId,
                @NonNull Bundle request,
                @NonNull final ITextClassificationCallback callback)
                throws RemoteException {
            Preconditions.checkNotNull(request);
            Preconditions.checkNotNull(callback);
            TextClassifierService.this.onClassifyText(
                    createTextClassificationSessionIdFromBundle(sessionId),
                    TextClassification.Request.createFromBundle(request),
                    mCancellationSignal,
                    new Callback<TextClassification>() {
                        @Override
                        public void onSuccess(TextClassification result) {
                            try {
                                callback.onSuccess(result.toBundle());
                            } catch (RemoteException e) {
                                Log.d(LOG_TAG, "Error calling callback");
                            }
                        }

                        @Override
                        public void onFailure(CharSequence error) {
                            try {
                                callback.onFailure();
                            } catch (RemoteException e) {
                                Log.d(LOG_TAG, "Error calling callback");
                            }
                        }
                    });
        }

        /** {@inheritDoc} */
        @Override
        public void onGenerateLinks(
                @Nullable Bundle sessionId,
                @NonNull Bundle request,
                @NonNull final ITextLinksCallback callback)
                throws RemoteException {
            Preconditions.checkNotNull(request);
            Preconditions.checkNotNull(callback);
            TextClassifierService.this.onGenerateLinks(
                    createTextClassificationSessionIdFromBundle(sessionId),
                    TextLinks.Request.createFromBundle(request),
                    mCancellationSignal,
                    new Callback<TextLinks>() {
                        @Override
                        public void onSuccess(TextLinks result) {
                            try {
                                callback.onSuccess(result.toBundle());
                            } catch (RemoteException e) {
                                Log.d(LOG_TAG, "Error calling callback");
                            }
                        }

                        @Override
                        public void onFailure(CharSequence error) {
                            try {
                                callback.onFailure();
                            } catch (RemoteException e) {
                                Log.d(LOG_TAG, "Error calling callback");
                            }
                        }
                    });
        }

        /** {@inheritDoc} */
        @Override
        public void onSelectionEvent(
                @Nullable Bundle sessionId, @NonNull Bundle event) throws RemoteException {
            Preconditions.checkNotNull(event);
            TextClassifierService.this.onSelectionEvent(
                    createTextClassificationSessionIdFromBundle(sessionId),
                    SelectionEvent.createFromBundle(event));
        }
    };

    @Nullable
    @Override
    public final IBinder onBind(Intent intent) {
        if (SERVICE_INTERFACE.equals(intent.getAction())) {
            return mBinder;
        }
        return null;
    }

    /**
     * Returns suggested text selection start and end indices, recognized entity types, and their
     * associated confidence scores. The entity types are ordered from highest to lowest scoring.
     *
     * @param sessionId the session id
     * @param request the text selection request
     * @param cancellationSignal object to watch for canceling the current operation
     * @param callback the callback to return the result to
     */
    public void onSuggestSelection(
            @Nullable TextClassificationSessionId sessionId,
            @NonNull TextSelection.Request request,
            @NonNull CancellationSignal cancellationSignal,
            @NonNull Callback<TextSelection> callback) {
        callback.onSuccess(TextClassifier.NO_OP.suggestSelection(request));
    }

    /**
     * Classifies the specified text and returns a {@link TextClassification} object that can be
     * used to generate a widget for handling the classified text.
     *
     * @param sessionId the session id
     * @param request the text classification request
     * @param cancellationSignal object to watch for canceling the current operation
     * @param callback the callback to return the result to
     */
    public void onClassifyText(
            @Nullable TextClassificationSessionId sessionId,
            @NonNull TextClassification.Request request,
            @NonNull CancellationSignal cancellationSignal,
            @NonNull Callback<TextClassification> callback) {
        callback.onSuccess(TextClassifier.NO_OP.classifyText(request));
    }


    /**
     * Generates and returns a {@link TextLinks} that may be applied to the text to annotate it with
     * links information.
     *
     * @param sessionId the session id
     * @param request the text classification request
     * @param cancellationSignal object to watch for canceling the current operation
     * @param callback the callback to return the result to
     */
    public void onGenerateLinks(
            @Nullable TextClassificationSessionId sessionId,
            @NonNull TextLinks.Request request,
            @NonNull CancellationSignal cancellationSignal,
            @NonNull Callback<TextLinks> callback) {
        callback.onSuccess(TextClassifier.NO_OP.generateLinks(request));
    }

    /**
     * Writes the selection event.
     * This is called when a selection event occurs. e.g. user changed selection; or smart selection
     * happened.
     *
     * <p>The default implementation ignores the event.
     *
     * @param sessionId the session id
     * @param event the selection event
     */
    public void onSelectionEvent(
            @Nullable TextClassificationSessionId sessionId, @NonNull SelectionEvent event) {}

    @Nullable
    private TextClassificationSessionId createTextClassificationSessionIdFromBundle(
            @Nullable Bundle bundle) {
        if (bundle == null) {
            return null;
        }
        return TextClassificationSessionId.createFromBundle(bundle);
    }

    /**
     * Callbacks for TextClassifierService results.
     *
     * @param <T> the type of the result
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface Callback<T> {
        /**
         * Returns the result.
         */
        void onSuccess(T result);

        /**
         * Signals a failure.
         */
        void onFailure(CharSequence error);
    }
}
