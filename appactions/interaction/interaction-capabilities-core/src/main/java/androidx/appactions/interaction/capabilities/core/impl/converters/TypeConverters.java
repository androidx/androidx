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
import androidx.appactions.builtintypes.experimental.properties.Attendee;
import androidx.appactions.builtintypes.experimental.properties.EndDate;
import androidx.appactions.builtintypes.experimental.properties.ItemListElement;
import androidx.appactions.builtintypes.experimental.properties.Name;
import androidx.appactions.builtintypes.experimental.properties.Participant;
import androidx.appactions.builtintypes.experimental.properties.Recipient;
import androidx.appactions.builtintypes.experimental.properties.StartDate;
import androidx.appactions.builtintypes.experimental.properties.Text;
import androidx.appactions.builtintypes.experimental.types.CalendarEvent;
import androidx.appactions.builtintypes.experimental.types.Call;
import androidx.appactions.builtintypes.experimental.types.ItemList;
import androidx.appactions.builtintypes.experimental.types.ListItem;
import androidx.appactions.builtintypes.experimental.types.Message;
import androidx.appactions.builtintypes.experimental.types.Person;
import androidx.appactions.builtintypes.experimental.types.SafetyCheck;
import androidx.appactions.interaction.capabilities.core.SearchAction;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.properties.StringValue;
import androidx.appactions.interaction.proto.Entity;
import androidx.appactions.interaction.proto.ParamValue;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

/** Converters for capability argument values. Convert from internal proto types to public types. */
public final class TypeConverters {
    public static final String FIELD_NAME_TYPE = "@type";
    public static final TypeSpec<ListItem> LIST_ITEM_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing(
                    "ListItem",
                    ListItem::Builder,
                    ListItem.Builder::build).build();
    public static final TypeSpec<ItemListElement> ITEM_LIST_ELEMENT_TYPE_SPEC =
            new UnionTypeSpec.Builder<ItemListElement>()
                    .bindMemberType(
                            ItemListElement::asListItem,
                            ItemListElement::new,
                            LIST_ITEM_TYPE_SPEC)
                    .build();
    public static final TypeSpec<ItemList> ITEM_LIST_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing(
                            "ItemList",
                            ItemList::Builder,
                            ItemList.Builder::build)
                    .bindRepeatedSpecField(
                            "itemListElement",
                            ItemList::getItemListElements,
                            ItemList.Builder::addItemListElements,
                            ITEM_LIST_ELEMENT_TYPE_SPEC)
                    .build();

