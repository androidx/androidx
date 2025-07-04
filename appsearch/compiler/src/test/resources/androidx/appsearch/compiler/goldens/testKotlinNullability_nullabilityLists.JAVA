package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.DocumentClassMappingContext;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__KotlinGift implements DocumentClassFactory<KotlinGift> {
  public static final String SCHEMA_NAME = "KotlinGift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("nonNullStrings")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("nullableStrings")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nonNullLongs")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("nullableLongs")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("nonNullBooleans")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("nullableBooleans")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("nonNullCustomTypes", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("nullableCustomTypes", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("nonNullEmbeddings")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("nullableEmbeddings")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    List<Class<?>> classSet = new ArrayList<Class<?>>();
    classSet.add(Gift.class);
    return classSet;
  }

  @Override
  public GenericDocument toGenericDocument(KotlinGift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.getNamespace(), document.getId(), SCHEMA_NAME);
    List<String> nonNullStringsCopy = document.getNonNullStrings();
    if (nonNullStringsCopy != null) {
      String[] nonNullStringsConv = nonNullStringsCopy.toArray(new String[0]);
      builder.setPropertyString("nonNullStrings", nonNullStringsConv);
    }
    List<String> nullableStringsCopy = document.getNullableStrings();
    if (nullableStringsCopy != null) {
      String[] nullableStringsConv = nullableStringsCopy.toArray(new String[0]);
      builder.setPropertyString("nullableStrings", nullableStringsConv);
    }
    List<Long> nonNullLongsCopy = document.getNonNullLongs();
    if (nonNullLongsCopy != null) {
      long[] nonNullLongsConv = new long[nonNullLongsCopy.size()];
      int i = 0;
      for (Long item : nonNullLongsCopy) {
        nonNullLongsConv[i++] = item;
      }
      builder.setPropertyLong("nonNullLongs", nonNullLongsConv);
    }
    List<Long> nullableLongsCopy = document.getNullableLongs();
    if (nullableLongsCopy != null) {
      long[] nullableLongsConv = new long[nullableLongsCopy.size()];
      int i = 0;
      for (Long item : nullableLongsCopy) {
        nullableLongsConv[i++] = item;
      }
      builder.setPropertyLong("nullableLongs", nullableLongsConv);
    }
    List<Boolean> nonNullBooleansCopy = document.getNonNullBooleans();
    if (nonNullBooleansCopy != null) {
      boolean[] nonNullBooleansConv = new boolean[nonNullBooleansCopy.size()];
      int i = 0;
      for (Boolean item : nonNullBooleansCopy) {
        nonNullBooleansConv[i++] = item;
      }
      builder.setPropertyBoolean("nonNullBooleans", nonNullBooleansConv);
    }
    List<Boolean> nullableBooleansCopy = document.getNullableBooleans();
    if (nullableBooleansCopy != null) {
      boolean[] nullableBooleansConv = new boolean[nullableBooleansCopy.size()];
      int i = 0;
      for (Boolean item : nullableBooleansCopy) {
        nullableBooleansConv[i++] = item;
      }
      builder.setPropertyBoolean("nullableBooleans", nullableBooleansConv);
    }
    List<Gift> nonNullCustomTypesCopy = document.getNonNullCustomTypes();
    if (nonNullCustomTypesCopy != null) {
      GenericDocument[] nonNullCustomTypesConv = new GenericDocument[nonNullCustomTypesCopy.size()];
      int i = 0;
      for (Gift item : nonNullCustomTypesCopy) {
        nonNullCustomTypesConv[i++] = GenericDocument.fromDocumentClass(item);
      }
      builder.setPropertyDocument("nonNullCustomTypes", nonNullCustomTypesConv);
    }
    List<Gift> nullableCustomTypesCopy = document.getNullableCustomTypes();
    if (nullableCustomTypesCopy != null) {
      GenericDocument[] nullableCustomTypesConv = new GenericDocument[nullableCustomTypesCopy.size()];
      int i = 0;
      for (Gift item : nullableCustomTypesCopy) {
        nullableCustomTypesConv[i++] = GenericDocument.fromDocumentClass(item);
      }
      builder.setPropertyDocument("nullableCustomTypes", nullableCustomTypesConv);
    }
    List<EmbeddingVector> nonNullEmbeddingsCopy = document.getNonNullEmbeddings();
    if (nonNullEmbeddingsCopy != null) {
      EmbeddingVector[] nonNullEmbeddingsConv = nonNullEmbeddingsCopy.toArray(new EmbeddingVector[0]);
      builder.setPropertyEmbedding("nonNullEmbeddings", nonNullEmbeddingsConv);
    }
    List<EmbeddingVector> nullableEmbeddingsCopy = document.getNullableEmbeddings();
    if (nullableEmbeddingsCopy != null) {
      EmbeddingVector[] nullableEmbeddingsConv = nullableEmbeddingsCopy.toArray(new EmbeddingVector[0]);
      builder.setPropertyEmbedding("nullableEmbeddings", nullableEmbeddingsConv);
    }
    return builder.build();
  }

  @Override
  public KotlinGift fromGenericDocument(GenericDocument genericDoc,
      DocumentClassMappingContext documentClassMappingContext) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    String[] nonNullStringsCopy = genericDoc.getPropertyStringArray("nonNullStrings");
    List<String> nonNullStringsConv = Collections.emptyList();
    if (nonNullStringsCopy != null) {
      nonNullStringsConv = Arrays.asList(nonNullStringsCopy);
    }
    String[] nullableStringsCopy = genericDoc.getPropertyStringArray("nullableStrings");
    List<String> nullableStringsConv = null;
    if (nullableStringsCopy != null) {
      nullableStringsConv = Arrays.asList(nullableStringsCopy);
    }
    long[] nonNullLongsCopy = genericDoc.getPropertyLongArray("nonNullLongs");
    List<Long> nonNullLongsConv = Collections.emptyList();
    if (nonNullLongsCopy != null) {
      nonNullLongsConv = new ArrayList<>(nonNullLongsCopy.length);
      for (int i = 0; i < nonNullLongsCopy.length; i++) {
        nonNullLongsConv.add(nonNullLongsCopy[i]);
      }
    }
    long[] nullableLongsCopy = genericDoc.getPropertyLongArray("nullableLongs");
    List<Long> nullableLongsConv = null;
    if (nullableLongsCopy != null) {
      nullableLongsConv = new ArrayList<>(nullableLongsCopy.length);
      for (int i = 0; i < nullableLongsCopy.length; i++) {
        nullableLongsConv.add(nullableLongsCopy[i]);
      }
    }
    boolean[] nonNullBooleansCopy = genericDoc.getPropertyBooleanArray("nonNullBooleans");
    List<Boolean> nonNullBooleansConv = Collections.emptyList();
    if (nonNullBooleansCopy != null) {
      nonNullBooleansConv = new ArrayList<>(nonNullBooleansCopy.length);
      for (int i = 0; i < nonNullBooleansCopy.length; i++) {
        nonNullBooleansConv.add(nonNullBooleansCopy[i]);
      }
    }
    boolean[] nullableBooleansCopy = genericDoc.getPropertyBooleanArray("nullableBooleans");
    List<Boolean> nullableBooleansConv = null;
    if (nullableBooleansCopy != null) {
      nullableBooleansConv = new ArrayList<>(nullableBooleansCopy.length);
      for (int i = 0; i < nullableBooleansCopy.length; i++) {
        nullableBooleansConv.add(nullableBooleansCopy[i]);
      }
    }
    GenericDocument[] nonNullCustomTypesCopy = genericDoc.getPropertyDocumentArray("nonNullCustomTypes");
    List<Gift> nonNullCustomTypesConv = Collections.emptyList();
    if (nonNullCustomTypesCopy != null) {
      nonNullCustomTypesConv = new ArrayList<>(nonNullCustomTypesCopy.length);
      for (int i = 0; i < nonNullCustomTypesCopy.length; i++) {
        nonNullCustomTypesConv.add(nonNullCustomTypesCopy[i].toDocumentClass(Gift.class, documentClassMappingContext));
      }
    }
    GenericDocument[] nullableCustomTypesCopy = genericDoc.getPropertyDocumentArray("nullableCustomTypes");
    List<Gift> nullableCustomTypesConv = null;
    if (nullableCustomTypesCopy != null) {
      nullableCustomTypesConv = new ArrayList<>(nullableCustomTypesCopy.length);
      for (int i = 0; i < nullableCustomTypesCopy.length; i++) {
        nullableCustomTypesConv.add(nullableCustomTypesCopy[i].toDocumentClass(Gift.class, documentClassMappingContext));
      }
    }
    EmbeddingVector[] nonNullEmbeddingsCopy = genericDoc.getPropertyEmbeddingArray("nonNullEmbeddings");
    List<EmbeddingVector> nonNullEmbeddingsConv = Collections.emptyList();
    if (nonNullEmbeddingsCopy != null) {
      nonNullEmbeddingsConv = Arrays.asList(nonNullEmbeddingsCopy);
    }
    EmbeddingVector[] nullableEmbeddingsCopy = genericDoc.getPropertyEmbeddingArray("nullableEmbeddings");
    List<EmbeddingVector> nullableEmbeddingsConv = null;
    if (nullableEmbeddingsCopy != null) {
      nullableEmbeddingsConv = Arrays.asList(nullableEmbeddingsCopy);
    }
    KotlinGift document = new KotlinGift(namespaceConv, idConv, nonNullStringsConv, nullableStringsConv, nonNullLongsConv, nullableLongsConv, nonNullBooleansConv, nullableBooleansConv, nonNullCustomTypesConv, nullableCustomTypesConv, nonNullEmbeddingsConv, nullableEmbeddingsConv);
    return document;
  }
}
