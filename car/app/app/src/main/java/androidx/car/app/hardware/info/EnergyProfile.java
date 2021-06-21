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
import java.util.List;
import java.util.Objects;

/** Information about car hardware fuel profile such as fuel types and connector ports. */
@CarProtocol
@RequiresCarApi(3)
public final class EnergyProfile {

    /**
     * Possible EV Connector types.
     *
     * @hide
     */
    @IntDef({
            EVCONNECTOR_TYPE_UNKNOWN,
            EVCONNECTOR_TYPE_J1772,
            EVCONNECTOR_TYPE_MENNEKES,
            EVCONNECTOR_TYPE_CHADEMO,
            EVCONNECTOR_TYPE_COMBO_1,
            EVCONNECTOR_TYPE_COMBO_2,
            EVCONNECTOR_TYPE_TESLA_ROADSTER,
            EVCONNECTOR_TYPE_TESLA_HPWC,
            EVCONNECTOR_TYPE_TESLA_SUPERCHARGER,
            EVCONNECTOR_TYPE_GBT,
            EVCONNECTOR_TYPE_GBT_DC,
            EVCONNECTOR_TYPE_SCAME,
            EVCONNECTOR_TYPE_OTHER,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface EvConnectorType {}

    /** Unknown connector type. */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_UNKNOWN = 0;

    /** Connector type SAE J1772 */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_J1772 = 1;

    /** IEC 62196 Type 2 connector */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_MENNEKES = 2;

    /** CHAdeMo fast charger connector */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_CHADEMO = 3;

    /** Combined Charging System Combo 1 */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_COMBO_1 = 4;

    /** Combined Charging System Combo 2 */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_COMBO_2 = 5;

    /** Connector of Tesla Roadster */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_TESLA_ROADSTER = 6;

    /** High Power Wall Charger of Tesla */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_TESLA_HPWC = 7;

    /** Supercharger of Tesla */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_TESLA_SUPERCHARGER = 8;

    /** GBT_AC Fast Charging Standard */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_GBT = 9;

    /** GBT_DC Fast Charging Standard */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_GBT_DC = 10;

    /** IEC_TYPE_3_AC connector */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_SCAME = 11;

    /**
     * Connector type to use when no other types apply.
     */
    @EvConnectorType
    public static final int EVCONNECTOR_TYPE_OTHER = 101;

    /**
     * Possible Fual types.
     *
     * @hide
     */
    @IntDef({
            FUEL_TYPE_UNKNOWN,
            FUEL_TYPE_UNLEADED,
            FUEL_TYPE_LEADED,
            FUEL_TYPE_DIESEL_1,
            FUEL_TYPE_DIESEL_2,
            FUEL_TYPE_BIODIESEL,
            FUEL_TYPE_E85,
            FUEL_TYPE_LPG,
            FUEL_TYPE_CNG,
            FUEL_TYPE_LNG,
            FUEL_TYPE_ELECTRIC,
            FUEL_TYPE_HYDROGEN,
            FUEL_TYPE_OTHER,
    })
    @Retention(RetentionPolicy.SOURCE)
    @RestrictTo(LIBRARY)
    public @interface FuelType {}

    /** Unknown fuel type */
    @FuelType public static final int FUEL_TYPE_UNKNOWN = 0;
    /** Unleaded gasoline */
    @FuelType public static final int FUEL_TYPE_UNLEADED = 1;
    /** Leaded gasoline */
    @FuelType public static final int FUEL_TYPE_LEADED = 2;
    /** #1 Grade Diesel */
    @FuelType public static final int FUEL_TYPE_DIESEL_1 = 3;
    /** #2 Grade Diesel */
    @FuelType public static final int FUEL_TYPE_DIESEL_2 = 4;
    /** Biodiesel */
    @FuelType public static final int FUEL_TYPE_BIODIESEL = 5;
    /** 85% ethanol/gasoline blend */
    @FuelType public static final int FUEL_TYPE_E85 = 6;
    /** Liquified petroleum gas */
    @FuelType public static final int FUEL_TYPE_LPG = 7;
    /** Compressed natural gas */
    @FuelType public static final int FUEL_TYPE_CNG = 8;
    /** Liquified natural gas */
    @FuelType public static final int FUEL_TYPE_LNG = 9;
    /** Electric */
    @FuelType public static final int FUEL_TYPE_ELECTRIC = 10;
    /** Hydrogen fuel cell */
    @FuelType public static final int FUEL_TYPE_HYDROGEN = 11;
    /** Fuel type to use when no other types apply. */
    @FuelType public static final int FUEL_TYPE_OTHER = 12;

    @Keep
    @NonNull
    private final CarValue<List<Integer>> mEvConnectorTypes;

    @Keep
    @NonNull
    private final CarValue<List<Integer>> mFuelTypes;

    /**
     *  Returns an array of the available EV connectors.
     *
     *  <p>If a vehicle does not know the EV connector type it will return
     *  {@link #EVCONNECTOR_TYPE_UNKNOWN} or {@link CarValue#STATUS_UNIMPLEMENTED}. If the value
     *  is known but not in the current list {@link #EVCONNECTOR_TYPE_UNKNOWN} will be returned.
     */
    @NonNull
    public CarValue<List<Integer>> getEvConnectorTypes() {
        return requireNonNull(mEvConnectorTypes);
    }

    /**
     *  Returns an array of the available fuel types.
     *
     *  <p>If a vehicle does not know the fuel type it will return {@link #FUEL_TYPE_UNKNOWN} or
     *  {@link CarValue#STATUS_UNIMPLEMENTED}. If the value is known but not in the current list
     *  {@link #EVCONNECTOR_TYPE_UNKNOWN} will be returned.
     */
    @NonNull
    public CarValue<List<Integer>> getFuelTypes() {
        return requireNonNull(mFuelTypes);
    }

    @Override
    @NonNull
    public String toString() {
        return "[ evConnectorTypes: " + mEvConnectorTypes + ", fuelTypes: " + mFuelTypes + "]";
    }

    @Override
    public int hashCode() {
        return Objects.hash(mEvConnectorTypes, mFuelTypes);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof EnergyProfile)) {
            return false;
        }
        EnergyProfile otherProfile = (EnergyProfile) other;

        return Objects.equals(mEvConnectorTypes, otherProfile.mEvConnectorTypes)
                && Objects.equals(mFuelTypes, otherProfile.mFuelTypes);
    }

