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

package androidx.resourceinspection.processor

import com.google.common.truth.Truth.assertThat
import com.google.testing.compile.Compilation
import com.google.testing.compile.CompilationSubject.assertThat
import com.google.testing.compile.Compiler.javac
import com.google.testing.compile.JavaFileObjects
import org.intellij.lang.annotations.Language
import org.junit.Test
import javax.lang.model.SourceVersion
import javax.tools.JavaFileObject

/** Integration and unit tests for [ResourceInspectionProcessor]. */
class ResourceInspectionProcessorTest {
    @Test
    fun `test layout inspection of a trivial view`() {
        val compilation = compile(
            fakeR("androidx.pkg", "testAttribute"),
            java(
                "androidx.pkg.TrivialTestView",
                """
                    package androidx.pkg;

                    import android.content.Context;
                    import android.util.AttributeSet;
                    import android.view.View;
                    import androidx.resourceinspection.annotation.Attribute;

                    public class TrivialTestView extends View {
                        public TrivialTestView(Context context, AttributeSet attrs) {
                            super(context, attrs);
                        }

                        @Attribute("androidx.pkg:testAttribute")
                        public int getTestAttribute() {
                            return 4;
                        }
                    }
                """
            ),
        )

        val expected = java(
            "androidx.pkg.TrivialTestView\$InspectionCompanion",
            """
                package androidx.pkg;

                import android.view.inspector.InspectionCompanion;
                import android.view.inspector.PropertyMapper;
                import android.view.inspector.PropertyReader;
                import androidx.annotation.NonNull;
                import androidx.annotation.RequiresApi;
                import java.lang.Override;
                import javax.annotation.processing.Generated;

                @RequiresApi(29)
                @Generated("androidx.resourceinspection.processor.ResourceInspectionProcessor")
                public final class TrivialTestView${'$'}InspectionCompanion
                        implements InspectionCompanion<TrivialTestView>
                {
                    private boolean mPropertiesMapped = false;
                    private int mTestAttributeId;

                    @Override
                    public void mapProperties(@NonNull PropertyMapper propertyMapper) {
                        mTestAttributeId = propertyMapper.mapInt(
                            "testAttribute", R.attr.testAttribute);
                    }

                    @Override
                    public void readProperties(
                            @NonNull TrivialTestView trivialTestView,
                            @NonNull PropertyReader propertyReader
                    ) {
                        if (!mPropertiesMapped) {
                            throw new InspectionCompanion.UninitializedPropertyMapException();
                        }
                        propertyReader.readInt(
                            mTestAttributeId, trivialTestView.getTestAttribute ());
                    }
                }
            """
        )

        assertThat(compilation).succeededWithoutWarnings()

        assertThat(compilation)
            .generatedSourceFile("androidx.pkg.TrivialTestView\$InspectionCompanion")
            .hasSourceEquivalentTo(expected)
    }

    @Test
    fun `test mixed namespaces`() {
        assertThat(
            compile(
                fakeR("androidx.pkg", "color"),
                java(
                    "androidx.pkg.MixedNamespaceTestView",
                    """
                        package androidx.pkg;

                        import android.content.Context;
                        import android.util.AttributeSet;
                        import android.view.View;
                        import androidx.annotation.ColorInt;
                        import androidx.resourceinspection.annotation.Attribute;

                        public class MixedNamespaceTestView extends View {
                            public MixedNamespaceTestView(Context context, AttributeSet attrs) {
                                super(context, attrs);
                            }

                            @ColorInt
                            @Attribute("android:color")
                            public int getPlatformColor() {
                                return 0;
                            }

                            @ColorInt
                            @Attribute("androidx.pkg:color")
                            public int getLibraryColor() {
                                return 1;
                            }
                        }

                    """
                )
            )
        )
            .generatedSourceFile("androidx.pkg.MixedNamespaceTestView\$InspectionCompanion")
            .hasSourceEquivalentTo(
                java(
                    "androidx.pkg.MixedNamespaceTestView\$InspectionCompanion",
                    """
                        package androidx.pkg;

                        import android.view.inspector.InspectionCompanion;
                        import android.view.inspector.PropertyMapper;
                        import android.view.inspector.PropertyReader;
                        import androidx.annotation.NonNull;
                        import androidx.annotation.RequiresApi;
                        import java.lang.Override;
                        import javax.annotation.processing.Generated;

                        @RequiresApi(29)
                        @Generated("androidx.resourceinspection.processor.ResourceInspectionProcessor")
                        public final class MixedNamespaceTestView${'$'}InspectionCompanion
                                implements InspectionCompanion<MixedNamespaceTestView> {
                            private boolean mPropertiesMapped = false;
                            private int mColorId;
                            private int mColorId_;

                            @Override
                            public void mapProperties(@NonNull PropertyMapper propertyMapper) {
                                mColorId = propertyMapper.mapColor("color", android.R.attr.color);
                                mColorId_ = propertyMapper.mapColor("color", R.attr.color);
                            }

                            @Override
                            public void readProperties(
                                    @NonNull MixedNamespaceTestView mixedNamespaceTestView,
                                    @NonNull PropertyReader propertyReader
                            ) {
                                if (!mPropertiesMapped) {
                                    throw new InspectionCompanion.UninitializedPropertyMapException();
                                }
                                propertyReader.readColor(
                                    mColorId,
                                    mixedNamespaceTestView.getPlatformColor());
                                propertyReader.readColor(
                                    mColorId_,
                                    mixedNamespaceTestView.getLibraryColor());
                            }
                        }
                    """
                )
            )
    }

