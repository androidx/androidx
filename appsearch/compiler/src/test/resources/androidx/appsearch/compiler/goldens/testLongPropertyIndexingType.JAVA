package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
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
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("defaultIndexNone")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("indexNone")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("boxInt")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("unboxInt")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("boxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("unboxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrBoxInt")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrUnboxInt")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrBoxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrUnboxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_RANGE)
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
    Long defaultIndexNoneCopy = document.defaultIndexNone;
    if (defaultIndexNoneCopy != null) {
      builder.setPropertyLong("defaultIndexNone", defaultIndexNoneCopy);
    }
    Long indexNoneCopy = document.indexNone;
    if (indexNoneCopy != null) {
      builder.setPropertyLong("indexNone", indexNoneCopy);
    }
    Integer boxIntCopy = document.boxInt;
    if (boxIntCopy != null) {
      builder.setPropertyLong("boxInt", boxIntCopy);
    }
    builder.setPropertyLong("unboxInt", document.unboxInt);
    Long boxLongCopy = document.boxLong;
    if (boxLongCopy != null) {
      builder.setPropertyLong("boxLong", boxLongCopy);
    }
    builder.setPropertyLong("unboxLong", document.unboxLong);
    Integer[] arrBoxIntCopy = document.arrBoxInt;
    if (arrBoxIntCopy != null) {
      long[] arrBoxIntConv = new long[arrBoxIntCopy.length];
      for (int i = 0 ; i < arrBoxIntCopy.length ; i++) {
        arrBoxIntConv[i] = arrBoxIntCopy[i];
      }
      builder.setPropertyLong("arrBoxInt", arrBoxIntConv);
    }
    int[] arrUnboxIntCopy = document.arrUnboxInt;
    if (arrUnboxIntCopy != null) {
      long[] arrUnboxIntConv = new long[arrUnboxIntCopy.length];
      for (int i = 0 ; i < arrUnboxIntCopy.length ; i++) {
        arrUnboxIntConv[i] = arrUnboxIntCopy[i];
      }
      builder.setPropertyLong("arrUnboxInt", arrUnboxIntConv);
    }
    Long[] arrBoxLongCopy = document.arrBoxLong;
    if (arrBoxLongCopy != null) {
      long[] arrBoxLongConv = new long[arrBoxLongCopy.length];
      for (int i = 0 ; i < arrBoxLongCopy.length ; i++) {
        arrBoxLongConv[i] = arrBoxLongCopy[i];
      }
      builder.setPropertyLong("arrBoxLong", arrBoxLongConv);
    }
    long[] arrUnboxLongCopy = document.arrUnboxLong;
    if (arrUnboxLongCopy != null) {
      builder.setPropertyLong("arrUnboxLong", arrUnboxLongCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    long[] defaultIndexNoneCopy = genericDoc.getPropertyLongArray("defaultIndexNone");
    Long defaultIndexNoneConv = null;
    if (defaultIndexNoneCopy != null && defaultIndexNoneCopy.length != 0) {
      defaultIndexNoneConv = defaultIndexNoneCopy[0];
    }
    long[] indexNoneCopy = genericDoc.getPropertyLongArray("indexNone");
    Long indexNoneConv = null;
    if (indexNoneCopy != null && indexNoneCopy.length != 0) {
      indexNoneConv = indexNoneCopy[0];
    }
    long[] boxIntCopy = genericDoc.getPropertyLongArray("boxInt");
    Integer boxIntConv = null;
    if (boxIntCopy != null && boxIntCopy.length != 0) {
      boxIntConv = (int) boxIntCopy[0];
    }
    int unboxIntConv = (int) genericDoc.getPropertyLong("unboxInt");
    long[] boxLongCopy = genericDoc.getPropertyLongArray("boxLong");
    Long boxLongConv = null;
    if (boxLongCopy != null && boxLongCopy.length != 0) {
      boxLongConv = boxLongCopy[0];
    }
    long unboxLongConv = genericDoc.getPropertyLong("unboxLong");
    long[] arrBoxIntCopy = genericDoc.getPropertyLongArray("arrBoxInt");
    Integer[] arrBoxIntConv = null;
    if (arrBoxIntCopy != null) {
      arrBoxIntConv = new Integer[arrBoxIntCopy.length];
      for (int i = 0; i < arrBoxIntCopy.length; i++) {
        arrBoxIntConv[i] = (int) arrBoxIntCopy[i];
      }
    }
    long[] arrUnboxIntCopy = genericDoc.getPropertyLongArray("arrUnboxInt");
    int[] arrUnboxIntConv = null;
    if (arrUnboxIntCopy != null) {
      arrUnboxIntConv = new int[arrUnboxIntCopy.length];
      for (int i = 0; i < arrUnboxIntCopy.length; i++) {
        arrUnboxIntConv[i] = (int) arrUnboxIntCopy[i];
      }
    }
    long[] arrBoxLongCopy = genericDoc.getPropertyLongArray("arrBoxLong");
    Long[] arrBoxLongConv = null;
    if (arrBoxLongCopy != null) {
      arrBoxLongConv = new Long[arrBoxLongCopy.length];
      for (int i = 0; i < arrBoxLongCopy.length; i++) {
        arrBoxLongConv[i] = arrBoxLongCopy[i];
      }
    }
    long[] arrUnboxLongConv = genericDoc.getPropertyLongArray("arrUnboxLong");
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.defaultIndexNone = defaultIndexNoneConv;
    document.indexNone = indexNoneConv;
    document.boxInt = boxIntConv;
    document.unboxInt = unboxIntConv;
    document.boxLong = boxLongConv;
    document.unboxLong = unboxLongConv;
    document.arrBoxInt = arrBoxIntConv;
    document.arrUnboxInt = arrUnboxIntConv;
    document.arrBoxLong = arrBoxLongConv;
    document.arrUnboxLong = arrUnboxLongConv;
    return document;
  }
}
