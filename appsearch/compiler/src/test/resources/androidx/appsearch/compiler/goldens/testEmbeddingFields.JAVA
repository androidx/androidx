package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.EmbeddingVector;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.Collection;
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
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("name")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("defaultIndexNone")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("indexNone")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("vec")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("listVec")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("collectVec")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
            .build())
          .addProperty(new AppSearchSchema.EmbeddingPropertyConfig.Builder("arrVec")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.EmbeddingPropertyConfig.INDEXING_TYPE_SIMILARITY)
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
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    String nameCopy = document.name;
    if (nameCopy != null) {
      builder.setPropertyString("name", nameCopy);
    }
    EmbeddingVector defaultIndexNoneCopy = document.defaultIndexNone;
    if (defaultIndexNoneCopy != null) {
      builder.setPropertyEmbedding("defaultIndexNone", defaultIndexNoneCopy);
    }
    EmbeddingVector indexNoneCopy = document.indexNone;
    if (indexNoneCopy != null) {
      builder.setPropertyEmbedding("indexNone", indexNoneCopy);
    }
    EmbeddingVector vecCopy = document.vec;
    if (vecCopy != null) {
      builder.setPropertyEmbedding("vec", vecCopy);
    }
    List<EmbeddingVector> listVecCopy = document.listVec;
    if (listVecCopy != null) {
      EmbeddingVector[] listVecConv = listVecCopy.toArray(new EmbeddingVector[0]);
      builder.setPropertyEmbedding("listVec", listVecConv);
    }
    Collection<EmbeddingVector> collectVecCopy = document.collectVec;
    if (collectVecCopy != null) {
      EmbeddingVector[] collectVecConv = collectVecCopy.toArray(new EmbeddingVector[0]);
      builder.setPropertyEmbedding("collectVec", collectVecConv);
    }
    EmbeddingVector[] arrVecCopy = document.arrVec;
    if (arrVecCopy != null) {
      builder.setPropertyEmbedding("arrVec", arrVecCopy);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc,
      Map<String, List<String>> documentClassMap) throws AppSearchException {
    String namespaceConv = genericDoc.getNamespace();
    String idConv = genericDoc.getId();
    String[] nameCopy = genericDoc.getPropertyStringArray("name");
    String nameConv = null;
    if (nameCopy != null && nameCopy.length != 0) {
      nameConv = nameCopy[0];
    }
    EmbeddingVector[] defaultIndexNoneCopy = genericDoc.getPropertyEmbeddingArray("defaultIndexNone");
    EmbeddingVector defaultIndexNoneConv = null;
    if (defaultIndexNoneCopy != null && defaultIndexNoneCopy.length != 0) {
      defaultIndexNoneConv = defaultIndexNoneCopy[0];
    }
    EmbeddingVector[] indexNoneCopy = genericDoc.getPropertyEmbeddingArray("indexNone");
    EmbeddingVector indexNoneConv = null;
    if (indexNoneCopy != null && indexNoneCopy.length != 0) {
      indexNoneConv = indexNoneCopy[0];
    }
    EmbeddingVector[] vecCopy = genericDoc.getPropertyEmbeddingArray("vec");
    EmbeddingVector vecConv = null;
    if (vecCopy != null && vecCopy.length != 0) {
      vecConv = vecCopy[0];
    }
    EmbeddingVector[] listVecCopy = genericDoc.getPropertyEmbeddingArray("listVec");
    List<EmbeddingVector> listVecConv = null;
    if (listVecCopy != null) {
      listVecConv = Arrays.asList(listVecCopy);
    }
    EmbeddingVector[] collectVecCopy = genericDoc.getPropertyEmbeddingArray("collectVec");
    List<EmbeddingVector> collectVecConv = null;
    if (collectVecCopy != null) {
      collectVecConv = Arrays.asList(collectVecCopy);
    }
    EmbeddingVector[] arrVecConv = genericDoc.getPropertyEmbeddingArray("arrVec");
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.name = nameConv;
    document.defaultIndexNone = defaultIndexNoneConv;
    document.indexNone = indexNoneConv;
    document.vec = vecConv;
    document.listVec = listVecConv;
    document.collectVec = collectVecConv;
    document.arrVec = arrVecConv;
    return document;
  }
}
