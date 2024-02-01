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

package androidx.car.app.sample.showcase.common.screens.mapdemos;

import static androidx.car.app.CarToast.LENGTH_SHORT;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.sample.showcase.common.common.SamplePlaces;
import androidx.car.app.sample.showcase.common.screens.navigationdemos.RoutingDemoModels;
import androidx.core.graphics.drawable.IconCompat;

/** Creates a screen using the {@link PlaceListNavigationTemplate} */
public final class PlaceListNavigationTemplateDemoScreen extends Screen {
    private static final int NUMBER_OF_REFRESHES = 10;
    private static final long SECOND_DELAY = 1000L;
    private final SamplePlaces mPlaces = SamplePlaces.create(this);

    private final Handler mHandler = new Handler(Looper.getMainLooper());

    private boolean mIsAppRefresh = false;

    private boolean mIsFavorite;

    public PlaceListNavigationTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        boolean isAppDrivenRefreshEnabled = this.getCarContext().getCarService(
                ConstraintManager.class).isAppDrivenRefreshEnabled();

        if (isAppDrivenRefreshEnabled && !mIsAppRefresh) {
            mIsAppRefresh = true;
            for (int i = 1; i <= NUMBER_OF_REFRESHES; i++) {
                mHandler.postDelayed(this::invalidate, i * SECOND_DELAY);
            }
        }

        Header header = new Header.Builder()
                .setStartHeaderAction(Action.BACK)
                .addEndHeaderAction(new Action.Builder()
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                mIsFavorite
                                                        ? R.drawable.ic_favorite_filled_white_24dp
                                                        : R.drawable.ic_favorite_white_24dp))
                                        .build())
                        .setOnClickListener(() -> {
                            mIsFavorite = !mIsFavorite;
                            CarToast.makeText(
                                            getCarContext(),
                                            mIsFavorite
                                                    ? getCarContext()
                                                    .getString(R.string.favorite_toast_msg)
                                                    : getCarContext().getString(
                                                            R.string.not_favorite_toast_msg),
                                            LENGTH_SHORT)
                                    .show();
                            invalidate();
                        })
                        .build())
                .addEndHeaderAction(new Action.Builder()
                        .setOnClickListener(this::finish)
                        .setIcon(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_close_white_24dp))
                                        .build())
                        .build())
                .setTitle(getCarContext().getString(R.string.place_list_nav_template_demo_title))
                .build();

        PlaceListNavigationTemplate.Builder placeListNavigationTemplateBuilder =
                new PlaceListNavigationTemplate.Builder()
                        .setItemList(
                                mPlaces.getPlaceList(/* randomOrder= */ true))
                        .setHeader(header)
                        .setMapActionStrip(RoutingDemoModels.getMapActionStrip(getCarContext()))
                        .setActionStrip(
                                new ActionStrip.Builder()
                                        .addAction(
                                                new Action.Builder()
                                                        .setTitle(getCarContext().getString(
                                                                R.string.search_action_title))
                                                        .setOnClickListener(() -> {
                                                        })
                                                        .build())
                                        .build());

        if (!isAppDrivenRefreshEnabled) {
            placeListNavigationTemplateBuilder.setOnContentRefreshListener(this::invalidate);
        }

        return placeListNavigationTemplateBuilder.build();
    }
}
