/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.webkit.internal;

import androidx.webkit.UserAgentMetadata;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Internal implementation of translation between {@code Map<String, Object>} and
 * {@link androidx.webkit.UserAgentMetadata}.
 */
public class UserAgentMetadataInternal {
    private UserAgentMetadataInternal() {}
    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata mobile,
     * used to generate user-agent client hint {@code sec-ch-ua-mobile}.
     */
    private static final String MOBILE = "MOBILE";
    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata brand version list,
     * used to generate user-agent client hints {@code sec-ch-ua}, and
     * {@code sec-ch-ua-full-version-list}.
     */
    private static final String BRAND_VERSION_LIST = "BRAND_VERSION_LIST";

    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata full version,
     * used to generate user-agent client hint {@code sec-ch-ua-full-version}.
     */
    private static final String FULL_VERSION = "FULL_VERSION";

    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata platform,
     * used to generate user-agent client hint {@code sec-ch-ua-platform}.
     */
    private static final String PLATFORM = "PLATFORM";

    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata platform version,
     * used to generate user-agent client hint {@code sec-ch-ua-platform-version}.
     */
    private static final String PLATFORM_VERSION = "PLATFORM_VERSION";

    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata architecture,
     * used to generate user-agent client hint {@code sec-ch-ua-arch}.
     */
    private static final String ARCHITECTURE = "ARCHITECTURE";

    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata model,
     * used to generate user-agent client hint {@code sec-ch-ua-model}.
     */
    private static final String MODEL = "MODEL";
    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata bitness,
     * used to generate user-agent client hint {@code sec-ch-ua-bitness}.
     */
    private static final String BITNESS = "BITNESS";
    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata wow64,
     * used to generate user-agent client hint {@code sec-ch-ua-wow64}.
     */
    private static final String WOW64 = "WOW64";
    /**
     * Predefined set of name for user-agent metadata key.
     * Key name for user-agent metadata form factors,
     * used to generate user-agent client hint {@code sec-ch-ua-form-factors}.
     */
    private static final String FORM_FACTORS = "FORM_FACTORS";
    /**
     * each brand should contains brand, major version and full version.
     */
    private static final int BRAND_VERSION_LENGTH = 3;

    /**
     * Convert the UserAgentMetadata setting to a map of object and pass down to chromium.
     *
     * @return A hashmap contains user-agent metadata key name, and corresponding objects.
     */
    static @NonNull Map<String, Object> convertUserAgentMetadataToMap(
            @NonNull UserAgentMetadata uaMetadata) {
        Map<String, Object> item = new HashMap<>();
        item.put(BRAND_VERSION_LIST, getBrandVersionArray(uaMetadata.getBrandVersionList()));
        item.put(FULL_VERSION, uaMetadata.getFullVersion());
        item.put(PLATFORM, uaMetadata.getPlatform());
        item.put(PLATFORM_VERSION, uaMetadata.getPlatformVersion());
        item.put(ARCHITECTURE, uaMetadata.getArchitecture());
        item.put(MODEL, uaMetadata.getModel());
        item.put(MOBILE, uaMetadata.isMobile());
        item.put(BITNESS, uaMetadata.getBitness());
        item.put(WOW64, uaMetadata.isWow64());

        ApiFeature.NoFramework feature =
                    WebViewFeatureInternal.USER_AGENT_METADATA_FORM_FACTORS;
        if (feature.isSupportedByWebView()) {
            item.put(FORM_FACTORS, getFormFactorsArray(uaMetadata.getFormFactors()));
        }
        return item;
    }

    private static String[][] getBrandVersionArray(
            List<UserAgentMetadata.BrandVersion> brandVersionList) {
        // If user's input of brand version list is empty, we consider to use system default value.
        // This means passing null to chromium.
        if (brandVersionList == null || brandVersionList.isEmpty()) {
            return null;
        }

        String[][] brandVersionArray = new String[brandVersionList.size()][BRAND_VERSION_LENGTH];
        for (int i = 0; i < brandVersionList.size(); i++) {
            brandVersionArray[i][0] = brandVersionList.get(i).getBrand();
            brandVersionArray[i][1] = brandVersionList.get(i).getMajorVersion();
            brandVersionArray[i][2] = brandVersionList.get(i).getFullVersion();
        }
        return brandVersionArray;
    }

    private static String[] getFormFactorsArray(List<String> formFactors) {
        // If user's input of form factors list is empty, we consider to use system default value.
        // This means passing null to chromium.
        if (formFactors == null || formFactors.isEmpty()) {
            return null;
        }

        String[] formFactorsArray = new String[formFactors.size()];
        for (int i = 0; i < formFactors.size(); i++) {
            formFactorsArray[i] = formFactors.get(i);
        }
        return formFactorsArray;
    }

    /**
     * Convert a map of object to an instance of UserAgentMetadata.
     *
     * @param uaMetadataMap A hashmap contains user-agent metadata key name, and corresponding
     *                      objects.
     * @return This UserAgentMetadata object
     */
    static @NonNull UserAgentMetadata getUserAgentMetadataFromMap(
            @NonNull Map<String, Object> uaMetadataMap) {
        UserAgentMetadata.Builder builder = new UserAgentMetadata.Builder();

        Object brandVersionValue = uaMetadataMap.get(BRAND_VERSION_LIST);
        if (brandVersionValue != null) {
            String[][] overrideBrandVersionList = (String[][]) brandVersionValue;
            List<UserAgentMetadata.BrandVersion> brandVersionList = new ArrayList<>();
            for (String[] brandVersionInfo : overrideBrandVersionList) {
                brandVersionList.add(new UserAgentMetadata.BrandVersion.Builder()
                        .setBrand(brandVersionInfo[0])
                        .setMajorVersion(brandVersionInfo[1])
                        .setFullVersion(brandVersionInfo[2])
                        .build());
            }
            builder.setBrandVersionList(brandVersionList);
        }

        String fullVersion = (String) uaMetadataMap.get(FULL_VERSION);
        if (fullVersion != null) {
            builder.setFullVersion(fullVersion);
        }

        String platform = (String) uaMetadataMap.get(PLATFORM);
        if (platform != null) {
            builder.setPlatform(platform);
        }

        String platformVersion = (String) uaMetadataMap.get(PLATFORM_VERSION);
        if (platformVersion != null) {
            builder.setPlatformVersion(platformVersion);
        }

        String architecture = (String) uaMetadataMap.get(ARCHITECTURE);
        if (architecture != null) {
            builder.setArchitecture(architecture);
        }

        String model = (String) uaMetadataMap.get(MODEL);
        if (model != null) {
            builder.setModel(model);
        }

        Boolean isMobile = (Boolean) uaMetadataMap.get(MOBILE);
        if (isMobile != null) {
            builder.setMobile(isMobile);
        }

        Integer bitness = (Integer) uaMetadataMap.get(BITNESS);
        if (bitness != null) {
            builder.setBitness(bitness);
        }

        Boolean isWow64 = (Boolean) uaMetadataMap.get(WOW64);
        if (isWow64 != null) {
            builder.setWow64(isWow64);
        }

        String[] formFactors = (String[]) uaMetadataMap.get(FORM_FACTORS);
        if (formFactors != null) {
            List<String> formFactorsList = new ArrayList<>();
            for (String formFactor : formFactors) {
                formFactorsList.add(formFactor);
            }
            builder.setFormFactors(formFactorsList);
        }

        return builder.build();
    }
}
