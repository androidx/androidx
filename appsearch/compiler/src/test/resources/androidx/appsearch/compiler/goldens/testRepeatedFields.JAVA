package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Integer;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("listOfString")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("setOfInt")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("repeatedByteArray")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("byteArray")
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
    List<String> listOfStringCopy = document.listOfString;
    if (listOfStringCopy != null) {
      String[] listOfStringConv = listOfStringCopy.toArray(new String[0]);
      builder.setPropertyString("listOfString", listOfStringConv);
    }
    Collection<Integer> setOfIntCopy = document.setOfInt;
    if (setOfIntCopy != null) {
      long[] setOfIntConv = new long[setOfIntCopy.size()];
      int i = 0;
      for (Integer item : setOfIntCopy) {
        setOfIntConv[i++] = item;
      }
      builder.setPropertyLong("setOfInt", setOfIntConv);
    }
    byte[][] repeatedByteArrayCopy = document.repeatedByteArray;
    if (repeatedByteArrayCopy != null) {
      builder.setPropertyBytes("repeatedByteArray", repeatedByteArrayCopy);
    }
    byte[] byteArrayCopy = document.byteArray;
    if (byteArrayCopy != null) {
      builder.setPropertyBytes("byteArray", byteArrayCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
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
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.listOfString = listOfStringConv;
    document.setOfInt = setOfIntConv;
    document.repeatedByteArray = repeatedByteArrayConv;
    document.byteArray = byteArrayConv;
    return document;
  }
}
