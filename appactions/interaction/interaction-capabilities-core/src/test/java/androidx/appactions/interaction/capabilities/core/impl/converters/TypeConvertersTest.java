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

import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.BOOLEAN_PARAM_VALUE_CONVERTER;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.CALL_TYPE_SPEC;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.INTEGER_PARAM_VALUE_CONVERTER;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.ITEM_LIST_TYPE_SPEC;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.LIST_ITEM_TYPE_SPEC;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.MESSAGE_TYPE_SPEC;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.PARTICIPANT_TYPE_SPEC;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.RECIPIENT_TYPE_SPEC;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.SAFETY_CHECK_TYPE_SPEC;
import static androidx.appactions.interaction.capabilities.core.impl.converters.TypeConverters.TIMER_TYPE_SPEC;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

import androidx.appactions.builtintypes.experimental.properties.Attendee;
import androidx.appactions.builtintypes.experimental.properties.Participant;
import androidx.appactions.builtintypes.experimental.properties.Recipient;
import androidx.appactions.builtintypes.experimental.types.CalendarEvent;
import androidx.appactions.builtintypes.experimental.types.Call;
import androidx.appactions.builtintypes.experimental.types.ItemList;
import androidx.appactions.builtintypes.experimental.types.ListItem;
import androidx.appactions.builtintypes.experimental.types.Message;
import androidx.appactions.builtintypes.experimental.types.Person;
import androidx.appactions.builtintypes.experimental.types.SafetyCheck;
import androidx.appactions.builtintypes.experimental.types.Timer;
import androidx.appactions.interaction.capabilities.core.SearchAction;
import androidx.appactions.interaction.capabilities.core.impl.exceptions.StructConversionException;
import androidx.appactions.interaction.capabilities.core.values.EntityValue;
import androidx.appactions.interaction.proto.Entity;
import androidx.appactions.interaction.proto.ParamValue;
import androidx.appactions.interaction.protobuf.ListValue;
import androidx.appactions.interaction.protobuf.Struct;
import androidx.appactions.interaction.protobuf.Value;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.zone.ZoneRulesException;
import java.util.Collections;
import java.util.List;

@RunWith(JUnit4.class)
public final class TypeConvertersTest {
    private static Value structToValue(Struct struct) {
        return Value.newBuilder().setStructValue(struct).build();
    }

