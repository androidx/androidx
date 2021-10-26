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

package androidx.window.sample.embedding;

import static android.app.PendingIntent.FLAG_IMMUTABLE;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.LayoutDirection;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.CompoundButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.util.Consumer;
import androidx.window.core.ExperimentalWindowApi;
import androidx.window.embedding.ActivityFilter;
import androidx.window.embedding.ActivityRule;
import androidx.window.embedding.EmbeddingRule;
import androidx.window.embedding.SplitController;
import androidx.window.embedding.SplitInfo;
import androidx.window.embedding.SplitPairFilter;
import androidx.window.embedding.SplitPairRule;
import androidx.window.embedding.SplitPlaceholderRule;
import androidx.window.sample.databinding.ActivitySplitActivityLayoutBinding;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kotlin.OptIn;

/**
 * Sample showcase of split activity rules. Allows the user to select some split configuration
 * options with checkboxes and launch activities with those options applied.
 */
@OptIn(markerClass = ExperimentalWindowApi.class)
public class SplitActivityBase extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "SplitActivityTest";
    private static final float MIN_SPLIT_WIDTH_DP = 600f;
    static final float SPLIT_RATIO = 0.3f;
    static final String EXTRA_LAUNCH_C_TO_SIDE = "launch_c_to_side";

    private SplitController mSplitController;
    private SplitInfoCallback mCallback;

    private ActivitySplitActivityLayoutBinding mViewBinding;

    /**
     * Flag indicating that the config is being updated from checkboxes changes in a loop.
     */
    private boolean mUpdatingConfigs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewBinding = ActivitySplitActivityLayoutBinding.inflate(getLayoutInflater());
        setContentView(mViewBinding.getRoot());

        // Setup activity launch buttons.
        mViewBinding.launchB.setOnClickListener((View v) ->
                startActivity(new Intent(this, SplitActivityB.class)));
        mViewBinding.launchBAndC.setOnClickListener((View v) -> {
            Intent bStartIntent = new Intent(this, SplitActivityB.class);
            bStartIntent.putExtra(EXTRA_LAUNCH_C_TO_SIDE, true);
            startActivity(bStartIntent);
        });
        mViewBinding.launchE.setOnClickListener((View v) ->
                startActivity(new Intent(this, SplitActivityE.class)));
        mViewBinding.launchF.setOnClickListener((View v) ->
                startActivity(new Intent(this, SplitActivityF.class)));
        mViewBinding.launchFPendingIntent.setOnClickListener((View v) -> {
            try {
                PendingIntent.getActivity(this, 0, new Intent(this, SplitActivityF.class),
                        FLAG_IMMUTABLE).send();
            } catch (PendingIntent.CanceledException e) {
                Log.e(TAG, e.getMessage());
            }
        });

        // Listen for split configuration checkboxes to update the rules before launching
        // activities.
        mViewBinding.splitMainCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.usePlaceholderCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.splitBCCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.finishBCCheckBox.setOnCheckedChangeListener(this);
        mViewBinding.fullscreenECheckBox.setOnCheckedChangeListener(this);
        mViewBinding.splitWithFCheckBox.setOnCheckedChangeListener(this);

        mSplitController = SplitController.Companion.getInstance();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mCallback = new SplitInfoCallback();
        mSplitController.addSplitListener(this, Runnable::run, mCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mSplitController.removeSplitListener(mCallback);
        mCallback = null;
    }

    class SplitInfoCallback implements Consumer<List<SplitInfo>> {
        @Override
        public void accept(List<SplitInfo> splitInfoList) {
            updateCheckboxesFromCurrentConfig();
        }
    }

    @Override
    public void onCheckedChanged(@NonNull CompoundButton c, boolean isChecked) {
        if (mUpdatingConfigs) {
            return;
        }
        if (c.getId() == mViewBinding.splitBCCheckBox.getId()) {
            if (isChecked) {
                mViewBinding.finishBCCheckBox.setEnabled(true);
            } else {
                mViewBinding.finishBCCheckBox.setEnabled(false);
                mViewBinding.finishBCCheckBox.setChecked(false);
            }
        }
        updateRulesFromCheckboxes();
    }

    void updateCheckboxesFromCurrentConfig() {
        mUpdatingConfigs = true;

        SplitPairRule splitMainConfig = getRuleFor(SplitActivityA.class, null);
        mViewBinding.splitMainCheckBox.setChecked(splitMainConfig != null);

        SplitPlaceholderRule placeholderForBConfig = getPlaceholderRule(SplitActivityB.class);
        mViewBinding.usePlaceholderCheckBox.setChecked(placeholderForBConfig != null);

        SplitPairRule bAndCPairConfig = getRuleFor(SplitActivityB.class,
                SplitActivityC.class);
        mViewBinding.splitBCCheckBox.setChecked(bAndCPairConfig != null);
        mViewBinding.finishBCCheckBox.setEnabled(bAndCPairConfig != null);
        mViewBinding.finishBCCheckBox.setChecked(bAndCPairConfig != null
                && bAndCPairConfig.getFinishPrimaryWithSecondary()
                && bAndCPairConfig.getFinishSecondaryWithPrimary());

        SplitPairRule fConfig = getRuleFor(null, SplitActivityF.class);
        mViewBinding.splitWithFCheckBox.setChecked(fConfig != null);

        ActivityRule configE = getRuleFor(SplitActivityE.class);
        mViewBinding.fullscreenECheckBox.setChecked(configE != null && configE.getAlwaysExpand());

        mUpdatingConfigs = false;
    }

    private SplitPairRule getRuleFor(Class<? extends Activity> a, Class<? extends Activity> b) {
        Set<EmbeddingRule> currentRules = mSplitController.getSplitRules();
        for (EmbeddingRule rule : currentRules) {
            if (rule instanceof SplitPairRule && isRuleFor(a, b, (SplitPairRule) rule)) {
                return (SplitPairRule) rule;
            }
        }
        return null;
    }

    SplitPlaceholderRule getPlaceholderRule(Class<? extends Activity> a) {
        Set<EmbeddingRule> currentRules = mSplitController.getSplitRules();
        for (EmbeddingRule rule : currentRules) {
            if (rule instanceof SplitPlaceholderRule
                    && ((SplitPlaceholderRule) rule).getPlaceholderIntent().getComponent()
                    .getClassName().equals(a.getName())) {
                return (SplitPlaceholderRule) rule;
            }
        }
        return null;
    }

    private ActivityRule getRuleFor(Class<? extends Activity> a) {
        Set<EmbeddingRule> currentRules = mSplitController.getSplitRules();
        for (EmbeddingRule rule : currentRules) {
            if (rule instanceof ActivityRule && isRuleFor(a, (ActivityRule) rule)) {
                return (ActivityRule) rule;
            }
        }
        return null;
    }

    private boolean isRuleFor(Class<? extends Activity> a, Class<? extends Activity> b,
            SplitPairRule pairConfig) {
        return isRuleFor(a != null ? a.getName() : "*", b != null ? b.getName() : "*",
                pairConfig);
    }

    private boolean isRuleFor(String primaryActivityName, String secondaryActivityName,
            SplitPairRule pairConfig) {
        for (SplitPairFilter filter : pairConfig.getFilters()) {
            if (filter.getPrimaryActivityName().getClassName().contains(primaryActivityName)
                    && filter.getSecondaryActivityName().getClassName()
                    .contains(secondaryActivityName)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRuleFor(Class<? extends Activity> a, ActivityRule config) {
        return isRuleFor(a != null ? a.getName() : "*", config);
    }

    private boolean isRuleFor(String activityName, ActivityRule config) {
        for (ActivityFilter filter : config.getFilters()) {
            if (filter.getComponentName().getClassName().contains(activityName)) {
                return true;
            }
        }
        return false;
    }

    private void updateRulesFromCheckboxes() {
        int minSplitWidth = minSplitWidth();
        mSplitController.clearRegisteredRules();

        Set<SplitPairFilter> pairFilters = new HashSet<>();
        pairFilters.add(new SplitPairFilter(componentName(SplitActivityA.class),
                componentName("*"), null));
        SplitPairRule rule = new SplitPairRule(pairFilters, false, false, true, minSplitWidth,
                minSplitWidth, SPLIT_RATIO, LayoutDirection.LOCALE);
        if (mViewBinding.splitMainCheckBox.isChecked()) {
            mSplitController.registerRule(rule);
        }

        Set<ActivityFilter> activityFilters = new HashSet<>();
        activityFilters.add(new ActivityFilter(componentName(SplitActivityB.class), null));
        Intent intent = new Intent();
        intent.setComponent(
                componentName("androidx.window.sample.embedding.SplitActivityPlaceholder"));
        SplitPlaceholderRule placeholderRule = new SplitPlaceholderRule(activityFilters, intent,
                minSplitWidth, minSplitWidth, SPLIT_RATIO, LayoutDirection.LOCALE);
        if (mViewBinding.usePlaceholderCheckBox.isChecked()) {
            mSplitController.registerRule(placeholderRule);
        }

        pairFilters = new HashSet<>();
        pairFilters.add(new SplitPairFilter(componentName(SplitActivityB.class),
                componentName(SplitActivityC.class), null));
        rule = new SplitPairRule(pairFilters, mViewBinding.finishBCCheckBox.isChecked(),
                mViewBinding.finishBCCheckBox.isChecked(), true, minSplitWidth, minSplitWidth,
                SPLIT_RATIO, LayoutDirection.LOCALE);
        if (mViewBinding.splitBCCheckBox.isChecked()) {
            mSplitController.registerRule(rule);
        }

        pairFilters = new HashSet<>();
        pairFilters.add(new SplitPairFilter(componentName("androidx.window.*"),
                componentName(SplitActivityF.class), null));
        rule = new SplitPairRule(pairFilters, false, false, true, minSplitWidth, minSplitWidth,
                SPLIT_RATIO, LayoutDirection.LOCALE);
        if (mViewBinding.splitWithFCheckBox.isChecked()) {
            mSplitController.registerRule(rule);
        }

        activityFilters = new HashSet<>();
        activityFilters.add(new ActivityFilter(componentName(SplitActivityE.class), null));
        ActivityRule activityRule = new ActivityRule(activityFilters, true);
        if (mViewBinding.fullscreenECheckBox.isChecked()) {
            mSplitController.registerRule(activityRule);
        }
    }

    ComponentName componentName(Class<? extends Activity> activityClass) {
        return new ComponentName(getPackageName(),
                activityClass != null ? activityClass.getName() : "*");
    }

    ComponentName componentName(String className) {
        return new ComponentName(getPackageName(), className);
    }

    int minSplitWidth() {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, MIN_SPLIT_WIDTH_DP, dm);
    }
}