    @Test
    fun `test simple attribute types`() {
        assertThat(
            compile(
                fakeR(
                    "androidx.pkg", "testBoolean", "testByte", "testCharacter", "testDouble",
                    "testFloat", "testInt", "testLong", "testShort", "testString",
                    "colorInt", "colorLong", "colorObject", "layoutResourceId", "anyResourceId",
                    "gravityInt"
                ),
                java(
                    "androidx.pkg.SimpleTypesTestView",
                    """
                        package androidx.pkg;

                        import android.content.Context;
                        import android.graphics.Color;
                        import android.util.AttributeSet;
                        import android.view.View;
                        import androidx.annotation.AnyRes;
                        import androidx.annotation.ColorInt;
                        import androidx.annotation.ColorLong;
                        import androidx.annotation.GravityInt;
                        import androidx.annotation.NonNull;
                        import androidx.annotation.LayoutRes;
                        import androidx.resourceinspection.annotation.Attribute;

                        public class SimpleTypesTestView extends View {
                            public SimpleTypesTestView(Context context, AttributeSet attrs) {
                                super(context, attrs);
                            }

                            @Attribute("androidx.pkg:testBoolean")
                            public boolean getTestBoolean() {
                                return false;
                            }

                            @Attribute("androidx.pkg:testByte")
                            public byte getTestByte() {
                                return 1;
                            }

                            @Attribute("androidx.pkg:testCharacter")
                            public char getTestCharacter() {
                                return '2';
                            }

                            @Attribute("androidx.pkg:testDouble")
                            public double getTestDouble() {
                                return 3.0;
                            }

                            @Attribute("androidx.pkg:testFloat")
                            public float getTestFloat() {
                                return 4.0F;
                            }

                            @Attribute("androidx.pkg:testInt")
                            public int getTestInt() {
                                return 5;
                            }

                            @Attribute("androidx.pkg:testLong")
                            public long getTestLong() {
                                return 6L;
                            }

                            @Attribute("androidx.pkg:testShort")
                            public short getTestShort() {
                                return 7;
                            }

                            @NonNull
                            @Attribute("androidx.pkg:testString")
                            public String getTestString() {
                                return "eight";
                            }

                            @ColorInt
                            @Attribute("androidx.pkg:colorInt")
                            public int getColorInt() {
                                return 9;
                            }

                            @ColorLong
                            @Attribute("androidx.pkg:colorLong")
                            public long getColorLong() {
                                return 10L;
                            }

                            @NonNull
                            @Attribute("androidx.pkg:colorObject")
                            public Color getColorObject() {
                                return new Color();
                            }

                            @LayoutRes
                            @Attribute("androidx.pkg:layoutResourceId")
                            public int getLayoutResourceId() {
                                return 12;
                            }

                            @AnyRes
                            @Attribute("androidx.pkg:anyResourceId")
                            public int getAnyResourceId() {
                                return 13;
                            }

                            @GravityInt
                            @Attribute("androidx.pkg:gravityInt")
                            public int getGravityInt() {
                                return 14;
                            }
                        }
                    """
                )
            )
        )
            .generatedSourceFile("androidx.pkg.SimpleTypesTestView\$InspectionCompanion")
            .hasSourceEquivalentTo(
                java(
                    "androidx.pkg.SimpleTypesTestView\$InspectionCompanion",
                    """
                        package androidx.pkg;

                        import android.view.inspector.InspectionCompanion;
                        import android.view.inspector.PropertyMapper;
                        import android.view.inspector.PropertyReader;
                        import androidx.annotation.NonNull;
                        import androidx.annotation.RequiresApi;
                        import java.lang.Override;
                        import javax.annotation.processing.Generated;

                        @RequiresApi(29)
                        @Generated("androidx.resourceinspection.processor.ResourceInspectionProcessor")
                        public final class SimpleTypesTestView${'$'}InspectionCompanion
                                implements InspectionCompanion<SimpleTypesTestView> {
                            private boolean mPropertiesMapped = false;
                            private int mAnyResourceIdId;
                            private int mColorIntId;
                            private int mColorLongId;
                            private int mColorObjectId;
                            private int mGravityIntId;
                            private int mLayoutResourceIdId;
                            private int mTestBooleanId;
                            private int mTestByteId;
                            private int mTestCharacterId;
                            private int mTestDoubleId;
                            private int mTestFloatId;
                            private int mTestIntId;
                            private int mTestLongId;
                            private int mTestShortId;
                            private int mTestStringId;

                            @Override
                            public void mapProperties(@NonNull PropertyMapper propertyMapper) {
                                mAnyResourceIdId = propertyMapper
                                    .mapResourceId("anyResourceId", R.attr.anyResourceId);
                                mColorIntId = propertyMapper
                                    .mapColor("colorInt", R.attr.colorInt);
                                mColorLongId = propertyMapper
                                    .mapColor("colorLong", R.attr.colorLong);
                                mColorObjectId = propertyMapper
                                    .mapColor("colorObject", R.attr.colorObject);
                                mGravityIntId = propertyMapper
                                    .mapGravity("gravityInt", R.attr.gravityInt)
                                mLayoutResourceIdId = propertyMapper
                                    .mapResourceId("layoutResourceId", R.attr.layoutResourceId);
                                mTestBooleanId = propertyMapper
                                    .mapBoolean("testBoolean", R.attr.testBoolean);
                                mTestByteId = propertyMapper
                                    .mapByte("testByte", R.attr.testByte);
                                mTestCharacterId = propertyMapper
                                    .mapChar("testCharacter", R.attr.testCharacter);
                                mTestDoubleId = propertyMapper
                                    .mapDouble("testDouble", R.attr.testDouble);
                                mTestFloatId = propertyMapper
                                    .mapFloat("testFloat", R.attr.testFloat);
                                mTestIntId = propertyMapper
                                    .mapInt("testInt", R.attr.testInt);
                                mTestLongId = propertyMapper
                                    .mapLong("testLong", R.attr.testLong);
                                mTestShortId = propertyMapper
                                    .mapShort("testShort", R.attr.testShort);
                                mTestStringId = propertyMapper
                                    .mapObject("testString", R.attr.testString);
                            }

                            @Override
                            public void readProperties(
                                    @NonNull SimpleTypesTestView simpleTypesTestView,
                                    @NonNull PropertyReader propertyReader
                            ) {
                                if (!mPropertiesMapped) {
                                    throw new InspectionCompanion.UninitializedPropertyMapException();
                                }
                                propertyReader.readResourceId(
                                    mAnyResourceIdId,
                                    simpleTypesTestView.getAnyResourceId());
                                propertyReader.readColor(
                                    mColorIntId,
                                    simpleTypesTestView.getColorInt());
                                propertyReader.readColor(
                                    mColorLongId,
                                    simpleTypesTestView.getColorLong());
                                propertyReader.readColor(
                                    mColorObjectId,
                                    simpleTypesTestView.getColorObject());
                                propertyReader.readGravity(
                                    mGravityIntId,
                                    simpleTypesTestView.getGravityInt());
                                propertyReader.readResourceId(
                                    mLayoutResourceIdId,
                                    simpleTypesTestView.getLayoutResourceId());
                                propertyReader.readBoolean(
                                    mTestBooleanId,
                                    simpleTypesTestView.getTestBoolean());
                                propertyReader.readByte(
                                    mTestByteId,
                                    simpleTypesTestView.getTestByte());
                                propertyReader.readChar(
                                    mTestCharacterId,
                                    simpleTypesTestView.getTestCharacter());
                                propertyReader.readDouble(
                                    mTestDoubleId,
                                    simpleTypesTestView.getTestDouble());
                                propertyReader.readFloat(
                                    mTestFloatId,
                                    simpleTypesTestView.getTestFloat());
                                propertyReader.readInt(
                                    mTestIntId,
                                    simpleTypesTestView.getTestInt());
                                propertyReader.readLong(
                                    mTestLongId,
                                    simpleTypesTestView.getTestLong());
                                propertyReader.readShort(
                                    mTestShortId,
                                    simpleTypesTestView.getTestShort());
                                propertyReader.readObject(
                                    mTestStringId,
                                    simpleTypesTestView.getTestString());
                            }
                        }
                    """

                )
            )
    }

