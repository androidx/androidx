/*
 * Copyright (C) 2021 The Android Open Source Project
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

package androidx.car.app.sample.showcase.common.navigation;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.model.PlaceListNavigationTemplate;
import androidx.car.app.sample.showcase.common.common.SamplePlaces;

/** Creates a screen using the {@link PlaceListNavigationTemplate} */
public final class PlaceListNavigationTemplateDemoScreen extends Screen {
    private final SamplePlaces mPlaces;

    public PlaceListNavigationTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mPlaces = SamplePlaces.create(this);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return new PlaceListNavigationTemplate.Builder()
                .setItemList(mPlaces.getPlaceList())
                .setTitle("Place List Navigation Template Demo")
                .setHeaderAction(Action.BACK)
                .setActionStrip(
                        new ActionStrip.Builder()
                                .addAction(
                                        new Action.Builder()
                                                .setTitle("Search")
                                                .setOnClickListener(() -> {
                                                })
                                                .build())
                                .build())
                .build();
    }
}
