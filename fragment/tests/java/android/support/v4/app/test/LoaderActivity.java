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

package android.support.v4.app.test;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.fragment.test.R;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.widget.TextView;

import java.util.concurrent.CountDownLatch;

public class LoaderActivity extends FragmentActivity {
    // These must be cleared after each test using clearState()
    public static LoaderActivity sActivity;
    public static CountDownLatch sResumed;

    public TextView textView;

    public static void clearState() {
        sActivity = null;
        sResumed = null;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sActivity = this;

        setContentView(R.layout.fragment_a);
        textView = (TextView) findViewById(R.id.textA);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSupportLoaderManager().initLoader(0, null, new TextLoaderCallback());
        if (sResumed != null) {
            sResumed.countDown();
        }
    }

    class TextLoaderCallback implements LoaderManager.LoaderCallbacks<String> {
        @Override
        public Loader<String> onCreateLoader(int id, Bundle args) {
            return new TextLoader(LoaderActivity.this);
        }

        @Override
        public void onLoadFinished(Loader<String> loader, String data) {
            textView.setText(data);
        }

        @Override
        public void onLoaderReset(Loader<String> loader) {
        }
    }

    static class TextLoader extends AsyncTaskLoader<String> {
        TextLoader(Context context) {
            super(context);
        }

        @Override
        protected void onStartLoading() {
            forceLoad();
        }

        @Override
        public String loadInBackground() {
            return "Loaded!";
        }
    }
}
