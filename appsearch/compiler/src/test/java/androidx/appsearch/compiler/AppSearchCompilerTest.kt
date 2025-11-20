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
package androidx.appsearch.compiler

import com.google.testing.compile.CompilationSubject
import org.junit.Test

class AppSearchCompilerTest : CompilerTestBase() {
    @Test
    fun testPrivate() {
        val compilation =
            compile(
                classSimpleName = "Wrapper",
                """
                public class Wrapper {
                  @Document
                  private class Gift {}
                }  // Wrapper
                """
                    .trimIndent(),
                restrictGeneratedCodeToLibrary = false,
            )
        CompilationSubject.assertThat(compilation).hadErrorContaining("annotated class is private")
    }

    @Test
    fun testNoId() {
        val compilation =
            compile(
                """
          @Document
          public class Gift {
            @Document.Namespace String namespace;
          }
        """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("must have exactly one field annotated with @Id")
    }

    @Test
    fun testManyIds() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id1;
                  @Document.Id String id2;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Duplicate member annotated with @Id")
    }

    @Test
    fun testAutoValueInheritance() {
        val docExtendsAutoValueDoc =
            compile(
                """
                import com.google.auto.value.AutoValue;
                import com.google.auto.value.AutoValue.*;
                @AutoValue
                @Document
                public abstract class Gift {
                  @CopyAnnotations @Document.Id abstract String id();
                  @CopyAnnotations @Document.Namespace abstract String namespace();
                  public static Gift create(String id, String namespace) {
                      return new AutoValue_Gift(id,namespace);
                  }
                  @Document
                  static abstract class CoolGift extends Gift {
                    @Document.BooleanProperty boolean cool;
                    CoolGift(String id, String namespace, boolean cool) {
                      super(id, message);
                      this.cool = cool;
                    }
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(docExtendsAutoValueDoc)
            .hadErrorContaining(
                "A class annotated with Document cannot inherit from a class annotated with " +
                    "AutoValue"
            )

        val autoValueDocExtendsDoc =
            compile(
                """
                import com.google.auto.value.AutoValue;
                import com.google.auto.value.AutoValue.*;
                @Document
                public class Gift {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @AutoValue
                  @Document
                  static abstract class CoolGift extends Gift {
                    @CopyAnnotations @Document.BooleanProperty abstract boolean cool();
                    public static CoolGift create(String id, String namespace, boolean cool) {
                      return new AutoValue_Gift_CoolGift(cool);
                    }
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(autoValueDocExtendsDoc)
            .hadErrorContaining(
                "A class annotated with AutoValue and Document cannot have a superclass"
            )
    }

    @Test
    fun testSuperClassErrors() {
        val specialFieldReassigned =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String prop;
                  Gift(String id, String namespace, String prop) {
                    this.id = id;
                    this.namespace = namespace;
                    this.prop = prop;
                  }
                }
                @Document
                class CoolGift extends Gift {
                  @Document.StringProperty String id;
                  CoolGift(String id, String namespace) {
                    super(id, namespace, "");
                    this.id = id;
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(specialFieldReassigned)
            .hadErrorContaining(
                "Property type must stay consistent when overriding annotated " +
                    "members but changed from @Id -> @StringProperty"
            )

        // error on collision
        val idCollision =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  Gift(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                }
                @Document
                class CoolGift extends Gift {
                  @Document.BooleanProperty private final boolean cool;
                  @Document.Id String badId;
                  CoolGift(String id, String namespace, String badId) {
                    super(id, namespace);
                    this.badId = badId;
                  }
                  public boolean getBadId() { return badId; }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(idCollision)
            .hadErrorContaining("Duplicate member annotated with @Id")

        val nsCollision =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  Gift(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                }
                @Document
                class CoolGift extends Gift {
                  @Document.Namespace String badNamespace;
                  CoolGift(String id, String namespace, String badId) {
                    super(id, namespace);
                    this.badNamespace = namespace;
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(nsCollision)
            .hadErrorContaining("Duplicate member annotated with @Namespace")
    }

    @Test
    fun testSuperClass() {
        // Try multiple levels of inheritance, nested, with properties, overriding properties
        val compilation =
            compile(
                """
                @Document
                class Ancestor {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note;
                  int score;
                  Ancestor(String id, String namespace, String note) {
                    this.id = id;
                    this.namespace = namespace;
                    this.note = note;
                  }
                  public String getNote() { return note; }
                }
                class Parent extends Ancestor {
                  Parent(String id, String namespace, String note) {
                    super(id, namespace, note);
                  }
                }
                @Document
                class Gift extends Parent {
                  @Document.StringProperty String sender;
                  Gift(String id, String namespace, String sender) {
                    super(id, namespace, "note");
                    this.sender = sender;
                  }
                  public String getSender() { return sender; }
                  @Document
                  class FooGift extends Gift {
                    @Document.Score int score;
                    @Document.BooleanProperty boolean foo;
                    FooGift(String id, String namespace, String note, int score, boolean foo) {
                      super(id, namespace, note);
                      this.score = score;
                      this.foo = foo;
                    }
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift$\$__FooGift.java")
    }

    @Test
    fun testSuperClass_changeSchemaName() {
        val compilation =
            compile(
                """
                @Document(name="MyParent")
                class Parent {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note;
                  Parent(String id, String namespace, String note) {
                    this.id = id;
                    this.namespace = namespace;
                    this.note = note;
                  }
                  public String getNote() { return note; }
                }

                @Document(name="MyGift")
                class Gift extends Parent {
                  @Document.StringProperty String sender;
                  Gift(String id, String namespace, String sender) {
                    super(id, namespace, "note");
                    this.sender = sender;
                  }
                  public String getSender() { return sender; }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            /*className=*/ "Gift.java",
            /*content=*/ "public static final String SCHEMA_NAME = \"MyGift\";",
        )
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testSuperClass_multipleChangedSchemaNames() {
        val compilation =
            compile(
                """
                @Document(name="MyParent")
                class Parent {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note;
                  Parent(String id, String namespace, String note) {
                    this.id = id;
                    this.namespace = namespace;
                    this.note = note;
                  }
                  public String getNote() { return note; }
                }

                @Document(name="MyGift")
                class Gift extends Parent {
                  @Document.StringProperty String sender;
                  Gift(String id, String namespace, String sender) {
                    super(id, namespace, "note");
                    this.sender = sender;
                  }
                  public String getSender() { return sender; }
                }

                @Document
                class FooGift extends Gift {
                  @Document.BooleanProperty boolean foo;
                  FooGift(String id, String namespace, String note, boolean foo) {
                    super(id, namespace, note);
                    this.foo = foo;
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            /*className=*/ "FooGift.java",
            /*content=*/ "public static final String SCHEMA_NAME = \"MyGift\";",
        )
        checkEqualsGolden("FooGift.java")
    }

    @Test
    fun testSuperClassPojoAncestor() {
        // Try multiple levels of inheritance, nested, with properties, overriding properties
        val compilation =
            compile(
                """
                class Ancestor {
                }
                @Document
                class Parent extends Ancestor {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note;
                  int score;
                  Parent(String id, String namespace, String note) {
                    this.id = id;
                    this.namespace = namespace;
                    this.note = note;
                  }
                }
                @Document
                class Gift extends Parent {
                  @Document.StringProperty String sender;
                  Gift(String id, String namespace, String sender) {
                    super(id, namespace, "note");
                    this.sender = sender;
                  }
                  public String getSender() { return sender; }
                  @Document
                  class FooGift extends Gift {
                    @Document.Score int score;
                    @Document.BooleanProperty boolean foo;
                    FooGift(String id, String namespace, String note, int score, boolean foo) {
                      super(id, namespace, note);
                      this.score = score;
                      this.foo = foo;
                    }
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift$\$__FooGift.java")
    }

    @Test
    fun testSuperClassWithPrivateFields() {
        // Parents has private fields with public getter. Children should be able to extend
        // Parents and inherit the public getters.
        // TODO(b/262916926): we should be able to support inheriting classes not annotated with
        //  @Document.
        val compilation =
            compile(
                """
                @Document
                class Ancestor {
                  @Document.Namespace private String mNamespace;
                  @Document.Id private String mId;
                  @Document.StringProperty private String mNote;
                  Ancestor(String id, String namespace, String note) {
                    this.mId = id;
                    this.mNamespace = namespace;
                    this.mNote = note;
                  }
                  public String getNamespace() { return mNamespace; }
                  public String getId() { return mId; }
                  public String getNote() { return mNote; }
                }
                @Document
                class Parent extends Ancestor {
                  @Document.StringProperty private String mReceiver;
                  Parent(String id, String namespace, String note, String receiver) {
                    super(id, namespace, note);
                    this.mReceiver = receiver;
                  }
                  public String getReceiver() { return mReceiver; }
                }
                @Document
                class Gift extends Parent {
                  @Document.StringProperty private String mSender;
                  Gift(String id, String namespace, String note, String receiver,
                    String sender) {
                    super(id, namespace, note, receiver);
                    this.mSender = sender;
                  }
                  public String getSender() { return mSender; }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains(/* className= */ "Gift.java", /* content= */ "document.getNote()")
        checkResultContains(/* className= */ "Gift.java", /* content= */ "document.getReceiver()")
        checkResultContains(/* className= */ "Gift.java", /* content= */ "document.getSender()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testManyCreationTimestamp() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.CreationTimestampMillis long ts1;
                  @Document.CreationTimestampMillis long ts2;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Duplicate member annotated with @CreationTimestampMillis")
    }

    @Test
    fun testNoNamespace() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Id String id;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("must have exactly one field annotated with @Namespace")
    }

    @Test
    fun testManyNamespace() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String ns1;
                  @Document.Namespace String ns2;
                  @Document.Id String id;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Duplicate member annotated with @Namespace")
    }

    @Test
    fun testManyTtlMillis() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.TtlMillis long ts1;
                  @Document.TtlMillis long ts2;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Duplicate member annotated with @TtlMillis")
    }

    @Test
    fun testManyScore() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.Score int score1;
                  @Document.Score int score2;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Duplicate member annotated with @Score")
    }

    @Test
    fun testClassSpecialValues() {
        compile(
            """
                @Document
                public class Gift {
                    @Document.Namespace
                    String mNamespace;
                    @Document.Id
                    String mId;
                    @Document.CreationTimestampMillis
                    Long mCreationTimestampMillis;
                    @Document.Score
                    Integer mScore;
                    @Document.TtlMillis
                    private Long mTtlMillis;
                    public Long getTtlMillis() {
                        return mTtlMillis;
                    }
                    public void setTtlMillis(Long ttlMillis) {
                        mTtlMillis = ttlMillis;
                    }
                    @Document.StringProperty
                    String mString;
                }
            """
                .trimIndent()
        )

        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testCantRead_noGetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "Field 'price' cannot be read: it is private and has no suitable getters " +
                    "[public] int price() OR [public] int getPrice()"
            )
    }

    @Test
    fun testCantRead_privateGetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  private int getPrice() { return 0; }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "Field 'price' cannot be read: it is private and has no suitable getters " +
                    "[public] int price() OR [public] int getPrice()"
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Getter cannot be used: private visibility")
    }

    @Test
    fun testCantRead_wrongParamGetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  int getPrice(int n) { return 0; }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "Field 'price' cannot be read: it is private and has no suitable getters " +
                    "[public] int price() OR [public] int getPrice()"
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Getter cannot be used: should take no parameters")
    }

    @Test
    fun testCantRead_isGetterNonBoolean() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  int isPrice() { return price; }
                  void setPrice(int price) {}
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "Field 'price' cannot be read: it is private and has no suitable getters " +
                    "[public] int price() OR [public] int getPrice()"
            )
    }

    @Test
    fun testCantRead_noSuitableBooleanGetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.BooleanProperty private boolean wrapped;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                ("Field 'wrapped' cannot be read: it is private and has no suitable getters " +
                    "[public] boolean wrapped() " +
                    "OR [public] boolean getWrapped() " +
                    "OR [public] boolean isWrapped()")
            )
    }

    @Test
    fun testRead_MultipleGetters() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  int getPrice(int n) { return 0; }
                  int getPrice() { return 0; }
                  void setPrice(int n) {}
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testRead_isGetterForBoolean() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.BooleanProperty private boolean forSale;
                  boolean isForSale() { return forSale; }
                  void setForSale(boolean forSale) {}
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testRead_GetterReturnsSubtype() {
        val compilation =
            compile(
                """
                import java.util.*;
                import com.google.common.collect.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty private List<String> from =
                    new ArrayList<>();
                  ImmutableList<String> getFrom() {
                    return ImmutableList.copyOf(from);
                  }
                  void setFrom(Collection<String> from) {
                    this.from = new ArrayList<>(from);
                  }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testGetterAndSetterFunctions_withFieldName() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  int price() { return 0; }
                  void price(int n) {}
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        // Check setter function is identified correctly.
        checkResultContains(
            /* className= */ "Gift.java",
            /* content= */ "builder.setPropertyLong(\"price\", document.price());",
        )
        // Check getter function is identified correctly.
        checkResultContains(
            /* className= */ "Gift.java",
            /* content= */ "document.price(priceConv);",
        )
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testCantWrite_noSetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  int getPrice() { return price; }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                ("Could not find a suitable constructor/factory method for " +
                    "\"com.example.appsearch.Gift\" that covers properties: [price]. " +
                    "See the warnings for more details.")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Could not find any of the setter(s): " +
                    "[public] void price(int)|" +
                    "[public] void setPrice(int)")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Cannot use this constructor to construct the class: " +
                    "\"com.example.appsearch.Gift\". " +
                    "No parameters for the properties: [price]")
            )
    }

    @Test
    fun testCantWrite_privateSetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  int getPrice() { return price; }
                  private void setPrice(int n) {}
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                ("Could not find a suitable constructor/factory method for " +
                    "\"com.example.appsearch.Gift\" that covers properties: [price]. " +
                    "See the warnings for more details.")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Could not find any of the setter(s): " +
                    "[public] void price(int)|" +
                    "[public] void setPrice(int)")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Setter cannot be used: private visibility")
    }

    @Test
    fun testCantWrite_wrongParamSetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  int getPrice() { return price; }
                  void setPrice() {}
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                ("Could not find a suitable constructor/factory method for " +
                    "\"com.example.appsearch.Gift\" that covers properties: [price]. " +
                    "See the warnings for more details.")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Could not find any of the setter(s): " +
                    "[public] void price(int)|" +
                    "[public] void setPrice(int)")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Setter cannot be used: takes 0 parameters instead of 1")
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Cannot use this constructor to construct the class: " +
                    "\"com.example.appsearch.Gift\". " +
                    "No parameters for the properties: [price]")
            )
    }

    @Test
    fun testWrite_multipleSetters() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty private int price;
                  int getPrice() { return price; }
                  void setPrice() {}
                  void setPrice(int n) {}
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testWrite_privateConstructor() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  private Gift() {}
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty int price;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Could not find a suitable creation method")
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                "Method cannot be used to create a document class: private visibility"
            )
    }

    @Test
    fun testWrite_constructorMissingParams() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  Gift(int price) {}
                  @Document.Namespace String namespace;
                  @Document.Id final String id;
                  @Document.LongProperty int price;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                ("Could not find a suitable constructor/factory method for " +
                    "\"com.example.appsearch.Gift\" that covers properties: [id]. " +
                    "See the warnings for more details.")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Could not find any of the setter(s): " +
                    "[public] void id(java.lang.String)|" +
                    "[public] void setId(java.lang.String)")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Cannot use this constructor to construct the class: " +
                    "\"com.example.appsearch.Gift\". " +
                    "No parameters for the properties: [id]")
            )
    }

    @Test
    fun testWrite_factoryMethodOnly() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  private Gift(int price, String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  public static Gift create(String id, String namespace, int price) {
                    return new Gift(price, id, namespace);
                  }
                  @Document.Namespace String namespace;
                  @Document.Id final String id;
                  @Document.LongProperty int price;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            /* className= */ "Gift.java",
            /* content= */ "Gift document = Gift.create(idConv, namespaceConv, priceConv);",
        )
    }

    /** With golden class for factory method. */
    @Test
    fun testWrite_bothUsableFactoryMethodAndConstructor_picksFirstUsableCreationMethod() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  Gift(String id, String namespace, int price) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  public static Gift create(String id, String namespace, int price) {
                    return new Gift(id, namespace, price);
                  }
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty int price;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            /* className= */ "Gift.java",
            /* content= */ "Gift document = new Gift(idConv, namespaceConv, priceConv);",
        )
    }

    @Test
    fun testWrite_usableFactoryMethod_unusableConstructor() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  private Gift(String id, String namespace, int price) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  public static Gift create(String id, String namespace, int price) {
                    return new Gift(id, namespace, price);
                  }
                  @Document.Namespace String namespace;
                  @Document.Id final String id;
                  @Document.LongProperty int price;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            /* className= */ "Gift.java",
            /* content= */ "Gift document = Gift.create(idConv, namespaceConv, priceConv);",
        )
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testWrite_unusableFactoryMethod_usableConstructor() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  Gift(String id, String namespace, int price) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  private static Gift create(String id, String namespace, int price){
                    return new Gift(id, namespace, price);
                  }
                  @Document.Namespace String namespace;
                  @Document.Id final String id;
                  @Document.LongProperty int price;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            /* className= */ "Gift.java",
            /* content= */ "Gift document = new Gift(idConv, namespaceConv, priceConv);",
        )
    }

    @Test
    fun testWrite_constructorExtraParams() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  Gift(int price, String id, String namespace, int unknownParam) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  @Document.Namespace String namespace;
                  @Document.Id final String id;
                  @Document.LongProperty int price;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Could not find a suitable creation method")
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                "Parameter \"unknownParam\" is not an AppSearch parameter; don't know how to " +
                    "supply it"
            )
    }

    @Test
    fun testWrite_multipleConventions() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id private String id_;
                  @Document.LongProperty private int price1;
                  @Document.LongProperty private int mPrice2;
                  @Document.LongProperty private int _price3;
                  @Document.LongProperty final int price4_;
                  int getPrice1() { return price1; }
                  int price2() { return mPrice2; }
                  int getPrice3() { return _price3; }
                  void setPrice1(int n) {}
                  void price2(int n) {}
                  void price3(int n) {}
                  String getId() {
                    return id_;
                  }
                  public Gift(String id, int price4) {
                    id_ = id;
                    price4_ = price4;
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testSuccessSimple() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  Gift(boolean dog, String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                    this.dog = dog;
                  }
                  @Document.Namespace String namespace;
                  @Document.Id final String id;
                  @Document.LongProperty int price;
                  @Document.BooleanProperty boolean cat = false;
                  public void setCat(boolean cat) {}
                  @Document.BooleanProperty private final boolean dog;
                  public boolean getDog() { return dog; }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testDifferentTypeName() {
        val compilation =
            compile(
                """
                @Document(name="DifferentType")
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testRepeatedFields() {
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty List<String> listOfString;
                  @Document.LongProperty Collection<Integer> setOfInt;
                  @Document.BytesProperty byte[][] repeatedByteArray;
                  @Document.BytesProperty byte[] byteArray;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testCardinality() {
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(required=true) List<String> repeatReq;
                  @Document.StringProperty(required=false) List<String> repeatNoReq;
                  @Document.DoubleProperty(required=true) Float req;
                  @Document.DoubleProperty(required=false) Float noReq;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testAllSingleTypes() {
        // TODO(b/156296904): Uncomment Gift in this test when it's supported
        val compilation =
            compile(
                """
                import java.util.*;
                import androidx.appsearch.app.AppSearchBlobHandle;
                import androidx.appsearch.app.EmbeddingVector;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @StringProperty String stringProp;
                  @LongProperty Integer integerProp;
                  @LongProperty Long longProp;
                  @DoubleProperty Float floatProp;
                  @DoubleProperty Double doubleProp;
                  @BooleanProperty Boolean booleanProp;
                  @BytesProperty byte[] bytesProp;
                  @EmbeddingProperty EmbeddingVector vectorProp;
                  @BlobHandleProperty AppSearchBlobHandle blobHandleProp;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testTokenizerType() {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;

                  // NONE index type will generate a NONE tokenizerType type.
                  @Document.StringProperty(tokenizerType=0, indexingType=0)
                  String tokNoneInvalid;
                  @Document.StringProperty(tokenizerType=1, indexingType=0)
                  String tokPlainInvalid;
                  @Document.StringProperty(tokenizerType=2, indexingType=0)
                  String tokVerbatimInvalid;
                  @Document.StringProperty(tokenizerType=3, indexingType=0)
                  String tokRfc822Invalid;

                  // Indexing type exact.
                  @Document.StringProperty(tokenizerType=0, indexingType=1)
                  String tokNone;
                  @Document.StringProperty(tokenizerType=1, indexingType=1)
                  String tokPlain;
                  @Document.StringProperty(tokenizerType=2, indexingType=1)
                  String tokVerbatim;
                  @Document.StringProperty(tokenizerType=3, indexingType=1)
                  String tokRfc822;

                  // Indexing type prefix.
                  @Document.StringProperty(tokenizerType=0, indexingType=2)
                  String tokNonePrefix;
                  @Document.StringProperty(tokenizerType=1, indexingType=2)
                  String tokPlainPrefix;
                  @Document.StringProperty(tokenizerType=2, indexingType=2)
                  String tokVerbatimPrefix;
                  @Document.StringProperty(tokenizerType=3, indexingType=2)
                  String tokRfc822Prefix;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testInvalidTokenizerType() {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(indexingType=1, tokenizerType=100)
                  String str;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).hadErrorContaining("Unknown tokenizer type 100")
    }

    @Test
    fun testIndexingType() {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(indexingType=0) String indexNone;
                  @Document.StringProperty(indexingType=1) String indexExact;
                  @Document.StringProperty(indexingType=2) String indexPrefix;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testInvalidIndexingType() {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(indexingType=100, tokenizerType=1)
                  String str;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).hadErrorContaining("Unknown indexing type 100")
    }

    @Test
    fun testLongPropertyIndexingType() {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty Long defaultIndexNone;
                  @Document.LongProperty(indexingType=0) Long indexNone;
                  @Document.LongProperty(indexingType=1) Integer boxInt;
                  @Document.LongProperty(indexingType=1) int unboxInt;
                  @Document.LongProperty(indexingType=1) Long boxLong;
                  @Document.LongProperty(indexingType=1) long unboxLong;
                  @Document.LongProperty(indexingType=1) Integer[] arrBoxInt;
                  @Document.LongProperty(indexingType=1) int[] arrUnboxInt;
                  @Document.LongProperty(indexingType=1) Long[] arrBoxLong;
                  @Document.LongProperty(indexingType=1) long[] arrUnboxLong;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testInvalidLongPropertyIndexingType() {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty(indexingType=100) Long invalidProperty;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).hadErrorContaining("Unknown indexing type 100")
    }

    @Test
    fun testStringPropertyJoinableType() {
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(joinableValueType=1)
                  String object;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testRepeatedPropertyJoinableType_throwsError() {
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(joinableValueType=1)
                  List<String> object;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Joinable value type 1 not allowed on repeated properties.")
    }

    @Test
    fun testPropertyName() {
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(name="newName") String oldName;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testToGenericDocument_allSupportedTypes() {
        val compilation =
            compile(
                """
                import java.util.*;
                import androidx.appsearch.app.AppSearchBlobHandle;
                import androidx.appsearch.app.GenericDocument;
                import androidx.appsearch.app.EmbeddingVector;
                @Document
                public class Gift {
                  @Namespace String namespace;
                  @Id String id;

                  // Collections
                  @LongProperty Collection<Long> collectLong; // 1a
                  @LongProperty Collection<Integer> collectInteger; // 1a
                  @DoubleProperty Collection<Double> collectDouble; // 1a
                  @DoubleProperty Collection<Float> collectFloat; // 1a
                  @BooleanProperty Collection<Boolean> collectBoolean; // 1a
                  @BytesProperty Collection<byte[]> collectByteArr; // 1a
                  @StringProperty Collection<String> collectString; // 1b
                  @DocumentProperty Collection<Gift> collectGift; // 1c
                  @EmbeddingProperty Collection<EmbeddingVector> collectVec; // 1b
                  @BlobHandleProperty Collection<AppSearchBlobHandle> collectBlob; // 1b

                  // Arrays
                  @LongProperty Long[] arrBoxLong; // 2a
                  @LongProperty long[] arrUnboxLong; // 2b
                  @LongProperty Integer[] arrBoxInteger; // 2a
                  @LongProperty int[] arrUnboxInt; // 2a
                  @DoubleProperty Double[] arrBoxDouble; // 2a
                  @DoubleProperty double[] arrUnboxDouble; // 2b
                  @DoubleProperty Float[] arrBoxFloat; // 2a
                  @DoubleProperty float[] arrUnboxFloat; // 2a
                  @BooleanProperty Boolean[] arrBoxBoolean; // 2a
                  @BooleanProperty boolean[] arrUnboxBoolean; // 2b
                  @BytesProperty byte[][] arrUnboxByteArr; // 2b
                  @StringProperty String[] arrString; // 2b
                  @DocumentProperty Gift[] arrGift; // 2c
                  @EmbeddingProperty EmbeddingVector[] arrVec; // 2b
                  @BlobHandleProperty AppSearchBlobHandle[] arrBlob; // 2b

                  // Single values
                  @StringProperty String string; // 3a
                  @LongProperty Long boxLong; // 3a
                  @LongProperty long unboxLong; // 3b
                  @LongProperty Integer boxInteger; // 3a
                  @LongProperty int unboxInt; // 3b
                  @DoubleProperty Double boxDouble; // 3a
                  @DoubleProperty double unboxDouble; // 3b
                  @DoubleProperty Float boxFloat; // 3a
                  @DoubleProperty float unboxFloat; // 3b
                  @BooleanProperty Boolean boxBoolean; // 3a
                  @BooleanProperty boolean unboxBoolean; // 3b
                  @BytesProperty byte[] unboxByteArr; // 3a
                  @DocumentProperty Gift gift; // 3c
                  @EmbeddingProperty EmbeddingVector vec; // 3a
                  @BlobHandleProperty AppSearchBlobHandle blob; // 3a
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testToGenericDocument_allSupportedTypes_kotlin() {
        val compilation =
            compileKotlin(
                """
                import androidx.appsearch.app.AppSearchBlobHandle
                import androidx.appsearch.app.EmbeddingVector
                import androidx.appsearch.app.GenericDocument

                @Document
                data class Gift(
                  @Namespace val namespace: String,
                  @Id val id: String,

                  // Collections
                  @LongProperty val collectLong: Collection<Long>, // 1a
                  @LongProperty val collectInteger: Collection<Int>, // 1a
                  @LongProperty val collectNullInteger: Collection<Int?>, // 1a
                  @LongProperty val nullCollectInteger: Collection<Int>?, // 1a
                  @LongProperty val nullCollectNullInteger: Collection<Int?>?, // 1a
                  @DoubleProperty val collectDouble: Collection<Double>, // 1a
                  @DoubleProperty val collectFloat: Collection<Float>, // 1a
                  @BooleanProperty val collectBoolean: Collection<Boolean>, // 1a
                  @BytesProperty val collectByteArr: Collection<ByteArray>, // 1a
                  @StringProperty val collectString: Collection<String>, // 1b
                  @DocumentProperty val collectGift: Collection<Gift>, // 1c
                  @EmbeddingProperty val collectVec: Collection<EmbeddingVector>, // 1b
                  @BlobHandleProperty val collectBlob: Collection<AppSearchBlobHandle>, // 1b

                  // Arrays
                  @LongProperty val arrBoxLong: Array<Long>, // 2a
                  @LongProperty val arrUnboxLong: LongArray, // 2b
                  @LongProperty val arrBoxInteger: Array<Int>, // 2a
                  @LongProperty val arrNullBoxInteger: Array<Int?>, // 2a
                  @LongProperty val nullArrBoxInteger: Array<Int>?, // 2a
                  @LongProperty val nullArrNullBoxInteger: Array<Int?>?, // 2a
                  @LongProperty val arrUnboxInt: IntArray, // 2a
                  @DoubleProperty val arrBoxDouble: Array<Double>, // 2a
                  @DoubleProperty val arrUnboxDouble: DoubleArray, // 2b
                  @DoubleProperty val arrBoxFloat: Array<Float>, // 2a
                  @DoubleProperty val arrUnboxFloat: FloatArray, // 2a
                  @BooleanProperty val arrBoxBoolean: Array<Boolean>, // 2a
                  @BooleanProperty val arrUnboxBoolean: BooleanArray, // 2b
                  @BytesProperty val arrUnboxByteArr: Array<ByteArray>, // 2b
                  @StringProperty val arrString: Array<String>, // 2b
                  @DocumentProperty val arrGift: Array<Gift>, // 2c
                  @EmbeddingProperty val arrVec: Array<EmbeddingVector>, // 2b
                  @BlobHandleProperty val arrBlob: Array<AppSearchBlobHandle>, // 2b

                  // Single values
                  @StringProperty val string: String, // 3a
                  @LongProperty val nonNullLong: Long, // 3a
                  @LongProperty val nullLong: Long?, // 3b
                  @LongProperty val nonNullInteger: Int, // 3a
                  @DoubleProperty val nonNullDouble: Double, // 3a
                  @DoubleProperty val nonNullFloat: Float, // 3a
                  @BooleanProperty val nonNullBoolean: Boolean, // 3a
                  @BytesProperty val byteArr: ByteArray, // 3a
                  @DocumentProperty val gift: Gift, // 3c
                  @DocumentProperty val nullGift: Gift?, // 3c
                  @EmbeddingProperty val vec: EmbeddingVector, // 3a
                  @BlobHandleProperty val blob: AppSearchBlobHandle, // 3a
                )
                """
                    .trimIndent()
            )
        checkKotlinCompilation(compilation)
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testPropertyAnnotation_invalidType() {
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Namespace String namespace;
                  @Id String id;
                  @BooleanProperty String[] arrString;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "@BooleanProperty must only be placed on a getter/field of type or array or " +
                    "collection of boolean|java.lang.Boolean"
            )
    }

    @Test
    fun testToGenericDocument_invalidTypes() {
        var compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Namespace String namespace;
                  @Id String id;
                  @BytesProperty Collection<Byte[]> collectBoxByteArr;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "@BytesProperty must only be placed on a getter/field of type or array or " +
                    "collection of byte[]"
            )

        compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Namespace String namespace;
                  @Id String id;
                  @BytesProperty Collection<Byte> collectByte;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "@BytesProperty must only be placed on a getter/field of type or array or " +
                    "collection of byte[]"
            )

        compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Namespace String namespace;
                  @Id String id;
                  @BytesProperty Byte[][] arrBoxByteArr;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "@BytesProperty must only be placed on a getter/field of type or array or " +
                    "collection of byte[]"
            )
    }

    @Test
    fun testAllSpecialFields_field() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.CreationTimestampMillis long creationTs;
                  @Document.TtlMillis int ttlMs;
                  @Document.LongProperty int price;
                  @Document.Score int score;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testAllSpecialFields_getter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id private String id;
                  @Document.Score private int score;
                  @Document.CreationTimestampMillis private long creationTs;
                  @Document.TtlMillis private int ttlMs;
                  @Document.LongProperty private int price;
                  public String getId() { return id; }
                  public void setId(String id) { this.id = id; }
                  public String getNamespace() { return namespace; }
                  public void setNamespace(String namespace) {
                    this.namespace = namespace;
                  }
                  public int getScore() { return score; }
                  public void setScore(int score) { this.score = score; }
                  public long getCreationTs() { return creationTs; }
                  public void setCreationTs(int creationTs) {
                    this.creationTs = creationTs;
                  }
                  public int getTtlMs() { return ttlMs; }
                  public void setTtlMs(int ttlMs) { this.ttlMs = ttlMs; }
                  public int getPrice() { return price; }
                  public void setPrice(int price) { this.price = price; }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testMultipleNestedAutoValueDocument() {
        val compilation =
            compile(
                """
                import com.google.auto.value.AutoValue;
                import com.google.auto.value.AutoValue.*;
                @Document
                @AutoValue
                public abstract class Gift {
                  @CopyAnnotations @Document.Id abstract String id();
                  @CopyAnnotations @Document.Namespace abstract String namespace();
                  @CopyAnnotations
                  @Document.StringProperty abstract String property();
                  public static Gift create(String id, String namespace, String property) {
                    return new AutoValue_Gift(id, namespace, property);
                  }
                  @Document
                  @AutoValue
                  abstract static class B {
                    @CopyAnnotations @Document.Id abstract String id();
                    @CopyAnnotations @Document.Namespace abstract String namespace();
                    public static B create(String id, String namespace) {
                      return new AutoValue_Gift_B(id, namespace);
                    }
                  }
                  @Document
                  @AutoValue
                  abstract static class A {
                    @CopyAnnotations @Document.Namespace abstract String namespace();
                    @CopyAnnotations @Document.Id abstract String id();
                    public static A create(String id, String namespace) {
                      return new AutoValue_Gift_A(id, namespace);
                    }
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("AutoValue_Gift_A.java")
    }

    @Test
    fun testAutoValueDocument() {
        val compilation =
            compile(
                """
                import com.google.auto.value.AutoValue;
                import com.google.auto.value.AutoValue.*;
                @Document
                @AutoValue
                public abstract class Gift {
                  @CopyAnnotations @Document.Namespace abstract String namespace();
                  @CopyAnnotations @Document.Id abstract String id();
                  @CopyAnnotations
                  @Document.StringProperty abstract String property();
                  public static Gift create(String id, String namespace, String property) {
                    return new AutoValue_Gift(id, namespace, property);
                  }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("AutoValue_Gift.java")
        checkDocumentMapEqualsGolden(/* roundIndex= */ 0)
        // The number of rounds that the annotation processor takes can vary from setup to setup.
        // In this test case, AutoValue documents are processed in the second round because their
        // generated classes are not available in the first turn.
        checkDocumentMapEqualsGolden(/* roundIndex= */ 1)
    }

    @Test
    fun testAutoValueDocumentWithNormalDocument() {
        val compilation =
            compile(
                """
                import com.google.auto.value.AutoValue;
                import com.google.auto.value.AutoValue.*;
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                }
                @Document
                @AutoValue
                public abstract class Gift {
                  @CopyAnnotations @Document.Id abstract String id();
                  @CopyAnnotations @Document.Namespace abstract String namespace();
                  @CopyAnnotations
                  @Document.StringProperty abstract String property();
                  public static Gift create(String id, String namespace, String property) {
                    return new AutoValue_Gift(id, namespace, property);
                  }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("AutoValue_Gift.java")
        checkDocumentMapEqualsGolden(/* roundIndex= */ 0)
        // The number of rounds that the annotation processor takes can vary from setup to setup.
        // In this test case, AutoValue documents are processed in the second round because their
        // generated classes are not available in the first turn.
        checkDocumentMapEqualsGolden(/* roundIndex= */ 1)
    }

    @Test
    fun testInnerClass() {
        val compilation =
            compile(
                """
                import java.util.*;
                import androidx.appsearch.app.GenericDocument;
                public class Gift {
                  @Document
                  public static class InnerGift{
                    @Document.Namespace String namespace;
                    @Document.Id String id;
                    @StringProperty String[] arrString;
                  }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift$\$__InnerGift.java")
    }

    @Test
    fun testOneBadConstructor() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace private String mNamespace;
                  @Document.Id private String mId;
                  public Gift(String id, String namespace, boolean nonAppSearchParam){
                    mId = id;
                    mNamespace = namespace;
                  }
                  public Gift(String id) {
                    mId = id;
                  }
                  public String getId() {
                    return mId;
                  }
                  public String getNamespace() {
                    return mNamespace;
                  }
                  public void setNamespace(String namespace){
                    mNamespace = namespace;
                  }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testNestedDocumentsIndexing() {
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @Document.DocumentProperty(indexNestedProperties = true)
                  Collection<GiftContent> giftContentsCollection;
                  @Document.DocumentProperty(indexNestedProperties = true)
                  GiftContent[] giftContentsArray;
                  @Document.DocumentProperty(indexNestedProperties = true)
                  GiftContent giftContent;
                  @Document.DocumentProperty()
                  Collection<GiftContent> giftContentsCollectionNotIndexed;
                  @Document.DocumentProperty GiftContent[] giftContentsArrayNotIndexed;
                  @Document.DocumentProperty GiftContent giftContentNotIndexed;
                }

                @Document
                class GiftContent {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @Document.StringProperty String[] contentsArray;
                  @Document.StringProperty String contents;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testMultipleNesting() {
        val compilation =
            compile(
                """
                import java.util.*;
                @Document
                public class Gift {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @Document.DocumentProperty Middle middleContentA;
                  @Document.DocumentProperty Middle middleContentB;
                }

                @Document
                class Middle {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @Document.DocumentProperty Inner innerContentA;
                  @Document.DocumentProperty Inner innerContentB;
                }
                @Document
                class Inner {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @Document.StringProperty String contents;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")

        // Check that Gift contains Middle, Middle contains Inner, and Inner returns empty
        checkResultContains(
            /* className= */ "Gift.java",
            /* content= */ "classSet.add(Middle.class);\n    return classSet;",
        )
        checkResultContains(
            /* className= */ "Middle.java",
            /* content= */ "classSet.add(Inner.class);\n    return classSet;",
        )
        checkResultContains(
            /* className= */ "Inner.java",
            /* content= */ "return Collections.emptyList();",
        )
    }

    @Test
    fun testPolymorphism() {
        // Gift should automatically get "note2" via Java's "extends" semantics, but "note1" need
        // to be manually provided so that Parent1 can be a parent of Gift.
        val compilation =
            compile(
                """
                @Document
                class Parent1 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note1;
                }
                @Document
                class Parent2 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note2;
                }
                @Document(name = "Gift", parent = {Parent1.class, Parent2.class})
                class Gift extends Parent2 {
                  @Document.StringProperty String sender;
                  @Document.StringProperty String note1;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains("Gift.java", "addParentType($\$__AppSearch__Parent1.SCHEMA_NAME)")
        checkResultContains("Gift.java", "addParentType($\$__AppSearch__Parent2.SCHEMA_NAME)")

        checkEqualsGolden("Gift.java")
        checkDocumentMapEqualsGolden(/* roundIndex= */ 0)
    }

    @Test
    fun testPolymorphismOverrideExtendedProperty() {
        val compilation =
            compile(
                """
                @Document
                class Parent1 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note1;
                }
                @Document
                class Parent2 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(indexingType=2) String note2;
                }
                @Document(name = "Gift", parent = {Parent1.class, Parent2.class})
                class Gift extends Parent2 {
                  @Document.StringProperty String sender;
                  @Document.StringProperty String note1;
                  @Document.StringProperty(indexingType=1) String note2;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // Should expect the indexingType of note2 from Gift is 1, which is
        // INDEXING_TYPE_EXACT_TERMS, instead of 2.
        checkResultContains(
            "Gift.java",
            "setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)",
        )

        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testPolymorphismOverrideExtendedPropertyInvalid() {
        // Overridden properties cannot change the names.
        var compilation =
            compile(
                """
                @Document
                class Parent1 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note1;
                }
                @Document
                class Parent2 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty(name="note2", indexingType=2) String note2;
                }
                @Document(name = "Gift", parent = {Parent1.class, Parent2.class})
                class Gift extends Parent2 {
                  @Document.StringProperty String sender;
                  @Document.StringProperty String note1;
                  @Document.StringProperty(name="note2_new", indexingType=1) String note2;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "Property name within the annotation must stay consistent when " +
                    "overriding annotated members but changed from 'note2' -> 'note2_new'"
            )

        // Overridden properties cannot change the types.
        compilation =
            compile(
                """
                @Document
                class Parent1 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note1;
                }
                @Document
                class Parent2 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note2;
                }
                @Document(name = "Gift", parent = {Parent1.class, Parent2.class})
                class Gift extends Parent2 {
                  @Document.StringProperty String sender;
                  @Document.StringProperty String note1;
                  @LongProperty Long note2;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "Property type must stay consistent when overriding annotated " +
                    "members but changed from @StringProperty -> @LongProperty"
            )
    }

    @Test
    fun testPolymorphismWithNestedType() {
        val compilation =
            compile(
                """
                @Document
                class Parent1 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note1;
                }
                @Document
                class Parent2 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note2;
                }
                @Document(name = "Gift", parent = {Parent1.class, Parent2.class})
                class Gift extends Parent2 {
                  @Document.StringProperty String sender;
                  @Document.StringProperty String note1;
                  @Document.DocumentProperty Inner innerContent;
                }
                @Document
                class Inner {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @Document.StringProperty String contents;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        // Should see that both the parent types and nested types are added to the generated
        // getDependencyDocumentClasses method in Gift.
        checkResultContains("Gift.java", "classSet.add(Parent1.class)")
        checkResultContains("Gift.java", "classSet.add(Parent2.class)")
        checkResultContains("Gift.java", "classSet.add(Inner.class)")

        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testPolymorphismDuplicatedParents() {
        // Should see that every parent can only be added once.
        val compilation =
            compile(
                """
                @Document
                class Parent1 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note1;
                }
                @Document
                class Parent2 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note2;
                }
                @Document(name = "Gift", parent = {Parent1.class, Parent2.class, Parent1.class})
                class Gift extends Parent2 {
                  @Document.StringProperty String sender;
                  @Document.StringProperty String note1;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testPolymorphismChildTypeWithoutName() {
        val compilation =
            compile(
                """
                @Document
                class Parent {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String note;
                }
                @Document(parent = Parent.class)
                class Gift extends Parent {
                  @Document.StringProperty String sender;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "All @Document classes with a parent must explicitly provide a name"
            )
    }

    @Test
    fun testIndexableNestedPropertiesListSimple() {
        val compilation =
            compile(
                """
                @Document
                class Address {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty long streetNumber;
                  @Document.StringProperty String streetName;
                  @Document.StringProperty String state;
                  @Document.LongProperty long zipCode;
                }
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @Document.DocumentProperty(indexableNestedPropertiesList =
                    {"streetNumber", "streetName"}) Address livesAt;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains("Person.java", "addIndexableNestedProperties(\"streetNumber\")")
        checkResultContains("Person.java", "addIndexableNestedProperties(\"streetName\")")

        checkEqualsGolden("Person.java")
    }

    @Test
    fun testIndexableNestedPropertiesListEmpty() {
        val compilation =
            compile(
                """
                @Document
                class Address {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty long streetNumber;
                  @Document.StringProperty String streetName;
                  @Document.StringProperty String state;
                  @Document.LongProperty long zipCode;
                }
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @Document.DocumentProperty(indexableNestedPropertiesList = {})
                  Address livesAt;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultDoesNotContain("Person.java", "addIndexableNestedProperties")
        checkEqualsGolden("Person.java")
    }

    @Test
    fun testIndexableNestedPropertiesListInheritSuperclassTrue() {
        val compilation =
            compile(
                """
                @Document
                class Address {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty long streetNumber;
                  @Document.StringProperty String streetName;
                  @Document.StringProperty String state;
                  @Document.LongProperty long zipCode;
                }
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @Document.DocumentProperty(
                    indexableNestedPropertiesList = {"streetNumber", "streetName"})
                  Address livesAt;
                }
                @Document(name = "Artist", parent = {Person.class})
                class Artist extends Person {
                  @Document.StringProperty String mostFamousWork;
                  @Document.DocumentProperty(
                    indexableNestedPropertiesList = {"state"},
                    inheritIndexableNestedPropertiesFromSuperclass = true)
                  Address livesAt;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains("Artist.java", "addIndexableNestedProperties(\"streetNumber\")")
        checkResultContains("Artist.java", "addIndexableNestedProperties(\"streetName\")")
        checkResultContains("Artist.java", "addIndexableNestedProperties(\"state\")")

        checkEqualsGolden("Artist.java")
    }

    @Test
    fun testIndexableNestedPropertiesListInheritSuperclassFalse() {
        val compilation =
            compile(
                """
                @Document
                class Address {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty long streetNumber;
                  @Document.StringProperty String streetName;
                  @Document.StringProperty String state;
                  @Document.LongProperty long zipCode;
                }
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @Document.DocumentProperty(indexableNestedPropertiesList =
                    {"streetNumber", "streetName"}) Address livesAt;
                }
                @Document(name = "Artist", parent = {Person.class})
                class Artist extends Person {
                  @Document.StringProperty String mostFamousWork;
                  @Document.DocumentProperty(
                    indexableNestedPropertiesList = {"state"},
                    inheritIndexableNestedPropertiesFromSuperclass = false)
                  Address livesAt;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains("Artist.java", "addIndexableNestedProperties(\"state\")")
        checkResultDoesNotContain("Artist.java", "addIndexableNestedProperties(\"streetNumber\")")
        checkResultDoesNotContain("Artist.java", "addIndexableNestedProperties(\"streetName\")")

        checkEqualsGolden("Artist.java")
    }

    @Test
    fun testIndexableNestedPropertiesListInheritWithMultipleParentsClasses() {
        // Tests that the child class inherits nested properties from the parent correctly. When
        // set to true, the field overridden by the child class should only inherit indexable
        // nested properties form its java parent class (i.e. the superclass/interface which the
        // child class extends from/implements).
        // In this test case, Artist's parent class is Person, and ArtistEmployee's parent class
        // is Artist. This means that ArtistEmployee.livesAt's indexable list should contain the
        // properties s specified in Artist.livesAt. and Person.livesAt (since both Artist and
        // ArtistEmployee sets inheritFromParent=true for this field). ArtistEmployee.livesAt
        // should not inherit the indexable list from Employee.livesAt since Employee is not
        // ArtistEmployee's java class parent.
        val compilation =
            compile(
                """
                @Document
                class Address {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty long streetNumber;
                  @Document.StringProperty String streetName;
                  @Document.StringProperty String state;
                  @Document.LongProperty long zipCode;
                }
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @Document.DocumentProperty(indexableNestedPropertiesList =
                    {"streetNumber", "streetName"}) Address livesAt;
                }
                @Document(name = "Artist", parent = {Person.class})
                class Artist extends Person {
                  @Document.StringProperty String mostFamousWork;
                  @Document.DocumentProperty(
                    indexableNestedPropertiesList = {"state"},
                    inheritIndexableNestedPropertiesFromSuperclass = true)
                  Address livesAt;
                }
                @Document(name = "Employee", parent = {Person.class})
                class Employee extends Person {
                  @Document.DocumentProperty(
                    indexableNestedPropertiesList = {"zipCode"},
                    inheritIndexableNestedPropertiesFromSuperclass = true)
                  Address livesAt;
                  @Document.DocumentProperty(
                    indexableNestedPropertiesList = {"zipCode", "streetName"})
                  Address worksAt;
                }
                @Document(name = "ArtistEmployee", parent = {Artist.class,Employee.class})
                class ArtistEmployee extends Artist {
                  @Document.StringProperty String mostFamousWork;
                  @Document.DocumentProperty(
                    inheritIndexableNestedPropertiesFromSuperclass = true)
                  Address livesAt;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains("ArtistEmployee.java", "addIndexableNestedProperties(\"streetNumber\")")
        checkResultContains("ArtistEmployee.java", "addIndexableNestedProperties(\"streetName\")")
        checkResultContains("ArtistEmployee.java", "addIndexableNestedProperties(\"state\")")
        // ArtistEmployee's indexable list should not contain 'zipCode' as  ArtistEmployee only
        // extends Artist, which does not index zipCode
        checkResultDoesNotContain(
            "ArtistEmployee.java",
            "addIndexableNestedProperties(\"zipCode\")",
        )

        checkEqualsGolden("ArtistEmployee.java")
    }

    @Test
    fun testIndexableNestedPropertiesListImplicitInheritance() {
        // Tests that properties that are not declared in the child class itself but exists in
        // the class due to java class inheritance indexes the correct indexable list.
        // Artist.livesAt should be defined for Artist and index the same indexable properties as
        // Person.livesAt.
        val compilation =
            compile(
                """
                @Document
                class Address {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty long streetNumber;
                  @Document.StringProperty String streetName;
                  @Document.StringProperty String state;
                  @Document.LongProperty long zipCode;
                }
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @Document.DocumentProperty(indexableNestedPropertiesList =
                    {"streetNumber", "streetName"}) Address livesAt;
                }
                @Document(name = "Artist", parent = {Person.class})
                class Artist extends Person {
                  @Document.StringProperty String mostFamousWork;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains("Artist.java", "addIndexableNestedProperties(\"streetNumber\")")
        checkResultContains("Artist.java", "addIndexableNestedProperties(\"streetName\")")

        checkResultDoesNotContain("Artist.java", "addIndexableNestedProperties(\"zipCode\")")
        checkResultDoesNotContain("Artist.java", "addIndexableNestedProperties(\"state\")")

        checkEqualsGolden("Artist.java")
    }

    @Test
    fun testIndexableNestedPropertiesListImplicitlyInheritFromMultipleLevels() {
        // Tests that the indexable list is inherited correctly across multiple java inheritance
        // levels.
        // ArtistEmployee.livesAt should index the nested properties defined in Person.livesAt.
        val compilation =
            compile(
                """
                @Document
                class Address {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty long streetNumber;
                  @Document.StringProperty String streetName;
                  @Document.StringProperty String state;
                  @Document.LongProperty long zipCode;
                }
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @Document.DocumentProperty(indexableNestedPropertiesList =
                    {"streetNumber", "streetName"}) Address livesAt;
                }
                @Document(name = "Artist", parent = {Person.class})
                class Artist extends Person {
                  @Document.StringProperty String mostFamousWork;
                }
                @Document(name = "ArtistEmployee", parent = {Artist.class})
                class ArtistEmployee extends Artist {
                  @Document.StringProperty String worksAt;
                  @Document.DocumentProperty(
                    inheritIndexableNestedPropertiesFromSuperclass = true)
                  Address livesAt;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains("ArtistEmployee.java", "addIndexableNestedProperties(\"streetNumber\")")
        checkResultContains("ArtistEmployee.java", "addIndexableNestedProperties(\"streetName\")")

        checkResultDoesNotContain(
            "ArtistEmployee.java",
            "addIndexableNestedProperties(\"zipCode\")",
        )
        checkResultDoesNotContain("ArtistEmployee.java", "addIndexableNestedProperties(\"state\")")

        checkEqualsGolden("ArtistEmployee.java")
    }

    @Test
    fun testIndexableNestedPropertiesListTopLevelInheritTrue() {
        val compilation =
            compile(
                """
                @Document
                class Address {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty long streetNumber;
                  @Document.StringProperty String streetName;
                  @Document.StringProperty String state;
                  @Document.LongProperty long zipCode;
                }
                @Document
                class Person {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @Document.DocumentProperty(indexableNestedPropertiesList =
                    {"streetNumber", "streetName"},
                    inheritIndexableNestedPropertiesFromSuperclass = true)
                  Address livesAt;
                }
                @Document(name = "Artist", parent = {Person.class})
                class Artist extends Person {
                  @Document.StringProperty String mostFamousWork;
                  @Document.DocumentProperty(indexableNestedPropertiesList =
                    {"state"}, inheritIndexableNestedPropertiesFromSuperclass = true)
                  Address livesAt;
                }
                @Document(name = "ArtistEmployee", parent = {Artist.class})
                class ArtistEmployee extends Artist {
                  @Document.StringProperty String worksAt;
                  @Document.DocumentProperty(
                    inheritIndexableNestedPropertiesFromSuperclass = true)
                  Address livesAt;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains("ArtistEmployee.java", "addIndexableNestedProperties(\"streetNumber\")")
        checkResultContains("ArtistEmployee.java", "addIndexableNestedProperties(\"streetName\")")
        checkResultContains("ArtistEmployee.java", "addIndexableNestedProperties(\"state\")")

        checkResultDoesNotContain(
            "ArtistEmployee.java",
            "addIndexableNestedProperties(\"zipCode\")",
        )

        checkEqualsGolden("ArtistEmployee.java")
    }

    @Test
    fun testAnnotationOnClassGetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty public int getPrice() { return 0; }
                  public void setPrice(int price) {}
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"price\")",
        )
        checkResultContains("Gift.java", "document.setPrice(getPriceConv)")
        checkResultContains("Gift.java", "document.getPrice()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testAnnotationOnClassGetterUsingFactory() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  private Gift() {}
                  public static Gift create(String id, String namespace, int price) {
                    return new Gift();
                  }
                  @Document.Namespace public String getNamespace() { return "hi"; }
                  @Document.Id public String getId() { return "0"; }
                  @Document.LongProperty public int getPrice() { return 0; }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"price\")",
        )
        checkResultContains("Gift.java", "Gift.create(getIdConv, getNamespaceConv, getPriceConv)")
        checkResultContains("Gift.java", "document.getPrice()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testAnnotationOnInterfaceGetter() {
        val compilation =
            compile(
                """
                @Document
                public interface Gift {
                  public static Gift create(String id, String namespace) {
                    return new GiftImpl(id, namespace);
                  }
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.LongProperty public int getPrice();
                  public void setPrice(int price);
                }
                class GiftImpl implements Gift{
                  public GiftImpl(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                  private String namespace;
                  private String id;
                  private int price;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                  public int getPrice() { return price; }
                  public void setPrice(int price) { this.price = price; }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"price\")",
        )
        checkResultContains("Gift.java", "Gift.create(getIdConv, getNamespaceConv)")
        checkResultContains("Gift.java", "document.setPrice(getPriceConv)")
        checkResultContains("Gift.java", "document.getPrice()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testAnnotationOnGetterWithoutFactory() {
        // An interface without any factory method is not able to initialize, as interfaces do
        // not have constructors.
        val compilation =
            compile(
                """
                @Document
                public interface Gift {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.LongProperty public int getPrice();
                  public void setPrice(int price);
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Could not find a suitable creation method")
    }

    @Test
    fun testAnnotationOnGetterWithoutSetter() {
        val compilation =
            compile(
                """
                @Document
                public interface Gift {
                  public static Gift create(String id, String namespace) {
                    return new GiftImpl(id, namespace);
                  }
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.LongProperty public int getPrice();
                }
                class GiftImpl implements Gift{
                  public GiftImpl(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                  private String namespace;
                  private String id;
                  private int price;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                  public int getPrice() { return price; }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Cannot use this creation method to construct the class: " +
                    "\"com.example.appsearch.Gift\". " +
                    "No parameters for the properties: [getPrice]")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Could not find any of the setter(s): " +
                    "[public] void namespace(java.lang.String)|" +
                    "[public] void setNamespace(java.lang.String)")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Could not find any of the setter(s): " +
                    "[public] void id(java.lang.String)|" +
                    "[public] void setId(java.lang.String)")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Could not find any of the setter(s): " +
                    "[public] void price(int)|" +
                    "[public] void setPrice(int)")
            )
    }

    @Test
    fun testInterfaceAsNestedDocument() {
        val compilation =
            compile(
                """
                @Document
                interface Thing {
                  public static Thing create(String id, String namespace) {
                    return new ThingImpl(id, namespace);
                  }
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                }
                class ThingImpl implements Thing {
                  public ThingImpl(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                  private String namespace;
                  private String id;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                }
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.DocumentProperty Thing thing;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Thing.java",
            "Thing document = Thing.create(getIdConv, getNamespaceConv)",
        )
        checkResultContains(
            "Gift.java",
            "thingConv = thingCopy.toDocumentClass(Thing.class, documentClassMappingContext)",
        )
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testInterfaceImplementingParents() {
        val compilation =
            compile(
                """
                @Document
                interface Root {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  public static Root create(String id, String namespace) {
                    return new GiftImpl();
                  }
                }
                @Document(name="Parent1", parent=Root.class)
                interface Parent1 extends Root {
                  @Document.StringProperty public String getStr1();
                  public static Parent1 create(String id, String namespace, String str1) {
                    return new GiftImpl();
                  }
                }
                @Document(name="Parent2", parent=Root.class)
                interface Parent2 extends Root {
                  @Document.StringProperty public String getStr2();
                  public static Parent2 create(String id, String namespace, String str2) {
                    return new GiftImpl();
                  }
                }
                @Document(name="Gift", parent={Parent1.class, Parent2.class})
                public interface Gift extends Parent1, Parent2 {
                  public static Gift create(
                      String id, String namespace, String str1, String str2, int price) {
                    return new GiftImpl();
                  }
                  @Document.LongProperty public int getPrice();
                }
                class GiftImpl implements Gift{
                  public GiftImpl() {}
                  public String getNamespace() { return "namespace"; }
                  public String getId() { return "id"; }
                  public String getStr1() { return "str1"; }
                  public String getStr2() { return "str2"; }
                  public int getPrice() { return 0; }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.StringPropertyConfig.Builder(\"str1\")",
        )
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.StringPropertyConfig.Builder(\"str2\")",
        )
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"price\")",
        )
        checkResultContains(
            "Gift.java",
            "Gift.create(getIdConv, getNamespaceConv, getStr1Conv, getStr2Conv, getPriceConv)",
        )
        checkResultContains("Gift.java", "document.getStr1()")
        checkResultContains("Gift.java", "document.getStr2()")
        checkResultContains("Gift.java", "document.getPrice()")
        checkEqualsGolden("Gift.java")
        checkDocumentMapEqualsGolden(/* roundIndex= */ 0)
    }

    @Test
    fun testSameNameGetterAndFieldAnnotatingGetter() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty public int getPrice() { return 0; }
                  public int price;
                  public void setPrice(int price) {}
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"price\")",
        )
        checkResultContains("Gift.java", "document.setPrice(getPriceConv)")
        checkResultContains("Gift.java", "document.getPrice()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testSameNameGetterAndFieldAnnotatingField() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty public int price;
                  public int getPrice() { return 0; }
                  public void setPrice(int price) {}
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"price\")",
        )
        checkResultContains("Gift.java", "document.price = priceConv")
        checkResultContains("Gift.java", "builder.setPropertyLong(\"price\", document.price)")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testSameNameGetterAndFieldAnnotatingBoth() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty(name="price1")
                  public int getPrice() { return 0; }
                  public void setPrice(int price) {}
                  @Document.LongProperty(name="price2")
                  public int price;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                ("Normalized name \"price\" is already taken up by pre-existing " +
                    "int Gift#getPrice(). " +
                    "Please rename this getter/field to something else.")
            )
    }

    @Test
    fun testSameNameGetterAndFieldAnnotatingBothButGetterIsPrivate() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty(name="price1")
                  private int getPrice() { return 0; }
                  public void setPrice(int price) {}
                  @Document.LongProperty(name="price2")
                  public int price;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Failed to find a suitable getter for element \"getPrice\"")
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Getter cannot be used: private visibility")
    }

    @Test
    fun testNameNormalization() {
        // getMPrice should correspond to a field named "mPrice"
        // mPrice should correspond to a field named "price"
        // isSold should correspond to a field named "sold"
        // mx should correspond to a field named "mx"
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty
                  public int getMPrice() { return 0; }
                  public void setMPrice(int price) {}
                  @Document.LongProperty
                  public int mPrice;
                  @Document.BooleanProperty
                  public boolean isSold() { return false; }
                  public void setSold(boolean sold) {}
                  @Document.LongProperty
                  public int mx() { return 0; }
                  public void setMx(int x) {}
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()

        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"mPrice\")",
        )
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"price\")",
        )
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.BooleanPropertyConfig.Builder(\"sold\")",
        )
        checkResultContains("Gift.java", "new AppSearchSchema.LongPropertyConfig.Builder(\"mx\")")

        checkResultContains("Gift.java", "document.setMPrice(getMPriceConv)")
        checkResultContains("Gift.java", "document.mPrice = mPriceConv")
        checkResultContains("Gift.java", "document.setSold(isSoldConv)")
        checkResultContains("Gift.java", "document.setMx(mxConv)")

        checkResultContains(
            "Gift.java",
            "builder.setPropertyLong(\"mPrice\", document.getMPrice())",
        )
        checkResultContains("Gift.java", "builder.setPropertyLong(\"price\", document.mPrice)")
        checkResultContains("Gift.java", "builder.setPropertyBoolean(\"sold\", document.isSold())")
        checkResultContains("Gift.java", "builder.setPropertyLong(\"mx\", document.mx())")

        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testGetterWithParameterCannotBeUsed() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty
                  public int getPrice(int price) { return 0; }
                  public void setPrice(int price) {}
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Failed to find a suitable getter for element \"getPrice\"")
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Getter cannot be used: should take no parameters")
    }

    @Test
    fun testPrivateGetterCannotBeUsed() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty
                  private int getPrice() { return 0; }
                  public void setPrice(int price) {}
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Failed to find a suitable getter for element \"getPrice\"")
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining("Getter cannot be used: private visibility")
    }

    @Test
    fun testOverloadedGetterIsOk() {
        // Overloaded getter should be ok because annotation processor will find the correct getter
        // that can be used.
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  public int getPrice(int price) { return 0; }
                  @Document.LongProperty
                  public int getPrice() { return 0; }
                  public void setPrice(int price) {}
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "new AppSearchSchema.LongPropertyConfig.Builder(\"price\")",
        )
        checkResultContains("Gift.java", "document.setPrice(getPriceConv)")
        checkResultContains("Gift.java", "document.getPrice()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testGetterWithWrongReturnType() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty
                  public int getPrice() { return 0; }
                  public void setPrice(int price) {}
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "@StringProperty must only be placed on a getter/field of type or array or " +
                    "collection of java.lang.String"
            )
    }

    @Test
    fun testCyclicalSchema() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @Document.DocumentProperty Letter letter;
                }

                @Document
                class Letter {
                  @Document.Id String id;
                  @Document.Namespace String namespace;
                  @Document.DocumentProperty Gift gift;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")

        checkResultContains("Gift.java", "classSet.add(Letter.class);\n    return classSet;")
        checkResultContains("Letter.java", "classSet.add(Gift.class);\n    return classSet;")
    }

    @Test
    fun testCreationByBuilder() {
        // Once @Document.BuilderProducer is found, AppSearch compiler will no longer consider other
        // creation method, so "create" will not be used.
        val compilation =
            compile(
                """
                @Document
                public interface Gift {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.LongProperty public int getPrice();
                  public static Gift create(String id, String namespace, int price) {
                    return new GiftImpl(id, namespace, price);
                  }
                  @Document.BuilderProducer static GiftBuilder getBuilder() {
                    return new GiftBuilder();
                  }
                }
                class GiftImpl implements Gift {
                  public GiftImpl(String id, String namespace, int price) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  private String namespace;
                  private String id;
                  private int price;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                  public int getPrice() { return price; }
                }
                class GiftBuilder {
                  private String namespace;
                  private String id;
                  private int price;
                  public GiftBuilder setNamespace(String namespace) {
                    this.namespace = namespace;
                    return this;
                  }
                  public GiftBuilder setId(String id) {
                    this.id = id;
                    return this;
                  }
                  public GiftBuilder setPrice(int price) {
                    this.price = price;
                    return this;
                  }
                  public Gift build() {
                    return new GiftImpl(this.id, this.namespace, this.price);
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("Gift.java", "GiftBuilder builder = Gift.getBuilder()")
        checkResultContains("Gift.java", "builder.setNamespace(getNamespaceConv)")
        checkResultContains("Gift.java", "builder.setId(getIdConv)")
        checkResultContains("Gift.java", "builder.setPrice(getPriceConv)")
        checkResultContains("Gift.java", "builder.build()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testBuilderThatUsesGenerics() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace private final String mNamespace;
                  @Document.Id private final String mId;
                  private Gift(String namespace, String id) {
                    mNamespace = namespace;
                    mId = id;
                  }
                  public String getNamespace() { return mNamespace; }
                  public String getId() { return mId; }
                  public static abstract class BaseBuilder<T> {
                    public final T build() { return buildInternal(false); }
                    // Give this a param to have zero methods with the signature
                    // () -> DocumentClass
                    protected abstract T buildInternal(boolean ignore);
                  }
                  @Document.BuilderProducer
                  public static class Builder extends BaseBuilder<Gift> {
                    private String mNamespace = "";
                    private String mId = "";
                    @Override
                    protected Gift buildInternal(boolean ignore) {
                      return new Gift(mNamespace, mId);
                    }
                    public Builder setNamespace(String namespace) {
                      mNamespace = namespace;
                      return this;
                    }
                    public Builder setId(String id) {
                      mId = id;
                      return this;
                    }
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("Gift.java", "Gift.Builder builder = new Gift.Builder()")
        checkResultContains("Gift.java", "return builder.build()")
    }

    @Test
    fun testCreationByBuilderWithParameter() {
        val compilation =
            compile(
                """
                @Document
                public interface Gift {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.LongProperty public int getPrice();
                  @Document.BuilderProducer static GiftBuilder getBuilder(int price) {
                    return new GiftBuilder().setPrice(price);
                  }
                }
                class GiftImpl implements Gift{
                  public GiftImpl(String id, String namespace, int price) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  private String namespace;
                  private String id;
                  private int price;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                  public int getPrice() { return price; }
                }
                class GiftBuilder {
                  private String namespace;
                  private String id;
                  private int price;
                  public GiftBuilder setNamespace(String namespace) {
                    this.namespace = namespace;
                    return this;
                  }
                  public GiftBuilder setId(String id) {
                    this.id = id;
                    return this;
                  }
                  public GiftBuilder setPrice(int price) {
                    this.price = price;
                    return this;
                  }
                  public Gift build() {
                    return new GiftImpl(this.id, this.namespace, this.price);
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("Gift.java", "GiftBuilder builder = Gift.getBuilder(getPriceConv)")
        checkResultContains("Gift.java", "builder.setNamespace(getNamespaceConv)")
        checkResultContains("Gift.java", "builder.setId(getIdConv)")
        checkResultContains("Gift.java", "builder.build()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testCreationByBuilderAnnotatingBuilderClass() {
        val compilation =
            compile(
                """
                @Document
                public interface Gift {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.LongProperty public int getPrice();
                  @Document.BuilderProducer
                  class GiftBuilder {
                    private String namespace;
                    private String id;
                    private int price;
                    public GiftBuilder setNamespace(String namespace) {
                      this.namespace = namespace;
                      return this;
                    }
                    public GiftBuilder setId(String id) {
                      this.id = id;
                      return this;
                    }
                    public GiftBuilder setPrice(int price) {
                      this.price = price;
                      return this;
                    }
                    public Gift build() {
                      return new GiftImpl(this.id, this.namespace, this.price);
                    }
                  }
                }
                class GiftImpl implements Gift {
                  public GiftImpl(String id, String namespace, int price) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  private String namespace;
                  private String id;
                  private int price;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                  public int getPrice() { return price; }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("Gift.java", "Gift.GiftBuilder builder = new Gift.GiftBuilder()")
        checkResultContains("Gift.java", "builder.setNamespace(getNamespaceConv)")
        checkResultContains("Gift.java", "builder.setId(getIdConv)")
        checkResultContains("Gift.java", "builder.setPrice(getPriceConv)")
        checkResultContains("Gift.java", "builder.build()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testCreationByBuilderWithParameterAnnotatingBuilderClass() {
        val compilation =
            compile(
                """
                @Document
                public interface Gift {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.LongProperty public int getPrice();
                  @Document.BuilderProducer
                  class GiftBuilder {
                    private String namespace;
                    private String id;
                    private int price;
                    public GiftBuilder(int price) {
                      this.price = price;
                    }
                    public GiftBuilder setNamespace(String namespace) {
                      this.namespace = namespace;
                      return this;
                    }
                    public GiftBuilder setId(String id) {
                      this.id = id;
                      return this;
                    }
                    public Gift build() {
                      return new GiftImpl(this.id, this.namespace, this.price);
                    }
                  }
                }
                class GiftImpl implements Gift {
                  public GiftImpl(String id, String namespace, int price) {
                    this.id = id;
                    this.namespace = namespace;
                    this.price = price;
                  }
                  private String namespace;
                  private String id;
                  private int price;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                  public int getPrice() { return price; }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "Gift.GiftBuilder builder = new Gift.GiftBuilder(getPriceConv)",
        )
        checkResultContains("Gift.java", "builder.setNamespace(getNamespaceConv)")
        checkResultContains("Gift.java", "builder.setId(getIdConv)")
        checkResultContains("Gift.java", "builder.build()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testCreationByBuilderOnly() {
        // Once a builder producer is provided, AppSearch will only use the builder pattern, even
        // if another creation method is available.
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty int price;
                  @Document.BuilderProducer static GiftBuilder getBuilder() {
                    return new GiftBuilder();
                  }
                }
                class GiftBuilder {
                  private String namespace;
                  private String id;
                  private int price;
                  public GiftBuilder setNamespace(String namespace) {
                    this.namespace = namespace;
                    return this;
                  }
                  public GiftBuilder setId(String id) {
                    this.id = id;
                    return this;
                  }
                  public GiftBuilder setPrice(int price) {
                    this.price = price;
                    return this;
                  }
                  public Gift build() {
                    return new Gift();
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("Gift.java", "GiftBuilder builder = Gift.getBuilder()")
        checkResultContains("Gift.java", "builder.setNamespace(namespaceConv)")
        checkResultContains("Gift.java", "builder.setId(idConv)")
        checkResultContains("Gift.java", "builder.setPrice(priceConv)")
        checkResultContains("Gift.java", "builder.build()")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testCreationByBuilderWithAutoValue() {
        val compilation =
            compile(
                """
                import com.google.auto.value.AutoValue;
                import com.google.auto.value.AutoValue.*;
                @Document
                @AutoValue
                public abstract class Gift {
                  @CopyAnnotations @Document.Id abstract String id();
                  @CopyAnnotations @Document.Namespace abstract String namespace();
                  @CopyAnnotations @Document.LongProperty abstract int price();
                  @Document.BuilderProducer static GiftBuilder getBuilder() {
                    return new GiftBuilder();
                  }
                }
                class GiftBuilder {
                  private String namespace;
                  private String id;
                  private int price;
                  public GiftBuilder setNamespace(String namespace) {
                    this.namespace = namespace;
                    return this;
                  }
                  public GiftBuilder setId(String id) {
                    this.id = id;
                    return this;
                  }
                  public GiftBuilder setPrice(int price) {
                    this.price = price;
                    return this;
                  }
                  public Gift build() {
                    return new AutoValue_Gift(id, namespace, price);
                  }
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("AutoValue_Gift.java", "GiftBuilder builder = Gift.getBuilder()")
        checkResultContains("AutoValue_Gift.java", "builder.setNamespace(namespaceConv)")
        checkResultContains("AutoValue_Gift.java", "builder.setId(idConv)")
        checkResultContains("AutoValue_Gift.java", "builder.setPrice(priceConv)")
        checkResultContains("AutoValue_Gift.java", "builder.build()")
        checkEqualsGolden("AutoValue_Gift.java")
    }

    @Test
    fun testCreationByBuilderErrors() {
        // Cannot have multiple builder producer
        var compilation =
            compile(
                """
                @Document
                public interface Gift {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.BuilderProducer static GiftBuilder getBuilder1() {
                    return new GiftBuilder();
                  }
                  @Document.BuilderProducer static GiftBuilder getBuilder2() {
                    return new GiftBuilder();
                  }
                }
                class GiftImpl implements Gift{
                  public GiftImpl(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                  private String namespace;
                  private String id;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                }
                class GiftBuilder {
                  private String namespace;
                  private String id;
                  public GiftBuilder setNamespace(String namespace) {
                    this.namespace = namespace;
                    return this;
                  }
                  public GiftBuilder setId(String id) {
                    this.id = id;
                    return this;
                  }
                  public Gift build() {
                    return new GiftImpl(this.id, this.namespace);
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Found duplicated builder producer")

        // Builder producer method must be static
        compilation =
            compile(
                """
                @Document
                public interface Gift {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.BuilderProducer GiftBuilder getBuilder() {
                    return new GiftBuilder();
                  }
                }
                class GiftImpl implements Gift{
                  public GiftImpl(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                  private String namespace;
                  private String id;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                }
                class GiftBuilder {
                  private String namespace;
                  private String id;
                  public GiftBuilder setNamespace(String namespace) {
                    this.namespace = namespace;
                    return this;
                  }
                  public GiftBuilder setId(String id) {
                    this.id = id;
                    return this;
                  }
                  public Gift build() {
                    return new GiftImpl(this.id, this.namespace);
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Builder producer must be static")

        // Builder producer class must be static
        compilation =
            compile(
                """
                @Document
                public class Gift {
                  public Gift(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                  @Document.Namespace public String namespace;
                  @Document.Id public String id;
                  @Document.BuilderProducer
                  class Builder {
                    private String namespace;
                    private String id;
                    public Builder setNamespace(String namespace) {
                      this.namespace = namespace;
                      return this;
                    }
                    public Builder setId(String id) {
                      this.id = id;
                      return this;
                    }
                    public Gift build() {
                      return new Gift(this.id, this.namespace);
                    }
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Builder producer must be static")

        // Builder producer method cannot be private
        compilation =
            compile(
                """
                @Document
                public interface Gift {
                  @Document.Namespace public String getNamespace();
                  @Document.Id public String getId();
                  @Document.BuilderProducer private static GiftBuilder getBuilder() {
                    return new GiftBuilder();
                  }
                }
                class GiftImpl implements Gift{
                  public GiftImpl(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                  private String namespace;
                  private String id;
                  public String getNamespace() { return namespace; }
                  public String getId() { return id; }
                }
                class GiftBuilder {
                  private String namespace;
                  private String id;
                  public GiftBuilder setNamespace(String namespace) {
                    this.namespace = namespace;
                    return this;
                  }
                  public GiftBuilder setId(String id) {
                    this.id = id;
                    return this;
                  }
                  public Gift build() {
                    return new GiftImpl(this.id, this.namespace);
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Builder producer cannot be private")

        // Builder producer class cannot be private
        compilation =
            compile(
                """
                @Document
                public class Gift {
                  public Gift(String id, String namespace) {
                    this.id = id;
                    this.namespace = namespace;
                  }
                  @Document.Namespace public String namespace;
                  @Document.Id public String id;
                  @Document.BuilderProducer
                  private static class Builder {
                    private String namespace;
                    private String id;
                    public Builder setNamespace(String namespace) {
                      this.namespace = namespace;
                      return this;
                    }
                    public Builder setId(String id) {
                      this.id = id;
                      return this;
                    }
                    public Gift build() {
                      return new Gift(this.id, this.namespace);
                    }
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Builder producer cannot be private")

        // Builder producer must be a method or a class.
        compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace public String namespace;
                  @Document.Id public String id;
                  @Document.BuilderProducer int getBuilder;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("annotation interface not applicable to this kind of declaration")

        // Missing a setter in the builder
        compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty int price;
                  @Document.BuilderProducer static GiftBuilder getBuilder() {
                    return new GiftBuilder();
                  }
                }
                class GiftBuilder {
                  private String namespace;
                  private String id;
                  private int price;
                  public GiftBuilder setNamespace(String namespace) {
                    this.namespace = namespace;
                    return this;
                  }
                  public GiftBuilder setId(String id) {
                    this.id = id;
                    return this;
                  }
                  public Gift build() {
                    return new Gift();
                  }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                ("Could not find a suitable builder producer for " +
                    "\"com.example.appsearch.Gift\" that covers properties: [price]. " +
                    "See the warnings for more details.")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Could not find any of the setter(s): " +
                    "[public] void price(int)|" +
                    "[public] void setPrice(int)")
            )
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                ("Cannot use this creation method to construct the class: " +
                    "\"com.example.appsearch.Gift\". " +
                    "No parameters for the properties: [price]")
            )
    }

    @Test
    fun testAbstractConstructor() {
        val compilation =
            compile(
                """
                @Document
                public abstract class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  public Gift() {}
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining("Could not find a suitable creation method")
        CompilationSubject.assertThat(compilation)
            .hadWarningContaining(
                "Method cannot be used to create a document class: abstract constructor"
            )
    }

    @Test
    fun testDocumentClassesWithDuplicatedNames() {
        val compilation =
            compile(
                """
                @Document(name="A")
                class MyClass1 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                }
                @Document(name="A")
                class MyClass2 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                }
                @Document(name="B")
                class MyClass3 {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkDocumentMapEqualsGolden(/* roundIndex= */ 0)
    }

    @Test
    fun testStringSerializer() {
        val compilation =
            compile(
                """
                import androidx.appsearch.app.StringSerializer;
                import java.net.URL;
                import java.net.MalformedURLException;
                import java.util.List;
                @Document
                class Gift {
                    @Document.Id String mId;
                    @Document.Namespace String mNamespace;
                    @Document.StringProperty(
                        serializer = UrlAsStringSerializer.class
                    )
                    URL mUrl;
                    @Document.StringProperty(
                        serializer = UrlAsStringSerializer.class
                    )
                    List<URL> mUrlList;
                    @Document.StringProperty(
                        serializer = UrlAsStringSerializer.class
                    )
                    URL[] mUrlArr;
                    static class UrlAsStringSerializer
                            implements StringSerializer<URL> {
                        @Override
                        public String serialize(URL url) {
                            return url.toString();
                        }
                        @Override
                        public URL deserialize(String string) {
                            try {
                                return new URL(string);
                            } catch (MalformedURLException e) {
                                return null;
                            }
                        }
                    }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
        checkResultContains(
            "Gift.java",
            "Gift.UrlAsStringSerializer serializer = new Gift.UrlAsStringSerializer()",
        )
        checkResultContains("Gift.java", "String mUrlConv = serializer.serialize(mUrlCopy)")
        checkResultContains(
            "Gift.java",
            "mUrlConv = new Gift.UrlAsStringSerializer().deserialize(mUrlCopy)",
        )
        checkResultContains("Gift.java", "mUrlListConv[i++] = serializer.serialize(item)")
        checkResultContains("Gift.java", "URL elem = serializer.deserialize(mUrlListCopy[i])")
        checkResultContains("Gift.java", "mUrlArrConv[i] = serializer.serialize(mUrlArrCopy[i])")
        checkResultContains("Gift.java", "URL elem = serializer.deserialize(mUrlArrCopy[i])")
    }

    @Test
    fun testLongSerializer() {
        val compilation =
            compile(
                """
                import androidx.appsearch.app.LongSerializer;
                import java.util.Arrays;
                import java.util.List;
                @Document
                class Gift {
                    @Document.Id String mId;
                    @Document.Namespace String mNamespace;
                    @Document.LongProperty(
                        serializer = PricePointAsOrdinalSerializer.class
                    )
                    PricePoint mPricePoint;
                    @Document.LongProperty(
                        serializer = PricePointAsOrdinalSerializer.class
                    )
                    List<PricePoint> mPricePointList;
                    @Document.LongProperty(
                        serializer = PricePointAsOrdinalSerializer.class
                    )
                    PricePoint[] mPricePointArr;
                    enum PricePoint { LOW, MID, HIGH }
                    static class PricePointAsOrdinalSerializer
                            implements LongSerializer<PricePoint> {
                        @Override
                        public long serialize(PricePoint pricePoint) {
                            return pricePoint.ordinal();
                        }
                        @Override
                        public PricePoint deserialize(long l) {
                            return Arrays.stream(PricePoint.values())
                                    .filter(pp -> pp.ordinal() == l)
                                    .findFirst()
                                    .orElse(null);
                        }
                    }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkEqualsGolden("Gift.java")
        checkResultContains(
            "Gift.java",
            "Gift.PricePointAsOrdinalSerializer serializer = " +
                "new Gift.PricePointAsOrdinalSerializer()",
        )
        checkResultContains(
            "Gift.java",
            "mPricePointConv = " +
                "new Gift.PricePointAsOrdinalSerializer().deserialize(mPricePointCopy)",
        )
        checkResultContains(
            "Gift.java",
            "long mPricePointConv = serializer.serialize(mPricePointCopy)",
        )
        checkResultContains(
            "Gift.java",
            "Gift.PricePoint elem = serializer.deserialize(mPricePointListCopy[i])",
        )
        checkResultContains("Gift.java", "mPricePointListConv[i++] = serializer.serialize(item)")
        checkResultContains(
            "Gift.java",
            "mPricePointArrConv[i] = serializer.serialize(mPricePointArrCopy[i])",
        )
        checkResultContains(
            "Gift.java",
            "Gift.PricePoint elem = serializer.deserialize(mPricePointArrCopy[i])",
        )
    }

    @Test
    fun testSerializerWithoutDefaultConstructor() {
        val compilation =
            compile(
                """
                import androidx.appsearch.app.LongSerializer;
                import java.time.Instant;
                @Document
                class Gift {
                    @Document.Id
                    String mId = null;
                    @Document.Namespace
                    String mNamespace = null;
                    @Document.LongProperty(
                        serializer = InstantAsEpochMillisSerializer.class
                    )
                    Instant mPurchaseTimeStamp = null;
                    final static class InstantAsEpochMillisSerializer
                            implements LongSerializer<Instant> {
                        InstantAsEpochMillisSerializer(boolean someParam) {}
                        @Override
                        public long serialize(Instant instant) {
                            return instant.toEpochMilli();
                        }
                        @Override
                        public Instant deserialize(long l) {
                            return Instant.ofEpochMilli(l);
                        }
                    }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "Serializer com.example.appsearch.Gift.InstantAsEpochMillisSerializer must have " +
                    "a zero-param constructor"
            )
    }

    @Test
    fun testSerializerWithPrivateDefaultConstructor() {
        val compilation =
            compile(
                """
                import androidx.appsearch.app.LongSerializer;
                import java.time.Instant;
                @Document
                class Gift {
                    @Document.Id
                    String mId = null;
                    @Document.Namespace
                    String mNamespace = null;
                    @Document.LongProperty(
                        serializer = InstantAsEpochMillisSerializer.class
                    )
                    Instant mPurchaseTimeStamp = null;
                    final static class InstantAsEpochMillisSerializer
                            implements LongSerializer<Instant> {
                        private InstantAsEpochMillisSerializer() {}
                        @Override
                        public long serialize(Instant instant) {
                            return instant.toEpochMilli();
                        }
                        @Override
                        public Instant deserialize(long l) {
                            return Instant.ofEpochMilli(l);
                        }
                    }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                ("The zero-param constructor of serializer " +
                    "com.example.appsearch.Gift.InstantAsEpochMillisSerializer must not " +
                    "be private")
            )
    }

    @Test
    fun testPropertyTypeDoesNotMatchSerializer() {
        val compilation =
            compile(
                """
                import androidx.appsearch.app.StringSerializer;
                import java.net.MalformedURLException;
                import java.net.URL;
                @Document
                class Gift {
                    @Document.Id
                    String mId = null;
                    @Document.Namespace
                    String mNamespace = null;
                    @Document.StringProperty(serializer = UrlAsStringSerializer.class)
                    int mProductUrl = null;
                    final static class UrlAsStringSerializer
                            implements StringSerializer<URL> {
                        @Override
                        public String serialize(URL url) {
                            return url.toString();
                        }
                        @Override
                        public URL deserialize(String string) {
                            try {
                                return new URL(string);
                            } catch (MalformedURLException e) {
                                return null;
                            }
                        }
                    }
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation)
            .hadErrorContaining(
                "@StringProperty with serializer = UrlAsStringSerializer must only be placed on " +
                    "a getter/field of type or array or collection of java.net.URL"
            )
    }

    @Test
    fun testPropertyNamedAsDocumentClassMap() {
        val compilation =
            compile(
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.LongProperty int documentClassMap;
                }
                """
                    .trimIndent()
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "int documentClassMapConv = (int) genericDoc.getPropertyLong" + "(\"documentClassMap\")",
        )
        checkResultContains("Gift.java", "document.documentClassMap = documentClassMapConv")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testGeneratedCodeRestrictedToLibrary() {
        val compilation =
            compile(
                /* classSimpleName=*/ "Gift",
                """
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                }
                """
                    .trimIndent(),
                restrictGeneratedCodeToLibrary = true,
            )
        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("Gift.java", "@RestrictTo(RestrictTo.Scope.LIBRARY)")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testEmbeddingFields() {
        val compilation =
            compile(
                """
                import java.util.*;
                import androidx.appsearch.app.EmbeddingVector;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name; // Embedding properties
                  @EmbeddingProperty EmbeddingVector defaultIndexNone;
                  @EmbeddingProperty(indexingType=0) EmbeddingVector indexNone;
                  @EmbeddingProperty(indexingType=1) EmbeddingVector vec;
                  @EmbeddingProperty(indexingType=1) List<EmbeddingVector> listVec;
                  @EmbeddingProperty(indexingType=1) Collection<EmbeddingVector> collectVec;
                  @EmbeddingProperty(indexingType=1) EmbeddingVector[] arrVec;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("Gift.java", "new AppSearchSchema.EmbeddingPropertyConfig.Builder")
        checkResultContains(
            "Gift.java",
            "AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY",
        )
        checkResultContains(
            "Gift.java",
            "AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE",
        )
        checkResultContains("Gift.java", "EmbeddingVector")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testQuantizedEmbeddingFields() {
        val compilation =
            compile(
                """
                import java.util.*;
                import androidx.appsearch.app.EmbeddingVector;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @EmbeddingProperty(indexingType=1)
                  EmbeddingVector defaultUnquantized;
                  @EmbeddingProperty(indexingType=1, quantizationType=0)
                  EmbeddingVector unquantized;
                  @EmbeddingProperty(indexingType=1, quantizationType=1)
                  EmbeddingVector quantized;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains(
            "Gift.java",
            "AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE",
        )
        checkResultContains(
            "Gift.java",
            "AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_8_BIT",
        )
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testBlobHandleFields() {
        val compilation =
            compile(
                """
                import java.util.*;
                import androidx.appsearch.app.AppSearchBlobHandle;
                @Document
                public class Gift {
                  @Document.Namespace String namespace;
                  @Document.Id String id;
                  @Document.StringProperty String name;
                  @BlobHandleProperty AppSearchBlobHandle blob;
                  @BlobHandleProperty Collection<AppSearchBlobHandle> collectBlob;
                  @BlobHandleProperty AppSearchBlobHandle[] arrBlob;
                }
                """
                    .trimIndent()
            )

        CompilationSubject.assertThat(compilation).succeededWithoutWarnings()
        checkResultContains("Gift.java", "new AppSearchSchema.BlobHandlePropertyConfig.Builder")
        checkResultContains("Gift.java", "AppSearchBlobHandle")
        checkEqualsGolden("Gift.java")
    }

    @Test
    fun testKotlinNullability() {
        val compilation =
            compileKotlin(
                """
                @Document
                data class KotlinGift(
                    @Document.Namespace val namespace: String,
                    @Document.Id val id: String,
                    @Document.StringProperty val nonNullList: List<String>,
                    @Document.StringProperty val nullableList: List<String>?,
                    @Document.BooleanProperty val nonNullBoolean: Boolean,
                    @Document.BooleanProperty val nullableBoolean: Boolean?,
                ) {}
                """
                    .trimIndent()
            )
        checkKotlinCompilation(compilation)

        checkEqualsGolden("KotlinGift.java")
        checkResultContains(
            "KotlinGift.java",
            "List<String> nonNullListConv = Collections.emptyList();",
        )
        checkResultContains("KotlinGift.java", "List<String> nullableListConv = null;")
        checkResultContains(
            "KotlinGift.java",
            "boolean nonNullBooleanConv = genericDoc.getPropertyBoolean(\"nonNullBoolean\");",
        )
        checkResultContains("KotlinGift.java", "Boolean nullableBooleanConv = null;")
    }

    @Test
    fun testKotlinNullability_nullabilityLists() {
        val compilation =
            compileKotlin(
                """
                import androidx.appsearch.app.EmbeddingVector

                @Document
                data class Gift(
                    @Document.Namespace val namespace: String,
                    @Document.Id val id: String)

                @Document
                data class KotlinGift(
                    @Document.Namespace val namespace: String,
                    @Document.Id val id: String,
                    @Document.StringProperty val nonNullStrings: List<String>,
                    @Document.StringProperty val nullableStrings: List<String>?,
                    @Document.LongProperty val nonNullLongs: List<Long>,
                    @Document.LongProperty val nullableLongs: List<Long>?,
                    @Document.BooleanProperty val nonNullBooleans: List<Boolean>,
                    @Document.BooleanProperty val nullableBooleans: List<Boolean>?,
                    @Document.DocumentProperty val nonNullCustomTypes: List<Gift>,
                    @Document.DocumentProperty val nullableCustomTypes: List<Gift>?,
                    @Document.EmbeddingProperty val nonNullEmbeddings: List<EmbeddingVector>,
                    @Document.EmbeddingProperty val nullableEmbeddings: List<EmbeddingVector>?,
                )
                """
                    .trimIndent()
            )
        checkKotlinCompilation(compilation)

        checkEqualsGolden("KotlinGift.java")

        checkResultContains(
            "KotlinGift.java",
            "List<String> nonNullStringsConv = Collections.emptyList();",
        )
        checkResultContains("KotlinGift.java", "List<String> nullableStringsConv = null;")
        checkResultContains(
            "KotlinGift.java",
            "List<Long> nonNullLongsConv = Collections.emptyList();",
        )
        checkResultContains("KotlinGift.java", "List<Long> nullableLongsConv = null;")
        checkResultContains(
            "KotlinGift.java",
            "List<Boolean> nonNullBooleansConv = Collections.emptyList();",
        )
        checkResultContains("KotlinGift.java", "List<Boolean> nullableBooleansConv = null;")
        checkResultContains(
            "KotlinGift.java",
            "List<Gift> nonNullCustomTypesConv = Collections.emptyList();",
        )
        checkResultContains("KotlinGift.java", "List<Gift> nullableCustomTypesConv = null;")
        checkResultContains(
            "KotlinGift.java",
            "List<EmbeddingVector> nonNullEmbeddingsConv = Collections.emptyList();",
        )
        checkResultContains(
            "KotlinGift.java",
            "List<EmbeddingVector> nullableEmbeddingsConv = null;",
        )
    }
}