    private static final Person PERSON_JAVA_THING =
            Person.Builder()
                    .setName("name")
                    .setEmail("email")
                    .setTelephone("telephone")
                    .setIdentifier("id")
                    .build();
    private static final Person PERSON_JAVA_THING_2 = Person.Builder().setIdentifier("id2").build();
    private static final CalendarEvent CALENDAR_EVENT_JAVA_THING =
            CalendarEvent.Builder()
                    .setStartDate(ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC))
                    .setEndDate(ZonedDateTime.of(2023, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC))
                    .addAttendee(new Attendee(PERSON_JAVA_THING))
                    .addAttendee(new Attendee(PERSON_JAVA_THING_2))
                    .build();
    private static final Call CALL_JAVA_THING =
            Call.Builder()
                    .setIdentifier("id")
                    .addParticipant(PERSON_JAVA_THING)
                    .build();
    private static final Message MESSAGE_JAVA_THING =
            Message.Builder()
                    .setIdentifier("id")
                    .addRecipient(PERSON_JAVA_THING)
                    .setText("hello")
                    .build();
    private static final SafetyCheck SAFETY_CHECK_JAVA_THING =
            SafetyCheck.Builder()
                    .setIdentifier("id")
                    .setDuration(Duration.ofMinutes(5))
                    .setCheckInTime(ZonedDateTime.of(2023, 01, 10, 10, 0, 0, 0, ZoneOffset.UTC))
                    .build();

    private static final Struct PERSON_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Person").build())
                    .putFields("identifier", Value.newBuilder().setStringValue("id").build())
                    .putFields("name",
                            Value.newBuilder().setStringValue("name").build())
                    .putFields("email", Value.newBuilder().setStringValue("email").build())
                    .putFields("telephone", Value.newBuilder().setStringValue("telephone").build())
                    .build();
    private static final Struct PERSON_STRUCT_2 =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Person").build())
                    .putFields("identifier", Value.newBuilder().setStringValue("id2").build())
                    .build();
    private static final Value CALENDAR_EVENT_VALUE =
            structToValue(
                    Struct.newBuilder()
                            .putFields(
                                    "@type",
                                    Value.newBuilder().setStringValue("CalendarEvent").build())
                            .putFields(
                                    "startDate",
                                    Value.newBuilder().setStringValue("2022-01-01T08:00Z").build())
                            .putFields(
                                    "endDate",
                                    Value.newBuilder().setStringValue("2023-01-01T08:00Z").build())
                            .putFields(
                                    "attendee",
                                    Value.newBuilder()
                                            .setListValue(
                                                    ListValue.newBuilder()
                                                            .addValues(
                                                                    Value.newBuilder()
                                                                            .setStructValue(
                                                                                    PERSON_STRUCT)
                                                                            .build())
                                                            .addValues(
                                                                    Value.newBuilder()
                                                                            .setStructValue(
                                                                                    PERSON_STRUCT_2)
                                                                            .build()))
                                            .build())
                            .build());
    private static final Struct CALL_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Call").build())
                    .putFields("identifier", Value.newBuilder().setStringValue("id").build())
                    .putFields(
                            "participant",
                            Value.newBuilder()
                                    .setListValue(
                                            ListValue.newBuilder()
                                                    .addValues(
                                                            Value.newBuilder()
                                                                    .setStructValue(PERSON_STRUCT)))
                                    .build())
                    .build();
    private static final Struct MESSAGE_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Message").build())
                    .putFields("identifier", Value.newBuilder().setStringValue("id").build())
                    .putFields(
                            "recipient",
                            Value.newBuilder()
                                    .setListValue(
                                            ListValue.newBuilder()
                                                    .addValues(
                                                            Value.newBuilder()
                                                                    .setStructValue(PERSON_STRUCT))
                                                    .build())
                                    .build())
                    .putFields("text", Value.newBuilder().setStringValue("hello").build())
                    .build();
    private static final Struct SAFETY_CHECK_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("SafetyCheck").build())
                    .putFields("identifier", Value.newBuilder().setStringValue("id").build())
                    .putFields("duration", Value.newBuilder().setStringValue("PT5M").build())
                    .putFields(
                            "checkInTime",
                            Value.newBuilder().setStringValue("2023-01-10T10:00Z").build())
                    .build();

    private static ParamValue toParamValue(Struct struct, String identifier) {
        return ParamValue.newBuilder().setIdentifier(identifier).setStructValue(struct).build();
    }

    @Test
    public void toEntityValue() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder()
                                .setIdentifier("entity-id")
                                .setStringValue("string-val")
                                .build());

        assertThat(
                SlotTypeConverter.ofSingular(TypeConverters.ENTITY_PARAM_VALUE_CONVERTER)
                        .convert(input))
                .isEqualTo(
                        EntityValue.newBuilder().setId("entity-id").setValue("string-val").build());
    }

    @Test
    public void toIntegerValue() throws Exception {
        ParamValue paramValue = ParamValue.newBuilder().setNumberValue(5).build();
        List<ParamValue> input = Collections.singletonList(paramValue);

        assertThat(SlotTypeConverter.ofSingular(INTEGER_PARAM_VALUE_CONVERTER).convert(input))
                .isEqualTo(5);

        assertThat(INTEGER_PARAM_VALUE_CONVERTER.toParamValue(5)).isEqualTo(paramValue);
        assertThat(INTEGER_PARAM_VALUE_CONVERTER.fromParamValue(paramValue)).isEqualTo(5);
    }

    @Test
    public void toStringValue_fromList() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("hello world").build());

        assertThat(
                SlotTypeConverter.ofSingular(TypeConverters.STRING_PARAM_VALUE_CONVERTER)
                        .convert(input))
                .isEqualTo("hello world");
    }

    @Test
    public void toStringValue_fromSingleParam() throws Exception {
        ParamValue input = ParamValue.newBuilder().setStringValue("hello world").build();

        assertThat(TypeConverters.STRING_PARAM_VALUE_CONVERTER.fromParamValue(input))
                .isEqualTo("hello world");
    }

    @Test
    public void listItem_conversions_matchesExpected() throws Exception {
        ListItem listItem = ListItem.Builder().setIdentifier("itemId").setName("Test Item").build();
        Struct listItemStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("ListItem").build())
                        .putFields(
                                "identifier", Value.newBuilder().setStringValue("itemId").build())
                        .putFields("name", Value.newBuilder().setStringValue("Test Item").build())
                        .build();
        Entity listItemProto =
                Entity.newBuilder().setIdentifier("itemId").setStructValue(listItemStruct).build();

        assertThat(EntityConverter.Companion.of(LIST_ITEM_TYPE_SPEC).convert(listItem))
                .isEqualTo(listItemProto);
        assertThat(
                ParamValueConverter.Companion.of(LIST_ITEM_TYPE_SPEC)
                        .fromParamValue(toParamValue(listItemStruct, "itemId")))
                .isEqualTo(listItem);
    }

    @Test
    public void itemList_conversions_matchesExpected() throws Exception {
        ItemList itemList =
                ItemList.Builder()
                        .setIdentifier("testList")
                        .setName("Test List")
                        .addItemListElement(
                                ListItem.Builder().setIdentifier("item1").setName("apple").build())
                        .addItemListElement(
                                ListItem.Builder().setIdentifier("item2").setName("banana").build())
                        .build();
        Struct itemListStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("ItemList").build())
                        .putFields(
                                "identifier", Value.newBuilder().setStringValue("testList").build())
                        .putFields("name", Value.newBuilder().setStringValue("Test List").build())
                        .putFields(
                                "itemListElement",
                                Value.newBuilder()
                                        .setListValue(
                                                ListValue.newBuilder()
                                                        .addValues(
                                                                Value.newBuilder()
                                                                        .setStructValue(
                                                                                Struct.newBuilder()
                                                                                        .putFields(
                                                                                                "@type",
                                                                                                Value
                                                                                                        .newBuilder()
                                                                                                        .setStringValue(
                                                                                                                "ListItem")
                                                                                                        .build())
                                                                                        .putFields(
                                                                                                "identifier",
                                                                                                Value
                                                                                                        .newBuilder()
                                                                                                        .setStringValue(
                                                                                                                "item1")
                                                                                                        .build())
                                                                                        .putFields(
                                                                                                "name",
                                                                                                Value
                                                                                                        .newBuilder()
                                                                                                        .setStringValue(
                                                                                                                "apple")
                                                                                                        .build())
                                                                                        .build())
                                                                        .build())
                                                        .addValues(
                                                                Value.newBuilder()
                                                                        .setStructValue(
                                                                                Struct.newBuilder()
                                                                                        .putFields(
                                                                                                "@type",
                                                                                                Value
                                                                                                        .newBuilder()
                                                                                                        .setStringValue(
                                                                                                                "ListItem")
                                                                                                        .build())
                                                                                        .putFields(
                                                                                                "identifier",
                                                                                                Value
                                                                                                        .newBuilder()
                                                                                                        .setStringValue(
                                                                                                                "item2")
                                                                                                        .build())
                                                                                        .putFields(
                                                                                                "name",
                                                                                                Value
                                                                                                        .newBuilder()
                                                                                                        .setStringValue(
                                                                                                                "banana")
                                                                                                        .build())
                                                                                        .build())
                                                                        .build())
                                                        .build())
                                        .build())
                        .build();
        Entity itemListProto =
                Entity.newBuilder()
                        .setIdentifier("testList")
                        .setStructValue(itemListStruct)
                        .build();

        assertThat(EntityConverter.Companion.of(ITEM_LIST_TYPE_SPEC).convert(itemList))
                .isEqualTo(itemListProto);
        assertThat(ParamValueConverter.Companion.of(ITEM_LIST_TYPE_SPEC)
                .fromParamValue(toParamValue(itemListStruct, "testList"))).isEqualTo(itemList);
    }

    @Test
    public void participant_conversions_matchesExpected() throws Exception {
        ParamValueConverter<Participant> paramValueConverter =
                ParamValueConverter.Companion.of(PARTICIPANT_TYPE_SPEC);
        ParamValue paramValue =
                ParamValue.newBuilder()
                        .setIdentifier(PERSON_JAVA_THING.getIdentifier())
                        .setStructValue(PERSON_STRUCT)
                        .build();
        Participant participant = new Participant(PERSON_JAVA_THING);

        assertThat(paramValueConverter.toParamValue(participant)).isEqualTo(paramValue);
        assertThat(paramValueConverter.fromParamValue(paramValue)).isEqualTo(participant);
    }

    @Test
    public void calendarEvent_conversions_matchesExpected() throws Exception {
        assertThat(TypeConverters.CALENDAR_EVENT_TYPE_SPEC.toValue(CALENDAR_EVENT_JAVA_THING))
                .isEqualTo(CALENDAR_EVENT_VALUE);
        assertThat(TypeConverters.CALENDAR_EVENT_TYPE_SPEC.fromValue(CALENDAR_EVENT_VALUE))
                .isEqualTo(CALENDAR_EVENT_JAVA_THING);
    }

    @Test
    public void recipient_conversions_matchesExpected() throws Exception {
        ParamValueConverter<Recipient> paramValueConverter =
                ParamValueConverter.Companion.of(RECIPIENT_TYPE_SPEC);
        ParamValue paramValue =
                ParamValue.newBuilder()
                        .setIdentifier(PERSON_JAVA_THING.getIdentifier() == null ? "id" :
                                PERSON_JAVA_THING.getIdentifier())
                        .setStructValue(PERSON_STRUCT)
                        .build();
        Recipient recipient = new Recipient(PERSON_JAVA_THING);

        assertThat(paramValueConverter.toParamValue(recipient)).isEqualTo(paramValue);
        assertThat(paramValueConverter.fromParamValue(paramValue)).isEqualTo(recipient);
    }

    @Test
    public void toParticipant_unexpectedType_throwsException() {
        ParamValueConverter<Participant> paramValueConverter =
                ParamValueConverter.Companion.of(PARTICIPANT_TYPE_SPEC);
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Malformed").build())
                        .build();

        assertThrows(
                StructConversionException.class,
                () -> paramValueConverter.fromParamValue(toParamValue(malformedStruct, "id")));
    }

    @Test
    public void toRecipient_unexpectedType_throwsException() {
        ParamValueConverter<Recipient> paramValueConverter =
                ParamValueConverter.Companion.of(RECIPIENT_TYPE_SPEC);
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Malformed").build())
                        .build();

        assertThrows(
                StructConversionException.class,
                () -> paramValueConverter.fromParamValue(toParamValue(malformedStruct, "id")));
    }

    @Test
    public void itemList_malformedStruct_throwsException() {
        ParamValueConverter<ItemList> paramValueConverter =
                ParamValueConverter.Companion.of(ITEM_LIST_TYPE_SPEC);
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Malformed").build())
                        .putFields("name", Value.newBuilder().setStringValue("List Name").build())
                        .putFields("identifier", Value.newBuilder().setStringValue("list1").build())
                        .build();

        assertThrows(
                StructConversionException.class,
                () -> paramValueConverter.fromParamValue(toParamValue(malformedStruct, "list1")));
    }

    @Test
    public void listItem_malformedStruct_throwsException() throws Exception {
        ParamValueConverter<ListItem> paramValueConverter =
                ParamValueConverter.Companion.of(LIST_ITEM_TYPE_SPEC);
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Malformed").build())
                        .putFields("name", Value.newBuilder().setStringValue("Apple").build())
                        .putFields("identifier", Value.newBuilder().setStringValue("item1").build())
                        .build();

        assertThrows(
                StructConversionException.class,
                () -> paramValueConverter.fromParamValue(toParamValue(malformedStruct, "item1")));
    }

    @Test
    public void toBoolean_success() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setBoolValue(false).build());

        assertThat(SlotTypeConverter.ofSingular(BOOLEAN_PARAM_VALUE_CONVERTER).convert(input))
                .isFalse();
    }

    @Test
    public void toBoolean_throwsException() {
        List<ParamValue> input = Collections.singletonList(ParamValue.getDefaultInstance());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(BOOLEAN_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .matches("cannot convert .+ into Value.");
    }

    @Test
    public void toInteger_throwsException() {
        List<ParamValue> input = Collections.singletonList(ParamValue.getDefaultInstance());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(INTEGER_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .matches("cannot convert .+ into Value.");
    }

    @Test
    public void localDate_success() throws Exception {
        ParamValueConverter<LocalDate> converter = TypeConverters.LOCAL_DATE_PARAM_VALUE_CONVERTER;
        ParamValue paramValue = ParamValue.newBuilder().setStringValue("2018-06-17").build();
        LocalDate localDate = LocalDate.of(2018, 6, 17);

        assertThat(converter.fromParamValue(paramValue)).isEqualTo(localDate);
        assertThat(converter.toParamValue(localDate)).isEqualTo(paramValue);
    }

    @Test
    public void toLocalDate_throwsException() {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("2018-0617").build());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.LOCAL_DATE_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Failed to parse ISO 8601 string to LocalDate");
    }

    @Test
    public void toLocalDateMissingValue_throwsException() {
        List<ParamValue> input = Collections.singletonList(ParamValue.getDefaultInstance());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.LOCAL_DATE_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Cannot parse date because string_value is missing from ParamValue.");
    }

    @Test
    public void localTime_success() throws Exception {
        ParamValueConverter<LocalTime> converter = TypeConverters.LOCAL_TIME_PARAM_VALUE_CONVERTER;
        ParamValue paramValue = ParamValue.newBuilder().setStringValue("15:10:05").build();
        LocalTime localTime = LocalTime.of(15, 10, 5);

        assertThat(converter.fromParamValue(paramValue)).isEqualTo(localTime);
        assertThat(converter.toParamValue(localTime)).isEqualTo(paramValue);
    }

    @Test
    public void toLocalTime_throwsException() {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setStringValue("15:1:5").build());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.LOCAL_TIME_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Failed to parse ISO 8601 string to LocalTime");
    }

    @Test
    public void toLocalTimeMissingValue_throwsException() {
        List<ParamValue> input = Collections.singletonList(ParamValue.getDefaultInstance());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.LOCAL_TIME_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Cannot parse time because string_value is missing from ParamValue.");
    }

    @Test
    public void zoneId_success() throws Exception {
        ParamValueConverter<ZoneId> converter = TypeConverters.ZONE_ID_PARAM_VALUE_CONVERTER;
        ParamValue paramValue = ParamValue.newBuilder().setStringValue("America/New_York").build();
        ZoneId zoneId = ZoneId.of("America/New_York");

        assertThat(converter.fromParamValue(paramValue)).isEqualTo(zoneId);
        assertThat(converter.toParamValue(zoneId)).isEqualTo(paramValue);
    }

    @Test
    public void toZoneId_throwsException() {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("America/New_Yo").build());

        ZoneRulesException thrown =
                assertThrows(
                        ZoneRulesException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.ZONE_ID_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown).hasMessageThat().isEqualTo("Unknown time-zone ID: America/New_Yo");
    }

    @Test
    public void toZoneIdMissingValue_throwsException() {
        List<ParamValue> input = Collections.singletonList(ParamValue.getDefaultInstance());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.ZONE_ID_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Cannot parse ZoneId because string_value is missing from ParamValue.");
    }

    @Test
    public void zonedDateTime_success() throws Exception {
        ParamValueConverter<ZonedDateTime> converter =
                TypeConverters.ZONED_DATETIME_PARAM_VALUE_CONVERTER;
        ParamValue paramValue =
                ParamValue.newBuilder().setStringValue("2018-06-17T15:10:05Z").build();
        ZonedDateTime zonedDateTime = ZonedDateTime.of(2018, 6, 17, 15, 10, 5, 0, ZoneOffset.UTC);

        assertThat(converter.fromParamValue(paramValue)).isEqualTo(zonedDateTime);
        assertThat(converter.toParamValue(zonedDateTime)).isEqualTo(paramValue);
    }

    @Test
    public void toZonedDateTime_invalidStringFormat_throwsException() {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder()
                                .setStringValue("Failed to parse ISO 8601 string to ZonedDateTime")
                                .build());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.ZONED_DATETIME_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Failed to parse ISO 8601 string to ZonedDateTime");
    }

    @Test
    public void toZonedDateTime_stringTypeMissing_throwsException() {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setNumberValue(100).build());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.ZONED_DATETIME_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo(
                        "Cannot parse datetime because string_value is missing from ParamValue.");
    }

    @Test
    public void duration_success() throws Exception {
        ParamValueConverter<Duration> converter = TypeConverters.DURATION_PARAM_VALUE_CONVERTER;
        ParamValue paramValue =
                ParamValue.newBuilder().setStringValue("PT5M").build();
        Duration duration = Duration.ofMinutes(5);

        assertThat(converter.fromParamValue(paramValue)).isEqualTo(duration);
        assertThat(converter.toParamValue(duration)).isEqualTo(paramValue);
    }

    @Test
    public void toDuration_stringTypeMissing_throwsException() {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setNumberValue(100).build());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(
                                                TypeConverters.DURATION_PARAM_VALUE_CONVERTER)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo(
                        "Cannot parse duration because string_value is missing from ParamValue.");
    }

    @Test
    public void searchActionConverter_withoutNestedObject() throws Exception {
        ParamValue input =
                ParamValue.newBuilder()
                        .setStructValue(
                                Struct.newBuilder()
                                        .putFields(
                                                "@type",
                                                Value.newBuilder()
                                                        .setStringValue("SearchAction")
                                                        .build())
                                        .putFields(
                                                "query",
                                                Value.newBuilder()
                                                        .setStringValue("grocery")
                                                        .build())
                                        .build())
                        .build();

        SearchAction<ItemList> output =
                TypeConverters.createSearchActionConverter(TypeConverters.ITEM_LIST_TYPE_SPEC)
                        .toSearchAction(input);

        assertThat(output)
                .isEqualTo(new SearchAction.Builder<String>().setQuery("grocery").build());
    }

    @Test
    public void searchActionConverter_withNestedObject() throws Exception {
        ItemList itemList =
                ItemList.Builder()
                        .addItemListElement(ListItem.Builder().setName("sugar").build())
                        .build();
        Struct nestedObject = TypeConverters.ITEM_LIST_TYPE_SPEC.toValue(itemList).getStructValue();
        ParamValue input =
                ParamValue.newBuilder()
                        .setStructValue(
                                Struct.newBuilder()
                                        .putFields(
                                                "@type",
                                                Value.newBuilder()
                                                        .setStringValue("SearchAction")
                                                        .build())
                                        .putFields(
                                                "filter",
                                                Value.newBuilder()
                                                        .setStructValue(nestedObject)
                                                        .build())
                                        .build())
                        .build();

        SearchAction<ItemList> output =
                TypeConverters.createSearchActionConverter(TypeConverters.ITEM_LIST_TYPE_SPEC)
                        .toSearchAction(input);

        assertThat(output)
                .isEqualTo(new SearchAction.Builder<ItemList>().setFilter(itemList).build());
    }

    @Test
    public void toParamValues_string_success() {
        ParamValue output = TypeConverters.STRING_PARAM_VALUE_CONVERTER.toParamValue("grocery");

        assertThat(output).isEqualTo(ParamValue.newBuilder().setStringValue("grocery").build());
    }

    @Test
    public void toTimer_success() throws Exception {
        ParamValueConverter<Timer> paramValueConverter =
                ParamValueConverter.Companion.of(TIMER_TYPE_SPEC);
        Timer timer = Timer.Builder().setIdentifier("abc").build();

        assertThat(
                paramValueConverter.fromParamValue(
                        ParamValue.newBuilder()
                                .setStructValue(
                                        Struct.newBuilder()
                                                .putFields(
                                                        "@type",
                                                        Value.newBuilder()
                                                                .setStringValue("Timer")
                                                                .build())
                                                .putFields(
                                                        "identifier",
                                                        Value.newBuilder()
                                                                .setStringValue("abc")
                                                                .build()))
                                .build()))
                .isEqualTo(timer);
    }

    @Test
    public void toParamValues_call_success() {
        assertThat(ParamValueConverter.Companion.of(CALL_TYPE_SPEC).toParamValue(CALL_JAVA_THING))
                .isEqualTo(
                        ParamValue.newBuilder()
                                .setStructValue(CALL_STRUCT)
                                .setIdentifier("id")
                                .build());
    }

    @Test
    public void toParamValues_message_success() {
        assertThat(
                ParamValueConverter.Companion.of(MESSAGE_TYPE_SPEC)
                        .toParamValue(MESSAGE_JAVA_THING))
                .isEqualTo(
                        ParamValue.newBuilder()
                                .setStructValue(MESSAGE_STRUCT)
                                .setIdentifier("id")
                                .build());
    }

    @Test
    public void toParamValues_safetyCheck_success() {
        assertThat(
                ParamValueConverter.Companion.of(SAFETY_CHECK_TYPE_SPEC)
                        .toParamValue(SAFETY_CHECK_JAVA_THING))
                .isEqualTo(
                        ParamValue.newBuilder()
                                .setStructValue(SAFETY_CHECK_STRUCT)
                                .setIdentifier("id")
                                .build());
    }
}
