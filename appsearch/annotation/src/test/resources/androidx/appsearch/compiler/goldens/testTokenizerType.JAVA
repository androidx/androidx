package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import java.lang.String;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("tokNone")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new AppSearchSchema.PropertyConfig.Builder("tokPlain")
      .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();

  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    String tokNoneCopy = dataClass.tokNone;
    if (tokNoneCopy != null) {
      builder.setProperty("tokNone", tokNoneCopy);
    }
    String tokPlainCopy = dataClass.tokPlain;
    if (tokPlainCopy != null) {
      builder.setProperty("tokPlain", tokPlainCopy);
    }
    return builder.build();
  }
}
