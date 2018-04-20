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

import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.integration.testapp.R;
import androidx.work.integration.testapp.db.Image;
import androidx.work.integration.testapp.db.TestDatabase;

import java.util.List;

/**
 * Image Processing Activity
 */
public class ImageProcessingActivity extends AppCompatActivity {
    private static final int IMAGE_REQUEST_CODE = 1;
    private static final String TAG = "ImageProcessingActivity";
    private ImageRecyclerViewAdapter mImageRecyclerViewAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_processing);

        mImageRecyclerViewAdapter = new ImageRecyclerViewAdapter();

        RecyclerView mRecyclerView = findViewById(R.id.image_recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setAdapter(mImageRecyclerViewAdapter);

        TestDatabase.getInstance(this)
                .getImageDao()
                .getImagesLiveData()
                .observe(this, new Observer<List<Image>>() {
                    @Override
                    public void onChanged(List<Image> images) {
                        mImageRecyclerViewAdapter.updateList(images);
                    }
                });

        findViewById(R.id.add_image).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent chooseIntent = new Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (Build.VERSION.SDK_INT >= 18) {
                    chooseIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                startActivityForResult(chooseIntent, IMAGE_REQUEST_CODE);
            }
        });

        findViewById(R.id.clear_all).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WorkManager.getInstance()
                        .enqueue(OneTimeWorkRequest.from(ImageCleanupWorker.class));
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK
                && Build.VERSION.SDK_INT >= 16 && data.getClipData() != null) {
            Log.d(TAG, "Image Selection Complete");
            int count = data.getClipData().getItemCount();
            OneTimeWorkRequest[] processingWork = new OneTimeWorkRequest[count];
            OneTimeWorkRequest[] setupWork = new OneTimeWorkRequest[count];
            for (int i = 0; i < count; i++) {
                String uriString = data.getClipData().getItemAt(i).getUri().toString();
                setupWork[i] = ImageSetupWorker.createWork(uriString);
                processingWork[i] = ImageProcessingWorker.createWork(uriString);
            }
            WorkManager.getInstance()
                    .beginWith(setupWork)
                    .then(processingWork)
                    .enqueue();
        } else if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_OK
                && data.getData() != null) {
            Log.d(TAG, "Image Selection Complete");
            String uriString = data.getData().toString();
            OneTimeWorkRequest setupWork = ImageSetupWorker.createWork(uriString);
            OneTimeWorkRequest processingWork = ImageProcessingWorker.createWork(uriString);
            WorkManager.getInstance()
                    .beginWith(setupWork)
                    .then(processingWork)
                    .enqueue();
        } else if (requestCode == IMAGE_REQUEST_CODE && resultCode == RESULT_CANCELED) {
            Log.d(TAG, "Image Selection Cancelled");
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
