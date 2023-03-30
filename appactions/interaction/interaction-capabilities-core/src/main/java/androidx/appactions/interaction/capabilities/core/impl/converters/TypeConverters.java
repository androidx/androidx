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

package androidx.appactions.interaction.capabilities.core.impl.converters;

import androidx.annotation.NonNull;
import androidx.appactions.interaction.capabilities.core.ExecutionResult;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.values.Alarm;
import androidx.appactions.interaction.capabilities.core.values.CalendarEvent;
import androidx.appactions.interaction.capabilities.core.values.Call;
import androidx.appactions.interaction.capabilities.core.values.EntityValue;
import androidx.appactions.interaction.capabilities.core.values.ItemList;
import androidx.appactions.interaction.capabilities.core.values.ListItem;
import androidx.appactions.interaction.capabilities.core.values.Message;
import androidx.appactions.interaction.capabilities.core.values.Order;
import androidx.appactions.interaction.capabilities.core.values.OrderItem;
import androidx.appactions.interaction.capabilities.core.values.Organization;
import androidx.appactions.interaction.capabilities.core.values.ParcelDelivery;
import androidx.appactions.interaction.capabilities.core.values.Person;
import androidx.appactions.interaction.capabilities.core.values.SafetyCheck;
import androidx.appactions.interaction.capabilities.core.values.SearchAction;
import androidx.appactions.interaction.capabilities.core.values.Timer;
import androidx.appactions.interaction.capabilities.core.values.properties.Attendee;
import androidx.appactions.interaction.capabilities.core.values.properties.Participant;
import androidx.appactions.interaction.capabilities.core.values.properties.Recipient;
import androidx.appactions.interaction.proto.Entity;
import androidx.appactions.interaction.proto.FulfillmentResponse;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Converters for capability argument values. Convert from internal proto types to public types. */
public final class TypeConverters {
    public static final String FIELD_NAME_TYPE = "@type";
    public static final TypeSpec<ListItem> LIST_ITEM_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("ListItem", ListItem::newBuilder).build();
    public static final TypeSpec<ItemList> ITEM_LIST_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("ItemList", ItemList::newBuilder)
                    .bindRepeatedSpecField(
                            "itemListElement",
                            ItemList::getListItems,
                            ItemList.Builder::addAllListItems,
                            LIST_ITEM_TYPE_SPEC)
                    .build();
    public static final TypeSpec<OrderItem> ORDER_ITEM_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("OrderItem", OrderItem::newBuilder).build();
    public static final TypeSpec<Organization> ORGANIZATION_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Organization", Organization::newBuilder).build();
    public static final TypeSpec<ParcelDelivery> PARCEL_DELIVERY_TYPE_SPEC =
            TypeSpecBuilder.newBuilder("ParcelDelivery", ParcelDelivery::newBuilder)
                    .bindStringField(
                            "deliveryAddress",
                            ParcelDelivery::getDeliveryAddress,
                            ParcelDelivery.Builder::setDeliveryAddress)
                    .bindZonedDateTimeField(
                            "expectedArrivalFrom",
                            ParcelDelivery::getExpectedArrivalFrom,
                            ParcelDelivery.Builder::setExpectedArrivalFrom)
                    .bindZonedDateTimeField(
                            "expectedArrivalUntil",
                            ParcelDelivery::getExpectedArrivalUntil,
                            ParcelDelivery.Builder::setExpectedArrivalUntil)
                    .bindStringField(
                            "hasDeliveryMethod",
                            ParcelDelivery::getDeliveryMethod,
                            ParcelDelivery.Builder::setDeliveryMethod)
                    .bindStringField(
                            "trackingNumber",
                            ParcelDelivery::getTrackingNumber,
                            ParcelDelivery.Builder::setTrackingNumber)
                    .bindStringField(
                            "trackingUrl", ParcelDelivery::getTrackingUrl,
                            ParcelDelivery.Builder::setTrackingUrl)
                    .build();
    public static final TypeSpec<Order> ORDER_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Order", Order::newBuilder)
                    .bindZonedDateTimeField("orderDate", Order::getOrderDate,
                            Order.Builder::setOrderDate)
                    .bindSpecField(
                            "orderDelivery",
                            Order::getOrderDelivery,
                            Order.Builder::setOrderDelivery,
                            PARCEL_DELIVERY_TYPE_SPEC)
                    .bindRepeatedSpecField(
                            "orderedItem",
                            Order::getOrderedItems,
                            Order.Builder::addAllOrderedItems,
                            ORDER_ITEM_TYPE_SPEC)
                    .bindEnumField(
                            "orderStatus",
                            Order::getOrderStatus,
                            Order.Builder::setOrderStatus,
                            Order.OrderStatus.class)
                    .bindSpecField(
                            "seller", Order::getSeller, Order.Builder::setSeller,
                            ORGANIZATION_TYPE_SPEC)
                    .build();
    public static final TypeSpec<Person> PERSON_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Person", Person::newBuilder)
                    .bindStringField("email", Person::getEmail, Person.Builder::setEmail)
                    .bindStringField("telephone", Person::getTelephone,
                            Person.Builder::setTelephone)
                    .bindStringField("name", Person::getName, Person.Builder::setName)
                    .build();
    public static final TypeSpec<Alarm> ALARM_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Alarm", Alarm::newBuilder).build();
    public static final TypeSpec<Timer> TIMER_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Timer", Timer::newBuilder).build();
    public static final TypeSpec<CalendarEvent> CALENDAR_EVENT_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("CalendarEvent", CalendarEvent::newBuilder)
                    .bindZonedDateTimeField(
                            "startDate", CalendarEvent::getStartDate,
                            CalendarEvent.Builder::setStartDate)
                    .bindZonedDateTimeField(
                            "endDate", CalendarEvent::getEndDate, CalendarEvent.Builder::setEndDate)
                    .bindRepeatedSpecField(
                            "attendee",
                            CalendarEvent::getAttendeeList,
                            CalendarEvent.Builder::addAllAttendee,
                            new AttendeeTypeSpec())
                    .build();
    public static final TypeSpec<SafetyCheck> SAFETY_CHECK_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("SafetyCheck", SafetyCheck::newBuilder)
                    .bindDurationField("duration", SafetyCheck::getDuration,
                            SafetyCheck.Builder::setDuration)
                    .bindZonedDateTimeField(
                            "checkinTime", SafetyCheck::getCheckinTime,
                            SafetyCheck.Builder::setCheckinTime)
                    .build();
    private static final String FIELD_NAME_CALL_FORMAT = "callFormat";
    private static final String FIELD_NAME_PARTICIPANT = "participant";
    private static final String FIELD_NAME_TYPE_CALL = "Call";
    private static final String FIELD_NAME_TYPE_PERSON = "Person";
    private static final String FIELD_NAME_TYPE_MESSAGE = "Message";
    private static final String FIELD_NAME_RECIPIENT = "recipient";
    private static final String FIELD_NAME_TEXT = "text";

