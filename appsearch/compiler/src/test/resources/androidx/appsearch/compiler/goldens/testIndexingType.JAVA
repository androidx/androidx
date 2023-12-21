package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("indexNone")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("indexExact")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_EXACT_TERMS)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("indexPrefix")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_PLAIN)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_PREFIXES)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getNestedDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    String indexNoneCopy = document.indexNone;
    if (indexNoneCopy != null) {
      builder.setPropertyString("indexNone", indexNoneCopy);
    }
    String indexExactCopy = document.indexExact;
    if (indexExactCopy != null) {
      builder.setPropertyString("indexExact", indexExactCopy);
    }
    String indexPrefixCopy = document.indexPrefix;
    if (indexPrefixCopy != null) {
      builder.setPropertyString("indexPrefix", indexPrefixCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    String[] indexNoneCopy = genericDoc.getPropertyStringArray("indexNone");
    String indexNoneConv = null;
    if (indexNoneCopy != null && indexNoneCopy.length != 0) {
      indexNoneConv = indexNoneCopy[0];
    }
    String[] indexExactCopy = genericDoc.getPropertyStringArray("indexExact");
    String indexExactConv = null;
    if (indexExactCopy != null && indexExactCopy.length != 0) {
      indexExactConv = indexExactCopy[0];
    }
    String[] indexPrefixCopy = genericDoc.getPropertyStringArray("indexPrefix");
    String indexPrefixConv = null;
    if (indexPrefixCopy != null && indexPrefixCopy.length != 0) {
      indexPrefixConv = indexPrefixCopy[0];
    }
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.indexNone = indexNoneConv;
    document.indexExact = indexExactConv;
    document.indexPrefix = indexPrefixConv;
    return document;
  }
}
