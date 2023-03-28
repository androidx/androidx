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
import androidx.appactions.interaction.proto.ParamValue;
import androidx.appactions.interaction.protobuf.ListValue;
import androidx.appactions.interaction.protobuf.Struct;
import androidx.appactions.interaction.protobuf.Value;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;

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
                            "trackingUrl",
                            ParcelDelivery::getTrackingUrl,
                            ParcelDelivery.Builder::setTrackingUrl)
                    .build();
    public static final TypeSpec<Order> ORDER_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Order", Order::newBuilder)
                    .bindZonedDateTimeField(
                            "orderDate", Order::getOrderDate, Order.Builder::setOrderDate)
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
                            "seller",
                            Order::getSeller,
                            Order.Builder::setSeller,
                            ORGANIZATION_TYPE_SPEC)
                    .build();
    public static final TypeSpec<Person> PERSON_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Person", Person::newBuilder)
                    .bindStringField("email", Person::getEmail, Person.Builder::setEmail)
                    .bindStringField(
                            "telephone", Person::getTelephone, Person.Builder::setTelephone)
                    .bindStringField("name", Person::getName, Person.Builder::setName)
                    .build();
    public static final TypeSpec<Alarm> ALARM_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Alarm", Alarm::newBuilder).build();
    public static final TypeSpec<Timer> TIMER_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Timer", Timer::newBuilder).build();
    public static final TypeSpec<Attendee> ATTENDEE_TYPE_SPEC =
            new UnionTypeSpec.Builder<Attendee>()
                    .bindMemberType(
                            (attendee) -> attendee.asPerson().orElse(null),
                            (person) -> new Attendee(person),
                            PERSON_TYPE_SPEC)
                    .build();
    public static final TypeSpec<CalendarEvent> CALENDAR_EVENT_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("CalendarEvent", CalendarEvent::newBuilder)
                    .bindZonedDateTimeField(
                            "startDate",
                            CalendarEvent::getStartDate,
                            CalendarEvent.Builder::setStartDate)
                    .bindZonedDateTimeField(
                            "endDate", CalendarEvent::getEndDate, CalendarEvent.Builder::setEndDate)
                    .bindRepeatedSpecField(
                            "attendee",
                            CalendarEvent::getAttendeeList,
                            CalendarEvent.Builder::addAllAttendee,
                            ATTENDEE_TYPE_SPEC)
                    .build();
    public static final TypeSpec<SafetyCheck> SAFETY_CHECK_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("SafetyCheck", SafetyCheck::newBuilder)
                    .bindDurationField(
                            "duration", SafetyCheck::getDuration, SafetyCheck.Builder::setDuration)
                    .bindZonedDateTimeField(
                            "checkinTime",
                            SafetyCheck::getCheckinTime,
                            SafetyCheck.Builder::setCheckinTime)
                    .build();
    public static final TypeSpec<Recipient> RECIPIENT_TYPE_SPEC =
            new UnionTypeSpec.Builder<Recipient>()
                    .bindMemberType(
                            (recipient) -> recipient.asPerson().orElse(null),
                            (person) -> new Recipient(person),
                            PERSON_TYPE_SPEC)
                    .build();
    public static final TypeSpec<Participant> PARTICIPANT_TYPE_SPEC =
            new UnionTypeSpec.Builder<Participant>()
                    .bindMemberType(
                            (participant) -> participant.asPerson().orElse(null),
                            (person) -> new Participant(person),
                            PERSON_TYPE_SPEC)
                    .build();
    private static final String FIELD_NAME_CALL_FORMAT = "callFormat";
    private static final String FIELD_NAME_PARTICIPANT = "participant";
    private static final String FIELD_NAME_TYPE_CALL = "Call";
    private static final String FIELD_NAME_TYPE_MESSAGE = "Message";
    private static final String FIELD_NAME_RECIPIENT = "recipient";
    private static final String FIELD_NAME_TEXT = "text";

    public static final ParamValueConverter<Integer> INTEGER_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<Integer>() {
                @NonNull
                @Override
                public ParamValue toParamValue(Integer value) {
                    return ParamValue.newBuilder().setNumberValue(value * 1.0).build();
                }

                @Override
                public Integer fromParamValue(@NonNull ParamValue paramValue)
                        throws StructConversionException {
                    if (paramValue.hasNumberValue()) {
                        return (int) paramValue.getNumberValue();
                    }
                    throw new StructConversionException(
                            "Cannot parse integer because number_value"
                                    + " is missing from ParamValue.");
                }
            };
    public static final ParamValueConverter<Boolean> BOOLEAN_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<Boolean>() {
                @NonNull
                @Override
                public ParamValue toParamValue(Boolean value) {
                    return ParamValue.newBuilder().setBoolValue(value).build();
                }

                @Override
                public Boolean fromParamValue(@NonNull ParamValue paramValue)
                        throws StructConversionException {
                    if (paramValue.hasBoolValue()) {
                        return paramValue.getBoolValue();
                    }
                    throw new StructConversionException(
                            "Cannot parse boolean because bool_value is missing from ParamValue.");
                }
            };
    public static final ParamValueConverter<EntityValue> ENTITY_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<EntityValue>() {
                @NonNull
                @Override
                public ParamValue toParamValue(EntityValue value) {
                    throw new IllegalStateException(
                            "EntityValue should never be sent back to " + "Assistant.");
                }

                @Override
                public EntityValue fromParamValue(@NonNull ParamValue paramValue) {
                    EntityValue.Builder value = EntityValue.newBuilder();
                    if (paramValue.hasIdentifier()) {
                        value.setId(paramValue.getIdentifier());
                    }
                    value.setValue(paramValue.getStringValue());
                    return value.build();
                }
            };
    public static final ParamValueConverter<String> STRING_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<String>() {
                @NonNull
                @Override
                public ParamValue toParamValue(String value) {
                    return ParamValue.newBuilder().setStringValue(value).build();
                }

                @Override
                public String fromParamValue(@NonNull ParamValue paramValue) {
                    if (paramValue.hasIdentifier()) {
                        return paramValue.getIdentifier();
                    }
                    return paramValue.getStringValue();
                }
            };
    public static final ParamValueConverter<LocalDate> LOCAL_DATE_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<LocalDate>() {
                @NonNull
                @Override
                public ParamValue toParamValue(LocalDate value) {
                    // TODO(b/275456249): Implement backwards conversion.
                    return ParamValue.getDefaultInstance();
                }

                @Override
                public LocalDate fromParamValue(@NonNull ParamValue paramValue)
                        throws StructConversionException {
                    if (paramValue.hasStringValue()) {
                        try {
                            return LocalDate.parse(paramValue.getStringValue());
                        } catch (DateTimeParseException e) {
                            throw new StructConversionException(
                                    "Failed to parse ISO 8601 string to LocalDate", e);
                        }
                    }
                    throw new StructConversionException(
                            "Cannot parse date because string_value is missing from ParamValue.");
                }
            };
    public static final ParamValueConverter<LocalTime> LOCAL_TIME_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<LocalTime>() {
                @NonNull
                @Override
                public ParamValue toParamValue(LocalTime value) {
                    // TODO(b/275456249)): Implement backwards conversion.
                    return ParamValue.getDefaultInstance();
                }

                @Override
                public LocalTime fromParamValue(@NonNull ParamValue paramValue)
                        throws StructConversionException {
                    if (paramValue.hasStringValue()) {
                        try {
                            return LocalTime.parse(paramValue.getStringValue());
                        } catch (DateTimeParseException e) {
                            throw new StructConversionException(
                                    "Failed to parse ISO 8601 string to LocalTime", e);
                        }
                    }
                    throw new StructConversionException(
                            "Cannot parse time because string_value is missing from ParamValue.");
                }
            };
    public static final ParamValueConverter<ZoneId> ZONE_ID_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<ZoneId>() {
                @NonNull
                @Override
                public ParamValue toParamValue(ZoneId value) {
                    // TODO(b/275456249)): Implement backwards conversion.
                    return ParamValue.getDefaultInstance();
                }

                @Override
                public ZoneId fromParamValue(@NonNull ParamValue paramValue)
                        throws StructConversionException {
                    if (paramValue.hasStringValue()) {
                        try {
                            return ZoneId.of(paramValue.getStringValue());
                        } catch (DateTimeParseException e) {
                            throw new StructConversionException(
                                    "Failed to parse ISO 8601 string to ZoneId", e);
                        }
                    }
                    throw new StructConversionException(
                            "Cannot parse ZoneId because string_value is missing from ParamValue.");
                }
            };
    public static final ParamValueConverter<ZonedDateTime> ZONED_DATETIME_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<ZonedDateTime>() {
                @NonNull
                @Override
                public ParamValue toParamValue(ZonedDateTime value) {
                    // TODO(b/275456249)): Implement backwards conversion.
                    return ParamValue.getDefaultInstance();
                }

                @Override
                public ZonedDateTime fromParamValue(@NonNull ParamValue paramValue)
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
                            "Cannot parse datetime because string_value"
                                    + " is missing from ParamValue.");
                }
            };
    public static final ParamValueConverter<Duration> DURATION_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<Duration>() {
                @NonNull
                @Override
                public ParamValue toParamValue(Duration value) {
                    // TODO(b/275456249)): Implement backwards conversion.
                    return ParamValue.getDefaultInstance();
                }

                @Override
                public Duration fromParamValue(@NonNull ParamValue paramValue)
                        throws StructConversionException {
                    if (!paramValue.hasStringValue()) {
                        throw new StructConversionException(
                                "Cannot parse duration because string_value"
                                        + " is missing from ParamValue.");
                    }
                    try {
                        return Duration.parse(paramValue.getStringValue());
                    } catch (DateTimeParseException e) {
                        throw new StructConversionException(
                                "Failed to parse ISO 8601 string to Duration", e);
                    }
                }
            };
    public static final ParamValueConverter<Call.CallFormat> CALL_FORMAT_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<Call.CallFormat>() {
                @NonNull
                @Override
                public ParamValue toParamValue(Call.CallFormat value) {
                    // TODO(b/275456249)): Implement backwards conversion.
                    return ParamValue.getDefaultInstance();
                }

                @Override
                public Call.CallFormat fromParamValue(@NonNull ParamValue paramValue)
                        throws StructConversionException {
                    String identifier = paramValue.getIdentifier();
                    if (identifier.equals(Call.CallFormat.AUDIO.toString())) {
                        return Call.CallFormat.AUDIO;
                    } else if (identifier.equals(Call.CallFormat.VIDEO.toString())) {
                        return Call.CallFormat.VIDEO;
                    }
                    throw new StructConversionException(
                            String.format("Unknown enum format '%s'.", identifier));
                }
            };

    private TypeConverters() {}

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
     * @param zonedDateTime
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull ZonedDateTime zonedDateTime) {
        // TODO(b/274838299): Do not set "name" field after protos are checked in.
        return Entity.newBuilder().setName(zonedDateTime.toOffsetDateTime().toString()).build();
    }

    /**
     * @param localTime
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull LocalTime localTime) {
        // TODO(b/274838299): Do not set "name" field after protos are checked in.
        return Entity.newBuilder().setName(localTime.toString()).build();
    }

    /**
     * @param duration
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull Duration duration) {
        // TODO(b/274838299): Do not set "name" field after protos are checked in.
        return Entity.newBuilder().setName(duration.toString()).build();
    }

    /**
     * @param callFormat
     * @return
     */
    @NonNull
    public static Entity toEntity(@NonNull Call.CallFormat callFormat) {
        return Entity.newBuilder().setIdentifier(callFormat.toString()).build();
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
                .bindStringField(
                        "query", SearchAction<T>::getQuery, SearchAction.Builder<T>::setQuery)
                .bindSpecField(
                        "object",
                        SearchAction<T>::getObject,
                        SearchAction.Builder<T>::setObject,
                        nestedTypeSpec)
                .build();
    }

    /** Given some class with a corresponding TypeSpec, create a SearchActionConverter instance. */
    @NonNull
    public static <T> SearchActionConverter<T> createSearchActionConverter(
            @NonNull TypeSpec<T> nestedTypeSpec) {
        final TypeSpec<SearchAction<T>> typeSpec = createSearchActionTypeSpec(nestedTypeSpec);
        return ParamValueConverter.Companion.of(typeSpec)::fromParamValue;
    }

    /** Converts a Call to a ParamValue. */
    @NonNull
    public static ParamValue toParamValue(@NonNull Call value) {
        ParamValue.Builder builder = ParamValue.newBuilder();
        Map<String, Value> fieldsMap = new HashMap<>();
        fieldsMap.put(
                FIELD_NAME_TYPE, Value.newBuilder().setStringValue(FIELD_NAME_TYPE_CALL).build());
        if (value.getCallFormat().isPresent()) {
            fieldsMap.put(
                    FIELD_NAME_CALL_FORMAT,
                    Value.newBuilder()
                            .setStringValue(value.getCallFormat().get().toString())
                            .build());
        }
        ListValue.Builder participantListBuilder = ListValue.newBuilder();
        for (Participant participant : value.getParticipantList()) {
            if (participant.asPerson().isPresent()) {
                participantListBuilder.addValues(
                        PERSON_TYPE_SPEC.toValue(participant.asPerson().get()));
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
                        PERSON_TYPE_SPEC.toValue(recipient.asPerson().get()));
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
}
