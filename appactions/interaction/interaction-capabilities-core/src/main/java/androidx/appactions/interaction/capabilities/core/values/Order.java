/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.appactions.interaction.capabilities.core.values;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.impl.BuilderOf;

import com.google.auto.value.AutoValue;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Represents an order object. */
@SuppressWarnings("AutoValueImmutableFields")
@AutoValue
public abstract class Order extends Thing {

    /** Create a new Order.Builder instance. */
    @NonNull
    public static Builder newBuilder() {
        return new AutoValue_Order.Builder();
    }

    /** Returns the date the order was placed. */
    @NonNull
    public abstract Optional<ZonedDateTime> getOrderDate();

    /** Returns the {@link OrderItem}s in the order. */
    @NonNull
    public abstract List<OrderItem> getOrderedItems();

    /** Returns the current status of the order. */
    @NonNull
    public abstract Optional<OrderStatus> getOrderStatus();

    /** Returns the name of the seller. */
    @NonNull
    public abstract Optional<Organization> getSeller();

    /** Returns the delivery information. */
    @NonNull
    public abstract Optional<ParcelDelivery> getOrderDelivery();

    /** Status of the order. */
    public enum OrderStatus {
        ORDER_CANCELED("OrderCanceled"),
        ORDER_DELIVERED("OrderDelivered"),
        ORDER_IN_TRANSIT("OrderInTransit"),
        ORDER_PAYMENT_DUE("OrderPaymentDue"),
        ORDER_PICKUP_AVAILABLE("OrderPickupAvailable"),
        ORDER_PROBLEM("OrderProblem"),
        ORDER_PROCESSING("OrderProcessing"),
        ORDER_RETURNED("OrderReturned");

        private final String mStringValue;

        OrderStatus(String stringValue) {
            this.mStringValue = stringValue;
        }

        @Override
        public String toString() {
            return mStringValue;
        }
    }

    /** Builder class for building an Order. */
    @AutoValue.Builder
    public abstract static class Builder extends Thing.Builder<Builder> implements
            BuilderOf<Order> {

        /** Order items to build. */
        private final List<OrderItem> mOrderItems = new ArrayList<>();

        /** Sets the date the order was placed. */
        @NonNull
        public abstract Builder setOrderDate(@NonNull ZonedDateTime orderDate);

        /** Sets the ordered items. */
        @NonNull
        abstract Builder setOrderedItems(@NonNull List<OrderItem> orderItems);

        /** Adds an item to the order. */
        @NonNull
        public final Builder addOrderedItem(@NonNull OrderItem orderItem) {
            mOrderItems.add(orderItem);
            return this;
        }

        /** Add a list of OrderItem. */
        @NonNull
        public final Builder addAllOrderedItems(@NonNull List<OrderItem> orderItems) {
            this.mOrderItems.addAll(orderItems);
            return this;
        }

        /** Sets the current order status. */
        @NonNull
        public abstract Builder setOrderStatus(@NonNull OrderStatus orderStatus);

        /** Sets the name of the seller. */
        @NonNull
        public abstract Builder setSeller(@NonNull Organization seller);

        /** Sets the order delivery. */
        @NonNull
        public abstract Builder setOrderDelivery(@NonNull ParcelDelivery parcelDelivery);

        /** Builds and returns the Order instance. */
        @Override
        @NonNull
        public final Order build() {
            setOrderedItems(mOrderItems);
            return autoBuild();
        }

        @NonNull
        abstract Order autoBuild();
    }
}