    @Test
    fun `int enum attributes`() {
        assertThat(
            compile(
                fakeR("androidx.pkg", "intEnum"),
                java(
                    "androidx.pkg.IntEnumTestView",
                    """
                        package androidx.pkg;

                        import android.content.Context;
                        import android.util.AttributeSet;
                        import android.view.View;
                        import androidx.resourceinspection.annotation.Attribute;

                        public class IntEnumTestView extends View {
                            public IntEnumTestView(Context context, AttributeSet attrs) {
                                super(context, attrs);
                            }

                            @Attribute(value = "androidx.pkg:intEnum", intMapping = {
                                @Attribute.IntMap(value = 0, name = "ZERO"),
                                @Attribute.IntMap(value = 1, name = "ONE"),
                                @Attribute.IntMap(value = 2, name = "TWO")
                            })
                            public int getIntEnum() {
                                return 0;
                            }
                        }
                    """
                )
            )
        )
            .generatedSourceFile("androidx.pkg.IntEnumTestView\$InspectionCompanion")
            .hasSourceEquivalentTo(
                java(
                    "androidx.pkg.IntEnumTestView\$InspectionCompanion",
                    """
                        package androidx.pkg;

                        import android.view.inspector.InspectionCompanion;
                        import android.view.inspector.PropertyMapper;
                        import android.view.inspector.PropertyReader;
                        import androidx.annotation.NonNull;
                        import androidx.annotation.RequiresApi;
                        import java.lang.Override;
                        import java.lang.String;
                        import java.util.function.IntFunction;
                        import javax.annotation.processing.Generated;

                        @RequiresApi(29)
                        @Generated("androidx.resourceinspection.processor.ResourceInspectionProcessor")
                        public final class IntEnumTestView${'$'}InspectionCompanion
                                implements InspectionCompanion<IntEnumTestView> {
                            private boolean mPropertiesMapped = false;

                            private int mIntEnumId;

                            @Override
                            public void mapProperties(@NonNull PropertyMapper propertyMapper) {
                                mIntEnumId = propertyMapper.mapIntEnum(
                                    "intEnum",
                                    R.attr.intEnum,
                                    new IntFunction<String>() {
                                        @Override
                                        public String apply(int value) {
                                            switch (value) {
                                                case 0:
                                                    return "ZERO";
                                                case 1:
                                                    return "ONE";
                                                case 2:
                                                    return "TWO";
                                                default:
                                                    return String.valueOf(value);
                                            }
                                        }
                                    }
                                );
                            }

                            @Override
                            public void readProperties(
                                    @NonNull IntEnumTestView intEnumTestView,
                                    @NonNull PropertyReader propertyReader
                            ) {
                                if (!mPropertiesMapped) {
                                    throw new InspectionCompanion.UninitializedPropertyMapException();
                                }
                                propertyReader
                                    .readIntEnum(mIntEnumId, intEnumTestView.getIntEnum());
                            }
                        }
                    """
                )
            )
    }

