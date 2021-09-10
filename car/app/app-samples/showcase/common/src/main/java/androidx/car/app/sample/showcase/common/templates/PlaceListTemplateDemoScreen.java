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

package androidx.car.app.sample.showcase.common.templates;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.common.SamplePlaces;

/** Creates a screen using the {@link PlaceListMapTemplate} */
public final class PlaceListTemplateDemoScreen extends Screen {
    private final SamplePlaces mPlaces;

    public PlaceListTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        mPlaces = SamplePlaces.create(this);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        return new PlaceListMapTemplate.Builder()
                .setItemList(mPlaces.getPlaceList())
                .setTitle("Place List Template Demo")
                .setHeaderAction(Action.BACK)
                .build();
    }
}
