/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.car.app.activity;

import android.annotation.SuppressLint;
import android.car.Car;
import android.car.drivingstate.CarUxRestrictionsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;

import androidx.annotation.VisibleForTesting;
import androidx.car.app.annotations.ExperimentalCarApi;
import androidx.fragment.app.FragmentActivity;

import org.jspecify.annotations.Nullable;

/**
 * <p> This class handles providing the right launcher activity when running native
 * applications and Car App Library applications.
 *
 * If distraction optimized is mandated {@link CarAppActivity} will be launched.
 * otherwise the activity with action {@link Intent#ACTION_MAIN} and category
 * {@link Intent#CATEGORY_DEFAULT} will be launched.
 */
@SuppressLint({"ForbiddenSuperClass"})
@ExperimentalCarApi
public final class LauncherActivity extends FragmentActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isDistractionOptimizedActivityRequired(this)) {
            startActivity(getCarAppActivityIntent(this));
        } else {
            startActivity(getDefaultIntent(this));
        }
        finish();
    }

    @SuppressWarnings("deprecation")
    @VisibleForTesting
    static Intent getDefaultIntent(Context context) {
        Intent intent =
                new Intent(Intent.ACTION_MAIN).addCategory(
                        Intent.CATEGORY_DEFAULT).setPackage(context.getPackageName());
        ResolveInfo resolveInfoList = context.getPackageManager().resolveActivity(intent,
                PackageManager.MATCH_DEFAULT_ONLY);

        if (resolveInfoList == null) {
            throw new IllegalStateException("Application requires an intent with Action Main and"
                    + " Category Default");
        } else {
            return new Intent().setComponent(new ComponentName(
                    resolveInfoList.activityInfo.packageName,
                    resolveInfoList.activityInfo.name));

        }
    }

    @VisibleForTesting
    static Intent getCarAppActivityIntent(Context context) {
        return new Intent(context, CarAppActivity.class);
    }

    @VisibleForTesting
    static boolean isDistractionOptimizedActivityRequired(Context context) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            return false;
        }

        Car car = Car.createCar(context);
        boolean isDistractionOptimizedRequired = ((CarUxRestrictionsManager) car.getCarManager(
                Car.CAR_UX_RESTRICTION_SERVICE))
                .getCurrentCarUxRestrictions().
                isRequiresDistractionOptimization();
        car.disconnect();
        car = null;
        return isDistractionOptimizedRequired;
    }

}