    private TypeConverters() {
    }

    /**
     * @param paramValue
     * @return
     */
    @NonNull
    public static EntityValue toEntityValue(@NonNull ParamValue paramValue) {
        EntityValue.Builder value = EntityValue.newBuilder();
        if (paramValue.hasIdentifier()) {
            value.setId(paramValue.getIdentifier());
        }
        value.setValue(paramValue.getStringValue());
        return value.build();
    }

    /**
     * @param paramValue
     * @return
     */
    public static int toIntegerValue(@NonNull ParamValue paramValue) {
        return (int) paramValue.getNumberValue();
    }

    /** Converts a ParamValue to a Boolean object.
     *
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static Boolean toBooleanValue(@NonNull ParamValue paramValue)
            throws StructConversionException {
        if (paramValue.hasBoolValue()) {
            return paramValue.getBoolValue();
        }

        throw new StructConversionException(
                "Cannot parse boolean because bool_value is missing from ParamValue.");
    }

    /**
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static LocalDate toLocalDate(@NonNull ParamValue paramValue)
            throws StructConversionException {
        if (paramValue.hasStringValue()) {
            try {
                return LocalDate.parse(paramValue.getStringValue());
            } catch (DateTimeParseException e) {
                throw new StructConversionException("Failed to parse ISO 8601 string to LocalDate",
                        e);
            }
        }
        throw new StructConversionException(
                "Cannot parse date because string_value is missing from ParamValue.");
    }

    /**
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static LocalTime toLocalTime(@NonNull ParamValue paramValue)
            throws StructConversionException {
        if (paramValue.hasStringValue()) {
            try {
                return LocalTime.parse(paramValue.getStringValue());
            } catch (DateTimeParseException e) {
                throw new StructConversionException("Failed to parse ISO 8601 string to LocalTime",
                        e);
            }
        }
        throw new StructConversionException(
                "Cannot parse time because string_value is missing from ParamValue.");
    }

    /**
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static ZoneId toZoneId(@NonNull ParamValue paramValue) throws StructConversionException {
        if (paramValue.hasStringValue()) {
            try {
                return ZoneId.of(paramValue.getStringValue());
            } catch (DateTimeParseException e) {
                throw new StructConversionException("Failed to parse ISO 8601 string to ZoneId", e);
            }
        }
        throw new StructConversionException(
                "Cannot parse ZoneId because string_value is missing from ParamValue.");
    }

    /**
     * Gets String value for a string property.
     *
     * <p>If identifier is present, it's the String value, otherwise it is {@code
     * paramValue.getStringValue()}
     *
     * @param paramValue
     * @return
     */
    @NonNull
    public static String toStringValue(@NonNull ParamValue paramValue) {
        if (paramValue.hasIdentifier()) {
            return paramValue.getIdentifier();
        }
        return paramValue.getStringValue();
    }

