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

package androidx.car.app.model.signin;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.ParkedOnlyOnClickListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link SignInTemplate}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class SignInTemplateTest {
    private final Action mAction =
            new Action.Builder().setTitle("Action").setOnClickListener(
                    ParkedOnlyOnClickListener.create(() -> {
                    })).build();

    @Test
    public void createInstance_noHeaderTitleOrAction_throws() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        assertThrows(IllegalStateException.class,
                () -> new SignInTemplate.Builder(signInMethod).build());

        // Positive cases.
        new SignInTemplate.Builder(signInMethod).setTitle("Title").build();
        new SignInTemplate.Builder(signInMethod).setHeaderAction(Action.BACK).build();
    }

    @Test
    public void moreThanTwoActions_throws() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        assertThrows(IllegalArgumentException.class,
                () -> new SignInTemplate.Builder(signInMethod)
                        .addAction(mAction)
                        .addAction(mAction)
                        .addAction(mAction));
    }

    @Test
    public void createInstance_defaultValues() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle("Title")
                .build();

        assertThat(template.getTitle().toString()).isEqualTo("Title");
        assertThat(template.isLoading()).isFalse();
        assertThat(template.getHeaderAction()).isNull();
        assertThat(template.getSignInMethod()).isEqualTo(signInMethod);
        assertThat(template.getActions()).isEmpty();
        assertThat(template.getActionStrip()).isNull();
        assertThat(template.getInstructions()).isNull();
        assertThat(template.getAdditionalText()).isNull();
    }

    @Test
    public void createInstance_setHeaderAction_invalidActionThrows() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new SignInTemplate.Builder(signInMethod)
                                .setHeaderAction(
                                        new Action.Builder().setTitle("Action").setOnClickListener(
                                                () -> {
                                                }).build()));
    }

    @Test
    public void createInstance_setLoading() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setHeaderAction(Action.BACK)
                .setLoading(true)
                .build();
        assertThat(template.isLoading()).isTrue();
    }

    @Test
    public void createInstance_setHeaderAction() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setHeaderAction(Action.BACK)
                .build();
        assertThat(template.getHeaderAction()).isEqualTo(Action.BACK);
    }

    @Test
    public void createInstance_setActionStrip() {
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle("Title")
                .setActionStrip(actionStrip)
                .build();

        assertThat(template.getActionStrip()).isEqualTo(actionStrip);
    }

    @Test
    public void createInstance_setInstructions() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle("Title")
                .setInstructions("Text")
                .build();

        assertThat(template.getInstructions().toString()).isEqualTo("Text");
    }

    @Test
    public void createInstance_setAdditionalText() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle("Title")
                .setAdditionalText("Text")
                .build();

        assertThat(template.getAdditionalText().toString()).isEqualTo("Text");
    }

    @Test
    public void createInstance_addActions() {
        Action action1 = new Action.Builder()
                .setTitle("Action")
                .setOnClickListener(ParkedOnlyOnClickListener.create(() -> { }))
                .build();
        Action action2 = new Action.Builder()
                .setTitle("Action").setOnClickListener(ParkedOnlyOnClickListener.create(() -> {
                })).build();
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle("Title")
                .addAction(action1)
                .addAction(action2)
                .build();

        assertThat(template.getActions()).containsExactly(action1, action2);
    }

    @Test
    public void createInstance_notParkedOnlyAction_throws() {
        Action action = new Action.Builder()
                .setTitle("Action")
                .setOnClickListener(() -> { })
                .build();
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();

        assertThrows(
                IllegalArgumentException.class,
                () -> new SignInTemplate.Builder(signInMethod).addAction(action));
    }

    @Test
    public void equals() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        String title = "Title";
        String instructions = "instructions";
        String additionalText = "Text";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle(title)
                .setLoading(true)
                .setInstructions(instructions)
                .setAdditionalText(additionalText)
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();

        assertThat(template)
                .isEqualTo(
                        new SignInTemplate.Builder(signInMethod)
                                .setLoading(true)
                                .setTitle(title)
                                .setInstructions(instructions)
                                .setAdditionalText(additionalText)
                                .addAction(mAction)
                                .setActionStrip(actionStrip)
                                .build());
    }

    @Test
    public void notEquals_differentLoadingState() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        String title = "Title";
        String instructions = "instructions";
        String additionalText = "Text";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle(title)
                .setLoading(true)
                .setInstructions(instructions)
                .setAdditionalText(additionalText)
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();

        assertThat(template)
                .isNotEqualTo(
                        new SignInTemplate.Builder(signInMethod)
                                .setLoading(false)
                                .setTitle(title)
                                .setInstructions(instructions)
                                .setAdditionalText(additionalText)
                                .addAction(mAction)
                                .setActionStrip(actionStrip)
                                .build());
    }

    @Test
    public void notEquals_differentSignInMethod() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        String title = "Title";
        String instructions = "instructions";
        String additionalText = "Text";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle(title)
                .setInstructions(instructions)
                .setAdditionalText(additionalText)
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();

        PinSignInMethod signInMethod2 = new PinSignInMethod.Builder("DEF").build();
        assertThat(template)
                .isNotEqualTo(
                        new SignInTemplate.Builder(signInMethod2)
                                .setTitle(title)
                                .setInstructions(instructions)
                                .setAdditionalText(additionalText)
                                .addAction(mAction)
                                .setActionStrip(actionStrip)
                                .build());
    }

    @Test
    public void notEquals_differentTitle() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        String instructions = "instructions";
        String additionalText = "Text";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle("Title")
                .setInstructions(instructions)
                .setAdditionalText(additionalText)
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();
        assertThat(template)
                .isNotEqualTo(
                        new SignInTemplate.Builder(signInMethod)
                                .setTitle("Title2")
                                .setInstructions(instructions)
                                .setAdditionalText(additionalText)
                                .addAction(mAction)
                                .setActionStrip(actionStrip)
                                .build());
    }

    @Test
    public void notEquals_differentInstructions() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        String title = "Title";
        String additionalText = "Text";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle(title)
                .setInstructions("instructions1")
                .setAdditionalText(additionalText)
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();
        assertThat(template)
                .isNotEqualTo(
                        new SignInTemplate.Builder(signInMethod)
                                .setTitle(title)
                                .setInstructions("instructions2")
                                .setAdditionalText(additionalText)
                                .addAction(mAction)
                                .setActionStrip(actionStrip)
                                .build());
    }

    @Test
    public void notEquals_differentAdditionalText() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        String instructions = "instructions";
        String title = "Title";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle(title)
                .setInstructions(instructions)
                .setAdditionalText("Text")
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();
        assertThat(template)
                .isNotEqualTo(
                        new SignInTemplate.Builder(signInMethod)
                                .setTitle(title)
                                .setInstructions(instructions)
                                .setAdditionalText("Text2")
                                .addAction(mAction)
                                .setActionStrip(actionStrip)
                                .build());
    }

    @Test
    public void notEquals_differentAction() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        String instructions = "instructions";
        String title = "Title";
        String additionalText = "Text";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.BACK).build();

        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle(title)
                .setInstructions(instructions)
                .setAdditionalText(additionalText)
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();
        assertThat(template)
                .isNotEqualTo(
                        new SignInTemplate.Builder(signInMethod)
                                .setTitle(title)
                                .setInstructions(instructions)
                                .setAdditionalText(additionalText)
                                .setActionStrip(actionStrip)
                                .build());
    }

    @Test
    public void notEquals_differentActionStrip() {
        PinSignInMethod signInMethod = new PinSignInMethod.Builder("ABC").build();
        String instructions = "instructions";
        String title = "Title";
        String additionalText = "Text";
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.APP_ICON).build();

        SignInTemplate template = new SignInTemplate.Builder(signInMethod)
                .setTitle(title)
                .setInstructions(instructions)
                .setAdditionalText(additionalText)
                .addAction(mAction)
                .setActionStrip(actionStrip)
                .build();
        ActionStrip actionStrip2 = new ActionStrip.Builder().addAction(Action.BACK).build();
        assertThat(template)
                .isNotEqualTo(
                        new SignInTemplate.Builder(signInMethod)
                                .setTitle(title)
                                .setInstructions(instructions)
                                .setAdditionalText(additionalText)
                                .addAction(mAction)
                                .setActionStrip(actionStrip2)
                                .build());
    }
}
