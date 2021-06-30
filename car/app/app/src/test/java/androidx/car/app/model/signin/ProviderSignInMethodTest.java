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
import static org.mockito.Mockito.mock;

import androidx.car.app.model.Action;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.ParkedOnlyOnClickListener;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

/** Tests for {@link ProviderSignInMethod}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class ProviderSignInMethodTest {
    @Test
    public void create_defaultValues() {
        OnClickListener clickListener = mock(OnClickListener.class);
        Action action = new Action.Builder()
                .setTitle("Signin")
                .setOnClickListener(ParkedOnlyOnClickListener.create(clickListener))
                .build();
        ProviderSignInMethod signIn = new ProviderSignInMethod(action);
        assertThat(signIn.getAction()).isEqualTo(action);
    }

    @Test
    public void create_standardAction_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProviderSignInMethod(Action.APP_ICON));
    }

    @Test
    public void create_nonParkedListener_throws() {
        OnClickListener clickListener = mock(OnClickListener.class);
        Action action = new Action.Builder()
                .setTitle("Signin")
                .setOnClickListener(clickListener)
                .build();
        assertThrows(
                IllegalArgumentException.class,
                () -> new ProviderSignInMethod(action));
    }

    @Test
    public void equals() {
        OnClickListener clickListener = mock(OnClickListener.class);
        Action action = new Action.Builder()
                .setTitle("Signin")
                .setOnClickListener(ParkedOnlyOnClickListener.create(clickListener))
                .build();
        ProviderSignInMethod signIn = new ProviderSignInMethod(action);

        assertThat(signIn).isEqualTo(new ProviderSignInMethod(action));
    }

    @Test
    public void notEquals_differentAction() {
        OnClickListener clickListener = mock(OnClickListener.class);
        Action action = new Action.Builder()
                .setTitle("Signin")
                .setOnClickListener(ParkedOnlyOnClickListener.create(clickListener))
                .build();
        ProviderSignInMethod signIn = new ProviderSignInMethod(action);

        Action action2 = new Action.Builder()
                .setTitle("Signin2")
                .setOnClickListener(ParkedOnlyOnClickListener.create(clickListener))
                .build();
        assertThat(signIn).isNotEqualTo(new ProviderSignInMethod(action2));
    }
}
