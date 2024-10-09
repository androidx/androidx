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

package androidx.appcompat.demo.receivecontent;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.recyclerview.widget.RecyclerView;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class AttachmentsRecyclerViewAdapter extends
        RecyclerView.Adapter<AttachmentsRecyclerViewAdapter.MyViewHolder> {

    static final class MyViewHolder extends RecyclerView.ViewHolder {
        public AppCompatImageView mAttachmentThumbnailView;

        MyViewHolder(AppCompatImageView attachmentThumbnailView) {
            super(attachmentThumbnailView);
            mAttachmentThumbnailView = attachmentThumbnailView;
        }
    }

    private final List<Uri> mAttachments;

    AttachmentsRecyclerViewAdapter(List<Uri> attachments) {
        mAttachments = new ArrayList<>(attachments);
    }

    public void addAttachments(Collection<Uri> uris) {
        mAttachments.addAll(uris);
    }
    public void clearAttachments() {
        mAttachments.clear();
    }

    @Override
    public int getItemCount() {
        return mAttachments.size();
    }

    @Override
    public @NonNull MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        AppCompatImageView view = (AppCompatImageView) LayoutInflater.from(parent.getContext())
                .inflate(R.layout.attachment, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Uri uri = mAttachments.get(position);
        holder.mAttachmentThumbnailView.setImageURI(uri);
        holder.mAttachmentThumbnailView.setClipToOutline(true);
    }
}
