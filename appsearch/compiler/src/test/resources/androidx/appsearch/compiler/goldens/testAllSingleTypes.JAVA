package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Boolean;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;

public class $$__AppSearch__Gift implements DataClassFactory<Gift> {
  private static final String SCHEMA_TYPE = "Gift";

  @Override
  public String getSchemaType() {
    return SCHEMA_TYPE;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_TYPE)
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("stringProp")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("integerProp")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("longProp")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("floatProp")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("doubleProp")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("booleanProp")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BOOLEAN)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("bytesProp")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public GenericDocument toGenericDocument(Gift dataClass) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(dataClass.uri, SCHEMA_TYPE);
    String stringPropCopy = dataClass.stringProp;
    if (stringPropCopy != null) {
      builder.setPropertyString("stringProp", stringPropCopy);
    }
    Integer integerPropCopy = dataClass.integerProp;
    if (integerPropCopy != null) {
      builder.setPropertyLong("integerProp", integerPropCopy);
    }
    Long longPropCopy = dataClass.longProp;
    if (longPropCopy != null) {
      builder.setPropertyLong("longProp", longPropCopy);
    }
    Float floatPropCopy = dataClass.floatProp;
    if (floatPropCopy != null) {
      builder.setPropertyDouble("floatProp", floatPropCopy);
    }
    Double doublePropCopy = dataClass.doubleProp;
    if (doublePropCopy != null) {
      builder.setPropertyDouble("doubleProp", doublePropCopy);
    }
    Boolean booleanPropCopy = dataClass.booleanProp;
    if (booleanPropCopy != null) {
      builder.setPropertyBoolean("booleanProp", booleanPropCopy);
    }
    byte[] bytesPropCopy = dataClass.bytesProp;
    if (bytesPropCopy != null) {
      builder.setPropertyBytes("bytesProp", bytesPropCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
    String[] stringPropCopy = genericDoc.getPropertyStringArray("stringProp");
    String stringPropConv = null;
    if (stringPropCopy != null && stringPropCopy.length != 0) {
      stringPropConv = stringPropCopy[0];
    }
    long[] integerPropCopy = genericDoc.getPropertyLongArray("integerProp");
    Integer integerPropConv = null;
    if (integerPropCopy != null && integerPropCopy.length != 0) {
      integerPropConv = (int) integerPropCopy[0];
    }
    long[] longPropCopy = genericDoc.getPropertyLongArray("longProp");
    Long longPropConv = null;
    if (longPropCopy != null && longPropCopy.length != 0) {
      longPropConv = longPropCopy[0];
    }
    double[] floatPropCopy = genericDoc.getPropertyDoubleArray("floatProp");
    Float floatPropConv = null;
    if (floatPropCopy != null && floatPropCopy.length != 0) {
      floatPropConv = (float) floatPropCopy[0];
    }
    double[] doublePropCopy = genericDoc.getPropertyDoubleArray("doubleProp");
    Double doublePropConv = null;
    if (doublePropCopy != null && doublePropCopy.length != 0) {
      doublePropConv = doublePropCopy[0];
    }
    boolean[] booleanPropCopy = genericDoc.getPropertyBooleanArray("booleanProp");
    Boolean booleanPropConv = null;
    if (booleanPropCopy != null && booleanPropCopy.length != 0) {
      booleanPropConv = booleanPropCopy[0];
    }
    byte[][] bytesPropCopy = genericDoc.getPropertyBytesArray("bytesProp");
    byte[] bytesPropConv = null;
    if (bytesPropCopy != null && bytesPropCopy.length != 0) {
      bytesPropConv = bytesPropCopy[0];
    }
    Gift dataClass = new Gift();
    dataClass.uri = uriConv;
    dataClass.stringProp = stringPropConv;
    dataClass.integerProp = integerPropConv;
    dataClass.longProp = longPropConv;
    dataClass.floatProp = floatPropConv;
    dataClass.doubleProp = doublePropConv;
    dataClass.booleanProp = booleanPropConv;
    dataClass.bytesProp = bytesPropConv;
    return dataClass;
  }
}
