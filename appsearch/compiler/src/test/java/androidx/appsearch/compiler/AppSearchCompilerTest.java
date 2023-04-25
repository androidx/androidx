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

package androidx.appsearch.compiler;

import static com.google.testing.compile.CompilationSubject.assertThat;

import com.google.auto.value.processor.AutoValueProcessor;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.common.truth.Truth;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import javax.tools.JavaFileObject;

public class AppSearchCompilerTest {
    private static final Logger LOG = Logger.getLogger(AppSearchCompilerTest.class.getSimpleName());

    @Rule
    public TemporaryFolder mTemporaryFolder = new TemporaryFolder();
    @Rule
    public TestName mTestName = new TestName();

    private File mGenFilesDir;

    @Before
    public void setUp() throws IOException {
        mGenFilesDir = mTemporaryFolder.newFolder("genFilesDir");
    }

    @Test
    public void testNonClass() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public interface Gift {}\n");

        assertThat(compilation).hadErrorContaining(
                "annotation on something other than a class");
    }

    @Test
    public void testPrivate() {
        Compilation compilation = compile(
                "Wrapper",
                "public class Wrapper {\n"
                        + "@Document\n"
                        + "private class Gift {}\n"
                        + "}  // Wrapper\n"
        );

        assertThat(compilation).hadErrorContaining("annotated class is private");
    }

    @Test
    public void testNoId() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "must have exactly one field annotated with @Id");
    }

    @Test
    public void testManyIds() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id1;\n"
                        + "  @Document.Id String id2;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "contains multiple fields annotated @Id");
    }

    @Test
    public void testAutoValueInheritance() throws Exception {
        Compilation docExtendsAutoValueDoc = compile(
                "import com.google.auto.value.AutoValue;\n"
                        + "import com.google.auto.value.AutoValue.*;\n"
                        + "@AutoValue\n"
                        + "@Document\n"
                        + "public abstract class Gift {\n"
                        + "  @CopyAnnotations @Document.Id abstract String id();\n"
                        + "  @CopyAnnotations @Document.Namespace abstract String namespace();\n"
                        + "  public static Gift create(String id, String namespace) {\n"
                        + "      return new AutoValue_Gift(id,namespace);\n"
                        + "  }\n"
                        + "  @Document\n"
                        + "  static abstract class CoolGift extends Gift {\n"
                        + "    @Document.BooleanProperty boolean cool;\n"
                        + "    CoolGift(String id, String namespace, boolean cool) {\n"
                        + "      super(id, message);\n"
                        + "      this.cool = cool;\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        assertThat(docExtendsAutoValueDoc).hadErrorContaining(
                "A class annotated with Document cannot inherit from a class annotated with "
                        + "AutoValue");

        Compilation autoValueDocExtendsDoc = compile(
                "import com.google.auto.value.AutoValue;\n"
                        + "import com.google.auto.value.AutoValue.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @AutoValue\n"
                        + "  @Document\n"
                        + "  static abstract class CoolGift extends Gift {\n"
                        + "    @CopyAnnotations @Document.BooleanProperty abstract boolean cool()"
                        + ";\n"
                        + "    public static CoolGift create(String id, String namespace, boolean"
                        + " cool) {\n"
                        + "      return new AutoValue_Gift_CoolGift(cool);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        assertThat(autoValueDocExtendsDoc).hadErrorContaining(
                "A class annotated with AutoValue and Document cannot have a superclass");
    }

    @Test
    public void testSuperClassErrors() throws Exception {
        Compilation specialFieldReassigned = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty String prop;\n"
                        + "  Gift(String id, String namespace, String prop) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.prop = prop;\n"
                        + "  }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class CoolGift extends Gift {\n"
                        + "  @Document.StringProperty String id;\n"
                        + "  CoolGift(String id, String namespace) {\n"
                        + "    super(id, namespace, \"\");\n"
                        + "    this.id = id;\n"
                        + "  }\n"
                        + "}\n");
        assertThat(specialFieldReassigned).hadErrorContaining(
                "Non-annotated field overriding special annotated fields named: id");

        Compilation nonAnnotatedFieldHasSameName = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  Gift(String id, String namespace) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "  }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class CoolGift extends Gift {\n"
                        + "  String id;\n"
                        + "  CoolGift(String id, String namespace) {\n"
                        + "    super(id, namespace);\n"
                        + "  }\n"
                        + "  public String getId() { return id; }\n"
                        + "}\n");
        assertThat(nonAnnotatedFieldHasSameName).hadErrorContaining(
                "Non-annotated field overriding special annotated fields named: id");

        Compilation propertyCollision = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty String prop;\n"
                        + "  Gift(String id, String namespace, String prop) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.prop = prop;\n"
                        + "  }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class CoolGift extends Gift {\n"
                        + "  @Document.BooleanProperty Boolean prop;\n"
                        + "  CoolGift(String id, String namespace, String prop) {\n"
                        + "    super(id, namespace, prop);\n"
                        + "  }\n"
                        + "}\n");
        assertThat(propertyCollision).hadErrorContaining(
                "Class hierarchy contains multiple annotated fields named: prop");

        //error on collision
        Compilation idCollision = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  Gift(String id, String namespace) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "  }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class CoolGift extends Gift {\n"
                        + "  @Document.BooleanProperty private final boolean cool;\n"
                        + "  @Document.Id String badId;\n"
                        + "  CoolGift(String id, String namespace, String badId) {\n"
                        + "    super(id, namespace);\n"
                        + "    this.badId = badId;\n"
                        + "  }\n"
                        + "  public boolean getBadId() { return badId; }\n"
                        + "}\n");
        assertThat(idCollision).hadErrorContaining(
                "Class hierarchy contains multiple fields annotated @Id");

        Compilation nsCollision = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  Gift(String id, String namespace) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "  }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class CoolGift extends Gift {\n"
                        + "  @Document.Namespace String badNamespace;\n"
                        + "  CoolGift(String id, String namespace, String badId) {\n"
                        + "    super(id, namespace);\n"
                        + "    this.badNamespace = namespace;\n"
                        + "  }\n"
                        + "}\n");
        assertThat(nsCollision).hadErrorContaining(
                "Class hierarchy contains multiple fields annotated @Namespace");
    }

    @Test
    public void testSuperClass() throws Exception {
        // Try multiple levels of inheritance, nested, with properties, overriding properties
        Compilation compilation = compile(
                "@Document\n"
                        + "class Ancestor {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty String note;\n"
                        + "  int score;\n"
                        + "  Ancestor(String id, String namespace, String note) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.note = note;\n"
                        + "  }\n"
                        + "  public String getNote() { return note; }\n"
                        + "}\n"
                        + "class Parent extends Ancestor {\n"
                        + "  Parent(String id, String namespace, String note) {\n"
                        + "    super(id, namespace, note);\n"
                        + "  }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class Gift extends Parent {\n"
                        + "  @Document.StringProperty String sender;\n"
                        + "  Gift(String id, String namespace, String sender) {\n"
                        + "    super(id, namespace, \"note\");\n"
                        + "    this.sender = sender;\n"
                        + "  }\n"
                        + "  public String getSender() { return sender; }\n"
                        + "  @Document\n"
                        + "  class FooGift extends Gift {\n"
                        + "    @Document.Score int score;\n"
                        + "    @Document.BooleanProperty boolean foo;\n"
                        + "    FooGift(String id, String namespace, String note, int score, "
                        + "boolean foo) {\n"
                        + "      super(id, namespace, note);\n"
                        + "      this.score = score;\n"
                        + "      this.foo = foo;\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift$$__FooGift.java");
    }

    @Test
    public void testSuperClass_changeSchemaName() throws Exception {
        Compilation compilation = compile(
                "@Document(name=\"MyParent\")\n"
                        + "class Parent {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty String note;\n"
                        + "  Parent(String id, String namespace, String note) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.note = note;\n"
                        + "  }\n"
                        + "  public String getNote() { return note; }\n"
                        + "}\n"
                        + "\n"
                        + "@Document(name=\"MyGift\")\n"
                        + "class Gift extends Parent {\n"
                        + "  @Document.StringProperty String sender;\n"
                        + "  Gift(String id, String namespace, String sender) {\n"
                        + "    super(id, namespace, \"note\");\n"
                        + "    this.sender = sender;\n"
                        + "  }\n"
                        + "  public String getSender() { return sender; }\n"
                        + "}\n");
        assertThat(compilation).succeededWithoutWarnings();
        checkResultContains(
                /*className=*/"Gift.java",
                /*content=*/"public static final String SCHEMA_NAME = \"MyGift\";");
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testSuperClass_multipleChangedSchemaNames() throws Exception {
        Compilation compilation = compile(
                "@Document(name=\"MyParent\")\n"
                        + "class Parent {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty String note;\n"
                        + "  Parent(String id, String namespace, String note) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.note = note;\n"
                        + "  }\n"
                        + "  public String getNote() { return note; }\n"
                        + "}\n"
                        + "\n"
                        + "@Document(name=\"MyGift\")\n"
                        + "class Gift extends Parent {\n"
                        + "  @Document.StringProperty String sender;\n"
                        + "  Gift(String id, String namespace, String sender) {\n"
                        + "    super(id, namespace, \"note\");\n"
                        + "    this.sender = sender;\n"
                        + "  }\n"
                        + "  public String getSender() { return sender; }\n"
                        + "}\n"
                        + "\n"
                        + "@Document\n"
                        + "class FooGift extends Gift {\n"
                        + "  @Document.BooleanProperty boolean foo;\n"
                        + "  FooGift(String id, String namespace, String note, boolean foo) {\n"
                        + "    super(id, namespace, note);\n"
                        + "    this.foo = foo;\n"
                        + "  }\n"
                        + "}\n");
        assertThat(compilation).succeededWithoutWarnings();
        checkResultContains(
                /*className=*/"FooGift.java",
                /*content=*/"public static final String SCHEMA_NAME = \"MyGift\";");
        checkEqualsGolden("FooGift.java");
    }

    @Test
    public void testSuperClassPojoAncestor() throws Exception {
        // Try multiple levels of inheritance, nested, with properties, overriding properties
        Compilation compilation = compile(
                "class Ancestor {\n"
                        + "}\n"
                        + "@Document\n"
                        + "class Parent extends Ancestor {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty String note;\n"
                        + "  int score;\n"
                        + "  Parent(String id, String namespace, String note) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.note = note;\n"
                        + "  }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class Gift extends Parent {\n"
                        + "  @Document.StringProperty String sender;\n"
                        + "  Gift(String id, String namespace, String sender) {\n"
                        + "    super(id, namespace, \"note\");\n"
                        + "    this.sender = sender;\n"
                        + "  }\n"
                        + "  public String getSender() { return sender; }\n"
                        + "  @Document\n"
                        + "  class FooGift extends Gift {\n"
                        + "    @Document.Score int score;\n"
                        + "    @Document.BooleanProperty boolean foo;\n"
                        + "    FooGift(String id, String namespace, String note, int score, "
                        + "boolean foo) {\n"
                        + "      super(id, namespace, note);\n"
                        + "      this.score = score;\n"
                        + "      this.foo = foo;\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift$$__FooGift.java");
    }

    @Test
    public void testSuperClassWithPrivateFields() throws Exception {
        // Parents has private fields with public getter. Children should be able to extend
        // Parents and inherit the public getters.
        // TODO(b/262916926): we should be able to support inheriting classes not annotated with
        //  @Document.
        Compilation compilation = compile(
                "@Document\n"
                        + "class Ancestor {\n"
                        + "  @Document.Namespace private String mNamespace;\n"
                        + "  @Document.Id private String mId;\n"
                        + "  @Document.StringProperty private String mNote;\n"
                        + "  Ancestor(String id, String namespace, String note) {\n"
                        + "    this.mId = id;\n"
                        + "    this.mNamespace = namespace;\n"
                        + "    this.mNote = note;\n"
                        + "  }\n"
                        + "  public String getNamespace() { return mNamespace; }\n"
                        + "  public String getId() { return mId; }\n"
                        + "  public String getNote() { return mNote; }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class Parent extends Ancestor {\n"
                        + "  @Document.StringProperty private String mReceiver;\n"
                        + "  Parent(String id, String namespace, String note, String receiver) {\n"
                        + "    super(id, namespace, note);\n"
                        + "    this.mReceiver = receiver;\n"
                        + "  }\n"
                        + "  public String getReceiver() { return mReceiver; }\n"
                        + "}\n"
                        + "@Document\n"
                        + "class Gift extends Parent {\n"
                        + "  @Document.StringProperty private String mSender;\n"
                        + "  Gift(String id, String namespace, String note, String receiver,\n"
                        + "    String sender) {\n"
                        + "    super(id, namespace, note, receiver);\n"
                        + "    this.mSender = sender;\n"
                        + "  }\n"
                        + "  public String getSender() { return mSender; }\n"
                        + "}\n");
        assertThat(compilation).succeededWithoutWarnings();

        checkResultContains(/*className=*/"Gift.java", /*content=*/"document.getNote()");
        checkResultContains(/*className=*/"Gift.java", /*content=*/"document.getReceiver()");
        checkResultContains(/*className=*/"Gift.java", /*content=*/"document.getSender()");
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testManyCreationTimestamp() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.CreationTimestampMillis long ts1;\n"
                        + "  @Document.CreationTimestampMillis long ts2;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "contains multiple fields annotated @CreationTimestampMillis");
    }

    @Test
    public void testNoNamespace() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Id String id;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "must have exactly one field annotated with @Namespace");
    }

    @Test
    public void testManyNamespace() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String ns1;\n"
                        + "  @Document.Namespace String ns2;\n"
                        + "  @Document.Id String id;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "contains multiple fields annotated @Namespace");
    }

    @Test
    public void testManyTtlMillis() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.TtlMillis long ts1;\n"
                        + "  @Document.TtlMillis long ts2;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "contains multiple fields annotated @TtlMillis");
    }

    @Test
    public void testManyScore() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.Score int score1;\n"
                        + "  @Document.Score int score2;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "contains multiple fields annotated @Score");
    }

    @Test
    public void testPropertyOnFieldForNonAutoValueClass() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int getPrice() { return 0; }\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "AppSearch annotation is not applicable to methods for Non-AutoValue class");
    }

    @Test
    public void testCantRead_noGetter() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Field cannot be read: it is private and we failed to find a suitable getter "
                        + "for field \"price\"");
    }

    @Test
    public void testCantRead_privateGetter() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  private int getPrice() { return 0; }\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Field cannot be read: it is private and we failed to find a suitable getter "
                        + "for field \"price\"");
        assertThat(compilation).hadWarningContaining("Getter cannot be used: private visibility");
    }

    @Test
    public void testCantRead_wrongParamGetter() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  int getPrice(int n) { return 0; }\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Field cannot be read: it is private and we failed to find a suitable getter "
                        + "for field \"price\"");
        assertThat(compilation).hadWarningContaining(
                "Getter cannot be used: should take no parameters");
    }

    @Test
    public void testCantRead_isGetterNonBoolean() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  int isPrice() { return price; }"
                        + "  void setPrice(int price) {}"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Field cannot be read: it is private and we failed to find a suitable getter "
                        + "for field \"price\"");
    }

    @Test
    public void testRead_MultipleGetters() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  int getPrice(int n) { return 0; }\n"
                        + "  int getPrice() { return 0; }\n"
                        + "  void setPrice(int n) {}\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testRead_isGetterForBoolean() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.BooleanProperty private boolean forSale;\n"
                        + "  boolean isForSale() { return forSale; }"
                        + "  void setForSale(boolean forSale) {}"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testGetterAndSetterFunctions_withFieldName() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  int price() { return 0; }\n"
                        + "  void price(int n) {}\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        // Check setter function is identified correctly.
        checkResultContains(/* className= */ "Gift.java",
                /* content= */ "builder.setPropertyLong(\"price\", document.price());");
        // Check getter function is identified correctly.
        checkResultContains(/* className= */ "Gift.java",
                /* content= */ "document.price(priceConv);");
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testCantWrite_noSetter() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  int getPrice() { return price; }\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Failed to find any suitable creation methods to build class "
                        + "\"com.example.appsearch.Gift\"");
        assertThat(compilation).hadWarningContainingMatch(
                "Field cannot be written .* failed to find a suitable setter for field \"price\"");
        assertThat(compilation).hadWarningContaining(
                "Cannot use this creation method to construct the class: This method doesn't have "
                        + "parameters for the following fields: [price]");
    }

    @Test
    public void testCantWrite_privateSetter() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  int getPrice() { return price; }\n"
                        + "  private void setPrice(int n) {}\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Failed to find any suitable creation methods to build class "
                        + "\"com.example.appsearch.Gift\"");
        assertThat(compilation).hadWarningContainingMatch(
                "Field cannot be written .* failed to find a suitable setter for field \"price\"");
        assertThat(compilation).hadWarningContaining(
                "Setter cannot be used: private visibility");
        assertThat(compilation).hadWarningContaining(
                "Cannot use this creation method to construct the class: This method doesn't have "
                        + "parameters for the following fields: [price]");
    }

    @Test
    public void testCantWrite_wrongParamSetter() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  int getPrice() { return price; }\n"
                        + "  void setPrice() {}\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Failed to find any suitable creation methods to build class "
                        + "\"com.example.appsearch.Gift\"");
        assertThat(compilation).hadWarningContainingMatch(
                "Field cannot be written .* failed to find a suitable setter for field \"price\"");
        assertThat(compilation).hadWarningContaining(
                "Setter cannot be used: takes 0 parameters instead of 1");
        assertThat(compilation).hadWarningContaining(
                "Cannot use this creation method to construct the class: This method doesn't have "
                        + "parameters for the following fields: [price]");
    }

    @Test
    public void testWrite_multipleSetters() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  int getPrice() { return price; }\n"
                        + "  void setPrice() {}\n"
                        + "  void setPrice(int n) {}\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testWrite_privateConstructor() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  private Gift() {}\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Failed to find any suitable creation methods to build class "
                        + "\"com.example.appsearch.Gift\"");
        assertThat(compilation).hadWarningContaining("Creation method is private");
    }

    @Test
    public void testWrite_constructorMissingParams() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  Gift(int price) {}\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id final String id;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Failed to find any suitable creation methods to build class "
                        + "\"com.example.appsearch.Gift\"");
        assertThat(compilation).hadWarningContaining(
                "doesn't have parameters for the following fields: [id]");
    }

    @Test
    public void testWrite_factoryMethodOnly() throws IOException {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  private Gift(int price, String id, String namespace) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.price = price;\n"
                        + "  }\n"
                        + "  public static Gift create(String id, String namespace, int price) {\n"
                        + "    return new Gift(price, id, namespace);"
                        + "  }\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id final String id;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkResultContains(/* className= */ "Gift.java",
                /* content= */ "Gift document = Gift.create(idConv, namespaceConv, priceConv);");
    }

    @Test
    // With golden class for factory method.
    public void testWrite_bothUsableFactoryMethodAndConstructor_picksFirstUsableCreationMethod()
            throws IOException {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  Gift(String id, String namespace, int price) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.price = price;\n"
                        + "  }\n"
                        + "  public static Gift create(String id, String namespace, int price) {\n"
                        + "    return new Gift(id, namespace, price);"
                        + "  }\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkResultContains(/* className= */ "Gift.java",
                /* content= */ "Gift document = new Gift(idConv, namespaceConv, priceConv);");
    }

    @Test
    public void testWrite_usableFactoryMethod_unusableConstructor()
            throws IOException {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  private Gift(String id, String namespace, int price) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.price = price;\n"
                        + "  }\n"
                        + "  public static Gift create(String id, String namespace, int price) {\n"
                        + "    return new Gift(id, namespace, price);"
                        + "  }\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id final String id;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkResultContains(/* className= */ "Gift.java",
                /* content= */ "Gift document = Gift.create(idConv, namespaceConv, priceConv);");
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testWrite_unusableFactoryMethod_usableConstructor()
            throws IOException {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  Gift(String id, String namespace, int price) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.price = price;\n"
                        + "  }\n"
                        + "  private static Gift create(String id, String namespace, int price){\n"
                        + "    return new Gift(id, namespace, price);"
                        + "  }\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id final String id;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkResultContains(/* className= */ "Gift.java",
                /* content= */ "Gift document = new Gift(idConv, namespaceConv, priceConv);");
    }

    @Test
    public void testWrite_constructorExtraParams() {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  Gift(int price, String id, String namespace, int unknownParam) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.price = price;\n"
                        + "  }\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id final String id;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Failed to find any suitable creation methods to build class "
                        + "\"com.example.appsearch.Gift\"");
        assertThat(compilation).hadWarningContaining(
                "Parameter \"unknownParam\" is not an AppSearch parameter; don't know how to "
                        + "supply it");
    }

    @Test
    public void testWrite_multipleConventions() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id private String id_;\n"
                        + "  @Document.LongProperty private int price1;\n"
                        + "  @Document.LongProperty private int mPrice2;\n"
                        + "  @Document.LongProperty private int _price3;\n"
                        + "  @Document.LongProperty final int price4_;\n"
                        + "  int getPrice1() { return price1; }\n"
                        + "  int price2() { return mPrice2; }\n"
                        + "  int getPrice3() { return _price3; }\n"
                        + "  void setPrice1(int n) {}\n"
                        + "  void price2(int n) {}\n"
                        + "  void price3(int n) {}\n"
                        + "  String getId() {\n"
                        + "    return id_;\n"
                        + "  }\n"
                        + "  public Gift(String id, int price4) {\n"
                        + "    id_ = id;"
                        + "    price4_ = price4;\n"
                        + "  }\n"
                        + "}\n");
        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testSuccessSimple() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  Gift(boolean dog, String id, String namespace) {\n"
                        + "    this.id = id;\n"
                        + "    this.namespace = namespace;\n"
                        + "    this.dog = dog;\n"
                        + "  }\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id final String id;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "  @Document.BooleanProperty boolean cat = false;\n"
                        + "  public void setCat(boolean cat) {}\n"
                        + "  @Document.BooleanProperty private final boolean dog;\n"
                        + "  public boolean getDog() { return dog; }\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testDifferentTypeName() throws Exception {
        Compilation compilation = compile(
                "@Document(name=\"DifferentType\")\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testRepeatedFields() throws Exception {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty List<String> listOfString;\n"
                        + "  @Document.LongProperty Collection<Integer> setOfInt;\n"
                        + "  @Document.BytesProperty byte[][] repeatedByteArray;\n"
                        + "  @Document.BytesProperty byte[] byteArray;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testCardinality() throws Exception {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty(required=true) List<String> repeatReq;\n"
                        + "  @Document.StringProperty(required=false) List<String> repeatNoReq;\n"
                        + "  @Document.DoubleProperty(required=true) Float req;\n"
                        + "  @Document.DoubleProperty(required=false) Float noReq;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testAllSingleTypes() throws Exception {
        // TODO(b/156296904): Uncomment Gift in this test when it's supported
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty String stringProp;\n"
                        + "  @Document.LongProperty Integer integerProp;\n"
                        + "  @Document.LongProperty Long longProp;\n"
                        + "  @Document.DoubleProperty Float floatProp;\n"
                        + "  @Document.DoubleProperty Double doubleProp;\n"
                        + "  @Document.BooleanProperty Boolean booleanProp;\n"
                        + "  @Document.BytesProperty byte[] bytesProp;\n"
                        //+ "  @Document.Property Gift documentProp;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testTokenizerType() throws Exception {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "\n"
                        // NONE index type will generate a NONE tokenizerType type.
                        + "  @Document.StringProperty(tokenizerType=0, indexingType=0) "
                        + "  String tokNoneInvalid;\n"
                        + "  @Document.StringProperty(tokenizerType=1, indexingType=0) "
                        + "  String tokPlainInvalid;\n"
                        + "  @Document.StringProperty(tokenizerType=2, indexingType=0) "
                        + "  String tokVerbatimInvalid;\n"
                        + "  @Document.StringProperty(tokenizerType=3, indexingType=0) "
                        + "  String tokRfc822Invalid;\n"
                        + "\n"
                        // Indexing type exact.
                        + "  @Document.StringProperty(tokenizerType=0, indexingType=1) "
                        + "  String tokNone;\n"
                        + "  @Document.StringProperty(tokenizerType=1, indexingType=1) "
                        + "  String tokPlain;\n"
                        + "  @Document.StringProperty(tokenizerType=2, indexingType=1) "
                        + "  String tokVerbatim;\n"
                        + "  @Document.StringProperty(tokenizerType=3, indexingType=1) "
                        + "  String tokRfc822;\n"
                        + "\n"
                        // Indexing type prefix.
                        + "  @Document.StringProperty(tokenizerType=0, indexingType=2) "
                        + "  String tokNonePrefix;\n"
                        + "  @Document.StringProperty(tokenizerType=1, indexingType=2) "
                        + "  String tokPlainPrefix;\n"
                        + "  @Document.StringProperty(tokenizerType=2, indexingType=2) "
                        + "  String tokVerbatimPrefix;\n"
                        + "  @Document.StringProperty(tokenizerType=3, indexingType=2) "
                        + "  String tokRfc822Prefix;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testInvalidTokenizerType() {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty(indexingType=1, tokenizerType=100)\n"
                        + "  String str;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining("Unknown tokenizer type 100");
    }

    @Test
    public void testIndexingType() throws Exception {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty(indexingType=0) String indexNone;\n"
                        + "  @Document.StringProperty(indexingType=1) String indexExact;\n"
                        + "  @Document.StringProperty(indexingType=2) String indexPrefix;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testInvalidIndexingType() {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty(indexingType=100, tokenizerType=1)\n"
                        + "  String str;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining("Unknown indexing type 100");
    }

    @Test
    public void testLongPropertyIndexingType() throws Exception {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty Long defaultIndexNone;\n"
                        + "  @Document.LongProperty(indexingType=0) Long indexNone;\n"
                        + "  @Document.LongProperty(indexingType=1) Integer boxInt;\n"
                        + "  @Document.LongProperty(indexingType=1) int unboxInt;\n"
                        + "  @Document.LongProperty(indexingType=1) Long boxLong;\n"
                        + "  @Document.LongProperty(indexingType=1) long unboxLong;\n"
                        + "  @Document.LongProperty(indexingType=1) Integer[] arrBoxInt;\n"
                        + "  @Document.LongProperty(indexingType=1) int[] arrUnboxInt;\n"
                        + "  @Document.LongProperty(indexingType=1) Long[] arrBoxLong;\n"
                        + "  @Document.LongProperty(indexingType=1) long[] arrUnboxLong;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testInvalidLongPropertyIndexingType() throws Exception {
        // AppSearchSchema requires Android and is not available in this desktop test, so we cheat
        // by using the integer constants directly.
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.LongProperty(indexingType=100) Long invalidProperty;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining("Unknown indexing type 100");
    }

    @Test
    public void testStringPropertyJoinableType() throws Exception {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty(joinableValueType=1)\n"
                        + "  String object;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testRepeatedPropertyJoinableType_throwsError() throws Exception {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty(joinableValueType=1)\n"
                        + "  List<String> object;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Joinable value type 1 not allowed on repeated properties.");
    }

    @Test
    public void testPropertyName() throws Exception {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.StringProperty(name=\"newName\") String oldName;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testToGenericDocument_allSupportedTypes() throws Exception {
        // TODO(b/156296904): Uncomment Gift and GenericDocument when it's supported
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "import androidx.appsearch.app.GenericDocument;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Namespace String namespace;\n"
                        + "  @Id String id;\n"
                        + "\n"
                        + "  // Collections\n"
                        + "  @LongProperty Collection<Long> collectLong;\n"         // 1a
                        + "  @LongProperty Collection<Integer> collectInteger;\n"   // 1a
                        + "  @DoubleProperty Collection<Double> collectDouble;\n"     // 1a
                        + "  @DoubleProperty Collection<Float> collectFloat;\n"       // 1a
                        + "  @BooleanProperty Collection<Boolean> collectBoolean;\n"   // 1a
                        + "  @BytesProperty Collection<byte[]> collectByteArr;\n"    // 1a
                        + "  @StringProperty Collection<String> collectString;\n"     // 1b
                        + "  @DocumentProperty Collection<Gift> collectGift;\n"         // 1c
                        + "\n"
                        + "  // Arrays\n"
                        + "  @LongProperty Long[] arrBoxLong;\n"         // 2a
                        + "  @LongProperty long[] arrUnboxLong;\n"       // 2b
                        + "  @LongProperty Integer[] arrBoxInteger;\n"   // 2a
                        + "  @LongProperty int[] arrUnboxInt;\n"         // 2a
                        + "  @DoubleProperty Double[] arrBoxDouble;\n"     // 2a
                        + "  @DoubleProperty double[] arrUnboxDouble;\n"   // 2b
                        + "  @DoubleProperty Float[] arrBoxFloat;\n"       // 2a
                        + "  @DoubleProperty float[] arrUnboxFloat;\n"     // 2a
                        + "  @BooleanProperty Boolean[] arrBoxBoolean;\n"   // 2a
                        + "  @BooleanProperty boolean[] arrUnboxBoolean;\n" // 2b
                        + "  @BytesProperty byte[][] arrUnboxByteArr;\n"  // 2b
                        + "  @BytesProperty Byte[] boxByteArr;\n"         // 2a
                        + "  @StringProperty String[] arrString;\n"        // 2b
                        + "  @DocumentProperty Gift[] arrGift;\n"            // 2c
                        + "\n"
                        + "  // Single values\n"
                        + "  @StringProperty String string;\n"        // 3a
                        + "  @LongProperty Long boxLong;\n"         // 3a
                        + "  @LongProperty long unboxLong;\n"       // 3b
                        + "  @LongProperty Integer boxInteger;\n"   // 3a
                        + "  @LongProperty int unboxInt;\n"         // 3b
                        + "  @DoubleProperty Double boxDouble;\n"     // 3a
                        + "  @DoubleProperty double unboxDouble;\n"   // 3b
                        + "  @DoubleProperty Float boxFloat;\n"       // 3a
                        + "  @DoubleProperty float unboxFloat;\n"     // 3b
                        + "  @BooleanProperty Boolean boxBoolean;\n"   // 3a
                        + "  @BooleanProperty boolean unboxBoolean;\n" // 3b
                        + "  @BytesProperty byte[] unboxByteArr;\n"  // 3a
                        + "  @DocumentProperty Gift gift;\n"            // 3c
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testPropertyAnnotation_invalidType() {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Namespace String namespace;\n"
                        + "  @Id String id;\n"
                        + "  @BooleanProperty String[] arrString;\n"
                        + "}\n");

        assertThat(compilation).hadErrorContaining(
                "Property Annotation androidx.appsearch.annotation.Document.BooleanProperty "
                        + "doesn't accept the data type of property field arrString");
    }

    @Test
    public void testToGenericDocument_invalidTypes() {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Namespace String namespace;\n"
                        + "  @Id String id;\n"
                        + "  @BytesProperty Collection<Byte[]> collectBoxByteArr;\n" // 1x
                        + "}\n");
        assertThat(compilation).hadErrorContaining(
                "Unhandled out property type (1x): java.util.Collection<java.lang.Byte[]>");

        compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Namespace String namespace;\n"
                        + "  @Id String id;\n"
                        + "  @BytesProperty Collection<Byte> collectByte;\n" // 1x
                        + "}\n");
        assertThat(compilation).hadErrorContaining(
                "Unhandled out property type (1x): java.util.Collection<java.lang.Byte>");

        compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Namespace String namespace;\n"
                        + "  @Id String id;\n"
                        + "  @BytesProperty Byte[][] arrBoxByteArr;\n" // 2x
                        + "}\n");
        assertThat(compilation).hadErrorContaining(
                "Unhandled out property type (2x): java.lang.Byte[][]");
    }

    @Test
    public void testAllSpecialFields_field() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.CreationTimestampMillis long creationTs;\n"
                        + "  @Document.TtlMillis int ttlMs;\n"
                        + "  @Document.LongProperty int price;\n"
                        + "  @Document.Score int score;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testAllSpecialFields_getter() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.Id private String id;\n"
                        + "  @Document.Score private int score;\n"
                        + "  @Document.CreationTimestampMillis private long creationTs;\n"
                        + "  @Document.TtlMillis private int ttlMs;\n"
                        + "  @Document.LongProperty private int price;\n"
                        + "  public String getId() { return id; }\n"
                        + "  public void setId(String id) { this.id = id; }\n"
                        + "  public String getNamespace() { return namespace; }\n"
                        + "  public void setNamespace(String namespace) {\n"
                        + "    this.namespace = namespace;\n"
                        + "  }\n"
                        + "  public int getScore() { return score; }\n"
                        + "  public void setScore(int score) { this.score = score; }\n"
                        + "  public long getCreationTs() { return creationTs; }\n"
                        + "  public void setCreationTs(int creationTs) {\n"
                        + "    this.creationTs = creationTs;\n"
                        + "  }\n"
                        + "  public int getTtlMs() { return ttlMs; }\n"
                        + "  public void setTtlMs(int ttlMs) { this.ttlMs = ttlMs; }\n"
                        + "  public int getPrice() { return price; }\n"
                        + "  public void setPrice(int price) { this.price = price; }\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testMultipleNestedAutoValueDocument() throws IOException {
        Compilation compilation = compile(
                "import com.google.auto.value.AutoValue;\n"
                        + "import com.google.auto.value.AutoValue.*;\n"
                        + "@Document\n"
                        + "@AutoValue\n"
                        + "public abstract class Gift {\n"
                        + "  @CopyAnnotations @Document.Id abstract String id();\n"
                        + "  @CopyAnnotations @Document.Namespace abstract String namespace();\n"
                        + "  @CopyAnnotations\n"
                        + "  @Document.StringProperty abstract String property();\n"
                        + "  public static Gift create(String id, String namespace, String"
                        + " property) {\n"
                        + "    return new AutoValue_Gift(id, namespace, property);\n"
                        + "  }\n"
                        + "  @Document\n"
                        + "  @AutoValue\n"
                        + "  abstract static class B {\n"
                        + "    @CopyAnnotations @Document.Id abstract String id();\n"
                        + "    @CopyAnnotations @Document.Namespace abstract String namespace();\n"
                        + "    public static B create(String id, String namespace) {\n"
                        + "      return new AutoValue_Gift_B(id, namespace);\n"
                        + "    }\n"
                        + "  }\n"
                        + "  @Document\n"
                        + "  @AutoValue\n"
                        + "  abstract static class A {\n"
                        + "    @CopyAnnotations @Document.Id abstract String id();\n"
                        + "    @CopyAnnotations @Document.Namespace abstract String namespace();\n"
                        + "    public static A create(String id, String namespace) {\n"
                        + "      return new AutoValue_Gift_A(id, namespace);\n"
                        + "    }\n"
                        + "  }\n"
                        + "}\n");
        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("AutoValue_Gift_A.java");
    }

    @Test
    public void testAutoValueDocument() throws IOException {
        Compilation compilation = compile(
                "import com.google.auto.value.AutoValue;\n"
                        + "import com.google.auto.value.AutoValue.*;\n"
                        + "@Document\n"
                        + "@AutoValue\n"
                        + "public abstract class Gift {\n"
                        + "  @CopyAnnotations @Document.Id abstract String id();\n"
                        + "  @CopyAnnotations @Document.Namespace abstract String namespace();\n"
                        + "  @CopyAnnotations\n"
                        + "  @Document.StringProperty abstract String property();\n"
                        + "  public static Gift create(String id, String namespace, String"
                        + " property) {\n"
                        + "    return new AutoValue_Gift(id, namespace, property);\n"
                        + "  }\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("AutoValue_Gift.java");
    }

    @Test
    public void testInnerClass() throws Exception {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "import androidx.appsearch.app.GenericDocument;\n"
                        + "public class Gift {\n"
                        + "  @Document\n"
                        + "  public static class InnerGift{\n"
                        + "    @Document.Namespace String namespace;\n"
                        + "    @Document.Id String id;\n"
                        + "    @StringProperty String[] arrString;\n"        // 2b
                        + "  }\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift$$__InnerGift.java");
    }

    @Test
    public void testOneBadConstructor() throws Exception {
        Compilation compilation = compile(
                "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Id private String mId;\n"
                        + "  @Document.Namespace private String mNamespace;\n"
                        + "  public Gift(String id, String namespace, boolean nonAppSearchParam){\n"
                        + "    mId = id;\n"
                        + "    mNamespace = namespace;\n"
                        + "  }\n"
                        + "  public Gift(String id){\n"
                        + "    mId = id;\n"
                        + "  }\n"
                        + "  public String getId(){"
                        + "    return mId;"
                        + "  }\n"
                        + "  public String getNamespace(){"
                        + "    return mNamespace;"
                        + "  }\n"
                        + "  public void setNamespace(String namespace){\n"
                        + "    mNamespace = namespace;"
                        + "  }\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testNestedDocumentsIndexing() throws Exception {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.DocumentProperty(indexNestedProperties = true) "
                        + "Collection<GiftContent> giftContentsCollection;\n"
                        + "  @Document.DocumentProperty(indexNestedProperties = true) GiftContent[]"
                        + " giftContentsArray;\n"
                        + "  @Document.DocumentProperty(indexNestedProperties = true) GiftContent "
                        + "giftContent;\n"
                        + "  @Document.DocumentProperty() Collection<GiftContent> "
                        + "giftContentsCollectionNotIndexed;\n"
                        + "  @Document.DocumentProperty GiftContent[] "
                        + "giftContentsArrayNotIndexed;\n"
                        + "  @Document.DocumentProperty GiftContent giftContentNotIndexed;\n"
                        + "}\n"
                        + "\n"
                        + "@Document\n"
                        + "class GiftContent {\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.StringProperty String[] contentsArray;\n"
                        + "  @Document.StringProperty String contents;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");
    }

    @Test
    public void testMultipleNesting() throws Exception {
        Compilation compilation = compile(
                "import java.util.*;\n"
                        + "@Document\n"
                        + "public class Gift {\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.DocumentProperty Middle middleContentA;\n"
                        + "  @Document.DocumentProperty Middle middleContentB;\n"
                        + "}\n"
                        + "\n"
                        + "@Document\n"
                        + "class Middle {\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.DocumentProperty Inner innerContentA;\n"
                        + "  @Document.DocumentProperty Inner innerContentB;\n"
                        + "}\n"
                        + "@Document\n"
                        + "class Inner {\n"
                        + "  @Document.Id String id;\n"
                        + "  @Document.Namespace String namespace;\n"
                        + "  @Document.StringProperty String contents;\n"
                        + "}\n");

        assertThat(compilation).succeededWithoutWarnings();
        checkEqualsGolden("Gift.java");

        // Check that Gift contains Middle, Middle contains Inner, and Inner returns empty
        checkResultContains(/* className= */ "Gift.java",
                /* content= */ "classSet.add(Middle.class);\n    return classSet;");
        checkResultContains(/* className= */ "Middle.java",
                /* content= */ "classSet.add(Inner.class);\n    return classSet;");
        checkResultContains(/* className= */ "Inner.java",
                /* content= */ "return Collections.emptyList();");
    }

    private Compilation compile(String classBody) {
        return compile("Gift", classBody);
    }

    private Compilation compile(String classSimpleName, String classBody) {
        String src = "package com.example.appsearch;\n"
                + "import androidx.appsearch.annotation.Document;\n"
                + "import androidx.appsearch.annotation.Document.*;\n"
                + classBody;
        JavaFileObject jfo = JavaFileObjects.forSourceString(
                "com.example.appsearch." + classSimpleName,
                src);
        // Fully compiling this source code requires AppSearch to be on the classpath, but it only
        // builds on Android. Instead, this test configures the annotation processor to write to a
        // test-controlled path which is then diffed.
        String outputDirFlag = String.format(
                "-A%s=%s",
                AppSearchCompiler.OUTPUT_DIR_OPTION,
                mGenFilesDir.getAbsolutePath());
        return Compiler.javac()
                .withProcessors(new AppSearchCompiler(), new AutoValueProcessor())
                .withOptions(outputDirFlag)
                .compile(jfo);
    }

    private void checkEqualsGolden(String className) throws IOException {
        // Get the expected file contents
        String goldenResPath = "goldens/" + mTestName.getMethodName() + ".JAVA";
        String expected = "";
        try (InputStream is = getClass().getResourceAsStream(goldenResPath)) {
            if (is == null) {
                LOG.warning("Failed to find resource \"" + goldenResPath + "\"; treating as empty");
            } else {
                InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);
                expected = CharStreams.toString(reader);
            }
        }

        // Get the actual file contents
        File actualPackageDir = new File(mGenFilesDir, "com/example/appsearch");
        File actualPath =
                new File(actualPackageDir, IntrospectionHelper.GEN_CLASS_PREFIX + className);
        Truth.assertWithMessage("Path " + actualPath + " is not a file")
                .that(actualPath.isFile()).isTrue();
        String actual = Files.asCharSource(actualPath, StandardCharsets.UTF_8).read();

        // Compare!
        if (expected.equals(actual)) {
            return;
        }

        // Sadness. If we're running in an environment where source is available, rewrite the golden
        // to match the actual content for ease of updating the goldens.
        try {
            // At runtime, our resources come from the build tree. However, our cwd is
            // frameworks/support, so find the source tree from that.
            File goldenSrcDir = new File("src/test/resources/androidx/appsearch/compiler");
            if (!goldenSrcDir.isDirectory()) {
                LOG.warning("Failed to update goldens: golden dir \""
                        + goldenSrcDir.getAbsolutePath() + "\" does not exist or is not a folder");
                return;
            }
            File goldenFile = new File(goldenSrcDir, goldenResPath);
            Files.asCharSink(goldenFile, StandardCharsets.UTF_8).write(actual);
            LOG.info("Successfully updated golden file \"" + goldenFile + "\"");
        } finally {
            // Now produce the real exception for the test runner.
            Truth.assertThat(actual).isEqualTo(expected);
        }
    }

    private void checkResultContains(String className, String content) throws IOException {
        // Get the actual file contents
        File actualPackageDir = new File(mGenFilesDir, "com/example/appsearch");
        File actualPath =
                new File(actualPackageDir, IntrospectionHelper.GEN_CLASS_PREFIX + className);
        Truth.assertWithMessage("Path " + actualPath + " is not a file")
                .that(actualPath.isFile()).isTrue();
        String actual = Files.asCharSource(actualPath, StandardCharsets.UTF_8).read();

        Truth.assertThat(actual).contains(content);
    }
}