    /**
     * @param entityValue
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull EntityValue entityValue) {
        return Entity.newBuilder()
                .setIdentifier(entityValue.getId().get())
                .setName(entityValue.getValue())
                .build();
    }

    /**
     * Converts an ItemList object to an Entity proto message.
     *
     * @param itemList
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull ItemList itemList) {
        Entity.Builder builder = Entity.newBuilder().setValue(
                ITEM_LIST_TYPE_SPEC.toStruct(itemList));
        itemList.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /**
     * Converts a ListItem object to an Entity proto message.
     *
     * @param listItem
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull ListItem listItem) {
        Entity.Builder builder = Entity.newBuilder().setValue(
                LIST_ITEM_TYPE_SPEC.toStruct(listItem));
        listItem.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /**
     * Converts an Order object to an Entity proto message.
     *
     * @param order
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull Order order) {
        Entity.Builder builder = Entity.newBuilder().setValue(ORDER_TYPE_SPEC.toStruct(order));
        order.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /**
     * Converts an Alarm object to an Entity proto message.
     *
     * @param alarm
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull Alarm alarm) {
        Entity.Builder builder = Entity.newBuilder().setValue(ALARM_TYPE_SPEC.toStruct(alarm));
        alarm.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /**
     * Converts a ParamValue to a single ItemList object.
     *
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static ItemList toItemList(@NonNull ParamValue paramValue)
            throws StructConversionException {
        return ITEM_LIST_TYPE_SPEC.fromStruct(paramValue.getStructValue());
    }

    /**
     * Converts a ParamValue to a single ListItem object.
     *
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static ListItem toListItem(@NonNull ParamValue paramValue)
            throws StructConversionException {
        return LIST_ITEM_TYPE_SPEC.fromStruct(paramValue.getStructValue());
    }

    /**
     * Converts a ParamValue to a single Order object.
     *
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static Order toOrder(@NonNull ParamValue paramValue) throws StructConversionException {
        return ORDER_TYPE_SPEC.fromStruct(paramValue.getStructValue());
    }

    /**
     * Converts a ParamValue to a Timer object.
     *
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static Timer toTimer(@NonNull ParamValue paramValue) throws StructConversionException {
        return TIMER_TYPE_SPEC.fromStruct(paramValue.getStructValue());
    }

    /**
     * Converts a ParamValue to a single Alarm object.
     *
     * @param paramValue
     * @return
     */
    @NonNull
    public static Alarm toAssistantAlarm(@NonNull ParamValue paramValue) {
        return Alarm.newBuilder().setId(paramValue.getIdentifier()).build();
    }