    @Test
    fun `int flag attributes`() {
        assertThat(
            compile(
                fakeR("androidx.pkg", "intFlag"),
                java(
                    "androidx.pkg.IntFlagTestView",
                    """
                        package androidx.pkg;

                        import android.content.Context;
                        import android.util.AttributeSet;
                        import android.view.View;
                        import androidx.resourceinspection.annotation.Attribute;

                        public class IntFlagTestView extends View {
                            public IntFlagTestView(Context context, AttributeSet attrs) {
                                super(context, attrs);
                            }

                            @Attribute(value = "androidx.pkg:intFlag", intMapping = {
                                @Attribute.IntMap(value = 1, mask = 3, name = "ONE"),
                                @Attribute.IntMap(value = 2, mask = 3, name = "TWO"),
                                @Attribute.IntMap(value = 4, mask = 4, name = "FOUR"),
                                @Attribute.IntMap(value = 5, name = "FIVE")
                            })
                            public int getIntFlag() {
                                return 0;
                            }
                        }
                    """
                )
            )
        )
            .generatedSourceFile("androidx.pkg.IntFlagTestView\$InspectionCompanion")
            .hasSourceEquivalentTo(
                java(
                    "androidx.pkg.IntFlagTestView\$InspectionCompanion",
                    """
                        package androidx.pkg;

                        import android.view.inspector.InspectionCompanion;
                        import android.view.inspector.PropertyMapper;
                        import android.view.inspector.PropertyReader;
                        import androidx.annotation.NonNull;
                        import androidx.annotation.RequiresApi;
                        import java.lang.Override;
                        import java.lang.String;
                        import java.util.HashSet;
                        import java.util.Set;
                        import java.util.function.IntFunction;
                        import javax.annotation.processing.Generated;

                        @RequiresApi(29)
                        @Generated("androidx.resourceinspection.processor.ResourceInspectionProcessor")
                        public final class IntFlagTestView${'$'}InspectionCompanion
                                implements InspectionCompanion<IntFlagTestView> {
                            private boolean mPropertiesMapped = false;
                            private int mIntFlagId;

                            @Override
                            public void mapProperties(@NonNull PropertyMapper propertyMapper) {
                                mIntFlagId = propertyMapper.mapIntFlag(
                                    "intFlag",
                                    R.attr.intFlag,
                                    new IntFunction<Set<String>>() {
                                        @Override
                                        public Set<String> apply(int value) {
                                            final Set<String> flags = new HashSet<String>();
                                            if ((value & 3) == 1) {
                                                flags.add("ONE");
                                            }
                                            if ((value & 3) == 2) {
                                                flags.add("TWO");
                                            }
                                            if ((value & 4) == 4) {
                                                flags.add("FOUR");
                                            }
                                            if (value == 5) {
                                                flags.add("FIVE");
                                            }
                                            return flags;
                                        }
                                    }
                                );
                            }

                            @Override
                            public void readProperties(
                                    @NonNull IntFlagTestView intFlagTestView,
                                    @NonNull PropertyReader propertyReader
                            ) {
                                if (!mPropertiesMapped) {
                                    throw new InspectionCompanion.UninitializedPropertyMapException();
                                }
                                propertyReader
                                    .readIntFlag(mIntFlagId, intFlagTestView.getIntFlag());
                            }
                        }
                    """
                )
            )
    }

