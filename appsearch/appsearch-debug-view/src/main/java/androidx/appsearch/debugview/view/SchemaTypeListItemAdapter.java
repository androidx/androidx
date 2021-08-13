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
import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.debugview.R;
import androidx.core.util.Preconditions;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adapter for displaying a list of {@link AppSearchSchema} objects.
 *
 * <p>This adapter displays each schema type with its name.
 *
 * <p>Schema types can be manually changed by calling {@link #setSchemaTypes}.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SchemaTypeListItemAdapter extends
        RecyclerView.Adapter<SchemaTypeListItemAdapter.ViewHolder> {
    private List<AppSearchSchema> mSchemaTypes;

    SchemaTypeListItemAdapter(@NonNull List<AppSearchSchema> schemaTypes) {
        mSchemaTypes = Preconditions.checkNotNull(schemaTypes);
    }

    /**
     * Sets the adapter's schema type list.
     *
     * @param schemaTypes list of {@link AppSearchSchema} objects to update adapter with.
     */
    public void setSchemaTypes(@NonNull List<AppSearchSchema> schemaTypes) {
        mSchemaTypes = Preconditions.checkNotNull(schemaTypes);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_schema_type_list_item, parent, /*attachToRoot=*/false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String schemaType = mSchemaTypes.get(position).getSchemaType();

        holder.getSchemaTypeLabel().setText(schemaType);
    }

    @Override
    public int getItemCount() {
        return mSchemaTypes.size();
    }

    /**
     * ViewHolder for {@link SchemaTypeListItemAdapter}.
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView mSchemaTypeLabel;

        public ViewHolder(@NonNull View view) {
            super(view);

            Preconditions.checkNotNull(view);

            mSchemaTypeLabel = view.findViewById(R.id.schema_type_item_title);
        }

        @NonNull
        public TextView getSchemaTypeLabel() {
            return mSchemaTypeLabel;
        }
    }
}