    /**
     * @param executionResult
     * @return
     */
    @NonNull
    public static FulfillmentResponse toFulfillmentResponseProto(
            @NonNull ExecutionResult<Void> executionResult) {
        return FulfillmentResponse.newBuilder()
                .setStartDictation(executionResult.getStartDictation())
                .build();
    }

    /**
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static ZonedDateTime toZonedDateTime(@NonNull ParamValue paramValue)
            throws StructConversionException {
        if (paramValue.hasStringValue()) {
            try {
                return ZonedDateTime.parse(paramValue.getStringValue());
            } catch (DateTimeParseException e) {
                throw new StructConversionException(
                        "Failed to parse ISO 8601 string to ZonedDateTime", e);
            }
        }
        throw new StructConversionException(
                "Cannot parse datetime because string_value is missing from ParamValue.");
    }

    /**
     * @param paramValue
     * @return
     * @throws StructConversionException
     */
    @NonNull
    public static Duration toDuration(@NonNull ParamValue paramValue)
            throws StructConversionException {
        if (!paramValue.hasStringValue()) {
            throw new StructConversionException(
                    "Cannot parse duration because string_value is missing from ParamValue.");
        }
        try {
            return Duration.parse(paramValue.getStringValue());
        } catch (DateTimeParseException e) {
            throw new StructConversionException("Failed to parse ISO 8601 string to Duration", e);
        }
    }

    /**
     * @param nestedTypeSpec
     * @param <T>
     * @return
     */
    @NonNull
    public static <T> TypeSpec<SearchAction<T>> createSearchActionTypeSpec(
            @NonNull TypeSpec<T> nestedTypeSpec) {
        return TypeSpecBuilder.<SearchAction<T>, SearchAction.Builder<T>>newBuilder(
                        "SearchAction", SearchAction::newBuilder)
                .bindStringField("query", SearchAction<T>::getQuery,
                        SearchAction.Builder<T>::setQuery)
                .bindSpecField(
                        "object",
                        SearchAction<T>::getObject,
                        SearchAction.Builder<T>::setObject,
                        nestedTypeSpec)
                .build();
    }

    /** Converts a ParamValue to a single Participant object. */
    @NonNull
    public static Participant toParticipant(@NonNull ParamValue paramValue)
            throws StructConversionException {
        if (FIELD_NAME_TYPE_PERSON.equals(getStructType(paramValue.getStructValue()))) {
            return new Participant(PERSON_TYPE_SPEC.fromStruct(paramValue.getStructValue()));
        }
        throw new StructConversionException("The type is not expected.");
    }

    /** Converts a ParamValue to a single Recipient object. */
    @NonNull
    public static Recipient toRecipient(@NonNull ParamValue paramValue)
            throws StructConversionException {
        if (FIELD_NAME_TYPE_PERSON.equals(getStructType(paramValue.getStructValue()))) {
            return new Recipient(PERSON_TYPE_SPEC.fromStruct(paramValue.getStructValue()));
        }
        throw new StructConversionException("The type is not expected.");
    }

    /** Given some class with a corresponding TypeSpec, create a SearchActionConverter instance. */
    @NonNull
    public static <T> SearchActionConverter<T> createSearchActionConverter(
            @NonNull TypeSpec<T> nestedTypeSpec) {
        final TypeSpec<SearchAction<T>> typeSpec = createSearchActionTypeSpec(nestedTypeSpec);
        return (paramValue) -> typeSpec.fromStruct(paramValue.getStructValue());
    }

