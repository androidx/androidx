package com.example.appsearch;

import androidx.annotation.NonNull;
import androidx.appsearch.app.AppSearchDocumentClassMap;
import com.google.auto.service.AutoService;
import java.lang.Override;
import java.lang.String;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.processing.Generated;

@AutoService(AppSearchDocumentClassMap.class)
@Generated("androidx.appsearch.compiler.AppSearchCompiler")
public final class $$__AppSearch__DocumentClassMap_3564af6e02075934601ae45e21da1e65bb69157de5fa8378ab5beb7f70e61d84_0 extends AppSearchDocumentClassMap {
  @NonNull
  @Override
  protected Map<String, List<String>> getMap() {
    Map<String, List<String>> result = new HashMap<>();
    result.put("Gift", Arrays.asList("com.example.appsearch.Gift"));
    result.put("Parent2", Arrays.asList("com.example.appsearch.Parent2"));
    result.put("Parent1", Arrays.asList("com.example.appsearch.Parent1"));
    return result;
  }
}
