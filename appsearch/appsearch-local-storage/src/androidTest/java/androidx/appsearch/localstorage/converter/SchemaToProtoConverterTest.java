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

package androidx.appsearch.localstorage.converter;

import static com.google.common.truth.Truth.assertThat;

import androidx.appsearch.app.AppSearchSchema;

import com.google.android.icing.proto.DocumentIndexingConfig;
import com.google.android.icing.proto.EmbeddingIndexingConfig;
import com.google.android.icing.proto.JoinableConfig;
import com.google.android.icing.proto.PropertyConfigProto;
import com.google.android.icing.proto.SchemaTypeConfigProto;
import com.google.android.icing.proto.StringIndexingConfig;
import com.google.android.icing.proto.TermMatchType;

import org.junit.Test;

import java.util.Arrays;

public class SchemaToProtoConverterTest {
    @Test
    public void testGetProto_DescriptionSet() {
        AppSearchSchema emailSchema =
                new AppSearchSchema.Builder("Email")
                        .setDescription("A type of electronic message.")
                        .addProperty(
                                new AppSearchSchema.StringPropertyConfig.Builder("subject")
                                        .setDescription("The most important part.")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .setIndexingType(
                                                AppSearchSchema.StringPropertyConfig
                                                        .INDEXING_TYPE_PREFIXES)
                                        .setTokenizerType(
                                                AppSearchSchema.StringPropertyConfig
                                                        .TOKENIZER_TYPE_PLAIN)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.LongPropertyConfig.Builder("timestamp")
                                        .setDescription("The time at which the email was sent.")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.DoublePropertyConfig.Builder("importanceScore")
                                        .setDescription(
                                                "A value representing this document's importance.")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.BooleanPropertyConfig.Builder("read")
                                        .setDescription(
                                                "Whether the email has been read by the recipient")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .addProperty(
                                new AppSearchSchema.BytesPropertyConfig.Builder("attachment")
                                        .setDescription("Documents that are attached to the email.")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                                        .build())
                        // We don't need to actually define the Person type for this test because
                        // the converter will process each schema individually.
                        .addProperty(
                                new AppSearchSchema.DocumentPropertyConfig.Builder(
                                        "sender", "Person")
                                        .setDescription("The person who wrote this email.")
                                        .setCardinality(
                                                AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                        .build())
                        .build();

        SchemaTypeConfigProto expectedEmailProto =
                SchemaTypeConfigProto.newBuilder()
                        .setSchemaType("Email")
                        .setDescription("A type of electronic message.")
                        .setVersion(12345)
                        .addProperties(
                                PropertyConfigProto.newBuilder()
                                        .setPropertyName("subject")
                                        .setDescription("The most important part.")
                                        .setDataType(PropertyConfigProto.DataType.Code.STRING)
                                        .setCardinality(
                                                PropertyConfigProto.Cardinality.Code.OPTIONAL)
                                        .setStringIndexingConfig(
                                                StringIndexingConfig.newBuilder()
                                                        .setTokenizerType(
                                                                StringIndexingConfig.TokenizerType
                                                                        .Code.PLAIN)
                                                        .setTermMatchType(
                                                                TermMatchType.Code.PREFIX)))
                        .addProperties(
                                PropertyConfigProto.newBuilder()
                                        .setPropertyName("timestamp")
                                        .setDescription("The time at which the email was sent.")
                                        .setDataType(PropertyConfigProto.DataType.Code.INT64)
                                        .setCardinality(
                                                PropertyConfigProto.Cardinality.Code.OPTIONAL))
                        .addProperties(
                                PropertyConfigProto.newBuilder()
                                        .setPropertyName("importanceScore")
                                        .setDescription(
                                                "A value representing this document's importance.")
                                        .setDataType(PropertyConfigProto.DataType.Code.DOUBLE)
                                        .setCardinality(
                                                PropertyConfigProto.Cardinality.Code.OPTIONAL))
                        .addProperties(
                                PropertyConfigProto.newBuilder()
                                        .setPropertyName("read")
                                        .setDescription(
                                                "Whether the email has been read by the recipient")
                                        .setDataType(PropertyConfigProto.DataType.Code.BOOLEAN)
                                        .setCardinality(
                                                PropertyConfigProto.Cardinality.Code.OPTIONAL))
                        .addProperties(
                                PropertyConfigProto.newBuilder()
                                        .setPropertyName("attachment")
                                        .setDescription("Documents that are attached to the email.")
                                        .setDataType(PropertyConfigProto.DataType.Code.BYTES)
                                        .setCardinality(
                                                PropertyConfigProto.Cardinality.Code.REPEATED))
                        .addProperties(
                                PropertyConfigProto.newBuilder()
                                        .setPropertyName("sender")
                                        .setSchemaType("Person")
                                        .setDescription("The person who wrote this email.")
                                        .setDataType(PropertyConfigProto.DataType.Code.DOCUMENT)
                                        .setCardinality(
                                                PropertyConfigProto.Cardinality.Code.OPTIONAL)
                                        .setDocumentIndexingConfig(
                                                DocumentIndexingConfig.newBuilder()
                                                        .setIndexNestedProperties(false)))
                        .build();

        assertThat(SchemaToProtoConverter.toSchemaTypeConfigProto(emailSchema, /*version=*/ 12345))
                .isEqualTo(expectedEmailProto);
        assertThat(SchemaToProtoConverter.toAppSearchSchema(expectedEmailProto))
                .isEqualTo(emailSchema);
    }

    @Test
    public void testGetProto_Email() {
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(
                                AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(
                                AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).build();

        SchemaTypeConfigProto expectedEmailProto = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("Email")
                .setDescription("")
                .setVersion(12345)
                .addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("subject")
                        .setDescription("")
                        .setDataType(PropertyConfigProto.DataType.Code.STRING)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                        .setStringIndexingConfig(
                                StringIndexingConfig.newBuilder()
                                        .setTokenizerType(
                                                StringIndexingConfig.TokenizerType.Code.PLAIN)
                                        .setTermMatchType(TermMatchType.Code.PREFIX)
                        )
                ).addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("body")
                        .setDescription("")
                        .setDataType(PropertyConfigProto.DataType.Code.STRING)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                        .setStringIndexingConfig(
                                StringIndexingConfig.newBuilder()
                                        .setTokenizerType(
                                                StringIndexingConfig.TokenizerType.Code.PLAIN)
                                        .setTermMatchType(TermMatchType.Code.PREFIX)
                        )
                ).build();

        assertThat(SchemaToProtoConverter.toSchemaTypeConfigProto(emailSchema, /*version=*/12345))
                .isEqualTo(expectedEmailProto);
        assertThat(SchemaToProtoConverter.toAppSearchSchema(expectedEmailProto))
                .isEqualTo(emailSchema);
    }

    @Test
    public void testGetProto_MusicRecording() {
        AppSearchSchema musicRecordingSchema = new AppSearchSchema.Builder("MusicRecording")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("artist")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(
                                AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new AppSearchSchema.LongPropertyConfig.Builder("pubDate")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .build()
                ).build();

        SchemaTypeConfigProto expectedMusicRecordingProto = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("MusicRecording")
                .setDescription("")
                .setVersion(0)
                .addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("artist")
                        .setDescription("")
                        .setDataType(PropertyConfigProto.DataType.Code.STRING)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.REPEATED)
                        .setStringIndexingConfig(
                                StringIndexingConfig.newBuilder()
                                        .setTokenizerType(
                                                StringIndexingConfig.TokenizerType.Code.PLAIN)
                                        .setTermMatchType(TermMatchType.Code.PREFIX)
                        )
                ).addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("pubDate")
                        .setDescription("")
                        .setDataType(PropertyConfigProto.DataType.Code.INT64)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                ).build();

        assertThat(SchemaToProtoConverter.toSchemaTypeConfigProto(
                musicRecordingSchema, /*version=*/0))
                .isEqualTo(expectedMusicRecordingProto);
        assertThat(SchemaToProtoConverter.toAppSearchSchema(expectedMusicRecordingProto))
                .isEqualTo(musicRecordingSchema);
    }

    @Test
    public void testGetProto_JoinableConfig() {
        AppSearchSchema albumSchema = new AppSearchSchema.Builder("Album")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("artist")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setJoinableValueType(AppSearchSchema.StringPropertyConfig
                                .JOINABLE_VALUE_TYPE_QUALIFIED_ID)
                        .build()
                ).build();

        JoinableConfig joinableConfig = JoinableConfig.newBuilder()
                .setValueType(JoinableConfig.ValueType.Code.QUALIFIED_ID)
                .build();

        SchemaTypeConfigProto expectedAlbumProto = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("Album")
                .setDescription("")
                .setVersion(0)
                .addProperties(
                        PropertyConfigProto.newBuilder()
                                .setPropertyName("artist")
                                .setDescription("")
                                .setDataType(PropertyConfigProto.DataType.Code.STRING)
                                .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                                .setStringIndexingConfig(StringIndexingConfig.newBuilder()
                                        .setTermMatchType(TermMatchType.Code.UNKNOWN)
                                        .setTokenizerType(
                                                StringIndexingConfig.TokenizerType.Code.NONE))
                                .setJoinableConfig(joinableConfig))
                .build();

        assertThat(SchemaToProtoConverter.toSchemaTypeConfigProto(albumSchema, /*version=*/0))
                .isEqualTo(expectedAlbumProto);
        assertThat(SchemaToProtoConverter.toAppSearchSchema(expectedAlbumProto))
                .isEqualTo(albumSchema);
    }

    @Test
    public void testGetProto_ParentTypes() {
        AppSearchSchema schema = new AppSearchSchema.Builder("EmailMessage")
                .addParentType("Email")
                .addParentType("Message")
                .build();

        SchemaTypeConfigProto expectedSchemaProto = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("EmailMessage")
                .setDescription("")
                .setVersion(12345)
                .addParentTypes("Email")
                .addParentTypes("Message")
                .build();

        assertThat(SchemaToProtoConverter.toSchemaTypeConfigProto(schema, /*version=*/12345))
                .isEqualTo(expectedSchemaProto);
        assertThat(SchemaToProtoConverter.toAppSearchSchema(expectedSchemaProto))
                .isEqualTo(schema);
    }

    @Test
    public void testGetProto_DocumentIndexingConfig() {
        AppSearchSchema personSchema = new AppSearchSchema.Builder("Person")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("name")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build())
                .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("worksFor",
                        "Organization")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setShouldIndexNestedProperties(false)
                        .addIndexableNestedProperties(Arrays.asList("orgName", "notes"))
                        .build())
                .build();

        DocumentIndexingConfig documentIndexingConfig = DocumentIndexingConfig.newBuilder()
                .setIndexNestedProperties(false)
                .addIndexableNestedPropertiesList("orgName")
                .addIndexableNestedPropertiesList("notes")
                .build();

        SchemaTypeConfigProto expectedPersonProto = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("Person")
                .setDescription("")
                .setVersion(0)
                .addProperties(
                        PropertyConfigProto.newBuilder()
                                .setPropertyName("name")
                                .setDescription("")
                                .setDataType(PropertyConfigProto.DataType.Code.STRING)
                                .setCardinality(PropertyConfigProto.Cardinality.Code.REQUIRED)
                                .setStringIndexingConfig(StringIndexingConfig.newBuilder()
                                        .setTermMatchType(TermMatchType.Code.PREFIX)
                                        .setTokenizerType(
                                                StringIndexingConfig.TokenizerType.Code.PLAIN)))
                .addProperties(
                        PropertyConfigProto.newBuilder()
                                .setPropertyName("worksFor")
                                .setDescription("")
                                .setDataType(PropertyConfigProto.DataType.Code.DOCUMENT)
                                .setSchemaType("Organization")
                                .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                                .setDocumentIndexingConfig(documentIndexingConfig))
                .build();

        assertThat(SchemaToProtoConverter.toSchemaTypeConfigProto(personSchema, /*version=*/0))
                .isEqualTo(expectedPersonProto);
        assertThat(SchemaToProtoConverter.toAppSearchSchema(expectedPersonProto))
                .isEqualTo(personSchema);
    }

    @Test
    public void testGetProto_EmbeddingProperty() {
        AppSearchSchema emailSchema = new AppSearchSchema.Builder("Email")
                .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("subject")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(
                                AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(new AppSearchSchema.StringPropertyConfig.Builder("body")
                        .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                        .setIndexingType(
                                AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
                        .setTokenizerType(
                                AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
                        .build()
                ).addProperty(
                        new AppSearchSchema.EmbeddingPropertyConfig.Builder("embedding")
                                .setCardinality(
                                        AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        AppSearchSchema.EmbeddingPropertyConfig
                                                .INDEXING_TYPE_NONE)
                                .build())
                .addProperty(
                        new AppSearchSchema.EmbeddingPropertyConfig.Builder("indexableEmbedding")
                                .setCardinality(
                                        AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
                                .setIndexingType(
                                        AppSearchSchema.EmbeddingPropertyConfig
                                                .INDEXING_TYPE_SIMILARITY)
                                .build())
                .build();

        SchemaTypeConfigProto expectedEmailProto = SchemaTypeConfigProto.newBuilder()
                .setSchemaType("Email")
                .setDescription("")
                .setVersion(12345)
                .addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("subject")
                        .setDescription("")
                        .setDataType(PropertyConfigProto.DataType.Code.STRING)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                        .setStringIndexingConfig(
                                StringIndexingConfig.newBuilder()
                                        .setTokenizerType(
                                                StringIndexingConfig.TokenizerType.Code.PLAIN)
                                        .setTermMatchType(TermMatchType.Code.PREFIX)
                        )
                ).addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("body")
                        .setDescription("")
                        .setDataType(PropertyConfigProto.DataType.Code.STRING)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                        .setStringIndexingConfig(
                                StringIndexingConfig.newBuilder()
                                        .setTokenizerType(
                                                StringIndexingConfig.TokenizerType.Code.PLAIN)
                                        .setTermMatchType(TermMatchType.Code.PREFIX)
                        )
                ).addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("embedding")
                        .setDescription("")
                        .setDataType(PropertyConfigProto.DataType.Code.VECTOR)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                ).addProperties(PropertyConfigProto.newBuilder()
                        .setPropertyName("indexableEmbedding")
                        .setDescription("")
                        .setDataType(PropertyConfigProto.DataType.Code.VECTOR)
                        .setCardinality(PropertyConfigProto.Cardinality.Code.OPTIONAL)
                        .setEmbeddingIndexingConfig(
                                EmbeddingIndexingConfig.newBuilder()
                                        .setEmbeddingIndexingType(
                                                EmbeddingIndexingConfig.EmbeddingIndexingType.Code
                                                        .LINEAR_SEARCH)
                        )
                ).build();

        assertThat(SchemaToProtoConverter.toSchemaTypeConfigProto(emailSchema, /*version=*/12345))
                .isEqualTo(expectedEmailProto);
        assertThat(SchemaToProtoConverter.toAppSearchSchema(expectedEmailProto))
                .isEqualTo(emailSchema);
    }
}
