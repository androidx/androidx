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
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.LinearLayoutCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/** Main activity for the app. */
public class MainActivity extends AppCompatActivity {
    private AttachmentsRepo mAttachmentsRepo;
    private AttachmentsRecyclerViewAdapter mAttachmentsRecyclerViewAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);

        mAttachmentsRepo = new AttachmentsRepo(this);
        ImmutableList<Uri> attachments = mAttachmentsRepo.getAllUris();

        RecyclerView attachmentsRecyclerView = findViewById(R.id.attachments_recycler_view);
        attachmentsRecyclerView.setHasFixedSize(true);
        mAttachmentsRecyclerViewAdapter = new AttachmentsRecyclerViewAdapter(attachments);
        attachmentsRecyclerView.setAdapter(mAttachmentsRecyclerViewAdapter);

        MyReceiver receiver = new MyReceiver(mAttachmentsRepo, mAttachmentsRecyclerViewAdapter);
        LinearLayoutCompat container = findViewById(R.id.container);
        ViewCompat.setOnReceiveContentListener(container, MyReceiver.SUPPORTED_MIME_TYPES,
                receiver);
        AppCompatEditText textInput = findViewById(R.id.text_input);
        ViewCompat.setOnReceiveContentListener(textInput, MyReceiver.SUPPORTED_MIME_TYPES,
                receiver);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.app_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_clear_attachments) {
            deleteAllAttachments();
            return true;
        }
        return false;
    }

    private void deleteAllAttachments() {
        ListenableFuture<Void> deleteAllFuture = MyExecutors.bg().submit(() -> {
            mAttachmentsRepo.deleteAll();
            return null;
        });
        Futures.addCallback(deleteAllFuture, new FutureCallback<Void>() {
            @Override
            public void onSuccess(@Nullable Void result) {
                mAttachmentsRecyclerViewAdapter.clearAttachments();
                mAttachmentsRecyclerViewAdapter.notifyDataSetChanged();
            }
            @Override
            public void onFailure(@NonNull Throwable t) {
                Log.e(Logcat.TAG, "Error deleting attachments", t);
            }
        }, MyExecutors.main());
    }
}
