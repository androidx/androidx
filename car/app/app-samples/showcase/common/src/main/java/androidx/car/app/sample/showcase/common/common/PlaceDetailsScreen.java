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

package androidx.car.app.sample.showcase.common.common;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.model.Action.BACK;

import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.HostException;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;

/** A screen that displays the details of a place. */
public class PlaceDetailsScreen extends Screen {
    private final PlaceInfo mPlace;

    /** Creates an instance of {@link PlaceDetailsScreen}. */
    @NonNull
    public static PlaceDetailsScreen create(
            @NonNull CarContext carContext, @NonNull PlaceInfo place) {
        return new PlaceDetailsScreen(carContext, place);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Pane.Builder paneBuilder =
                new Pane.Builder()
                        .addAction(
                                new Action.Builder()
                                        .setTitle("Navigate")
                                        .setBackgroundColor(CarColor.BLUE)
                                        .setOnClickListener(this::onClickNavigate)
                                        .build())
                        .addAction(
                                new Action.Builder()
                                        .setTitle("Dial")
                                        .setOnClickListener(this::onClickDial)
                                        .build())
                        .addRow(
                                new Row.Builder()
                                        .setTitle("Address")
                                        .addText(mPlace.address)
                                        .build())
                        .addRow(
                                new Row.Builder()
                                        .setTitle("Phone")
                                        .addText(mPlace.phoneNumber)
                                        .build());

        return new PaneTemplate.Builder(paneBuilder.build())
                .setTitle(mPlace.title)
                .setHeaderAction(BACK)
                .build();
    }

    private void onClickNavigate() {
        Uri uri = Uri.parse("geo:0,0?q=" + mPlace.address);
        Intent intent = new Intent(CarContext.ACTION_NAVIGATE, uri);

        try {
            getCarContext().startCarApp(intent);
        } catch (HostException e) {
            CarToast.makeText(
                    getCarContext(),
                    "Failure starting navigation",
                    LENGTH_LONG)
                    .show();
        }
    }

    private void onClickDial() {
        Uri uri = Uri.parse("tel:" + mPlace.phoneNumber);
        Intent intent = new Intent(Intent.ACTION_DIAL, uri);

        try {
            getCarContext().startCarApp(intent);
        } catch (HostException e) {
            CarToast.makeText(
                    getCarContext(),
                    "Failure starting dialer",
                    LENGTH_LONG)
                    .show();
        }
    }

    private PlaceDetailsScreen(@NonNull CarContext carContext, @NonNull PlaceInfo place) {
        super(carContext);
        mPlace = place;
    }
}
