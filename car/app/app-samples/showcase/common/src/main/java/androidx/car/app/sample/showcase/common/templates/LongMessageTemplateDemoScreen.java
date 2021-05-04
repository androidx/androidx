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

package androidx.car.app.sample.showcase.common.templates;

import static androidx.car.app.CarToast.LENGTH_LONG;
import static androidx.car.app.model.Action.BACK;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.CarToast;
import androidx.car.app.Screen;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarColor;
import androidx.car.app.model.LongMessageTemplate;
import androidx.car.app.model.MessageTemplate;
import androidx.car.app.model.ParkedOnlyOnClickListener;
import androidx.car.app.model.Template;
import androidx.car.app.versioning.CarAppApiLevels;

/** A screen that demonstrates the long message template. */
public class LongMessageTemplateDemoScreen extends Screen {
    private static final String TEXT = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. "
            + "Aliquam laoreet ac metus eu commodo. Sed a congue diam, sed dictum lectus. Nam nec"
            + " tristique dolor, quis sodales arcu. Etiam at metus eu nulla auctor varius. "
            + "Integer dolor lorem, placerat sit amet lacus in, imperdiet semper dui. Vestibulum "
            + "ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia curae; "
            + "Quisque gravida fermentum egestas.\n"
            + "\n"
            + "Ut ut sodales mi. Aenean porta vel ipsum sed lacinia. Morbi odio ipsum, hendrerit "
            + "eu est et, sollicitudin finibus diam. Nunc sit amet felis elit. Orci varius "
            + "natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Sed "
            + "vestibulum, tellus a rutrum auctor, diam arcu vestibulum purus, nec mollis ligula "
            + "nisi in nisi. Donec sem tortor, pharetra sed fermentum sit amet, ullamcorper nec "
            + "sapien. Aliquam risus arcu, porttitor eu dui nec, vulputate tempus libero. "
            + "Curabitur sit amet tristique orci. Suspendisse et odio tempus, tempus turpis quis,"
            + " euismod est.\n"
            + "\n"
            + "Vestibulum mauris ante, luctus viverra nisi eget, blandit facilisis nulla. "
            + "Phasellus ex lorem, semper in vestibulum nec, aliquet vel elit. Aliquam vitae "
            + "ligula nec enim dictum lobortis. Sed varius turpis quis nisi tempus varius. Sed "
            + "non sollicitudin magna, at mattis tortor. Curabitur quis ligula eget lorem mattis "
            + "tincidunt et in sapien. Curabitur a elit nisi. Aliquam ex arcu, hendrerit eget "
            + "turpis vitae, bibendum vulputate nibh. Fusce vitae ex aliquet, tristique magna eu,"
            + " vulputate dui. Aenean tempor viverra tortor non pharetra. Pellentesque convallis "
            + "nec risus a auctor. Praesent non sem non eros tincidunt ullamcorper efficitur non "
            + "lacus.\n"
            + "\n"
            + "Suspendisse accumsan ultricies egestas. Aenean leo ligula, congue ac erat eu, "
            + "lobortis ultricies lorem. Nulla finibus, arcu sed tincidunt lobortis, magna justo "
            + "rutrum ligula, et mattis felis turpis vel ex. Morbi ac auctor ex, at bibendum sem."
            + " Vestibulum a tortor iaculis, viverra felis vitae, lobortis est. Duis sit amet "
            + "condimentum sem. Ut molestie, dolor pretium imperdiet maximus, enim orci porta "
            + "quam, id gravida enim nunc vitae lacus. Pellentesque habitant morbi tristique "
            + "senectus et netus et malesuada fames ac turpis egestas. Nullam vel justo eu risus "
            + "lobortis dignissim sit amet ullamcorper nulla. Donec finibus cursus purus "
            + "porttitor pellentesque.\n"
            + "\n"
            + "Donec at vehicula ante. Suspendisse rutrum nisl quis metus faucibus lacinia. "
            + "Vestibulum eros sapien, eleifend nec accumsan a, interdum sed nisi. Aenean posuere"
            + " ultrices lorem non pharetra. Nulla non porta ligula. Maecenas at elit diam. "
            + "Nullam gravida augue et semper eleifend. Fusce venenatis ac arcu et luctus. Mauris"
            + " ultricies urna non dui interdum, vel hendrerit est aliquam. Fusce id dictum leo, "
            + "fringilla egestas ipsum.";

    protected LongMessageTemplateDemoScreen(@NonNull CarContext carContext) {
        super(carContext);
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        if (getCarContext().getCarAppApiLevel() < CarAppApiLevels.LEVEL_2) {
            return new MessageTemplate.Builder("Your host doesn't support Long Message template")
                    .setTitle("Incompatible host")
                    .setHeaderAction(Action.BACK)
                    .build();
        }
        return new LongMessageTemplate.Builder(TEXT)
                .setTitle("Long Message Template Demo")
                .setHeaderAction(BACK)
                .addAction(new Action.Builder()
                        .setOnClickListener(
                                ParkedOnlyOnClickListener.create(() -> getScreenManager().pop()))
                        .setTitle("Accept")
                        .build())
                .addAction(new Action.Builder()
                        .setBackgroundColor(CarColor.RED)
                        .setOnClickListener(
                                ParkedOnlyOnClickListener.create(() -> getScreenManager().pop()))
                        .setTitle("Reject")
                        .build())
                .setActionStrip(new ActionStrip.Builder()
                        .addAction(new Action.Builder()
                            .setTitle("More")
                            .setOnClickListener(
                                    () ->
                                            CarToast.makeText(
                                                    getCarContext(),
                                                    "Clicked More",
                                                    LENGTH_LONG)
                                                    .show())
                            .build())
                        .build())
                .build();
    }
}
