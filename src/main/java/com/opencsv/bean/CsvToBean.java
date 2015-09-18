package com.opencsv.bean;

/**
 Copyright 2007 Kyle Miller.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

import com.opencsv.CSVReader;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.Reader;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Converts CSV data to objects.
 *
 * @param <T> - class to convert the objects to.
 */
public class CsvToBean<T> {
   private Map<Class<?>, PropertyEditor> editorMap = null;

   /**
    * Default constructor.
    */
   public CsvToBean() {
   }

   /**
    * parse the values from a csvReader constructed from the passed in Reader.
    * @param mapper - mapping strategy for the bean.
    * @param reader - Reader used to construct a CSVReader
    * @return List of Objects.
    */

   public List<T> parse(MappingStrategy<T> mapper, Reader reader) {
      return parse(mapper, new CSVReader(reader));
   }

   /**
    * parse the values from a csvReader constructed from the passed in Reader.
    * @param mapper - mapping strategy for the bean.
    * @param reader - Reader used to construct a CSVReader
    * @param filter - CsvToBeanFilter to apply - null if no filter.
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, Reader reader, CsvToBeanFilter filter) {
      return parse(mapper, new CSVReader(reader), filter);
   }

   /**
    * parse the values from the csvReader.
    * @param mapper - mapping strategy for the bean.
    * @param csv - CSVReader
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, CSVReader csv) {
      return parse(mapper, csv, null);
   }

   /**
    * parse the values from the csvReader.
    * @param mapper - mapping strategy for the bean.
    * @param csv - CSVReader
    * @param filter - CsvToBeanFilter to apply - null if no filter.
    * @return List of Objects.
    */
   public List<T> parse(MappingStrategy<T> mapper, CSVReader csv, CsvToBeanFilter filter) {
      long lineProcessed = 0;
      String[] line = null;

      try {
         mapper.captureHeader(csv);
      } catch (Exception e) {
         throw new RuntimeException("Error capturing CSV header!", e);
      }

      try {
         List<T> list = new ArrayList<T>();
         while (null != (line = csv.readNext())) {
            lineProcessed++;
            processLine(mapper, filter, line, list);
         }
         return list;
      } catch (Exception e) {
         throw new RuntimeException("Error parsing CSV line: " + lineProcessed + " values: " + Arrays.toString(line), e);
      }
   }

   private void processLine(MappingStrategy<T> mapper, CsvToBeanFilter filter, String[] line, List<T> list) throws IllegalAccessException, InvocationTargetException, InstantiationException, IntrospectionException {
      if (filter == null || filter.allowLine(line)) {
         T obj = processLine(mapper, line);
         list.add(obj);
      }
   }

   /**
    * Creates a single object from a line from the csv file.
    * @param mapper - MappingStrategy
    * @param line  - array of Strings from the csv file.
    * @return - object containing the values.
    * @throws IllegalAccessException - thrown on error creating bean.
    * @throws InvocationTargetException - thrown on error calling the setters.
    * @throws InstantiationException - thrown on error creating bean.
    * @throws IntrospectionException - thrown on error getting the PropertyDescriptor.
    */
   protected T processLine(MappingStrategy<T> mapper, String[] line) throws IllegalAccessException, InvocationTargetException, InstantiationException, IntrospectionException {
      T bean = mapper.createBean();
      for (int col = 0; col < line.length; col++) {
         if (mapper.isAnnotationDriven()) {
            processField(mapper, line, bean, col);
         } else {
            processProperty(mapper, line, bean, col);
         }
      }
      return bean;
   }

   private void processProperty(MappingStrategy<T> mapper, String[] line, T bean, int col) throws IntrospectionException, InstantiationException, IllegalAccessException, InvocationTargetException {
      PropertyDescriptor prop = mapper.findDescriptor(col);
      if (null != prop) {
         String value = checkForTrim(line[col], prop);
         Object obj = convertValue(value, prop);
         prop.getWriteMethod().invoke(bean, obj);
      }
   }

   private void processField(MappingStrategy<T> mapper, String[] line, T bean, int col) throws IllegalAccessException {
      Pair<Field, Boolean> field = mapper.findField(col);
      if (field != null) {
         String value = line[col];
         setFieldValue(bean, field, value);
      }
   }

