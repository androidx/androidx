/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.work.integration.testapp.imageprocessing;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.work.integration.testapp.R;
import androidx.work.integration.testapp.db.Image;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages a gallery of {@link Image} entities
 */
public class ImageRecyclerViewAdapter extends
        RecyclerView.Adapter<ImageRecyclerViewAdapter.ViewHolder> {
    private static final String TAG = "ImageAdapter";
    private List<Image> mImages = new ArrayList<>();


    void updateList(List<Image> images) {
        mImages = images;
        notifyDataSetChanged();
    }

    /**
     * Defines a single gallery item
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView mImageView;
        private TextView mTextView;
        private ProgressBar mProgressBar;
        private AsyncTask<Image, Void, Bitmap> mBitmapLoadTask;
        private boolean mShouldLoad;

        ViewHolder(View itemView) {
            super(itemView);
            mImageView = itemView.findViewById(R.id.image);
            mTextView = itemView.findViewById(R.id.label);
            mProgressBar = itemView.findViewById(R.id.progressBar);
        }

        public void load(final Image image) {
            if (TextUtils.isEmpty(image.mProcessedFilePath)) {
                mTextView.setText(image.mOriginalAssetName);
                mProgressBar.setVisibility(View.VISIBLE);
                mImageView.setVisibility(View.GONE);
            } else {
                mBitmapLoadTask = new AsyncTask<Image, Void, Bitmap>() {
                    @Override
                    protected void onPostExecute(Bitmap bitmap) {
                        if (mShouldLoad) {
                            Log.d(TAG, "Setting image to holder");
                            mImageView.setImageBitmap(bitmap);
                            mTextView.setText(image.mOriginalAssetName);
                            mProgressBar.setVisibility(View.GONE);
                            mImageView.setVisibility(View.VISIBLE);
                        } else {
                            Log.e(TAG, "Should not load!");
                        }
                    }

                    @Override
                    protected Bitmap doInBackground(Image... images) {
                        Image image = images[0];
                        if (image == null) {
                            throw new IllegalArgumentException();
                        } else if (image.mImage == null) {
                            Log.d(TAG, "Loading image into memory");
                            image.mImage = BitmapFactory.decodeFile(image.mProcessedFilePath);
                        }
                        return image.mImage;
                    }
                };
                mShouldLoad = true;
                mBitmapLoadTask.execute(image);
            }
        }

        public void stopLoad() {
            if (mBitmapLoadTask != null) {
                Log.d(TAG, "Cancelling load");
                mShouldLoad = false;
                mBitmapLoadTask.cancel(true);
                mBitmapLoadTask = null;
            }
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_processed_image, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.load(mImages.get(position));
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
        super.onViewRecycled(holder);
        holder.stopLoad();
    }

    @Override
    public int getItemCount() {
        return mImages.size();
    }
}
