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
package androidx.car.app.hardware.info;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;

import static java.util.Objects.requireNonNull;

import androidx.annotation.IntDef;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.annotations.RequiresCarApi;
import androidx.car.app.hardware.common.CarValue;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

/**
 * Information about toll card capabilities in a car.
 *
 * @deprecated Use TollCard.
 */
@CarProtocol
@RequiresCarApi(3)
@Deprecated
public final class Toll {

    /**
     * Possible toll card states.
     *
     * @hide
     */
    @IntDef({
            TOLLCARD_STATE_UNKNOWN,
            TOLLCARD_STATE_VALID,
            TOLLCARD_STATE_INVALID,
            TOLLCARD_STATE_NOT_INSERTED,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface State {
    }

    /**
     * Toll card state is unknown.
     */
    @State
    public static final int TOLLCARD_STATE_UNKNOWN = 0;

    /**
     * Toll card state is valid.
     */
    @State
    public static final int TOLLCARD_STATE_VALID = 1;

    /**
     * Toll card state invalid.
     *
     * <p>On some vehicles this may be that the toll card is inserted but not valid while other
     * vehicles might not have a toll card inserted.
     */
    @State
    public static final int TOLLCARD_STATE_INVALID = 2;

    /**
     * Toll card state is not inserted.
     *
     * <p>Will be returned if the car hardware is able to detect that the card is not inserted.
     */
    @State
    public static final int TOLLCARD_STATE_NOT_INSERTED = 3;

    @Keep
    @NonNull
    private final CarValue<Integer> mCardState;

    /** Returns the toll card state if available. */
    @NonNull
    public CarValue<Integer> getCardState() {
        return requireNonNull(mCardState);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ card state: " + mCardState + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mCardState);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Toll)) {
            return false;
        }
        Toll otherToll = (Toll) other;

        return Objects.equals(mCardState, otherToll.mCardState);
    }

    Toll(Builder builder) {
        mCardState = requireNonNull(builder.mCardState);
    }

    /** Constructs an empty instance, used by serialization code. */
    private Toll() {
        mCardState = CarValue.UNIMPLEMENTED_INTEGER;
    }

    /** A builder of {@link Toll}. */
    public static final class Builder {
        CarValue<Integer> mCardState = CarValue.UNIMPLEMENTED_INTEGER;

        /**
         * Sets the toll card state.
         *
         * @throws NullPointerException if {@code cardState} is {@code null}
         */
        @NonNull
        public Builder setCardState(@NonNull CarValue<Integer> cardState) {
            mCardState = requireNonNull(cardState);
            return this;
        }

        /**
         * Constructs the {@link Toll} defined by this builder.
         */
        @NonNull
        public Toll build() {
            return new Toll(this);
        }

    }
}
