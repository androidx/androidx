package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Float;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("repeatReq")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("repeatNoReq")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("req")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("noReq")
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
    List<String> repeatReqCopy = document.repeatReq;
    if (repeatReqCopy != null) {
      String[] repeatReqConv = repeatReqCopy.toArray(new String[0]);
      builder.setPropertyString("repeatReq", repeatReqConv);
    }
    List<String> repeatNoReqCopy = document.repeatNoReq;
    if (repeatNoReqCopy != null) {
      String[] repeatNoReqConv = repeatNoReqCopy.toArray(new String[0]);
      builder.setPropertyString("repeatNoReq", repeatNoReqConv);
    }
    Float reqCopy = document.req;
    if (reqCopy != null) {
      builder.setPropertyDouble("req", reqCopy);
    }
    Float noReqCopy = document.noReq;
    if (noReqCopy != null) {
      builder.setPropertyDouble("noReq", noReqCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    String[] repeatReqCopy = genericDoc.getPropertyStringArray("repeatReq");
    List<String> repeatReqConv = null;
    if (repeatReqCopy != null) {
      repeatReqConv = Arrays.asList(repeatReqCopy);
    }
    String[] repeatNoReqCopy = genericDoc.getPropertyStringArray("repeatNoReq");
    List<String> repeatNoReqConv = null;
    if (repeatNoReqCopy != null) {
      repeatNoReqConv = Arrays.asList(repeatNoReqCopy);
    }
    double[] reqCopy = genericDoc.getPropertyDoubleArray("req");
    Float reqConv = null;
    if (reqCopy != null && reqCopy.length != 0) {
      reqConv = (float) reqCopy[0];
    }
    double[] noReqCopy = genericDoc.getPropertyDoubleArray("noReq");
    Float noReqConv = null;
    if (noReqCopy != null && noReqCopy.length != 0) {
      noReqConv = (float) noReqCopy[0];
    }
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.repeatReq = repeatReqConv;
    document.repeatNoReq = repeatNoReqConv;
    document.req = reqConv;
    document.noReq = noReqConv;
    return document;
  }
}
