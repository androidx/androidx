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

package androidx.work.integration.testapp.sherlockholmes;

import static androidx.work.State.FAILED;
import static androidx.work.State.SUCCEEDED;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.work.ArrayCreatingInputMerger;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.integration.testapp.R;
import androidx.work.integration.testapp.db.TestDatabase;
import androidx.work.integration.testapp.db.WordCount;

import java.util.List;

/**
 * Analyze Sherlock Holmes activity.
 */
public class AnalyzeSherlockHolmesActivity extends AppCompatActivity {
    private Button mAnalyzeButton;
    private ProgressBar mProgressBar;
    private TextView mResultsView;

    final StringBuilder mStringBuilder = new StringBuilder();

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_analyze_sherlock_holmes);

        mAnalyzeButton = findViewById(R.id.analyze);
        mAnalyzeButton.setOnClickListener(v -> enqueueWork());

        mProgressBar = findViewById(R.id.progress);
        mProgressBar.setVisibility(View.GONE);
        mResultsView = findViewById(R.id.results);
        mResultsView.setVisibility(View.GONE);

        TestDatabase.getInstance(this).getWordCountDao().getWordCounts().observe(
                this,
                wordCounts -> {
                    if (wordCounts == null) {
                        return;
                    }
                    mResultsView.setText(getWordCountsString(wordCounts));
                });
    }

    private void enqueueWork() {
        WorkManager workManager = WorkManager.getInstance();

        OneTimeWorkRequest textReducingWork =
                new OneTimeWorkRequest.Builder(TextReducingWorker.class)
                    .setInputMerger(ArrayCreatingInputMerger.class)
                    .build();

        OneTimeWorkRequest startupWork =
                new OneTimeWorkRequest.Builder(TextStartupWorker.class).build();

        workManager
                .beginWith(startupWork)
                .then(TextMappingWorker.create("advs.txt").build(),
                        TextMappingWorker.create("case.txt").build(),
                        TextMappingWorker.create("lstb.txt").build(),
                        TextMappingWorker.create("mems.txt").build(),
                        TextMappingWorker.create("retn.txt").build())
                .then(textReducingWork)
                .enqueue();

        workManager.getStatusById(textReducingWork.getId()).observe(
                this,
                status -> {
                    boolean loading = (status != null
                            && status.getState() != SUCCEEDED
                            && status.getState() != FAILED);
                    mProgressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
                    mResultsView.setVisibility(loading ? View.GONE : View.VISIBLE);
                    mAnalyzeButton.setEnabled(!loading);
                }
        );
    }

    private String getWordCountsString(List<WordCount> wordCounts) {
        // TODO: not efficient, this should be part of its own LiveData thing.
        mStringBuilder.setLength(0);
        for (WordCount wc : wordCounts) {
            mStringBuilder
                    .append(wc.mWord)
                    .append(" ")
                    .append(wc.mCount)
                    .append("\n");
        }
        return mStringBuilder.toString();
    }
}