    /** Converts a string to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull String value) {
        return ParamValue.newBuilder().setStringValue(value).build();
    }

    /** Converts an EntityValue to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull EntityValue value) {
        ParamValue.Builder builder = ParamValue.newBuilder().setStringValue(value.getValue());
        value.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /** Converts a ItemList to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull ItemList value) {
        ParamValue.Builder builder =
                ParamValue.newBuilder().setStructValue(ITEM_LIST_TYPE_SPEC.toStruct(value));
        value.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /** Converts a ListItem to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull ListItem value) {
        ParamValue.Builder builder =
                ParamValue.newBuilder().setStructValue(LIST_ITEM_TYPE_SPEC.toStruct(value));
        value.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /** Converts an Order to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull Order value) {
        ParamValue.Builder builder =
                ParamValue.newBuilder().setStructValue(ORDER_TYPE_SPEC.toStruct(value));
        value.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /** Converts an Alarm to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull Alarm value) {
        ParamValue.Builder builder =
                ParamValue.newBuilder().setStructValue(ALARM_TYPE_SPEC.toStruct(value));
        value.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /** Converts an SafetyCheck to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull SafetyCheck value) {
        ParamValue.Builder builder =
                ParamValue.newBuilder().setStructValue(SAFETY_CHECK_TYPE_SPEC.toStruct(value));
        value.getId().ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /** Converts a Participant to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull Participant value) {
        ParticipantTypeSpec typeSpec = new ParticipantTypeSpec();
        ParamValue.Builder builder = ParamValue.newBuilder().setStructValue(
                typeSpec.toStruct(value));
        typeSpec.getId(value).ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /** Converts a Recipient to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull Recipient value) {
        RecipientTypeSpec typeSpec = new RecipientTypeSpec();
        ParamValue.Builder builder = ParamValue.newBuilder().setStructValue(
                typeSpec.toStruct(value));
        typeSpec.getId(value).ifPresent(builder::setIdentifier);
        return builder.build();
    }

    /** Converts a Call to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull Call value) {
        ParamValue.Builder builder = ParamValue.newBuilder();
        Map<String, Value> fieldsMap = new HashMap<>();
        fieldsMap.put(FIELD_NAME_TYPE,
                Value.newBuilder().setStringValue(FIELD_NAME_TYPE_CALL).build());
        if (value.getCallFormat().isPresent()) {
            fieldsMap.put(
                    FIELD_NAME_CALL_FORMAT,
                    Value.newBuilder().setStringValue(
                            value.getCallFormat().get().toString()).build());
        }
        ListValue.Builder participantListBuilder = ListValue.newBuilder();
        for (Participant participant : value.getParticipantList()) {
            if (participant.asPerson().isPresent()) {
                participantListBuilder.addValues(
                        Value.newBuilder()
                                .setStructValue(
                                        PERSON_TYPE_SPEC.toStruct(participant.asPerson().get()))
                                .build());
            }
        }
        if (!participantListBuilder.getValuesList().isEmpty()) {
            fieldsMap.put(
                    FIELD_NAME_PARTICIPANT,
                    Value.newBuilder().setListValue(participantListBuilder.build()).build());
        }
        value.getId().ifPresent(builder::setIdentifier);
        return builder.setStructValue(Struct.newBuilder().putAllFields(fieldsMap).build()).build();
    }

    /** Converts a Message to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull Message value) {
        ParamValue.Builder builder = ParamValue.newBuilder();
        Map<String, Value> fieldsMap = new HashMap<>();
        fieldsMap.put(
                FIELD_NAME_TYPE,
                Value.newBuilder().setStringValue(FIELD_NAME_TYPE_MESSAGE).build());
        if (value.getMessageText().isPresent()) {
            fieldsMap.put(
                    FIELD_NAME_TEXT,
                    Value.newBuilder().setStringValue(value.getMessageText().get()).build());
        }
        ListValue.Builder recipientListBuilder = ListValue.newBuilder();
        for (Recipient recipient : value.getRecipientList()) {
            if (recipient.asPerson().isPresent()) {
                recipientListBuilder.addValues(
                        Value.newBuilder()
                                .setStructValue(
                                        PERSON_TYPE_SPEC.toStruct(recipient.asPerson().get()))
                                .build());
            }
        }
        if (!recipientListBuilder.getValuesList().isEmpty()) {
            fieldsMap.put(
                    FIELD_NAME_RECIPIENT,
                    Value.newBuilder().setListValue(recipientListBuilder.build()).build());
        }
        value.getId().ifPresent(builder::setIdentifier);
        return builder.setStructValue(Struct.newBuilder().putAllFields(fieldsMap).build()).build();
    }

    private static String getStructType(Struct struct) throws StructConversionException {
        Map<String, Value> fieldsMap = struct.getFieldsMap();
        if (!fieldsMap.containsKey(FIELD_NAME_TYPE)
                || fieldsMap.get(FIELD_NAME_TYPE).getStringValue().isEmpty()) {
            throw new StructConversionException("There is no type specified.");
        }
        return fieldsMap.get(FIELD_NAME_TYPE).getStringValue();
    }

    /** {@link TypeSpec} for {@link Participant}. */
    public static class ParticipantTypeSpec implements TypeSpec<Participant> {
        @Override
        @NonNull
        public Struct toStruct(@NonNull Participant object) {
            if (object.asPerson().isPresent()) {
                return PERSON_TYPE_SPEC.toStruct(object.asPerson().get());
            }
            return Struct.getDefaultInstance();
        }