    @Test
    fun `supports the latest source version`() {
        assertThat(ResourceInspectionProcessor().supportedSourceVersion)
            .isEqualTo(SourceVersion.latest())
    }

    @Test
    fun `fails with @Attribute on non view classes`() {
        assertThat(
            compile(
                java(
                    "androidx.pkg.NonViewTest",
                    """
                        package androidx.pkg;

                        import androidx.annotation.ColorInt;
                        import androidx.resourceinspection.annotation.Attribute;

                        public final class NonViewTest {
                            @ColorInt
                            @Attribute("android:color")
                            public int getColor() {
                                return 1;
                            }
                        }
                    """
                )
            )
        ).hadErrorContaining("@Attribute must only annotate subclasses of android.view.View")
    }

    @Test
    fun `fails on non-getter methods`() {
        assertThat(
            compile(
                java(
                    "androidx.pkg.SetterNotGetterTestView",
                    """
                        package androidx.pkg;

                        import android.content.Context;
                        import android.util.AttributeSet;
                        import android.view.View;
                        import androidx.annotation.ColorInt;
                        import androidx.resourceinspection.annotation.Attribute;

                        public final class SetterNotGetterTestView extends View {
                            public SetterNotGetterTestView(Context context, AttributeSet attrs) {
                                super(context, attrs);
                            }

                            @Attribute("android:color")
                            public void setColor(@ColorInt int color) {
                                // no-op
                            }
                        }
                    """
                )
            )
        ).hadErrorContaining("@Attribute must annotate a getter")
    }

