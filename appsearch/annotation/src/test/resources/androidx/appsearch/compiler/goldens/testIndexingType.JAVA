package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PropertyConfig;
import java.lang.String;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new PropertyConfig.Builder("indexNone")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("indexExact")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_EXACT_TERMS)
      .build())
    .addProperty(new PropertyConfig.Builder("indexPrefix")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_PREFIXES)
      .build())
    .build();

  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    String indexNoneCopy = dataClass.indexNone;
    if (indexNoneCopy != null) {
      builder.setProperty("indexNone", indexNoneCopy);
    }
    String indexExactCopy = dataClass.indexExact;
    if (indexExactCopy != null) {
      builder.setProperty("indexExact", indexExactCopy);
    }
    String indexPrefixCopy = dataClass.indexPrefix;
    if (indexPrefixCopy != null) {
      builder.setProperty("indexPrefix", indexPrefixCopy);
    }
    return builder.build();
  }
}
