/*
 *******************************************************************************
 * Copyright (C) 2008-2013, International Business Machines Corporation and    *
 * others. All Rights Reserved.                                                *
 *******************************************************************************
 */
package androidx.core.i18n.messageformat_icu.simple;

import java.text.ParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.TreeMap;

import androidx.annotation.RestrictTo;
import androidx.core.i18n.messageformat_icu.simple.PluralRules.PluralType;

/**
 * Loader for plural rules data.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class PluralRulesLoader extends PluralRules.Factory {
    // Data created from ICU4C with the command
    // ~/svn.icu/trunk/bld$ LD_LIBRARY_PATH=lib bin/genrb --write-java UTF-8 --java-package com.ibm.icu.simple -s ../src/source/data/misc/ plurals.txt -d /tmp/icu
    private static final ResourceBundle DATA_RB = new LocaleElements_plurals();

    private final Map<String, PluralRules> rulesIdToRules;
    // lazy init, use getLocaleIdToRulesIdMap to access
    private Map<String, String> localeIdToCardinalRulesId;
    private Map<String, String> localeIdToOrdinalRulesId;

    /**
     * Access through singleton.
     */
    private PluralRulesLoader() {
        rulesIdToRules = new HashMap<String, PluralRules>();
    }

   /**
     * Returns the lazily-constructed map.
     */
    private Map<String, String> getLocaleIdToRulesIdMap(PluralType type) {
        checkBuildRulesIdMaps();
        return (type == PluralType.CARDINAL) ? localeIdToCardinalRulesId : localeIdToOrdinalRulesId;
    }

    /**
     * Lazily constructs the localeIdToRulesId and rulesIdToEquivalentULocale
     * maps if necessary. These exactly reflect the contents of the locales
     * resource in plurals.res.
     */
    private void checkBuildRulesIdMaps() {
        boolean haveMap;
        synchronized (this) {
            haveMap = localeIdToCardinalRulesId != null;
        }
        if (!haveMap) {
            Map<String, String> tempLocaleIdToCardinalRulesId;
            Map<String, String> tempLocaleIdToOrdinalRulesId;
            try {
                ResourceBundle pluralb = DATA_RB;
                // Read cardinal-number rules.
                Object[][] localeb = (Object[][]) pluralb.getObject("locales");

                // sort for convenience of getAvailableULocales
                tempLocaleIdToCardinalRulesId = new TreeMap<String, String>();

                for (Object[] langAndId : localeb) {
                    String id = (String) langAndId[0];
                    String value = (String) langAndId[1];
                    tempLocaleIdToCardinalRulesId.put(id, value);
                }

                // Read ordinal-number rules.
                localeb = (Object[][]) pluralb.getObject("locales_ordinals");
                tempLocaleIdToOrdinalRulesId = new TreeMap<String, String>();
                for (Object[] langAndId : localeb) {
                    String id = (String) langAndId[0];
                    String value = (String) langAndId[1];
                    tempLocaleIdToOrdinalRulesId.put(id, value);
                }
            } catch (MissingResourceException e) {
                // dummy so we don't try again
                tempLocaleIdToCardinalRulesId = Collections.emptyMap();
                tempLocaleIdToOrdinalRulesId = Collections.emptyMap();
            }
            
            synchronized(this) {
                if (localeIdToCardinalRulesId == null) {
                    localeIdToCardinalRulesId = tempLocaleIdToCardinalRulesId;
                    localeIdToOrdinalRulesId = tempLocaleIdToOrdinalRulesId;
                }
            }
        }
    }

    /**
     * Gets the rulesId from the locale,with locale fallback. If there is no
     * rulesId, return null. The rulesId might be the empty string if the rule
     * is the default rule.
     */
    public String getRulesIdForLocale(Locale locale, PluralType type) {
        Map<String, String> idMap = getLocaleIdToRulesIdMap(type);
        String lang = locale.getLanguage();
        String rulesId = idMap.get(lang);
        return rulesId;
    }

    /**
     * Gets the rule from the rulesId. If there is no rule for this rulesId,
     * return null.
     */
    public PluralRules getRulesForRulesId(String rulesId) {
        // synchronize on the map.  release the lock temporarily while we build the rules.
        PluralRules rules = null;
        boolean hasRules;  // Separate boolean because stored rules can be null.
        synchronized (rulesIdToRules) {
            hasRules = rulesIdToRules.containsKey(rulesId);
            if (hasRules) {
                rules = rulesIdToRules.get(rulesId);  // can be null
            }
        }
        if (!hasRules) {
            try {
                ResourceBundle pluralb = DATA_RB;
                Object[][] rulesb = (Object[][]) pluralb.getObject("rules");
                Object[][] setb = null;
                for (Object[] idAndRule : rulesb) {  // Unbounded loop: We must find the rulesId.
                    if (rulesId.equals(idAndRule[0])) {
                        setb = (Object[][]) idAndRule[1];
                        break;
                    }
                }

                StringBuilder sb = new StringBuilder();
                for (Object[] keywordAndRule : setb) {
                    if (sb.length() > 0) {
                        sb.append("; ");
                    }
                    sb.append((String) keywordAndRule[0]);
                    sb.append(": ");
                    sb.append((String) keywordAndRule[1]);
                }
                rules = PluralRules.parseDescription(sb.toString());
            } catch (ParseException e) {
            } catch (MissingResourceException e) {
            }
            synchronized (rulesIdToRules) {
                if (rulesIdToRules.containsKey(rulesId)) {
                    rules = rulesIdToRules.get(rulesId);
                } else {
                    rulesIdToRules.put(rulesId, rules);  // can be null
                }
            }
        }
        return rules;
    }

    /**
     * Returns the plural rules for the locale. If we don't have data,
     * com.ibm.icu.text.PluralRules.DEFAULT is returned.
     */
    @Override
    public PluralRules forLocale(Locale locale, PluralType type) {
        String rulesId = getRulesIdForLocale(locale, type);
        if (rulesId == null || rulesId.trim().length() == 0) {
            return PluralRules.DEFAULT;
        }
        PluralRules rules = getRulesForRulesId(rulesId);
        if (rules == null) {
            rules = PluralRules.DEFAULT;
        }
        return rules;
    }

    /**
     * The only instance of the loader.
     */
    public static final PluralRulesLoader loader = new PluralRulesLoader();
}
