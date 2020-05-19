package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.app.PropertyConfig;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.String;

class $$__AppSearch__Gift {
  public static final String SCHEMA_TYPE = "Gift";

  public static final AppSearchSchema SCHEMA = new AppSearchSchema.Builder(SCHEMA_TYPE)
    .addProperty(new PropertyConfig.Builder("stringProp")
      .setDataType(PropertyConfig.DATA_TYPE_STRING)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("integerProp")
      .setDataType(PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("longProp")
      .setDataType(PropertyConfig.DATA_TYPE_INT64)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("floatProp")
      .setDataType(PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("doubleProp")
      .setDataType(PropertyConfig.DATA_TYPE_DOUBLE)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("booleanProp")
      .setDataType(PropertyConfig.DATA_TYPE_BOOLEAN)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .addProperty(new PropertyConfig.Builder("bytesProp")
      .setDataType(PropertyConfig.DATA_TYPE_BYTES)
      .setCardinality(PropertyConfig.CARDINALITY_OPTIONAL)
      .setTokenizerType(PropertyConfig.TOKENIZER_TYPE_PLAIN)
      .setIndexingType(PropertyConfig.INDEXING_TYPE_NONE)
      .build())
    .build();

  public GenericDocument toGenericDocument(Gift dataClass) {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    String stringPropCopy = dataClass.stringProp;
    if (stringPropCopy != null) {
      builder.setProperty("stringProp", stringPropCopy);
    }
    Integer integerPropCopy = dataClass.integerProp;
    if (integerPropCopy != null) {
      builder.setProperty("integerProp", integerPropCopy);
    }
    Long longPropCopy = dataClass.longProp;
    if (longPropCopy != null) {
      builder.setProperty("longProp", longPropCopy);
    }
    Float floatPropCopy = dataClass.floatProp;
    if (floatPropCopy != null) {
      builder.setProperty("floatProp", floatPropCopy);
    }
    Double doublePropCopy = dataClass.doubleProp;
    if (doublePropCopy != null) {
      builder.setProperty("doubleProp", doublePropCopy);
    }
    Boolean booleanPropCopy = dataClass.booleanProp;
    if (booleanPropCopy != null) {
      builder.setProperty("booleanProp", booleanPropCopy);
    }
    byte[] bytesPropCopy = dataClass.bytesProp;
    if (bytesPropCopy != null) {
      builder.setProperty("bytesProp", bytesPropCopy);
    }
    return builder.build();
  }
}
