package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Boolean;
import java.lang.Class;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("stringProp")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("integerProp")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("longProp")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("floatProp")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("doubleProp")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("booleanProp")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("bytesProp")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
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
    String stringPropCopy = document.stringProp;
    if (stringPropCopy != null) {
      builder.setPropertyString("stringProp", stringPropCopy);
    }
    Integer integerPropCopy = document.integerProp;
    if (integerPropCopy != null) {
      builder.setPropertyLong("integerProp", integerPropCopy);
    }
    Long longPropCopy = document.longProp;
    if (longPropCopy != null) {
      builder.setPropertyLong("longProp", longPropCopy);
    }
    Float floatPropCopy = document.floatProp;
    if (floatPropCopy != null) {
      builder.setPropertyDouble("floatProp", floatPropCopy);
    }
    Double doublePropCopy = document.doubleProp;
    if (doublePropCopy != null) {
      builder.setPropertyDouble("doubleProp", doublePropCopy);
    }
    Boolean booleanPropCopy = document.booleanProp;
    if (booleanPropCopy != null) {
      builder.setPropertyBoolean("booleanProp", booleanPropCopy);
    }
    byte[] bytesPropCopy = document.bytesProp;
    if (bytesPropCopy != null) {
      builder.setPropertyBytes("bytesProp", bytesPropCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
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
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.stringProp = stringPropConv;
    document.integerProp = integerPropConv;
    document.longProp = longPropConv;
    document.floatProp = floatPropConv;
    document.doubleProp = doublePropConv;
    document.booleanProp = booleanPropConv;
    document.bytesProp = bytesPropConv;
    return document;
  }
}
