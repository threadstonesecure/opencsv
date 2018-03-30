/*
 * Copyright 2017 Andrew Rucker Jones.
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
package com.opencsv.bean;

import com.opencsv.ICSVParser;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.util.*;

/**
 * This class maintains a mapping from header names out of a CSV file to bean
 * fields.
 * Simple entries are matched using string equality. Complex entries are matched
 * using regular expressions.
 * 
 * @author Andrew Rucker Jones
 * @since 4.2
 */
public class FieldMapByName<T> extends AbstractFieldMap<String, String, RegexToBeanField<T>, T> {
    
    /**
     * Initializes this {@link FieldMap}.
     * 
     * @param errorLocale The locale to be used for error messages
     */
    public FieldMapByName(final Locale errorLocale) {
        super(errorLocale);
    }
    
    /**
     * @param key A regular expression matching header names
     */
    // The rest of the Javadoc is inherited
    @Override
    public void putComplex(final String key, final BeanField<T> value) {
        complexMapList.add(new RegexToBeanField<>(key, value, errorLocale));
    }
    
    /**
     * Returns a list of required headers that were not present in the input.
     * 
     * @param headersPresent An array of all headers present from the input
     * @return A list of name + field for all of the required headers that were
     *   not found
     */
    public List<FieldMapByNameEntry<T>> determineMissingRequiredHeaders(final String[] headersPresent) {
        
        // Start with collections of all required headers
        final List<String> requiredStringList = new LinkedList<>();
        for(Map.Entry<String, BeanField<T>> entry : simpleMap.entrySet()) {
            if(entry.getValue().isRequired()) {
                requiredStringList.add(entry.getKey());
            }
        }
        final List<ComplexFieldMapEntry<String, String, T>> requiredRegexList = new LinkedList<>();
        for(ComplexFieldMapEntry<String, String, T> r : complexMapList) {
            if(r.getBeanField().isRequired()) {
                requiredRegexList.add(r);
            }
        }
        
        // Now remove the ones we found
        for(String h : headersPresent) {
            if(!requiredStringList.remove(h.toUpperCase())) {
                final ListIterator<ComplexFieldMapEntry<String, String, T>> requiredRegexListIterator = requiredRegexList.listIterator();
                boolean found = false;
                while(!found && requiredRegexListIterator.hasNext()) {
                    final ComplexFieldMapEntry<String, String, T> r = requiredRegexListIterator.next();
                    if(r.contains(h)) {
                        found = true;
                        requiredRegexListIterator.remove();
                    }
                }
            }
        }
        
        // Repackage what remains
        List<FieldMapByNameEntry<T>> missingRequiredHeaders = new LinkedList<>();
        for(String s : requiredStringList) {
            missingRequiredHeaders.add(new FieldMapByNameEntry<T>(s, simpleMap.get(s), false));
        }
        for(ComplexFieldMapEntry<String, String, T> r : requiredRegexList) {
            missingRequiredHeaders.add(new FieldMapByNameEntry<T>(r.getInitializer().toString(), r.getBeanField(), true));
        }
        
        return missingRequiredHeaders;
    }
    
    /**
     * This method generates a header that can be used for writing beans of the
     * type provided back to a file.
     * <p>The ordering of the headers is alphabetically ascending.</p>
     * <p>This implementation will not write headers discovered in multi-valued
     * bean fields if the headers would not be matched by the bean field on
     * reading. There are two reasons for this:</p>
     * <ol><li>opencsv always tries to create data that are round-trip
     * equivalent, and that would not be the case if it generated data on
     * writing that it would discard on reading.</li>
     * <li>As the code is currently written, the header name is used on writing
     * each bean field to determine the appropriate {@link BeanField} for
     * information concerning conversions, locales, necessity (whether or not
     * the field is required). Without this information, conversion is
     * impossible, and every value written under the unmatched header is blank,
     * regardless of the contents of the bean.</li></ol>
     */
    // The rest of the Javadoc is inherited.
    @Override
    public String[] generateHeader(final T bean) throws CsvRequiredFieldEmptyException {
        final List<Field> missingRequiredHeaders = new LinkedList<>();
        final List<String> headerList = new ArrayList<>(simpleMap.keySet());
        for(ComplexFieldMapEntry<String, String, T> r : complexMapList) {
            @SuppressWarnings("unchecked")
            final MultiValuedMap<String,T> m = (MultiValuedMap<String,T>) r.getBeanField().getFieldValue(bean);
            if(m != null && !m.isEmpty()) {
                for(Map.Entry<String,T> entry : m.entries()) {
                    String key = entry.getKey();
                    if(r.contains(key)) {
                        headerList.add(key);
                    }
                }
            }
            else {
                if(r.getBeanField().isRequired()) {
                    missingRequiredHeaders.add(r.getBeanField().getField());
                }
            }
        }
        
        // Report headers that should have been present
        if(!missingRequiredHeaders.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for(Field f : missingRequiredHeaders) {
                sb.append(f.getName()); sb.append(' ');
            }
            String errorMessage = String.format(
                    ResourceBundle
                            .getBundle(ICSVParser.DEFAULT_BUNDLE_NAME, errorLocale)
                            .getString("header.required.field.absent"),
                    sb.toString(),
                    StringUtils.join(headerList, ' '));
            throw new CsvRequiredFieldEmptyException(bean.getClass(), missingRequiredHeaders, errorMessage);
        }
        
        // To make testing simpler and because not all receivers are guaranteed
        // to be as flexible with column order as opencsv, make the column
        // ordering deterministic by sorting the column headers alphabetically.
        Collections.sort(headerList);
        return headerList.toArray(new String[headerList.size()]);
    }
}
