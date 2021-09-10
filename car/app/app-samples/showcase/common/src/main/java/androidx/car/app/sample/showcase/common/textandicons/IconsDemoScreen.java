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

package androidx.car.app.sample.showcase.common.textandicons;

import static androidx.car.app.model.Action.BACK;
import static androidx.car.app.model.CarColor.GREEN;

import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.core.graphics.drawable.IconCompat;

/** Creates a screen that demonstrate the usage of icons in the library. */
public final class IconsDemoScreen extends Screen {
    public IconsDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList.Builder listBuilder = new ItemList.Builder();

        listBuilder.addItem(
                new Row.Builder()
                        .setImage(new CarIcon.Builder(CarIcon.APP_ICON).build())
                        .setTitle("The app icon")
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setImage(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_fastfood_white_48dp))
                                        .build(),
                                Row.IMAGE_TYPE_ICON)
                        .setTitle("A vector drawable, without a tint")
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setImage(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_fastfood_white_48dp))
                                        .setTint(GREEN)
                                        .build())
                        .setTitle("A vector drawable, with a tint")
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setImage(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(),
                                                R.drawable.ic_themed_icon_48dp))
                                        .build())
                        .setTitle("A vector drawable, with an app's theme attribute for its color")
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setImage(
                                new CarIcon.Builder(
                                        IconCompat.createWithResource(
                                                getCarContext(), R.drawable.banana))
                                        .build())
                        .setTitle("A PNG, sent as a resource")
                        .build());

        listBuilder.addItem(
                new Row.Builder()
                        .setImage(
                                new CarIcon.Builder(
                                        IconCompat.createWithBitmap(
                                                BitmapFactory.decodeResource(
                                                        getCarContext().getResources(),
                                                        R.drawable.banana)))
                                        .build())
                        .setTitle("A PNG, sent as a bitmap")
                        .build());

        return new ListTemplate.Builder()
                .setSingleList(listBuilder.build())
                .setTitle("Icons Demo")
                .setHeaderAction(BACK)
                .build();
    }
}