    public static final TypeSpec<Person> PERSON_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Person", Person::Builder, Person.Builder::build)
                    .bindStringField("email", Person::getEmail, Person.Builder::setEmail)
                    .bindStringField(
                            "telephone", Person::getTelephone, Person.Builder::setTelephone)
                    .bindStringField(
                            "name",
                            person ->
                                    Optional.ofNullable(person)
                                            .map(Person::getName)
                                            .map(Name::asText)
                                            .orElse(null),
                            Person.Builder::setName)
                    .build();
    public static final TypeSpec<Attendee> ATTENDEE_TYPE_SPEC =
            new UnionTypeSpec.Builder<Attendee>()
                    .bindMemberType(
                            Attendee::asPerson,
                            Attendee::new,
                            PERSON_TYPE_SPEC)
                    .build();
    public static final TypeSpec<CalendarEvent> CALENDAR_EVENT_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing(
                            "CalendarEvent",
                            CalendarEvent::Builder,
                            CalendarEvent.Builder::build)
                    .bindSpecField(
                            "startDate",
                            calendarEvent ->
                                    Optional.ofNullable(calendarEvent)
                                            .map(CalendarEvent::getStartDate)
                                            .map(StartDate::asZonedDateTime)
                                            .orElse(null),
                            CalendarEvent.Builder::setStartDate,
                            TypeSpec.ZONED_DATE_TIME_TYPE_SPEC)
                    .bindSpecField(
                            "endDate",
                            calendarEvent ->
                                    Optional.ofNullable(calendarEvent)
                                            .map(CalendarEvent::getEndDate)
                                            .map(EndDate::asZonedDateTime)
                                            .orElse(null),
                            CalendarEvent.Builder::setEndDate,
                            TypeSpec.ZONED_DATE_TIME_TYPE_SPEC)
                    .bindRepeatedSpecField(
                            "attendee",
                            CalendarEvent::getAttendeeList,
                            CalendarEvent.Builder::addAttendees,
                            ATTENDEE_TYPE_SPEC)
                    .build();
    public static final TypeSpec<SafetyCheck> SAFETY_CHECK_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing(
                            "SafetyCheck",
                            SafetyCheck::Builder,
                            SafetyCheck.Builder::build)
                    .bindSpecField(
                            "duration",
                            SafetyCheck::getDuration,
                            SafetyCheck.Builder::setDuration,
                            TypeSpec.DURATION_TYPE_SPEC)
                    .bindSpecField(
                            "checkInTime",
                            SafetyCheck::getCheckInTime,
                            SafetyCheck.Builder::setCheckInTime,
                            TypeSpec.ZONED_DATE_TIME_TYPE_SPEC)
                    .build();
    public static final TypeSpec<Recipient> RECIPIENT_TYPE_SPEC =
            new UnionTypeSpec.Builder<Recipient>()
                    .bindMemberType(
                            Recipient::asPerson,
                            Recipient::new,
                            PERSON_TYPE_SPEC)
                    .build();
    public static final TypeSpec<Participant> PARTICIPANT_TYPE_SPEC =
            new UnionTypeSpec.Builder<Participant>()
                    .bindMemberType(
                            Participant::asPerson,
                            Participant::new,
                            PERSON_TYPE_SPEC)
                    .build();
    public static final TypeSpec<Message> MESSAGE_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing("Message", Message::Builder, Message.Builder::build)
                    .bindIdentifier(Message::getIdentifier)
                    .bindRepeatedSpecField(
                            "recipient",
                            Message::getRecipientList,
                            Message.Builder::addRecipients,
                            RECIPIENT_TYPE_SPEC)
                    .bindStringField(
                            "text",
                            message ->
                                    Optional.ofNullable(message)
                                            .map(Message::getText)
                                            .map(Text::asText)
                                            .orElse(null),
                            Message.Builder::setText)
                    .build();
    public static final TypeSpec<Call> CALL_TYPE_SPEC =
            TypeSpecBuilder.newBuilderForThing(
                            "Call",
                            Call::Builder,
                            Call.Builder::build)
                    .bindIdentifier(Call::getIdentifier)
                    .bindRepeatedSpecField(
                            "participant",
                            Call::getParticipantList,
                            Call.Builder::addAllParticipant,
                            PARTICIPANT_TYPE_SPEC)
                    .build();

    public static final ParamValueConverter<Integer> INTEGER_PARAM_VALUE_CONVERTER =
            ParamValueConverter.of(TypeSpec.INTEGER_TYPE_SPEC);

    public static final ParamValueConverter<Boolean> BOOLEAN_PARAM_VALUE_CONVERTER =
            ParamValueConverter.of(TypeSpec.BOOL_TYPE_SPEC);

    public static final ParamValueConverter<String> STRING_PARAM_VALUE_CONVERTER =
            ParamValueConverter.of(TypeSpec.STRING_TYPE_SPEC);

    public static final ParamValueConverter<LocalDate> LOCAL_DATE_PARAM_VALUE_CONVERTER =
            ParamValueConverter.of(TypeSpec.LOCAL_DATE_TYPE_SPEC);

    public static final ParamValueConverter<LocalTime> LOCAL_TIME_PARAM_VALUE_CONVERTER =
            ParamValueConverter.of(TypeSpec.LOCAL_TIME_TYPE_SPEC);

    public static final ParamValueConverter<ZoneId> ZONE_ID_PARAM_VALUE_CONVERTER =
            ParamValueConverter.of(TypeSpec.ZONE_ID_TYPE_SPEC);

    public static final ParamValueConverter<ZonedDateTime> ZONED_DATE_TIME_PARAM_VALUE_CONVERTER =
            ParamValueConverter.of(TypeSpec.ZONED_DATE_TIME_TYPE_SPEC);

    public static final ParamValueConverter<Duration> DURATION_PARAM_VALUE_CONVERTER =
            ParamValueConverter.of(TypeSpec.DURATION_TYPE_SPEC);

