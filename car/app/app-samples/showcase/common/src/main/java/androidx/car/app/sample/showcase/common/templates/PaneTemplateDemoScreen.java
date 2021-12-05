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

import static androidx.car.app.CarToast.LENGTH_SHORT;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

/**
 * Creates a screen that demonstrates usage of the full screen {@link PaneTemplate} to display a
 * details screen.
 */
public final class PaneTemplateDemoScreen extends Screen implements DefaultLifecycleObserver {
    @Nullable
    private IconCompat mPaneImage;

    @Nullable
    private IconCompat mRowLargeIcon;

    @Nullable
    private IconCompat mCommuteIcon;

    public PaneTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
        getLifecycle().addObserver(this);
    }

    @Override
    public void onCreate(@NonNull LifecycleOwner owner) {
        Resources resources = getCarContext().getResources();
        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.patio);
        mPaneImage = IconCompat.createWithBitmap(bitmap);
        mRowLargeIcon = IconCompat.createWithResource(getCarContext(),
                R.drawable.ic_fastfood_white_48dp);
        mCommuteIcon = IconCompat.createWithResource(getCarContext(), R.drawable.ic_commute_24px);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        Pane.Builder paneBuilder = new Pane.Builder();

        // Add a row with a large image.
        paneBuilder.addRow(
                new Row.Builder()
                        .setTitle("Row with a large image")
                        .addText("Text text text")
                        .addText("Text text text")
                        .setImage(new CarIcon.Builder(mRowLargeIcon).build())
                        .build());

        // Add a non-clickable rows.
        paneBuilder.addRow(
                new Row.Builder()
                        .setTitle("Row title")
                        .addText("Row text 1")
                        .addText("Row text 2")
                        .build());

        // Also set a large image outside of the rows.
        paneBuilder.setImage(new CarIcon.Builder(mPaneImage).build());

        Action.Builder primaryActionBuilder = new Action.Builder()
                .setTitle("Search")
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(
                        () -> CarToast.makeText(
                                getCarContext(),
                                "Search/Primary button pressed",
                                LENGTH_SHORT)
                                .show());
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_4) {
            primaryActionBuilder.setFlags(FLAG_PRIMARY);
        }

        paneBuilder
                .addAction(primaryActionBuilder.build())
                .addAction(
                        new Action.Builder()
                                .setTitle("Options")
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                getCarContext(),
                                                "Options button pressed",
                                                LENGTH_SHORT)
                                                .show())
                                .build());

        return new PaneTemplate.Builder(paneBuilder.build())
                .setHeaderAction(Action.BACK)
                .setActionStrip(
                        new ActionStrip.Builder()
                                .addAction(
                                        new Action.Builder()
                                                .setTitle("Commute")
                                                .setIcon(
                                                        new CarIcon.Builder(mCommuteIcon)
                                                                .setTint(CarColor.BLUE)
                                                                .build())
                                                .setOnClickListener(
                                                        () -> CarToast.makeText(
                                                                getCarContext(),
                                                                "Commute button"
                                                                        + " pressed",
                                                                LENGTH_SHORT)
                                                                .show())
                                                .build())
                                .build())
                .setTitle("Pane Template Demo")
                .build();
    }
}
