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

package androidx.car.app.sample.showcase.common.screens.templatelayouts;

import static androidx.car.app.CarToast.LENGTH_SHORT;
import static androidx.car.app.model.Action.FLAG_PRIMARY;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.constraints.ConstraintManager;
import androidx.car.app.model.Action;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.Header;
import androidx.car.app.model.Pane;
import androidx.car.app.model.PaneTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.sample.showcase.common.R;
import androidx.car.app.versioning.CarAppApiLevels;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Creates a screen that demonstrates usage of the full screen {@link PaneTemplate} to display a
 * details screen.
 */
public final class PaneTemplateDemoScreen extends Screen implements DefaultLifecycleObserver {
    private @Nullable IconCompat mPaneImage;

    private @Nullable IconCompat mRowLargeIcon;

    private @Nullable IconCompat mCommuteIcon;

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

    private Row createRow(int index) {
        switch (index) {
            case 0:
                // Row with a large image.
                return new Row.Builder()
                        .setTitle(getCarContext().getString(R.string.first_row_title))
                        .addText(getCarContext().getString(R.string.first_row_text))
                        .addText(getCarContext().getString(R.string.first_row_text))
                        .setImage(new CarIcon.Builder(mRowLargeIcon).build())
                        .build();
            default:
                return new Row.Builder()
                        .setTitle(
                                getCarContext().getString(R.string.other_row_title_prefix) + (index
                                        + 1))
                        .addText(getCarContext().getString(R.string.other_row_text))
                        .addText(getCarContext().getString(R.string.other_row_text))
                        .build();
        }
    }

    @Override
    public @NonNull Template onGetTemplate() {
        int listLimit = getCarContext().getCarService(ConstraintManager.class).getContentLimit(
                ConstraintManager.CONTENT_LIMIT_TYPE_PANE);

        Pane.Builder paneBuilder = new Pane.Builder();
        for (int i = 0; i < listLimit; i++) {
            paneBuilder.addRow(createRow(i));
        }

        // Also set a large image outside of the rows.
        paneBuilder.setImage(new CarIcon.Builder(mPaneImage).build());

        Action.Builder primaryActionBuilder = new Action.Builder()
                .setTitle(getCarContext().getString(R.string.search_action_title))
                .setBackgroundColor(CarColor.BLUE)
                .setOnClickListener(
                        () -> CarToast.makeText(
                                        getCarContext(),
                                        getCarContext().getString(R.string.search_toast_msg),
                                        LENGTH_SHORT)
                                .show());
        if (getCarContext().getCarAppApiLevel() >= CarAppApiLevels.LEVEL_4) {
            primaryActionBuilder.setFlags(FLAG_PRIMARY);
        }

        paneBuilder
                .addAction(primaryActionBuilder.build())
                .addAction(
                        new Action.Builder()
                                .setTitle(getCarContext().getString(R.string.options_action_title))
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        getCarContext().getString(
                                                                R.string.options_toast_msg),
                                                        LENGTH_SHORT)
                                                .show())
                                .build());

        return new PaneTemplate.Builder(paneBuilder.build())
                .setHeader(new Header.Builder()
                        .setTitle(getCarContext().getString(R.string.pane_template_demo_title))
                        .setStartHeaderAction(Action.BACK)
                        .addEndHeaderAction(new Action.Builder()
                                .setTitle(getCarContext().getString(
                                        R.string.commute_action_title))
                                .setIcon(
                                        new CarIcon.Builder(mCommuteIcon)
                                                .setTint(CarColor.BLUE)
                                                .build())
                                .setOnClickListener(
                                        () -> CarToast.makeText(
                                                        getCarContext(),
                                                        getCarContext().getString(
                                                                R.string.commute_toast_msg),
                                                        LENGTH_SHORT)
                                                .show())
                                .build())
                        .build())
                .build();
    }
}
