package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DataClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Float;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
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
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("repeatReq")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("repeatNoReq")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_STRING)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("req")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REQUIRED)
            .setTokenizerType(AppSearchSchema.PropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.PropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.PropertyConfig.Builder("noReq")
            .setDataType(AppSearchSchema.PropertyConfig.DATA_TYPE_DOUBLE)
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
    List<String> repeatReqCopy = dataClass.repeatReq;
    if (repeatReqCopy != null) {
      String[] repeatReqConv = repeatReqCopy.toArray(new String[0]);
      builder.setPropertyString("repeatReq", repeatReqConv);
    }
    List<String> repeatNoReqCopy = dataClass.repeatNoReq;
    if (repeatNoReqCopy != null) {
      String[] repeatNoReqConv = repeatNoReqCopy.toArray(new String[0]);
      builder.setPropertyString("repeatNoReq", repeatNoReqConv);
    }
    Float reqCopy = dataClass.req;
    if (reqCopy != null) {
      builder.setPropertyDouble("req", reqCopy);
    }
    Float noReqCopy = dataClass.noReq;
    if (noReqCopy != null) {
      builder.setPropertyDouble("noReq", noReqCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String uriConv = genericDoc.getUri();
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
    Gift dataClass = new Gift();
    dataClass.uri = uriConv;
    dataClass.repeatReq = repeatReqConv;
    dataClass.repeatNoReq = repeatNoReqConv;
    dataClass.req = reqConv;
    dataClass.noReq = noReqConv;
    return dataClass;
  }
}