        @Override
        @NonNull
        public Participant fromStruct(@NonNull Struct struct) throws StructConversionException {
            if (FIELD_NAME_TYPE_PERSON.equals(getStructType(struct))) {
                return new Participant(PERSON_TYPE_SPEC.fromStruct(struct));
            }
            throw new StructConversionException(
                    String.format(
                            "Unexpected type, expected type is %s while actual type is %s",
                            FIELD_NAME_TYPE_PERSON, getStructType(struct)));
        }

        /**
         * Retrieves identifier from the object within union value.
         *
         * @param object
         * @return
         */
        @NonNull
        public Optional<String> getId(@NonNull Participant object) {
            return object.asPerson().isPresent() ? object.asPerson().get().getId()
                    : Optional.empty();
        }
    }

    /** {@link TypeSpec} for {@link Recipient}. */
    public static class RecipientTypeSpec implements TypeSpec<Recipient> {
        @NonNull
        @Override
        public Struct toStruct(@NonNull Recipient object) {
            if (object.asPerson().isPresent()) {
                return PERSON_TYPE_SPEC.toStruct(object.asPerson().get());
            }
            return Struct.getDefaultInstance();
        }

        @Override
        @NonNull
        public Recipient fromStruct(@NonNull Struct struct) throws StructConversionException {
            if (FIELD_NAME_TYPE_PERSON.equals(getStructType(struct))) {
                return new Recipient(PERSON_TYPE_SPEC.fromStruct(struct));
            }
            throw new StructConversionException(
                    String.format(
                            "Unexpected type, expected type is %s while actual type is %s",
                            FIELD_NAME_TYPE_PERSON, getStructType(struct)));
        }

        /**
         * Retrieves identifier from the object within union value.
         *
         * @param object
         * @return
         */
        @NonNull
        public Optional<String> getId(@NonNull Recipient object) {
            return object.asPerson().isPresent() ? object.asPerson().get().getId()
                    : Optional.empty();
        }
    }

    /** {@link TypeSpec} for {@link Attendee}. */
    public static class AttendeeTypeSpec implements TypeSpec<Attendee> {
        @Override
        @NonNull
        public Struct toStruct(@NonNull Attendee object) {
            if (object.asPerson().isPresent()) {
                return PERSON_TYPE_SPEC.toStruct(object.asPerson().get());
            }
            return Struct.getDefaultInstance();
        }

        @NonNull
        @Override
        public Attendee fromStruct(@NonNull Struct struct) throws StructConversionException {
            if (FIELD_NAME_TYPE_PERSON.equals(getStructType(struct))) {
                return new Attendee(TypeConverters.PERSON_TYPE_SPEC.fromStruct(struct));
            }
            throw new StructConversionException(
                    String.format(
                            "Unexpected type, expected type is %s while actual type is %s",
                            FIELD_NAME_TYPE_PERSON, getStructType(struct)));
        }

        /**
         * Retrieves identifier from the object within union value.
         *
         * @param object
         * @return
         */
        @NonNull
        public Optional<String> getId(@NonNull Attendee object) {
            return object.asPerson().isPresent() ? object.asPerson().get().getId()
                    : Optional.empty();
        }
    }
}
