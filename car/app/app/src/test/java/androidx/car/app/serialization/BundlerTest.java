/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.car.app.serialization;

import static com.google.common.truth.Truth.assertThat;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;
import androidx.car.app.OnDoneCallback;
import androidx.car.app.TestUtils;
import androidx.car.app.annotations.CarProtocol;
import androidx.car.app.model.Action;
import androidx.car.app.model.ActionStrip;
import androidx.car.app.model.CarIcon;
import androidx.car.app.model.CarLocation;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Metadata;
import androidx.car.app.model.OnClickListener;
import androidx.car.app.model.Place;
import androidx.car.app.model.PlaceListMapTemplate;
import androidx.car.app.model.PlaceMarker;
import androidx.car.app.model.Row;
import androidx.car.app.serialization.Bundler.CycleDetectedBundlerException;
import androidx.car.app.serialization.Bundler.TracedBundlerException;
import androidx.core.app.Person;
import androidx.core.graphics.drawable.IconCompat;
import androidx.test.core.app.ApplicationProvider;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Tests for {@link Bundler}. */
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class BundlerTest {
    private static final String TAG_CLASS_NAME = "tag_class_name";
    private static final String TAG_CLASS_TYPE = "tag_class_type";
    private static final String TAG_VALUE = "tag_value";
    private static final int CLASS_TYPE_OBJECT = 5;

    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Test
    public void booleanSerialization() throws BundlerException {
        boolean value = true;
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void byteSerialization() throws BundlerException {
        byte value = Byte.MAX_VALUE;
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void charSerialization() throws BundlerException {
        char value = 'c';
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void shortSerialization() throws BundlerException {
        short value = 5;
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void integerSerialization() throws BundlerException {
        int value = 500;
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void longSerialization() throws BundlerException {
        long value = 500000000L;
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void doubleSerialization() throws BundlerException {
        double value = 4.23423;
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void floatSerialization() throws BundlerException {
        double value = 5f;
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void stringSerialization() throws BundlerException {
        String value = "fooTest";
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void parcelableSerialization() throws BundlerException {
        TestParcelable value = new TestParcelable("howdy");
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void iBinderSerialization() throws BundlerException {
        IBinder binder = new Binder();
        Bundle bundle = Bundler.toBundle(binder);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(binder);
    }

    @Test
    public void iInterfaceSerialization() throws BundlerException {
        OnClickListener clickListener = mock(OnClickListener.class);

        ItemList itemList =
                new ItemList.Builder()
                        .addItem(
                                new Row.Builder()
                                        .setOnClickListener(clickListener)
                                        .setTitle("foo")
                                        .setBrowsable(true)
                                        .build())
                        .build();
        PlaceListMapTemplate mapTemplate =
                new PlaceListMapTemplate.Builder().setTitle("Title").setItemList(itemList).build();

        Bundle bundle = Bundler.toBundle(mapTemplate);
        PlaceListMapTemplate readIn = (PlaceListMapTemplate) Bundler.fromBundle(bundle);

        Row row = (Row) readIn.getItemList().getItems().get(0);
        assertThat(row.getTitle().toString()).isEqualTo("foo");
        OnDoneCallback onDoneCallback = mock(OnDoneCallback.class);
        row.getOnClickDelegate().sendClick(onDoneCallback);
        verify(clickListener).onClick();
        verify(onDoneCallback).onSuccess(null);
    }

    @Test
    public void mapSerialization() throws BundlerException {
        Map<String, CarLocation> value = new HashMap<>();
        value.put("bank", CarLocation.create(23.32, 134.34));
        value.put("banana", CarLocation.create(2.4532, 3982.23));

        Bundle bundle = Bundler.toBundle(value);
        @SuppressWarnings("unchecked") // Casting of deserialized Object from Bundler.
        Map<String, CarLocation> readIn = (Map<String, CarLocation>) Bundler.fromBundle(bundle);

        assertThat(readIn.keySet()).isEqualTo(value.keySet());

        assertThat(readIn.values()).hasSize(value.size());
        for (CarLocation carLocation : value.values()) {
            assertThat(readIn.containsValue(carLocation)).isTrue();
        }
    }

    @Test
    public void listOfStringSerialization() throws BundlerException {
        List<String> value = new ArrayList<>();
        value.add("string");
        value.add("another");

        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void listOfCarLocationSerialization() throws BundlerException {
        List<CarLocation> value = new ArrayList<>();
        value.add(CarLocation.create(1.0, 2.0));
        value.add(CarLocation.create(2.0, 1.0));

        Bundle bundle = Bundler.toBundle(value);
        @SuppressWarnings("unchecked") // Casting of deserialized Object from Bundler.
        List<CarLocation> readIn = (List<CarLocation>) Bundler.fromBundle(bundle);
        assertThat(readIn).isEqualTo(value);
    }

    @Test
    public void setSerialization() throws BundlerException {
        Set<Integer> value = new HashSet<>();
        value.add(4);
        value.add(992);

        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void enumSerialization() throws BundlerException {
        TestEnum value = TestEnum.BAR;
        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void enumSerialization_wrongEnumName_throwsBundlerException() throws BundlerException {
        TestEnum value = TestEnum.BAR;
        Bundle bundle = Bundler.toBundle(value);
        bundle.putString(TAG_VALUE, "invalid");

        assertThrows(TracedBundlerException.class, () -> Bundler.fromBundle(bundle));
    }

    @Test
    public void classSerialization() throws BundlerException {
        Class<?> clazz = ListTemplate.class;

        Bundle bundle = Bundler.toBundle(clazz);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(clazz);
    }

    @Test
    public void classSerialization_missingClassName_throwsBundlerException()
            throws BundlerException {
        Class<?> clazz = ListTemplate.class;

        Bundle bundle = Bundler.toBundle(clazz);
        bundle.putString(TAG_VALUE, "unknownClass");

        assertThrows(TracedBundlerException.class, () -> Bundler.fromBundle(bundle));
    }

    @Test
    @Ignore("TODO(b/151105067): reenable once we support content URI.")
    public void imageSerialization_contentUri() throws BundlerException {
        IconCompat image = IconCompat.createWithContentUri("content://foo/bar");
        Bundle bundle = Bundler.toBundle(image);

        assertThat(new CarIcon.Builder((IconCompat) Bundler.fromBundle(bundle)).build()).isEqualTo(
                new CarIcon.Builder(image).build());
    }

    @Test
    public void imageSerialization_resource() throws BundlerException {
        Context context = ApplicationProvider.getApplicationContext();
        IconCompat image = IconCompat.createWithResource(context,
                TestUtils.getTestDrawableResId(mContext, "ic_test_1"));
        Bundle bundle = Bundler.toBundle(image);

        assertThat(new CarIcon.Builder((IconCompat) Bundler.fromBundle(bundle)).build()).isEqualTo(
                new CarIcon.Builder(image).build());
    }

    @Test
    @Ignore("TODO(b/151105067): reenable once we support content URI.")
    public void imageSerialization_iconId() throws BundlerException {
        IconCompat image = IconCompat.createWithContentUri("car://icons/1");
        Bundle bundle = Bundler.toBundle(image);

        assertThat(new CarIcon.Builder((IconCompat) Bundler.fromBundle(bundle)).build()).isEqualTo(
                new CarIcon.Builder(image).build());
    }

    @Test
    public void imageSerialization_Icon() throws BundlerException {
        Bitmap bitmap = Bitmap.createBitmap(48, 48, Bitmap.Config.ARGB_8888);
        try {
            IconCompat image = IconCompat.createWithBitmap(bitmap);
            Bundle bundle = Bundler.toBundle(image);
            IconCompat readImage = (IconCompat) Bundler.fromBundle(bundle);

            assertThat(
                    new CarIcon.Builder((IconCompat) Bundler.fromBundle(bundle)).build()).isEqualTo(
                    new CarIcon.Builder(image).build());
            Drawable drawable = readImage.loadDrawable(mContext);
            assertThat(drawable).isNotNull();
        } finally {
            bitmap.recycle();
        }
    }

    @Test
    public void personSerialization() throws BundlerException {
        IconCompat icon =
                IconCompat.createWithBitmap(
                        Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
                );
        Uri uri = Uri.parse("http://foo.com/test/sender/uri");
        Person person = new Person.Builder()
                .setName("Person Name")
                .setKey("sender_key")
                .setKey("Foo Person")
                .setIcon(icon)
                .setUri(uri.toString())
                .setBot(true)
                .setImportant(true)
                .build();

        Bundle bundle = Bundler.toBundle(person);
        Person reconstitutedPerson = (Person) Bundler.fromBundle(bundle);

        assertThat(reconstitutedPerson.getName()).isEqualTo(person.getName());
        assertThat(reconstitutedPerson.getKey()).isEqualTo(person.getKey());
        assertThat(reconstitutedPerson.getKey()).isEqualTo(person.getKey());
        assertThat(reconstitutedPerson.getIcon()).isNotNull();
        assertThat(reconstitutedPerson.getUri()).isEqualTo(person.getUri());
        assertThat(reconstitutedPerson.isBot()).isEqualTo(person.isBot());
        assertThat(reconstitutedPerson.isImportant()).isEqualTo(person.isImportant());
    }

    @Test
    public void templateSerialization() throws BundlerException {
        String title = "titleOfThePane";

        String row1Title = "row1";
        String row1Subtitle = "row1subtitle";

        CarLocation carLocation2 = CarLocation.create(4522.234, 34.234);
        CarIcon carIcon = TestUtils.getTestCarIcon(mContext, "ic_test_1");
        PlaceMarker marker2 = new PlaceMarker.Builder().setIcon(carIcon,
                PlaceMarker.TYPE_ICON).build();
        String row2Title = "row2";
        String row2Subtitle = "row2subtitle";

        Row row1 =
                new Row.Builder()
                        .setImage(carIcon)
                        .setTitle(row1Title)
                        .addText(row1Subtitle)
                        .setBrowsable(true)
                        .setOnClickListener(() -> {
                        })
                        .build();
        Row row2 =
                new Row.Builder()
                        .setTitle(row2Title)
                        .addText(row2Subtitle)
                        .setBrowsable(true)
                        .setOnClickListener(() -> {
                        })
                        .setMetadata(new Metadata.Builder().setPlace(new Place.Builder(
                                carLocation2).setMarker(
                                marker2).build()).build())
                        .build();
        ItemList itemList = new ItemList.Builder().addItem(row1).addItem(row2).build();
        ActionStrip actionStrip = new ActionStrip.Builder().addAction(Action.APP_ICON).build();
        PlaceListMapTemplate mapTemplate =
                new PlaceListMapTemplate.Builder()
                        .setItemList(itemList)
                        .setTitle(title)
                        .setActionStrip(actionStrip)
                        .build();

        Bundle bundle = Bundler.toBundle(mapTemplate);
        PlaceListMapTemplate readIn = (PlaceListMapTemplate) Bundler.fromBundle(bundle);

        assertThat(readIn.getTitle().toString()).isEqualTo(mapTemplate.getTitle().toString());

        ItemList readInItemList = readIn.getItemList();
        assertThat(readInItemList.getItems()).hasSize(2);
        assertThat(row1).isEqualTo(readInItemList.getItems().get(0));
        assertThat(row2).isEqualTo(readInItemList.getItems().get(1));
        assertThat(actionStrip).isEqualTo(readIn.getActionStrip());
    }

    @Test
    public void objectContainingMultipleVariablesSerialization() throws BundlerException {
        TestClass value =
                new TestClass(
                        1,
                        "foobar",
                        true,
                        94f,
                        123.98,
                        CarLocation.create(4.3, 9.6),
                        Arrays.asList("a", "z", "foo", "hey yo, what's up?", "a"));

        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void objectContainingMultipleValuesSerialization_overloadsParentVariableName()
            throws BundlerException {
        HashMap<Integer, Double> map = new HashMap<>();
        map.put(1, 5.6);
        map.put(9, 4.7);
        Set<String> set = new HashSet<>();
        set.add("a");
        set.add("z");
        TestClassExtended value =
                new TestClassExtended(
                        false,
                        map,
                        set,
                        8,
                        "howdy",
                        true,
                        8f,
                        3432.932,
                        CarLocation.create(23.32, 234.234),
                        Arrays.asList("rafael", "lima"));

        Bundle bundle = Bundler.toBundle(value);

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void bundleHasExtraFieldsForObjectWhenDeserializing_worksFine() throws BundlerException {
        TestClass value =
                new TestClass(
                        1,
                        "foobar",
                        true,
                        94f,
                        123.98,
                        CarLocation.create(4.3, 9.6),
                        Arrays.asList("a", "z", "foo", "hey yo, what's up?", "a"));

        Bundle bundle = Bundler.toBundle(value);

        bundle.putString("fooField", "I'm extra special!");

        assertThat(Bundler.fromBundle(bundle)).isEqualTo(value);
    }

    @Test
    public void bundleMissingFieldsForObjectWhenDeserializing_worksFine()
            throws BundlerException, NoSuchFieldException {
        TestClass value =
                new TestClass(
                        1,
                        "foobar",
                        true,
                        94f,
                        123.98,
                        CarLocation.create(4.3, 9.6),
                        Arrays.asList("a", "z", "foo", "hey yo, what's up?", "a"));

        Bundle bundle = Bundler.toBundle(value);

        Field field = TestClass.class.getDeclaredField("mInt");
        field.setAccessible(true);

        bundle.remove(Bundler.getFieldName(field));

        TestClass readIn = (TestClass) Bundler.fromBundle(bundle);
        assertThat(readIn).isNotEqualTo(value);

        // Set the missing value to what we expect to ensure all other fields are correctly set.
        readIn.mInt = 1;
        assertThat(readIn).isEqualTo(value);
    }

    @Test
    public void classNotKnownWhenDeserializing_throwsBundlerException() throws BundlerException {
        Bundle bundle =
                Bundler.toBundle(
                        new TestClass(
                                1,
                                "foobar",
                                true,
                                94f,
                                123.98,
                                CarLocation.create(4.3, 9.6),
                                Arrays.asList("a", "z", "foo", "hey yo, what's up?", "a")));

        bundle.putString(TAG_CLASS_NAME, "com.foo.class");
        assertThrows(BundlerException.class, () -> Bundler.fromBundle(bundle));
    }

    @Test
    public void missingClassName_throwsBundlerException() throws BundlerException {
        Bundle bundle =
                Bundler.toBundle(
                        new TestClass(
                                1,
                                "foobar",
                                true,
                                94f,
                                123.98,
                                CarLocation.create(4.3, 9.6),
                                Arrays.asList("a", "z", "foo", "hey yo, what's up?", "a")));

        bundle.remove(TAG_CLASS_NAME);
        assertThrows(BundlerException.class, () -> Bundler.fromBundle(bundle));
    }

    @Test
    public void missingValueInBundle_throwsBundlerException() throws BundlerException {
        Bundle bundle = Bundler.toBundle(5);

        bundle.remove(TAG_VALUE);
        assertThrows(BundlerException.class, () -> Bundler.fromBundle(bundle));
    }

    @Test
    public void missingClassTypeBundle_throwsBundlerException() throws BundlerException {
        Bundle bundle =
                Bundler.toBundle(
                        new TestClass(
                                1,
                                "foobar",
                                true,
                                94f,
                                123.98,
                                CarLocation.create(4.3, 9.6),
                                Arrays.asList("a", "z", "foo", "hey yo, what's up?", "a")));

        bundle.remove(TAG_CLASS_TYPE);
        assertThrows(BundlerException.class, () -> Bundler.fromBundle(bundle));
    }

    @Test
    public void missingCarProtocolAnnotationBundle_throwsBundlerException() {
        TestClassMissingCarProtocolAnnotation invalidObj =
                new TestClassMissingCarProtocolAnnotation(3);
        Bundle bundle = new Bundle(3);
        bundle.putInt(TAG_CLASS_TYPE, CLASS_TYPE_OBJECT);
        bundle.putString(TAG_CLASS_NAME, invalidObj.getClass().getName());

        assertThrows(BundlerException.class, () -> Bundler.fromBundle(bundle));
    }

    @Test
    public void classMissingCarProtocolAnnotation_throwsBundlerException() {
        assertThrows(
                BundlerException.class,
                () -> Bundler.toBundle(new TestClassMissingCarProtocolAnnotation(1)));
    }

    //TODO(rampara): Investigate why default constructor is still found when running with
    // robolectric.
//    @Test
//    public void classMissingDefaultConstructorSerialization_throwsBundlerException() {
//        assertThrows(
//                BundlerException.class,
//                () -> Bundler.toBundle(new TestClassMissingDefaultConstructor(1)));
//    }

    @Test
    public void arraySerialization_throwsBundlerException() {
        String[] array = new String[5];
        assertThrows(BundlerException.class, () -> Bundler.toBundle(array));
    }

    @Test
    public void detectCycle() {
        Click click = new Click();
        assertThrows(CycleDetectedBundlerException.class, () -> Bundler.toBundle(click));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void imageCompat_dejetify() throws BundlerException {
        CarIcon image = TestUtils.getTestCarIcon(mContext, "ic_test_1");
        Bundle bundle = Bundler.toBundle(image);

        // Get the field for the image, and re-write it with a "jetified" key.
        String fieldName = Bundler.getFieldName(BundlerTest.class.getName(), "image");
        Bundle imageBundle = (Bundle) bundle.get(fieldName);
        bundle.putBundle(
                fieldName.replaceAll(Bundler.ICON_COMPAT_SUPPORT, Bundler.ICON_COMPAT_ANDROIDX),
                imageBundle);
        bundle.remove(fieldName);

        CarIcon iconOut = (CarIcon) Bundler.fromBundle(bundle);
        assertThat(iconOut).isEqualTo(image);
    }

    @SuppressWarnings("unused")
    @CarProtocol
    private static class Click {
        private final Clack mClack;

        Click() {
            mClack = new Clack(this);
        }
    }

    @SuppressWarnings("unused")
    @CarProtocol
    private static class Clack {
        @Nullable
        private final Click mClick;

        private Clack(@Nullable Click click) {
            mClick = click;
        }

        private Clack() {
            this(null);
        }
    }

    @CarProtocol
    private static class TestClass {
        private int mInt;
        private final String mString;
        private final boolean mBoolean;
        private final float mFloat;
        private final double mDouble;
        private final CarLocation mLocation;
        private final List<String> mStrings;

        private TestClass(
                int i,
                String s,
                boolean b,
                float f,
                double d,
                CarLocation ll,
                List<String> strings) {
            this.mInt = i;
            this.mString = s;
            this.mBoolean = b;
            this.mFloat = f;
            this.mDouble = d;
            this.mLocation = ll;
            this.mStrings = strings;
        }

        private TestClass() {
            this(0,
                    "",
                    false,
                    0f,
                    0.0,
                    CarLocation.create(0.0, 0.0),
                    Collections.emptyList());
        }

        @Override
        public int hashCode() {
            return Objects.hash(mInt, mString, mBoolean, mFloat, mDouble, mLocation, mStrings);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof TestClass)) {
                return false;
            }

            TestClass o = (TestClass) other;
            return mInt == o.mInt
                    && Objects.equals(mString, o.mString)
                    && Objects.equals(mBoolean, o.mBoolean)
                    && Float.compare(mFloat, o.mFloat) == 0
                    && Double.compare(mDouble, o.mDouble) == 0
                    && Double.compare(mLocation.getLatitude(), o.mLocation.getLatitude()) == 0
                    && Double.compare(mLocation.getLongitude(), o.mLocation.getLongitude()) == 0
                    && Objects.equals(mStrings, o.mStrings);
        }
    }

    @CarProtocol
    private static class TestClassExtended extends TestClass {
        private final boolean mBoolean;
        private final Map<Integer, Double> mIntegerDoubleMap;
        private final Set<String> mStringSet;

        private TestClassExtended(
                boolean childI,
                Map<Integer, Double> childS,
                Set<String> childE,
                int i,
                String s,
                boolean b,
                float f,
                double d,
                CarLocation ll,
                List<String> strings) {
            super(i, s, b, f, d, ll, strings);
            this.mBoolean = childI;
            this.mIntegerDoubleMap = childS;
            this.mStringSet = childE;
        }

        private TestClassExtended() {
            this(
                    false,
                    Collections.emptyMap(),
                    Collections.emptySet(),
                    0,
                    "",
                    false,
                    0f,
                    0.0,
                    CarLocation.create(0.0, 0.0),
                    Collections.emptyList());
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), mBoolean, mIntegerDoubleMap, mStringSet);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof TestClassExtended)) {
                return false;
            }

            TestClassExtended o = (TestClassExtended) other;
            return super.equals(other)
                    && Objects.equals(mBoolean, o.mBoolean)
                    && Objects.equals(mIntegerDoubleMap, o.mIntegerDoubleMap)
                    && Objects.equals(mStringSet, o.mStringSet);
        }
    }

    @CarProtocol
    private static class TestClassMissingDefaultConstructor {
        @SuppressWarnings("unused")
        private TestClassMissingDefaultConstructor(int i) {
        }
    }

    private static class TestClassMissingCarProtocolAnnotation {
        private final int mInt;
        @SuppressWarnings("unused")
        private TestClassMissingCarProtocolAnnotation(int i) {
            this.mInt = i;
        }

        private TestClassMissingCarProtocolAnnotation() {
            this(33);
        }
    }

    public static class TestParcelable implements Parcelable {
        private final String mFoo;

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(mFoo);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(mFoo);
        }

        @Override
        public boolean equals(@Nullable Object other) {
            if (this == other) {
                return true;
            }

            if (!(other instanceof TestParcelable)) {
                return false;
            }

            return Objects.equals(mFoo, ((TestParcelable) other).mFoo);
        }

        private TestParcelable(String foo) {
            this.mFoo = foo;
        }

        private TestParcelable() {
            this("");
        }

        public static final Creator<TestParcelable> CREATOR =
                new Creator<TestParcelable>() {
                    @Override
                    public TestParcelable createFromParcel(final Parcel source) {
                        return new TestParcelable(source.readString());
                    }

                    @Override
                    public TestParcelable[] newArray(final int size) {
                        return new TestParcelable[size];
                    }
                };
    }

    private enum TestEnum {
        FOO,
        BAR;
    }
}
