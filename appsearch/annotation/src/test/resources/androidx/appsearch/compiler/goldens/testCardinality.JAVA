package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PropertyConfig;
import java.lang.Float;
import java.lang.String;
import java.util.List;
import java.util.Set;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new PropertyConfig.Builder("repeatReq")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("repeatNoReq")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_REPEATED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("req")
      .setDataType(PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(PropertyConfig.CARDINALITY_REQUIRED)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("noReq")
      .setDataType(PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();

  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    List<String> repeatReqCopy = dataClass.repeatReq;
    if (repeatReqCopy != null) {
      String[] repeatReqConv = repeatReqCopy.toArray(new String[0]);
      builder.setProperty("repeatReq", repeatReqConv);
    }
    Set<String> repeatNoReqCopy = dataClass.repeatNoReq;
    if (repeatNoReqCopy != null) {
      String[] repeatNoReqConv = repeatNoReqCopy.toArray(new String[0]);
      builder.setProperty("repeatNoReq", repeatNoReqConv);
    }
    Float reqCopy = dataClass.req;
    if (reqCopy != null) {
      builder.setProperty("req", reqCopy);
    }
    Float noReqCopy = dataClass.noReq;
    if (noReqCopy != null) {
      builder.setProperty("noReq", noReqCopy);
    }
    return builder.build();
  }
}