   private void setFieldValue(T bean, Pair<Field, Boolean> fieldPair, String value) throws IllegalAccessException {
      Field field = fieldPair.getLeft();
      if (fieldPair.getRight() && StringUtils.isBlank(value)) {
         throw new IllegalStateException(String.format("Field '%s' is mandatory but no value was provided.", field.getName()));
      }

      if (StringUtils.isNotBlank(value)) {
         field.setAccessible(true);
         Class<?> fieldType = field.getType();
         if (fieldType.equals(Boolean.TYPE)) {
            field.setBoolean(bean, Boolean.valueOf(value.trim()));
         } else if (fieldType.equals(Byte.TYPE)) {
            field.setByte(bean, Byte.valueOf(value.trim()));
         } else if (fieldType.equals(Double.TYPE)) {
            field.setDouble(bean, Double.valueOf(value.trim()));
         } else if (fieldType.equals(Float.TYPE)) {
            field.setFloat(bean, Float.valueOf(value.trim()));
         } else if (fieldType.equals(Integer.TYPE)) {
            field.setInt(bean, Integer.valueOf(value.trim()));
         } else if (fieldType.equals(Long.TYPE)) {
            field.setLong(bean, Long.valueOf(value.trim()));
         } else if (fieldType.equals(Short.TYPE)) {
            field.setShort(bean, Short.valueOf(value.trim()));
         } else if (fieldType.equals(Character.TYPE)) {
            field.setChar(bean, value.charAt(0));
         } else if (fieldType.isAssignableFrom(String.class)) {
            field.set(bean, value);
         } else {
            throw new IllegalStateException(String.format("Unable to set field value for field '%s' with value '%s' " +
                    "- type is unsupported. Use primitive and String types only.", fieldType, value));
         }
      }
   }

   private String checkForTrim(String s, PropertyDescriptor prop) {
      return trimmableProperty(prop) ? s.trim() : s;
   }

   private boolean trimmableProperty(PropertyDescriptor prop) {
      return !prop.getPropertyType().getName().contains("String");
   }

   /**
    * Convert a string value to its Object value.
    *
    * @param value - String value
    * @param prop  - PropertyDescriptor
    * @return The object set to value (i.e. Integer).  Will return String if no PropertyEditor is found.
    * @throws InstantiationException - Thrown on error getting the property editor from the property descriptor.
    * @throws IllegalAccessException - Thrown on error getting the property editor from the property descriptor.
    */
   protected Object convertValue(String value, PropertyDescriptor prop) throws InstantiationException, IllegalAccessException {
      PropertyEditor editor = getPropertyEditor(prop);
      Object obj = value;
      if (null != editor) {
         editor.setAsText(value);
         obj = editor.getValue();
      }
      return obj;
   }

   private PropertyEditor getPropertyEditorValue(Class<?> cls) {
      if (editorMap == null) {
         editorMap = new HashMap<Class<?>, PropertyEditor>();
      }

      PropertyEditor editor = editorMap.get(cls);

      if (editor == null) {
         editor = PropertyEditorManager.findEditor(cls);
         addEditorToMap(cls, editor);
      }

      return editor;
   }

   private void addEditorToMap(Class<?> cls, PropertyEditor editor) {
      if (editor != null) {
         editorMap.put(cls, editor);
      }
   }


   /**
    * Attempt to find custom property editor on descriptor first, else try the propery editor manager.
    *
    * @param desc - PropertyDescriptor.
    * @return - the PropertyEditor for the given PropertyDescriptor.
    * @throws InstantiationException - thrown when getting the PropertyEditor for the class.
    * @throws IllegalAccessException - thrown when getting the PropertyEditor for the class.
    */
   protected PropertyEditor getPropertyEditor(PropertyDescriptor desc) throws InstantiationException, IllegalAccessException {
      Class<?> cls = desc.getPropertyEditorClass();
      if (null != cls) {
         return (PropertyEditor) cls.newInstance();
      }
      return getPropertyEditorValue(desc.getPropertyType());
   }
}