    EnergyProfile(Builder builder) {
        mEvConnectorTypes = requireNonNull(builder.mEvConnectorTypes);
        mFuelTypes = requireNonNull(builder.mFuelTypes);
    }

    /** Constructs an empty instance, used by serialization code. */
    private EnergyProfile() {
        mEvConnectorTypes = CarValue.UNIMPLEMENTED_INTEGER_LIST;
        mFuelTypes = CarValue.UNIMPLEMENTED_INTEGER_LIST;
    }

    /** A builder of {@link EnergyProfile}. */
    public static final class Builder {
        CarValue<List<Integer>> mEvConnectorTypes = CarValue.UNIMPLEMENTED_INTEGER_LIST;
        CarValue<List<Integer>> mFuelTypes = CarValue.UNIMPLEMENTED_INTEGER_LIST;

        /**
         * Sets the cars EV connector types.
         *
         * @throws NullPointerException if {@code evConnectorTypes} is {@code null}
         */
        @NonNull
        public Builder setEvConnectorTypes(@NonNull CarValue<List<Integer>> evConnectorTypes) {
            mEvConnectorTypes = requireNonNull(evConnectorTypes);
            return this;
        }

        /**
         * Sets the cars fuel types.
         *
         * @throws NullPointerException if {@code fuelTypes} is {@code null}
         */
        @NonNull
        public Builder setFuelTypes(@NonNull CarValue<List<Integer>> fuelTypes) {
            mFuelTypes = requireNonNull(fuelTypes);
            return this;
        }

        /**
         * Constructs the {@link EnergyProfile} defined by this builder.
         *
         * <p>Any fields which have not been set are added with {@code null} value and
         * {@link CarValue#STATUS_UNIMPLEMENTED}.
         */
        @NonNull
        public EnergyProfile build() {
            return new EnergyProfile(this);
        }

    }
}
