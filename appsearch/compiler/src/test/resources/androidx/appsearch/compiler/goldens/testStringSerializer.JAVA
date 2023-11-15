package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("url")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("urlList")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("urlArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getDependencyDocumentClasses() throws AppSearchException {
    return Collections.emptyList();
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.mNamespace, document.mId, SCHEMA_NAME);
    URL mUrlCopy = document.mUrl;
    if (mUrlCopy != null) {
      Gift.UrlAsStringSerializer serializer = new Gift.UrlAsStringSerializer();
      String mUrlConv = serializer.serialize(mUrlCopy);
      builder.setPropertyString("url", mUrlConv);
    }
    List<URL> mUrlListCopy = document.mUrlList;
    if (mUrlListCopy != null) {
      String[] mUrlListConv = new String[mUrlListCopy.size()];
      Gift.UrlAsStringSerializer serializer = new Gift.UrlAsStringSerializer();
      int i = 0;
      for (URL item : mUrlListCopy) {
        mUrlListConv[i++] = serializer.serialize(item);
      }
      builder.setPropertyString("urlList", mUrlListConv);
    }
    URL[] mUrlArrCopy = document.mUrlArr;
    if (mUrlArrCopy != null) {
      String[] mUrlArrConv = new String[mUrlArrCopy.length];
      Gift.UrlAsStringSerializer serializer = new Gift.UrlAsStringSerializer();
      for (int i = 0; i < mUrlArrConv.length; i++) {
        mUrlArrConv[i] = serializer.serialize(mUrlArrCopy[i]);
      }
      builder.setPropertyString("urlArr", mUrlArrConv);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc,
      Map<String, List<String>> documentClassMap) throws AppSearchException {
    String mIdConv = genericDoc.getId();
    String mNamespaceConv = genericDoc.getNamespace();
    String mUrlCopy = genericDoc.getPropertyString("url");
    URL mUrlConv = null;
    if (mUrlCopy != null) {
      mUrlConv = new Gift.UrlAsStringSerializer().deserialize(mUrlCopy);
    }
    String[] mUrlListCopy = genericDoc.getPropertyStringArray("urlList");
    List<URL> mUrlListConv = null;
    if (mUrlListCopy != null) {
      mUrlListConv = new ArrayList<>(mUrlListCopy.length);
      Gift.UrlAsStringSerializer serializer = new Gift.UrlAsStringSerializer();
      for (int i = 0; i < mUrlListCopy.length; i++) {
        URL elem = serializer.deserialize(mUrlListCopy[i]);
        if (elem == null) {
          mUrlListConv = null;
          break;
        }
        mUrlListConv.add(elem);
      }
    }
    String[] mUrlArrCopy = genericDoc.getPropertyStringArray("urlArr");
    URL[] mUrlArrConv = null;
    if (mUrlArrCopy != null) {
      mUrlArrConv = new URL[mUrlArrCopy.length];
      Gift.UrlAsStringSerializer serializer = new Gift.UrlAsStringSerializer();
      for (int i = 0; i < mUrlArrCopy.length; i++) {
        URL elem = serializer.deserialize(mUrlArrCopy[i]);
        if (elem == null) {
          mUrlArrConv = null;
          break;
        }
        mUrlArrConv[i] = elem;
      }
    }
    Gift document = new Gift();
    document.mId = mIdConv;
    document.mNamespace = mNamespaceConv;
    document.mUrl = mUrlConv;
    document.mUrlList = mUrlListConv;
    document.mUrlArr = mUrlArrConv;
    return document;
  }
}
