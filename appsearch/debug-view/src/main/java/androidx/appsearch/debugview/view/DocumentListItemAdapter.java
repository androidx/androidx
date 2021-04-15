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
 * <p>This adapter displays each item as a URI and namespace.
 *
 * <p>Documents can be manually changed by calling {@link #setDocuments}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DocumentListItemAdapter extends
        RecyclerView.Adapter<DocumentListItemAdapter.ViewHolder> {
    private List<GenericDocument> mDocuments;

    DocumentListItemAdapter(@NonNull List<GenericDocument> documents) {
        mDocuments = Preconditions.checkNotNull(documents);
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
        holder.getUriLabel().setText("URI: " + "\"" + mDocuments.get(position).getUri() + "\"");
        holder.getNamespaceLabel().setText(
                "Namespace: " + "\"" + mDocuments.get(position).getNamespace() + "\"");
    }

    @Override
    public int getItemCount() {
        return mDocuments.size();
    }

    /**
     * ViewHolder for {@link DocumentListItemAdapter}.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mUriLabel;
        private final TextView mNamespaceLabel;

        public ViewHolder(@NonNull View view) {
            super(view);

            Preconditions.checkNotNull(view);

            mUriLabel = (TextView) view.findViewById(R.id.doc_item_uri);
            mNamespaceLabel = (TextView) view.findViewById(R.id.doc_item_namespace);
        }

        @NonNull
        public TextView getUriLabel() {
            return mUriLabel;
        }

        @NonNull
        public TextView getNamespaceLabel() {
            return mNamespaceLabel;
        }
    }
}
