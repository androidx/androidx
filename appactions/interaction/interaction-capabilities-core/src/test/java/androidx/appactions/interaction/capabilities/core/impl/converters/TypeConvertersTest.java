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

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;

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
import androidx.appactions.interaction.capabilities.core.values.properties.Participant;
import androidx.appactions.interaction.capabilities.core.values.properties.Recipient;
import androidx.appactions.interaction.proto.Entity;
import androidx.appactions.interaction.proto.ParamValue;

import com.google.protobuf.ListValue;
import com.google.protobuf.Struct;
import com.google.protobuf.Value;

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

    private static final Order ORDER_JAVA_THING =
            Order.newBuilder()
                    .setId("id")
                    .setName("name")
                    .addOrderedItem(OrderItem.newBuilder().setName("apples").build())
                    .addOrderedItem(OrderItem.newBuilder().setName("oranges").build())
                    .setSeller(Organization.newBuilder().setName("Google").build())
                    .setOrderDate(ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC))
                    .setOrderStatus(Order.OrderStatus.ORDER_DELIVERED)
                    .setOrderDelivery(
                            ParcelDelivery.newBuilder()
                                    .setDeliveryAddress("test address")
                                    .setDeliveryMethod("UPS")
                                    .setTrackingNumber("A12345")
                                    .setTrackingUrl("https://")
                                    .build())
                    .build();
    private static final Person PERSON_JAVA_THING =
            Person.newBuilder()
                    .setName("name")
                    .setEmail("email")
                    .setTelephone("telephone")
                    .setId("id")
                    .build();
    private static final Person PERSON_JAVA_THING_2 = Person.newBuilder().setId("id2").build();
    private static final CalendarEvent CALENDAR_EVENT_JAVA_THING =
            CalendarEvent.newBuilder()
                    .setStartDate(ZonedDateTime.of(2022, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC))
                    .setEndDate(ZonedDateTime.of(2023, 1, 1, 8, 0, 0, 0, ZoneOffset.UTC))
                    .addAttendee(PERSON_JAVA_THING)
                    .addAttendee(PERSON_JAVA_THING_2)
                    .build();
    private static final Call CALL_JAVA_THING =
            Call.newBuilder()
                    .setId("id")
                    .setCallFormat(Call.CallFormat.AUDIO)
                    .addParticipant(PERSON_JAVA_THING)
                    .build();
    private static final Message MESSAGE_JAVA_THING =
            Message.newBuilder()
                    .setId("id")
                    .addRecipient(PERSON_JAVA_THING)
                    .setMessageText("hello")
                    .build();
    private static final SafetyCheck SAFETY_CHECK_JAVA_THING =
            SafetyCheck.newBuilder()
                    .setId("id")
                    .setDuration(Duration.ofMinutes(5))
                    .setCheckinTime(ZonedDateTime.of(2023, 01, 10, 10, 0, 0, 0, ZoneOffset.UTC))
                    .build();
    private static final ListValue ORDER_ITEMS_STRUCT =
            ListValue.newBuilder()
                    .addValues(
                            Value.newBuilder()
                                    .setStructValue(
                                            Struct.newBuilder()
                                                    .putFields(
                                                            "@type",
                                                            Value.newBuilder()
                                                                    .setStringValue("OrderItem")
                                                                    .build())
                                                    .putFields(
                                                            "name",
                                                            Value.newBuilder()
                                                                    .setStringValue("apples")
                                                                    .build()))
                                    .build())
                    .addValues(
                            Value.newBuilder()
                                    .setStructValue(
                                            Struct.newBuilder()
                                                    .putFields(
                                                            "@type",
                                                            Value.newBuilder()
                                                                    .setStringValue("OrderItem")
                                                                    .build())
                                                    .putFields(
                                                            "name",
                                                            Value.newBuilder()
                                                                    .setStringValue("oranges")
                                                                    .build()))
                                    .build())
                    .build();
    private static final Struct PARCEL_DELIVERY_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("ParcelDelivery").build())
                    .putFields(
                            "deliveryAddress",
                            Value.newBuilder().setStringValue("test address").build())
                    .putFields(
                            "hasDeliveryMethod", Value.newBuilder().setStringValue("UPS").build())
                    .putFields(
                            "trackingNumber", Value.newBuilder().setStringValue("A12345").build())
                    .putFields("trackingUrl", Value.newBuilder().setStringValue("https://").build())
                    .build();
    private static final Struct ORGANIZATION_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Organization").build())
                    .putFields("name", Value.newBuilder().setStringValue("Google").build())
                    .build();
    private static final Struct ORDER_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Order").build())
                    .putFields("identifier", Value.newBuilder().setStringValue("id").build())
                    .putFields("name", Value.newBuilder().setStringValue("name").build())
                    .putFields(
                            "orderDate",
                            Value.newBuilder().setStringValue("2022-01-01T08:00Z").build())
                    .putFields(
                            "orderDelivery",
                            Value.newBuilder().setStructValue(PARCEL_DELIVERY_STRUCT).build())
                    .putFields(
                            "orderedItem",
                            Value.newBuilder().setListValue(ORDER_ITEMS_STRUCT).build())
                    .putFields(
                            "orderStatus",
                            Value.newBuilder().setStringValue("OrderDelivered").build())
                    .putFields(
                            "seller",
                            Value.newBuilder().setStructValue(ORGANIZATION_STRUCT).build())
                    .build();
    private static final Struct PERSON_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Person").build())
                    .putFields("identifier", Value.newBuilder().setStringValue("id").build())
                    .putFields("name", Value.newBuilder().setStringValue("name").build())
                    .putFields("email", Value.newBuilder().setStringValue("email").build())
                    .putFields("telephone", Value.newBuilder().setStringValue("telephone").build())
                    .build();
    private static final Struct PERSON_STRUCT_2 =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Person").build())
                    .putFields("identifier", Value.newBuilder().setStringValue("id2").build())
                    .build();
    private static final Struct CALENDAR_EVENT_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("CalendarEvent").build())
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
                                                                    .setStructValue(PERSON_STRUCT)
                                                                    .build())
                                                    .addValues(
                                                            Value.newBuilder()
                                                                    .setStructValue(PERSON_STRUCT_2)
                                                                    .build()))
                                    .build())
                    .build();
    private static final Struct CALL_STRUCT =
            Struct.newBuilder()
                    .putFields("@type", Value.newBuilder().setStringValue("Call").build())
                    .putFields("callFormat", Value.newBuilder().setStringValue("Audio").build())
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
                            "checkinTime",
                            Value.newBuilder().setStringValue("2023-01-10T10:00Z").build())
                    .build();

    private static ParamValue toParamValue(Struct struct, String identifier) {
        return ParamValue.newBuilder().setIdentifier(identifier).setStructValue(struct).build();
    }

    private static Entity toEntity(Struct struct) {
        return Entity.newBuilder().setIdentifier("id").setValue(struct).build();
    }

    @Test
    public void toEntityValue() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder()
                                .setIdentifier("entity-id")
                                .setStringValue("string-val")
                                .build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toEntityValue).convert(input))
                .isEqualTo(
                        EntityValue.newBuilder().setId("entity-id").setValue("string-val").build());
    }

    @Test
    public void toIntegerValue() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setNumberValue(5).build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toIntegerValue).convert(input))
                .isEqualTo(5);
    }

    @Test
    public void toStringValue_fromList() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("hello world").build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toStringValue).convert(input))
                .isEqualTo("hello world");
    }

    @Test
    public void toStringValue_withIdentifier() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder()
                                .setIdentifier("id1")
                                .setStringValue("hello world")
                                .build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toStringValue).convert(input))
                .isEqualTo("id1");
    }

    @Test
    public void toStringValue_fromSingleParam() {
        ParamValue input = ParamValue.newBuilder().setStringValue("hello world").build();

        assertThat(TypeConverters.toStringValue(input)).isEqualTo("hello world");
    }

    @Test
    public void alarm_conversions_matchesExpected() throws Exception {
        Alarm alarm = Alarm.newBuilder().setId("id").build();

        assertThat(
                        TypeConverters.toAssistantAlarm(
                                ParamValue.newBuilder().setIdentifier("id").build()))
                .isEqualTo(alarm);
    }

    @Test
    public void listItem_conversions_matchesExpected() throws Exception {
        ListItem listItem = ListItem.create("itemId", "Test Item");
        Struct listItemStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("ListItem").build())
                        .putFields(
                                "identifier", Value.newBuilder().setStringValue("itemId").build())
                        .putFields("name", Value.newBuilder().setStringValue("Test Item").build())
                        .build();
        Entity listItemProto =
                Entity.newBuilder().setIdentifier("itemId").setValue(listItemStruct).build();

        assertThat(TypeConverters.toEntity(listItem)).isEqualTo(listItemProto);
        assertThat(TypeConverters.toListItem(toParamValue(listItemStruct, "itemId")))
                .isEqualTo(listItem);
    }

    @Test
    public void itemList_conversions_matchesExpected() throws Exception {
        ItemList itemList =
                ItemList.newBuilder()
                        .setId("testList")
                        .setName("Test List")
                        .addListItem(
                                ListItem.create("item1", "apple"),
                                ListItem.create("item2", "banana"))
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
                Entity.newBuilder().setIdentifier("testList").setValue(itemListStruct).build();

        assertThat(TypeConverters.toEntity(itemList)).isEqualTo(itemListProto);
        assertThat(TypeConverters.toItemList(toParamValue(itemListStruct, "testList")))
                .isEqualTo(itemList);
    }

    @Test
    public void order_conversions_matchesExpected() throws Exception {
        assertThat(TypeConverters.toParamValue(ORDER_JAVA_THING))
                .isEqualTo(toParamValue(ORDER_STRUCT, "id"));
        assertThat(TypeConverters.toOrder(toParamValue(ORDER_STRUCT, "id")))
                .isEqualTo(ORDER_JAVA_THING);
        assertThat(TypeConverters.toEntity(ORDER_JAVA_THING)).isEqualTo(toEntity(ORDER_STRUCT));
    }

    @Test
    public void participant_conversions_matchesExpected() throws Exception {
        ParamValue paramValue =
                ParamValue.newBuilder()
                        .setIdentifier(PERSON_JAVA_THING.getId().orElse("id"))
                        .setStructValue(PERSON_STRUCT)
                        .build();
        Participant participant = new Participant(PERSON_JAVA_THING);

        assertThat(TypeConverters.toParamValue(participant)).isEqualTo(paramValue);
        assertThat(TypeConverters.toParticipant(paramValue)).isEqualTo(participant);
    }

    @Test
    public void calendarEvent_conversions_matchesExpected() throws Exception {
        assertThat(TypeConverters.CALENDAR_EVENT_TYPE_SPEC.toStruct(CALENDAR_EVENT_JAVA_THING))
                .isEqualTo(CALENDAR_EVENT_STRUCT);
        assertThat(TypeConverters.CALENDAR_EVENT_TYPE_SPEC.fromStruct(CALENDAR_EVENT_STRUCT))
                .isEqualTo(CALENDAR_EVENT_JAVA_THING);
    }

    @Test
    public void recipient_conversions_matchesExpected() throws Exception {
        ParamValue paramValue =
                ParamValue.newBuilder()
                        .setIdentifier(PERSON_JAVA_THING.getId().orElse("id"))
                        .setStructValue(PERSON_STRUCT)
                        .build();
        Recipient recipient = new Recipient(PERSON_JAVA_THING);

        assertThat(TypeConverters.toParamValue(recipient)).isEqualTo(paramValue);
        assertThat(TypeConverters.toRecipient(paramValue)).isEqualTo(recipient);
    }

    @Test
    public void toParticipant_unexpectedType_throwsException() {
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Malformed").build())
                        .build();

        assertThrows(
                StructConversionException.class,
                () -> TypeConverters.toParticipant(toParamValue(malformedStruct, "id")));
    }

    @Test
    public void toRecipient_unexpectedType_throwsException() {
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Malformed").build())
                        .build();

        assertThrows(
                StructConversionException.class,
                () -> TypeConverters.toRecipient(toParamValue(malformedStruct, "id")));
    }

    @Test
    public void itemList_malformedStruct_throwsException() {
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Malformed").build())
                        .putFields("name", Value.newBuilder().setStringValue("List Name").build())
                        .putFields("identifier", Value.newBuilder().setStringValue("list1").build())
                        .build();

        assertThrows(
                StructConversionException.class,
                () -> TypeConverters.toItemList(toParamValue(malformedStruct, "list1")));
    }

    @Test
    public void listItem_malformedStruct_throwsException() throws Exception {
        Struct malformedStruct =
                Struct.newBuilder()
                        .putFields("@type", Value.newBuilder().setStringValue("Malformed").build())
                        .putFields("name", Value.newBuilder().setStringValue("Apple").build())
                        .putFields("identifier", Value.newBuilder().setStringValue("item1").build())
                        .build();

        assertThrows(
                StructConversionException.class,
                () -> TypeConverters.toListItem(toParamValue(malformedStruct, "item1")));
    }

    @Test
    public void toBoolean_success() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setBoolValue(false).build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toBooleanValue).convert(input))
                .isFalse();
    }

    @Test
    public void toBoolean_throwsException() {
        List<ParamValue> input = Collections.singletonList(ParamValue.getDefaultInstance());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(TypeConverters::toBooleanValue)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Cannot parse boolean because bool_value is missing from ParamValue.");
    }

    @Test
    public void toLocalDate_success() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("2018-06-17").build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toLocalDate).convert(input))
                .isEqualTo(LocalDate.of(2018, 6, 17));
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
                                SlotTypeConverter.ofSingular(TypeConverters::toLocalDate)
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
                                SlotTypeConverter.ofSingular(TypeConverters::toLocalDate)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Cannot parse date because string_value is missing from ParamValue.");
    }

    @Test
    public void toLocalTime_success() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("15:10:05").build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toLocalTime).convert(input))
                .isEqualTo(LocalTime.of(15, 10, 5));
    }

    @Test
    public void toLocalTime_throwsException() {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setStringValue("15:1:5").build());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(TypeConverters::toLocalTime)
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
                                SlotTypeConverter.ofSingular(TypeConverters::toLocalTime)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Cannot parse time because string_value is missing from ParamValue.");
    }

    @Test
    public void toZoneId_success() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("America/New_York").build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toZoneId).convert(input))
                .isEqualTo(ZoneId.of("America/New_York"));
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
                                SlotTypeConverter.ofSingular(TypeConverters::toZoneId)
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
                                SlotTypeConverter.ofSingular(TypeConverters::toZoneId)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo("Cannot parse ZoneId because string_value is missing from ParamValue.");
    }

    @Test
    public void toZonedDateTime_fromList() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(
                        ParamValue.newBuilder().setStringValue("2018-06-17T15:10:05Z").build());

        assertThat(SlotTypeConverter.ofSingular(TypeConverters::toZonedDateTime).convert(input))
                .isEqualTo(ZonedDateTime.of(2018, 6, 17, 15, 10, 5, 0, ZoneOffset.UTC));
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
                                SlotTypeConverter.ofSingular(TypeConverters::toZonedDateTime)
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
                                SlotTypeConverter.ofSingular(TypeConverters::toZonedDateTime)
                                        .convert(input));
        assertThat(thrown)
                .hasMessageThat()
                .isEqualTo(
                        "Cannot parse datetime because string_value is missing from ParamValue.");
    }

    @Test
    public void toDuration_success() throws Exception {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setStringValue("PT5M").build());

        Duration convertedDuration =
                SlotTypeConverter.ofSingular(TypeConverters::toDuration).convert(input);

        assertThat(convertedDuration).isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void toDuration_stringTypeMissing_throwsException() {
        List<ParamValue> input =
                Collections.singletonList(ParamValue.newBuilder().setNumberValue(100).build());

        StructConversionException thrown =
                assertThrows(
                        StructConversionException.class,
                        () ->
                                SlotTypeConverter.ofSingular(TypeConverters::toDuration)
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

        assertThat(output).isEqualTo(SearchAction.newBuilder().setQuery("grocery").build());
    }

    @Test
    public void searchActionConverter_withNestedObject() throws Exception {
        ItemList itemList =
                ItemList.newBuilder()
                        .addListItem(ListItem.newBuilder().setName("sugar").build())
                        .build();
        Struct nestedObject = TypeConverters.ITEM_LIST_TYPE_SPEC.toStruct(itemList);
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
                                                "object",
                                                Value.newBuilder()
                                                        .setStructValue(nestedObject)
                                                        .build())
                                        .build())
                        .build();

        SearchAction<ItemList> output =
                TypeConverters.createSearchActionConverter(TypeConverters.ITEM_LIST_TYPE_SPEC)
                        .toSearchAction(input);

        assertThat(output).isEqualTo(SearchAction.newBuilder().setObject(itemList).build());
    }

    @Test
    public void toParamValues_string_success() {
        ParamValue output = TypeConverters.toParamValue("grocery");

        assertThat(output).isEqualTo(ParamValue.newBuilder().setStringValue("grocery").build());
    }

    @Test
    public void toTimer_success() throws Exception {
        Timer timer = Timer.newBuilder().setId("abc").build();

        assertThat(
                        TypeConverters.toTimer(
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
        assertThat(TypeConverters.toParamValue(CALL_JAVA_THING))
                .isEqualTo(
                        ParamValue.newBuilder()
                                .setStructValue(CALL_STRUCT)
                                .setIdentifier("id")
                                .build());
    }

    @Test
    public void toParamValues_message_success() {
        assertThat(TypeConverters.toParamValue(MESSAGE_JAVA_THING))
                .isEqualTo(
                        ParamValue.newBuilder()
                                .setStructValue(MESSAGE_STRUCT)
                                .setIdentifier("id")
                                .build());
    }

    @Test
    public void toParamValues_safetyCheck_success() {
        assertThat(TypeConverters.toParamValue(SAFETY_CHECK_JAVA_THING))
                .isEqualTo(
                        ParamValue.newBuilder()
                                .setStructValue(SAFETY_CHECK_STRUCT)
                                .setIdentifier("id")
                                .build());
    }
}
