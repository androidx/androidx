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

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.debugview.R;
import androidx.core.util.Preconditions;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying a list of {@link GenericDocument} objects.
 *
 * <p>This adapter displays each item as a namespace and document ID.
 *
 * <p>Documents can be manually changed by calling {@link #setDocuments}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DocumentListItemAdapter extends
        RecyclerView.Adapter<DocumentListItemAdapter.ViewHolder> {
    private List<GenericDocument> mDocuments;
    private DocumentListFragment mDocumentListFragment;

    DocumentListItemAdapter(@NonNull List<GenericDocument> documents,
            @NonNull DocumentListFragment documentListFragment) {
        mDocuments = Preconditions.checkNotNull(documents);
        mDocumentListFragment = Preconditions.checkNotNull(documentListFragment);
    }

    /**
     * Sets the adapter's document list.
     *
     * @param documents list of {@link GenericDocument} objects to update adapter with.
     */
    public void setDocuments(@NonNull List<GenericDocument> documents) {
        mDocuments = Preconditions.checkNotNull(documents);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_document_list_item, parent, /*attachToRoot=*/false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String namespace = mDocuments.get(position).getNamespace();
        String id = mDocuments.get(position).getId();
        holder.getNamespaceLabel().setText(
                "Namespace: \"" + namespace + "\"");
        holder.getIdLabel().setText("ID: \"" + id + "\"");

        holder.itemView.setOnClickListener(unusedView -> {
                    DocumentFragment documentFragment =
                            DocumentFragment.createDocumentFragment(namespace, id);
                    mDocumentListFragment.getActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.fragment_container, documentFragment)
                            .addToBackStack(/*name=*/null)
                            .commit();
                }
        );
    }

    @Override
    public int getItemCount() {
        return mDocuments.size();
    }

    /**
     * ViewHolder for {@link DocumentListItemAdapter}.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mNamespaceLabel;
        private final TextView mIdLabel;

        public ViewHolder(@NonNull View view) {
            super(view);

            Preconditions.checkNotNull(view);

            mNamespaceLabel = view.findViewById(R.id.doc_item_namespace);
            mIdLabel = view.findViewById(R.id.doc_item_id);
        }

        @NonNull
        public TextView getNamespaceLabel() {
            return mNamespaceLabel;
        }

        @NonNull
        public TextView getIdLabel() {
            return mIdLabel;
        }
    }
}