    public static final ParamValueConverter<Call.CanonicalValue.CallFormat>
            CALL_FORMAT_PARAM_VALUE_CONVERTER =
            new ParamValueConverter<Call.CanonicalValue.CallFormat>() {

                @NonNull
                @Override
                public ParamValue toParamValue(Call.CanonicalValue.CallFormat value) {
                    return ParamValue.newBuilder()
                        .setStringValue(value.getTextValue())
                        .build();
                }

                @Override
                public Call.CanonicalValue.CallFormat fromParamValue(@NonNull ParamValue paramValue)
                        throws StructConversionException {
                    String identifier = paramValue.getIdentifier();
                    if (identifier.equals(Call.CanonicalValue.CallFormat.Audio.getTextValue())) {
                        return Call.CanonicalValue.CallFormat.Audio;
                    } else if (identifier.equals(
                            Call.CanonicalValue.CallFormat.Video.getTextValue())) {
                        return Call.CanonicalValue.CallFormat.Video;
                    }
                    throw new StructConversionException(
                            String.format("Unknown enum format '%s'.", identifier));
                }
            };
    public static final EntityConverter<StringValue> STRING_VALUE_ENTITY_CONVERTER =
            (stringValue) ->
                    Entity.newBuilder()
                            .setIdentifier(stringValue.getName())
                            .setName(stringValue.getName())
                            .addAllAlternateNames(stringValue.getAlternateNames())
                            .build();
    public static final EntityConverter<ZonedDateTime> ZONED_DATE_TIME_ENTITY_CONVERTER =
            EntityConverter.of(TypeSpec.ZONED_DATE_TIME_TYPE_SPEC);
    public static final EntityConverter<LocalTime> LOCAL_TIME_ENTITY_CONVERTER =
            EntityConverter.of(TypeSpec.LOCAL_TIME_TYPE_SPEC);
    public static final EntityConverter<Duration> DURATION_ENTITY_CONVERTER =
            EntityConverter.of(TypeSpec.DURATION_TYPE_SPEC);
    public static final EntityConverter<Call.CanonicalValue.CallFormat>
            CALL_FORMAT_ENTITY_CONVERTER =
                    (callFormat) ->
                            Entity.newBuilder().setIdentifier(callFormat.getTextValue()).build();

    @NonNull
    public static <T> TypeSpec<SearchAction<T>> createSearchActionTypeSpec(
            @NonNull TypeSpec<T> nestedTypeSpec) {
        return TypeSpecBuilder.newBuilder(
                        "SearchAction",
                        SearchAction.Builder<T>::new,
                        SearchAction.Builder::build)
                .bindStringField(
                        "query",
                        SearchAction::getQuery,
                        SearchAction.Builder::setQuery)
                .bindSpecField(
                        "filter",
                        SearchAction::getFilter,
                        SearchAction.Builder::setFilter,
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

    /** Given a list of supported Enum Types, creates a ParamValueConverter instance. */
    @NonNull
    public static <T> ParamValueConverter<T> createEnumParamValueConverter(
            @NonNull List<T> supportedValues) {
        return new ParamValueConverter<T>() {
            @Override
            public T fromParamValue(@NonNull ParamValue paramValue) throws
                    StructConversionException {
                for (T supportedValue : supportedValues) {
                    if (supportedValue.toString().equals(paramValue.getIdentifier())) {
                        return supportedValue;
                    }
                }
                throw new StructConversionException("cannot convert paramValue to protobuf "
                        + "Value because identifier " + paramValue.getIdentifier() + " is not "
                        + "one of the supported values");
            }

            @NonNull
            @Override
            public ParamValue toParamValue(@NonNull T obj) {
                for (T supportedValue : supportedValues) {
                    if (supportedValue.equals(obj)) {
                        return ParamValue.newBuilder().setIdentifier(obj.toString()).build();
                    }
                }
                throw new IllegalStateException("cannot convert " + obj + " to ParamValue "
                        + "because it did not match one of the supported values");
            }
        };
    }

    /** Given a list of supported Enum Types, creates a EntityConverter instance. */
    @NonNull
    public static <T> EntityConverter<T> createEnumEntityConverter(
            @NonNull List<T> supportedValues) {
        return new EntityConverter<T>() {
            @NonNull
            @Override
            public Entity convert(T obj) throws IllegalStateException {
                for (T supportedValue : supportedValues) {
                    if (supportedValue.toString().equals(obj.toString())) {
                        return Entity.newBuilder().setIdentifier(obj.toString()).build();
                    }
                }
                throw new IllegalStateException("cannot convert " + obj + " to entity "
                        + "because it did not match one of the supported values");
            }
        };
    }

    private TypeConverters() {
    }
}
