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

import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.dynamicfeatures.fragment.R;

import com.google.android.play.core.splitinstall.model.SplitInstallErrorCode;

/**
 * The default fragment to display during installation progress.
 */
public final class DefaultProgressFragment extends AbstractProgressFragment {

    private TextView mModuleName;
    private ProgressBar mProgressBar;
    private static final int PROGRESS_MAX = 100;

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
        mProgressBar.setIndeterminate(true);
        mProgressBar.setMax(PROGRESS_MAX);

        ImageView activityIcon = view.findViewById(R.id.progress_icon);
        Drawable icon;
        try {
            icon = requireActivity().getPackageManager().getActivityIcon(
                            new ComponentName(requireContext(), requireActivity().getClass()));
        } catch (PackageManager.NameNotFoundException e) {
            icon = requireActivity().getPackageManager().getDefaultActivityIcon();
        }
        activityIcon.setImageDrawable(icon);
    }

    @Override
    protected void onProgress(int status, long bytesDownloaded, long bytesTotal) {
        mProgressBar.setVisibility(View.VISIBLE);
        if (bytesTotal > 0) {
            mProgressBar.setIndeterminate(false);
            mProgressBar.setProgress(
                    Long.valueOf((PROGRESS_MAX * bytesDownloaded) / bytesTotal).intValue()
            );
        } else {
            mProgressBar.setIndeterminate(true);
        }
    }

    @Override
    protected void onCancelled() {
        mProgressBar.setVisibility(View.INVISIBLE);
        mModuleName.setText(R.string.installation_cancelled);
    }

    @Override
    protected void onFailed(@SplitInstallErrorCode int errorCode) {
        mProgressBar.setVisibility(View.INVISIBLE);
        mModuleName.setText(R.string.installation_failed);
    }
}