    @Test
    fun `fails on missing namespace`() {
        assertThat(
            compile(
                java(
                    "androidx.pkg.MissingNamespaceTestView",
                    """
                        package androidx.pkg;

                        import android.content.Context;
                        import android.util.AttributeSet;
                        import android.view.View;
                        import androidx.annotation.ColorInt;
                        import androidx.resourceinspection.annotation.Attribute;

                        public final class MissingNamespaceTestView extends View {
                            public MissingNamespaceTestView(Context context, AttributeSet attrs) {
                                super(context, attrs);
                            }

                            @ColorInt
                            @Attribute("color")
                            public int getColor() {
                                return 1;
                            }
                        }
                    """
                )
            )
        ).hadErrorContaining("Attribute name must include namespace")
    }

    @Test
    fun `fails on invalid name`() {
        assertThat(
            compile(
                java(
                    "androidx.pkg.InvalidNameTestView",
                    """
                        package androidx.pkg;

                        import android.content.Context;
                        import android.util.AttributeSet;
                        import android.view.View;
                        import androidx.resourceinspection.annotation.Attribute;

                        public final class InvalidNameTestView extends View {
                            public InvalidNameTestView(Context context, AttributeSet attrs) {
                                super(context, attrs);
                            }

                            @Attribute("androidx.pkg:!@#%")
                            public int getInvalid() {
                                return 1;
                            }
                        }
                    """
                )
            )
        ).hadErrorContaining("Invalid attribute name")
    }

    @Test
    fun `fails on non-public getter`() {
        assertThat(
            compile(
                java(
                    "androidx.pkg.NonPublicGetterTestView",
                    """
                        package androidx.pkg;

                        import android.content.Context;
                        import android.util.AttributeSet;
                        import android.view.View;
                        import androidx.annotation.ColorInt;
                        import androidx.resourceinspection.annotation.Attribute;

                        public final class NonPublicGetterTestView extends View {
                            public NonPublicGetterTestView(Context context, AttributeSet attrs) {
                                super(context, attrs);
                            }

                            @ColorInt
                            @Attribute("android:color")
                            int getColor() {
                                return 1;
                            }
                        }
                    """
                )
            )
        ).hadErrorContaining("@Attribute getter must be public")
    }

    @Test
    fun `fails on duplicate attributes`() {
        val compilation = compile(
            fakeR("androidx.pkg", "duplicated"),
            java(
                "androidx.pkg.DuplicateAttributesTestView",
                """
                    package androidx.pkg;

                    import android.content.Context;
                    import android.util.AttributeSet;
                    import android.view.View;
                    import androidx.annotation.ColorInt;
                    import androidx.resourceinspection.annotation.Attribute;

                    public final class DuplicateAttributesTestView extends View {
                        public DuplicateAttributesTestView(Context context, AttributeSet attrs) {
                            super(context, attrs);
                        }

                        @Attribute("androidx.pkg:duplicated")
                        public int getDuplicated1() {
                            return 1;
                        }

                        @Attribute("androidx.pkg:duplicated")
                        public int getDuplicated2() {
                            return 2;
                        }
                    }
                """
            )
        )

        assertThat(compilation).hadErrorContaining(
            "Duplicate attribute androidx.pkg:duplicated is also present on getDuplicated1()"
        )

        assertThat(compilation).hadErrorContaining(
            "Duplicate attribute androidx.pkg:duplicated is also present on getDuplicated2()"
        )
    }

    private fun compile(vararg sources: JavaFileObject): Compilation {
        return javac()
            .withProcessors(ResourceInspectionProcessor())
            .compile(sources.toList())
    }

    private fun java(qualifiedName: String, @Language("JAVA") source: String): JavaFileObject {
        return JavaFileObjects.forSourceString(qualifiedName, source.trimIndent())
    }

    private fun fakeR(packageName: String, vararg attributeNames: String): JavaFileObject {
        return java(
            "$packageName.R",
            """
                package $packageName;

                public final class R{
                    public static final class attr {
            ${ attributeNames.mapIndexed { index, name ->
                "${" ".repeat(8)}public static final int $name = $index;"
            }.joinToString(separator = "\n") }
                    }
                }
            """
        )
    }
}
