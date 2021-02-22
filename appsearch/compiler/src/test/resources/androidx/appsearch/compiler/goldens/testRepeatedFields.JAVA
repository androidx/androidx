package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class $$__AppSearch__Gift implements DataClassFactory<Gift> {
  private static final String SCHEMA_TYPE = "Gift";

  @Override
  public String getSchemaType() {
    return SCHEMA_TYPE;
  }

  @Override
  public AppSearchSchema getSchema() throws AppSearchException {
    return new AppSearchSchema.Builder(SCHEMA_TYPE)
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("listOfString")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("setOfInt")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_INT64)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("repeatedByteArray")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_BYTES)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("byteArray")
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
    List<String> listOfStringCopy = dataClass.listOfString;
    if (listOfStringCopy != null) {
      String[] listOfStringConv = listOfStringCopy.toArray(new String[0]);
      builder.setPropertyString("listOfString", listOfStringConv);
    }
    Collection<Integer> setOfIntCopy = dataClass.setOfInt;
    if (setOfIntCopy != null) {
      long[] setOfIntConv = new long[setOfIntCopy.size()];
      int i = 0;
      for (Integer item : setOfIntCopy) {
        setOfIntConv[i++] = item;
      }
      builder.setPropertyLong("setOfInt", setOfIntConv);
    }
    byte[][] repeatedByteArrayCopy = dataClass.repeatedByteArray;
    if (repeatedByteArrayCopy != null) {
      builder.setPropertyBytes("repeatedByteArray", repeatedByteArrayCopy);
    }
    byte[] byteArrayCopy = dataClass.byteArray;
    if (byteArrayCopy != null) {
      builder.setPropertyBytes("byteArray", byteArrayCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
    String[] listOfStringCopy = genericDoc.getPropertyStringArray("listOfString");
    List<String> listOfStringConv = null;
    if (listOfStringCopy != null) {
      listOfStringConv = Arrays.asList(listOfStringCopy);
    }
    long[] setOfIntCopy = genericDoc.getPropertyLongArray("setOfInt");
    List<Integer> setOfIntConv = null;
    if (setOfIntCopy != null) {
      setOfIntConv = new ArrayList<>(setOfIntCopy.length);
      for (int i = 0; i < setOfIntCopy.length; i++) {
        setOfIntConv.add((int) setOfIntCopy[i]);
      }
    }
    byte[][] repeatedByteArrayConv = genericDoc.getPropertyBytesArray("repeatedByteArray");
    byte[][] byteArrayCopy = genericDoc.getPropertyBytesArray("byteArray");
    byte[] byteArrayConv = null;
    if (byteArrayCopy != null && byteArrayCopy.length != 0) {
      byteArrayConv = byteArrayCopy[0];
    }
    Gift dataClass = new Gift();
    dataClass.uri = uriConv;
    dataClass.listOfString = listOfStringConv;
    dataClass.setOfInt = setOfIntConv;
    dataClass.repeatedByteArray = repeatedByteArrayConv;
    dataClass.byteArray = byteArrayConv;
    return dataClass;
  }
}
