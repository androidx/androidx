package com.example.appsearch;

import androidx.appsearch.app.AppSearchSchema;
import androidx.appsearch.app.DocumentClassFactory;
import androidx.appsearch.app.GenericDocument;
import androidx.appsearch.exceptions.AppSearchException;
import java.lang.Boolean;
import java.lang.Byte;
import java.lang.Class;
import java.lang.Double;
import java.lang.Float;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Override;
import java.lang.String;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("collectLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("collectInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("collectDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("collectFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("collectBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("collectByteArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("collectString")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("collectGift", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrBoxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrUnboxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrBoxInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("arrUnboxInt")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("arrBoxDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("arrUnboxDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("arrBoxFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("arrUnboxFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("arrBoxBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("arrUnboxBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("arrUnboxByteArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("boxByteArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("arrString")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("arrGift", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_REPEATED)
            .setShouldIndexNestedProperties(false)
            .build())
          .addProperty(new AppSearchSchema.StringPropertyConfig.Builder("string")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setTokenizerType(AppSearchSchema.StringPropertyConfig.TOKENIZER_TYPE_NONE)
            .setIndexingType(AppSearchSchema.StringPropertyConfig.INDEXING_TYPE_NONE)
            .setJoinableValueType(AppSearchSchema.StringPropertyConfig.JOINABLE_VALUE_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("boxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("unboxLong")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("boxInteger")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.LongPropertyConfig.Builder("unboxInt")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setIndexingType(AppSearchSchema.LongPropertyConfig.INDEXING_TYPE_NONE)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("boxDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("unboxDouble")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("boxFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.DoublePropertyConfig.Builder("unboxFloat")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("boxBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.BooleanPropertyConfig.Builder("unboxBoolean")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.BytesPropertyConfig.Builder("unboxByteArr")
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .build())
          .addProperty(new AppSearchSchema.DocumentPropertyConfig.Builder("gift", $$__AppSearch__Gift.SCHEMA_NAME)
            .setCardinality(AppSearchSchema.PropertyConfig.CARDINALITY_OPTIONAL)
            .setShouldIndexNestedProperties(false)
            .build())
          .build();
  }

  @Override
  public List<Class<?>> getNestedDocumentClasses() throws AppSearchException {
    List<Class<?>> classSet = new ArrayList<Class<?>>();
    classSet.add(Gift.class);
    return classSet;
  }

  @Override
  public GenericDocument toGenericDocument(Gift document) throws AppSearchException {
    GenericDocument.Builder<?> builder =
        new GenericDocument.Builder<>(document.namespace, document.id, SCHEMA_NAME);
    Collection<Long> collectLongCopy = document.collectLong;
    if (collectLongCopy != null) {
      long[] collectLongConv = new long[collectLongCopy.size()];
      int i = 0;
      for (Long item : collectLongCopy) {
        collectLongConv[i++] = item;
      }
      builder.setPropertyLong("collectLong", collectLongConv);
    }
    Collection<Integer> collectIntegerCopy = document.collectInteger;
    if (collectIntegerCopy != null) {
      long[] collectIntegerConv = new long[collectIntegerCopy.size()];
      int i = 0;
      for (Integer item : collectIntegerCopy) {
        collectIntegerConv[i++] = item;
      }
      builder.setPropertyLong("collectInteger", collectIntegerConv);
    }
    Collection<Double> collectDoubleCopy = document.collectDouble;
    if (collectDoubleCopy != null) {
      double[] collectDoubleConv = new double[collectDoubleCopy.size()];
      int i = 0;
      for (Double item : collectDoubleCopy) {
        collectDoubleConv[i++] = item;
      }
      builder.setPropertyDouble("collectDouble", collectDoubleConv);
    }
    Collection<Float> collectFloatCopy = document.collectFloat;
    if (collectFloatCopy != null) {
      double[] collectFloatConv = new double[collectFloatCopy.size()];
      int i = 0;
      for (Float item : collectFloatCopy) {
        collectFloatConv[i++] = item;
      }
      builder.setPropertyDouble("collectFloat", collectFloatConv);
    }
    Collection<Boolean> collectBooleanCopy = document.collectBoolean;
    if (collectBooleanCopy != null) {
      boolean[] collectBooleanConv = new boolean[collectBooleanCopy.size()];
      int i = 0;
      for (Boolean item : collectBooleanCopy) {
        collectBooleanConv[i++] = item;
      }
      builder.setPropertyBoolean("collectBoolean", collectBooleanConv);
    }
    Collection<byte[]> collectByteArrCopy = document.collectByteArr;
    if (collectByteArrCopy != null) {
      byte[][] collectByteArrConv = new byte[collectByteArrCopy.size()][];
      int i = 0;
      for (byte[] item : collectByteArrCopy) {
        collectByteArrConv[i++] = item;
      }
      builder.setPropertyBytes("collectByteArr", collectByteArrConv);
    }
    Collection<String> collectStringCopy = document.collectString;
    if (collectStringCopy != null) {
      String[] collectStringConv = collectStringCopy.toArray(new String[0]);
      builder.setPropertyString("collectString", collectStringConv);
    }
    Collection<Gift> collectGiftCopy = document.collectGift;
    if (collectGiftCopy != null) {
      GenericDocument[] collectGiftConv = new GenericDocument[collectGiftCopy.size()];
      int i = 0;
      for (Gift item : collectGiftCopy) {
        collectGiftConv[i++] = GenericDocument.fromDocumentClass(item);
      }
      builder.setPropertyDocument("collectGift", collectGiftConv);
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
    Integer[] arrBoxIntegerCopy = document.arrBoxInteger;
    if (arrBoxIntegerCopy != null) {
      long[] arrBoxIntegerConv = new long[arrBoxIntegerCopy.length];
      for (int i = 0 ; i < arrBoxIntegerCopy.length ; i++) {
        arrBoxIntegerConv[i] = arrBoxIntegerCopy[i];
      }
      builder.setPropertyLong("arrBoxInteger", arrBoxIntegerConv);
    }
    int[] arrUnboxIntCopy = document.arrUnboxInt;
    if (arrUnboxIntCopy != null) {
      long[] arrUnboxIntConv = new long[arrUnboxIntCopy.length];
      for (int i = 0 ; i < arrUnboxIntCopy.length ; i++) {
        arrUnboxIntConv[i] = arrUnboxIntCopy[i];
      }
      builder.setPropertyLong("arrUnboxInt", arrUnboxIntConv);
    }
    Double[] arrBoxDoubleCopy = document.arrBoxDouble;
    if (arrBoxDoubleCopy != null) {
      double[] arrBoxDoubleConv = new double[arrBoxDoubleCopy.length];
      for (int i = 0 ; i < arrBoxDoubleCopy.length ; i++) {
        arrBoxDoubleConv[i] = arrBoxDoubleCopy[i];
      }
      builder.setPropertyDouble("arrBoxDouble", arrBoxDoubleConv);
    }
    double[] arrUnboxDoubleCopy = document.arrUnboxDouble;
    if (arrUnboxDoubleCopy != null) {
      builder.setPropertyDouble("arrUnboxDouble", arrUnboxDoubleCopy);
    }
    Float[] arrBoxFloatCopy = document.arrBoxFloat;
    if (arrBoxFloatCopy != null) {
      double[] arrBoxFloatConv = new double[arrBoxFloatCopy.length];
      for (int i = 0 ; i < arrBoxFloatCopy.length ; i++) {
        arrBoxFloatConv[i] = arrBoxFloatCopy[i];
      }
      builder.setPropertyDouble("arrBoxFloat", arrBoxFloatConv);
    }
    float[] arrUnboxFloatCopy = document.arrUnboxFloat;
    if (arrUnboxFloatCopy != null) {
      double[] arrUnboxFloatConv = new double[arrUnboxFloatCopy.length];
      for (int i = 0 ; i < arrUnboxFloatCopy.length ; i++) {
        arrUnboxFloatConv[i] = arrUnboxFloatCopy[i];
      }
      builder.setPropertyDouble("arrUnboxFloat", arrUnboxFloatConv);
    }
    Boolean[] arrBoxBooleanCopy = document.arrBoxBoolean;
    if (arrBoxBooleanCopy != null) {
      boolean[] arrBoxBooleanConv = new boolean[arrBoxBooleanCopy.length];
      for (int i = 0 ; i < arrBoxBooleanCopy.length ; i++) {
        arrBoxBooleanConv[i] = arrBoxBooleanCopy[i];
      }
      builder.setPropertyBoolean("arrBoxBoolean", arrBoxBooleanConv);
    }
    boolean[] arrUnboxBooleanCopy = document.arrUnboxBoolean;
    if (arrUnboxBooleanCopy != null) {
      builder.setPropertyBoolean("arrUnboxBoolean", arrUnboxBooleanCopy);
    }
    byte[][] arrUnboxByteArrCopy = document.arrUnboxByteArr;
    if (arrUnboxByteArrCopy != null) {
      builder.setPropertyBytes("arrUnboxByteArr", arrUnboxByteArrCopy);
    }
    Byte[] boxByteArrCopy = document.boxByteArr;
    if (boxByteArrCopy != null) {
      byte[] boxByteArrConv = new byte[boxByteArrCopy.length];
      for (int i = 0 ; i < boxByteArrCopy.length ; i++) {
        boxByteArrConv[i] = boxByteArrCopy[i];
      }
      builder.setPropertyBytes("boxByteArr", boxByteArrConv);
    }
    String[] arrStringCopy = document.arrString;
    if (arrStringCopy != null) {
      builder.setPropertyString("arrString", arrStringCopy);
    }
    Gift[] arrGiftCopy = document.arrGift;
    if (arrGiftCopy != null) {
      GenericDocument[] arrGiftConv = new GenericDocument[arrGiftCopy.length];
      for (int i = 0; i < arrGiftConv.length; i++) {
        arrGiftConv[i] = GenericDocument.fromDocumentClass(arrGiftCopy[i]);
      }
      builder.setPropertyDocument("arrGift", arrGiftConv);
    }
    String stringCopy = document.string;
    if (stringCopy != null) {
      builder.setPropertyString("string", stringCopy);
    }
    Long boxLongCopy = document.boxLong;
    if (boxLongCopy != null) {
      builder.setPropertyLong("boxLong", boxLongCopy);
    }
    builder.setPropertyLong("unboxLong", document.unboxLong);
    Integer boxIntegerCopy = document.boxInteger;
    if (boxIntegerCopy != null) {
      builder.setPropertyLong("boxInteger", boxIntegerCopy);
    }
    builder.setPropertyLong("unboxInt", document.unboxInt);
    Double boxDoubleCopy = document.boxDouble;
    if (boxDoubleCopy != null) {
      builder.setPropertyDouble("boxDouble", boxDoubleCopy);
    }
    builder.setPropertyDouble("unboxDouble", document.unboxDouble);
    Float boxFloatCopy = document.boxFloat;
    if (boxFloatCopy != null) {
      builder.setPropertyDouble("boxFloat", boxFloatCopy);
    }
    builder.setPropertyDouble("unboxFloat", document.unboxFloat);
    Boolean boxBooleanCopy = document.boxBoolean;
    if (boxBooleanCopy != null) {
      builder.setPropertyBoolean("boxBoolean", boxBooleanCopy);
    }
    builder.setPropertyBoolean("unboxBoolean", document.unboxBoolean);
    byte[] unboxByteArrCopy = document.unboxByteArr;
    if (unboxByteArrCopy != null) {
      builder.setPropertyBytes("unboxByteArr", unboxByteArrCopy);
    }
    Gift giftCopy = document.gift;
    if (giftCopy != null) {
      GenericDocument giftConv = GenericDocument.fromDocumentClass(giftCopy);
      builder.setPropertyDocument("gift", giftConv);
    }
    return builder.build();
  }

  @Override
  public Gift fromGenericDocument(GenericDocument genericDoc) throws AppSearchException {
    String idConv = genericDoc.getId();
    String namespaceConv = genericDoc.getNamespace();
    long[] collectLongCopy = genericDoc.getPropertyLongArray("collectLong");
    List<Long> collectLongConv = null;
    if (collectLongCopy != null) {
      collectLongConv = new ArrayList<>(collectLongCopy.length);
      for (int i = 0; i < collectLongCopy.length; i++) {
        collectLongConv.add(collectLongCopy[i]);
      }
    }
    long[] collectIntegerCopy = genericDoc.getPropertyLongArray("collectInteger");
    List<Integer> collectIntegerConv = null;
    if (collectIntegerCopy != null) {
      collectIntegerConv = new ArrayList<>(collectIntegerCopy.length);
      for (int i = 0; i < collectIntegerCopy.length; i++) {
        collectIntegerConv.add((int) collectIntegerCopy[i]);
      }
    }
    double[] collectDoubleCopy = genericDoc.getPropertyDoubleArray("collectDouble");
    List<Double> collectDoubleConv = null;
    if (collectDoubleCopy != null) {
      collectDoubleConv = new ArrayList<>(collectDoubleCopy.length);
      for (int i = 0; i < collectDoubleCopy.length; i++) {
        collectDoubleConv.add(collectDoubleCopy[i]);
      }
    }
    double[] collectFloatCopy = genericDoc.getPropertyDoubleArray("collectFloat");
    List<Float> collectFloatConv = null;
    if (collectFloatCopy != null) {
      collectFloatConv = new ArrayList<>(collectFloatCopy.length);
      for (int i = 0; i < collectFloatCopy.length; i++) {
        collectFloatConv.add((float) collectFloatCopy[i]);
      }
    }
    boolean[] collectBooleanCopy = genericDoc.getPropertyBooleanArray("collectBoolean");
    List<Boolean> collectBooleanConv = null;
    if (collectBooleanCopy != null) {
      collectBooleanConv = new ArrayList<>(collectBooleanCopy.length);
      for (int i = 0; i < collectBooleanCopy.length; i++) {
        collectBooleanConv.add(collectBooleanCopy[i]);
      }
    }
    byte[][] collectByteArrCopy = genericDoc.getPropertyBytesArray("collectByteArr");
    List<byte[]> collectByteArrConv = null;
    if (collectByteArrCopy != null) {
      collectByteArrConv = new ArrayList<>(collectByteArrCopy.length);
      for (int i = 0; i < collectByteArrCopy.length; i++) {
        collectByteArrConv.add(collectByteArrCopy[i]);
      }
    }
    String[] collectStringCopy = genericDoc.getPropertyStringArray("collectString");
    List<String> collectStringConv = null;
    if (collectStringCopy != null) {
      collectStringConv = Arrays.asList(collectStringCopy);
    }
    GenericDocument[] collectGiftCopy = genericDoc.getPropertyDocumentArray("collectGift");
    List<Gift> collectGiftConv = null;
    if (collectGiftCopy != null) {
      collectGiftConv = new ArrayList<>(collectGiftCopy.length);
      for (int i = 0; i < collectGiftCopy.length; i++) {
        collectGiftConv.add(collectGiftCopy[i].toDocumentClass(Gift.class));
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
    long[] arrBoxIntegerCopy = genericDoc.getPropertyLongArray("arrBoxInteger");
    Integer[] arrBoxIntegerConv = null;
    if (arrBoxIntegerCopy != null) {
      arrBoxIntegerConv = new Integer[arrBoxIntegerCopy.length];
      for (int i = 0; i < arrBoxIntegerCopy.length; i++) {
        arrBoxIntegerConv[i] = (int) arrBoxIntegerCopy[i];
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
    double[] arrBoxDoubleCopy = genericDoc.getPropertyDoubleArray("arrBoxDouble");
    Double[] arrBoxDoubleConv = null;
    if (arrBoxDoubleCopy != null) {
      arrBoxDoubleConv = new Double[arrBoxDoubleCopy.length];
      for (int i = 0; i < arrBoxDoubleCopy.length; i++) {
        arrBoxDoubleConv[i] = arrBoxDoubleCopy[i];
      }
    }
    double[] arrUnboxDoubleConv = genericDoc.getPropertyDoubleArray("arrUnboxDouble");
    double[] arrBoxFloatCopy = genericDoc.getPropertyDoubleArray("arrBoxFloat");
    Float[] arrBoxFloatConv = null;
    if (arrBoxFloatCopy != null) {
      arrBoxFloatConv = new Float[arrBoxFloatCopy.length];
      for (int i = 0; i < arrBoxFloatCopy.length; i++) {
        arrBoxFloatConv[i] = (float) arrBoxFloatCopy[i];
      }
    }
    double[] arrUnboxFloatCopy = genericDoc.getPropertyDoubleArray("arrUnboxFloat");
    float[] arrUnboxFloatConv = null;
    if (arrUnboxFloatCopy != null) {
      arrUnboxFloatConv = new float[arrUnboxFloatCopy.length];
      for (int i = 0; i < arrUnboxFloatCopy.length; i++) {
        arrUnboxFloatConv[i] = (float) arrUnboxFloatCopy[i];
      }
    }
    boolean[] arrBoxBooleanCopy = genericDoc.getPropertyBooleanArray("arrBoxBoolean");
    Boolean[] arrBoxBooleanConv = null;
    if (arrBoxBooleanCopy != null) {
      arrBoxBooleanConv = new Boolean[arrBoxBooleanCopy.length];
      for (int i = 0; i < arrBoxBooleanCopy.length; i++) {
        arrBoxBooleanConv[i] = arrBoxBooleanCopy[i];
      }
    }
    boolean[] arrUnboxBooleanConv = genericDoc.getPropertyBooleanArray("arrUnboxBoolean");
    byte[][] arrUnboxByteArrConv = genericDoc.getPropertyBytesArray("arrUnboxByteArr");
    byte[] boxByteArrCopy = genericDoc.getPropertyBytes("boxByteArr");
    Byte[] boxByteArrConv = null;
    if (boxByteArrCopy != null) {
      boxByteArrConv = new Byte[boxByteArrCopy.length];
      for (int i = 0; i < boxByteArrCopy.length; i++) {
        boxByteArrConv[i] = boxByteArrCopy[i];
      }
    }
    String[] arrStringConv = genericDoc.getPropertyStringArray("arrString");
    GenericDocument[] arrGiftCopy = genericDoc.getPropertyDocumentArray("arrGift");
    Gift[] arrGiftConv = null;
    if (arrGiftCopy != null) {
      arrGiftConv = new Gift[arrGiftCopy.length];
      for (int i = 0; i < arrGiftCopy.length; i++) {
        arrGiftConv[i] = arrGiftCopy[i].toDocumentClass(Gift.class);
      }
    }
    String[] stringCopy = genericDoc.getPropertyStringArray("string");
    String stringConv = null;
    if (stringCopy != null && stringCopy.length != 0) {
      stringConv = stringCopy[0];
    }
    long[] boxLongCopy = genericDoc.getPropertyLongArray("boxLong");
    Long boxLongConv = null;
    if (boxLongCopy != null && boxLongCopy.length != 0) {
      boxLongConv = boxLongCopy[0];
    }
    long unboxLongConv = genericDoc.getPropertyLong("unboxLong");
    long[] boxIntegerCopy = genericDoc.getPropertyLongArray("boxInteger");
    Integer boxIntegerConv = null;
    if (boxIntegerCopy != null && boxIntegerCopy.length != 0) {
      boxIntegerConv = (int) boxIntegerCopy[0];
    }
    int unboxIntConv = (int) genericDoc.getPropertyLong("unboxInt");
    double[] boxDoubleCopy = genericDoc.getPropertyDoubleArray("boxDouble");
    Double boxDoubleConv = null;
    if (boxDoubleCopy != null && boxDoubleCopy.length != 0) {
      boxDoubleConv = boxDoubleCopy[0];
    }
    double unboxDoubleConv = genericDoc.getPropertyDouble("unboxDouble");
    double[] boxFloatCopy = genericDoc.getPropertyDoubleArray("boxFloat");
    Float boxFloatConv = null;
    if (boxFloatCopy != null && boxFloatCopy.length != 0) {
      boxFloatConv = (float) boxFloatCopy[0];
    }
    float unboxFloatConv = (float) genericDoc.getPropertyDouble("unboxFloat");
    boolean[] boxBooleanCopy = genericDoc.getPropertyBooleanArray("boxBoolean");
    Boolean boxBooleanConv = null;
    if (boxBooleanCopy != null && boxBooleanCopy.length != 0) {
      boxBooleanConv = boxBooleanCopy[0];
    }
    boolean unboxBooleanConv = genericDoc.getPropertyBoolean("unboxBoolean");
    byte[][] unboxByteArrCopy = genericDoc.getPropertyBytesArray("unboxByteArr");
    byte[] unboxByteArrConv = null;
    if (unboxByteArrCopy != null && unboxByteArrCopy.length != 0) {
      unboxByteArrConv = unboxByteArrCopy[0];
    }
    GenericDocument giftCopy = genericDoc.getPropertyDocument("gift");
    Gift giftConv = null;
    if (giftCopy != null) {
      giftConv = giftCopy.toDocumentClass(Gift.class);
    }
    Gift document = new Gift();
    document.namespace = namespaceConv;
    document.id = idConv;
    document.collectLong = collectLongConv;
    document.collectInteger = collectIntegerConv;
    document.collectDouble = collectDoubleConv;
    document.collectFloat = collectFloatConv;
    document.collectBoolean = collectBooleanConv;
    document.collectByteArr = collectByteArrConv;
    document.collectString = collectStringConv;
    document.collectGift = collectGiftConv;
    document.arrBoxLong = arrBoxLongConv;
    document.arrUnboxLong = arrUnboxLongConv;
    document.arrBoxInteger = arrBoxIntegerConv;
    document.arrUnboxInt = arrUnboxIntConv;
    document.arrBoxDouble = arrBoxDoubleConv;
    document.arrUnboxDouble = arrUnboxDoubleConv;
    document.arrBoxFloat = arrBoxFloatConv;
    document.arrUnboxFloat = arrUnboxFloatConv;
    document.arrBoxBoolean = arrBoxBooleanConv;
    document.arrUnboxBoolean = arrUnboxBooleanConv;
    document.arrUnboxByteArr = arrUnboxByteArrConv;
    document.boxByteArr = boxByteArrConv;
    document.arrString = arrStringConv;
    document.arrGift = arrGiftConv;
    document.string = stringConv;
    document.boxLong = boxLongConv;
    document.unboxLong = unboxLongConv;
    document.boxInteger = boxIntegerConv;
    document.unboxInt = unboxIntConv;
    document.boxDouble = boxDoubleConv;
    document.unboxDouble = unboxDoubleConv;
    document.boxFloat = boxFloatConv;
    document.unboxFloat = unboxFloatConv;
    document.boxBoolean = boxBooleanConv;
    document.unboxBoolean = unboxBooleanConv;
    document.unboxByteArr = unboxByteArrConv;
    document.gift = giftConv;
    return document;
  }
}
