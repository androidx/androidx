package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
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
          .addParentType($$__AppSearch__Parent1.SCHEMA_NAME)
          .addParentType($$__AppSearch__Parent2.SCHEMA_NAME)
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("str2")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("str1")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("price")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    List<Class<?>> classSet = new ArrayList<Class<?>>();
    classSet.add(Parent1.class);
    classSet.add(Parent2.class);
    return classSet;
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.getNamespace(), document.getId(), SCHEMA_NAME);
    String getStr2Copy = document.getStr2();
    if (getStr2Copy != null) {
      builder.setPropertyString("str2", getStr2Copy);
    }
    String getStr1Copy = document.getStr1();
    if (getStr1Copy != null) {
      builder.setPropertyString("str1", getStr1Copy);
    }
    builder.setPropertyLong("price", document.getPrice());
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String getNamespaceConv = genericDoc.getNamespace();
    String getIdConv = genericDoc.getId();
    String[] getStr2Copy = genericDoc.getPropertyStringArray("str2");
    String getStr2Conv = null;
    if (getStr2Copy != null && getStr2Copy.length != 0) {
      getStr2Conv = getStr2Copy[0];
    }
    String[] getStr1Copy = genericDoc.getPropertyStringArray("str1");
    String getStr1Conv = null;
    if (getStr1Copy != null && getStr1Copy.length != 0) {
      getStr1Conv = getStr1Copy[0];
    }
    int getPriceConv = (int) genericDoc.getPropertyLong("price");
    Gift document = Gift.create(getIdConv, getNamespaceConv, getStr1Conv, getStr2Conv, getPriceConv);
    return document;
  }
}
