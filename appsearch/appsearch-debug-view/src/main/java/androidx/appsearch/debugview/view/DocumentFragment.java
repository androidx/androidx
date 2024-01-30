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

package androidx.appsearch.debugview.view;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.debugview.DebugAppSearchManager;
import androidx.appsearch.debugview.R;
import androidx.appsearch.debugview.model.DocumentModel;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

/**
 * A fragment for displaying a {@link GenericDocument} object.
 *
 * @exportToFramework:hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DocumentFragment extends Fragment {
    private static final String TAG = "AppSearchDocumentFrag";
    private static final String ARG_NAMESPACE = "document_namespace";
    private static final String ARG_ID = "document_id";

    private String mNamespace;
    private String mId;
    private ListeningExecutorService mExecutor;
    private ListenableFuture<DebugAppSearchManager> mDebugAppSearchManager;
    private DocumentModel mDocumentModel;

    /**
     * Factory for creating a {@link DocumentFragment} instance.
     */
    @NonNull
    public static DocumentFragment createDocumentFragment(
            @NonNull String namespace, @NonNull String id) {
        DocumentFragment documentFragment = new DocumentFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAMESPACE, namespace);
        args.putString(ARG_ID, id);
        documentFragment.setArguments(args);
        return documentFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mId = getArguments().getString(ARG_ID);
            mNamespace = getArguments().getString(ARG_NAMESPACE);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_document, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mExecutor = ((AppSearchDebugActivity) getActivity()).getBackgroundExecutor();
        mDebugAppSearchManager =
                ((AppSearchDebugActivity) getActivity()).getDebugAppSearchManager();

        Futures.addCallback(mDebugAppSearchManager,
                new FutureCallback<DebugAppSearchManager>() {
                    @Override
                    public void onSuccess(DebugAppSearchManager debugAppSearchManager) {
                        displayDocument(debugAppSearchManager);
                    }

                    @Override
                    public void onFailure(@NonNull Throwable t) {
                        Toast.makeText(getContext(),
                                "Failed to initialize AppSearch: " + t.getMessage(),
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG,
                                "Failed to initialize AppSearch. Verify that the database name "
                                        + "has been provided in the intent with key: databaseName",
                                t);
                    }
                }, ContextCompat.getMainExecutor(getActivity()));
    }

    protected void displayDocument(@NonNull DebugAppSearchManager debugAppSearchManager) {
        mDocumentModel =
                new ViewModelProvider(this,
                        new DocumentModel.DocumentModelFactory(mExecutor, debugAppSearchManager)
                ).get(DocumentModel.class);

        mDocumentModel.getDocument(mNamespace, mId).observe(this, document -> {
            TextView documentView = getView().findViewById(R.id.document_string);
            documentView.setText(document.toString());
        });
    }
}
