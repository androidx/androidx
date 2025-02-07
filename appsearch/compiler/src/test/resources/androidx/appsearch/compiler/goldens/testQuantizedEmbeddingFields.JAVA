package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.DocumentClassMappingContext;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.Collections;
import java.util.List;
import javax.annotation.processing.Generated;

@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__Gift implements DocumentClassFactory<Gift> {
  public static final String SCHEMA_NAME = "Gift";

  @Override
  public String getSchemaName() {
    return SCHEMA_NAME;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("name")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("defaultUnquantized")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("unquantized")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("quantized")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
            .setQuantizationType(AppSearchSchema.EmbeddingPropertyConfig.QUANTIZATION_TYPE_8_BIT)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    String nameCopy = document.name;
    if (nameCopy != null) {
      builder.setPropertyString("name", nameCopy);
    }
    EmbeddingVector defaultUnquantizedCopy = document.defaultUnquantized;
    if (defaultUnquantizedCopy != null) {
      builder.setPropertyEmbedding("defaultUnquantized", defaultUnquantizedCopy);
    }
    EmbeddingVector unquantizedCopy = document.unquantized;
    if (unquantizedCopy != null) {
      builder.setPropertyEmbedding("unquantized", unquantizedCopy);
    }
    EmbeddingVector quantizedCopy = document.quantized;
    if (quantizedCopy != null) {
      builder.setPropertyEmbedding("quantized", quantizedCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc,
      DocumentClassMappingContext documentClassMappingContext) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    String[] nameCopy = genericDoc.getPropertyStringArray("name");
    String nameConv = null;
    if (nameCopy != null && nameCopy.length != 0) {
      nameConv = nameCopy[0];
    }
    EmbeddingVector[] defaultUnquantizedCopy = genericDoc.getPropertyEmbeddingArray("defaultUnquantized");
    EmbeddingVector defaultUnquantizedConv = null;
    if (defaultUnquantizedCopy != null && defaultUnquantizedCopy.length != 0) {
      defaultUnquantizedConv = defaultUnquantizedCopy[0];
    }
    EmbeddingVector[] unquantizedCopy = genericDoc.getPropertyEmbeddingArray("unquantized");
    EmbeddingVector unquantizedConv = null;
    if (unquantizedCopy != null && unquantizedCopy.length != 0) {
      unquantizedConv = unquantizedCopy[0];
    }
    EmbeddingVector[] quantizedCopy = genericDoc.getPropertyEmbeddingArray("quantized");
    EmbeddingVector quantizedConv = null;
    if (quantizedCopy != null && quantizedCopy.length != 0) {
      quantizedConv = quantizedCopy[0];
    }
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.name = nameConv;
    document.defaultUnquantized = defaultUnquantizedConv;
    document.unquantized = unquantizedConv;
    document.quantized = quantizedConv;
    return document;
  }
}
