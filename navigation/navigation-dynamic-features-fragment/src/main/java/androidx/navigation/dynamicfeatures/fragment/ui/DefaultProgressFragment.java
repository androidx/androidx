/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.navigation.dynamicfeatures.fragment.ui;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.dynamicfeatures.fragment.R;

/**
 * The default fragment to display during installation progress.
 */
public final class DefaultProgressFragment extends AbstractProgressFragment {

    private TextView mModuleName;
    private ProgressBar mProgressBar;
    private int mTotalDownloadSize = 0;

    /**
     * Create a {@link DefaultProgressFragment}.
     */
    public DefaultProgressFragment() {
        super(R.layout.dynamic_feature_install_fragment);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mModuleName = view.findViewById(R.id.module_name);
        mProgressBar = view.findViewById(R.id.installation_progress);

        ImageView activityIcon = view.findViewById(R.id.progress_icon);
        // TODO: Change this for the target activity icon.
        activityIcon.setImageDrawable(
                requireActivity().getPackageManager().getDefaultActivityIcon());
    }

    @Override
    protected void onException(@NonNull Exception exception) {
        mModuleName.setText(exception.getMessage());
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void onProgress(long bytesDownloaded, long bytesTotal) {

        if (mTotalDownloadSize == 0) {
            mTotalDownloadSize = Long.valueOf(bytesTotal).intValue();
        }
        mProgressBar.setProgress(Long.valueOf(bytesDownloaded).intValue());
    }

    @Override
    protected void onCancelled() {
        mModuleName.setText(R.string.installation_cancelled);
    }

    @Override
    protected void onFailed() {
        mModuleName.setText(R.string.installation_failed);
    }

}
