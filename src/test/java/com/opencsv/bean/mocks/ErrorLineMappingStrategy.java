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
package com.opencsv.bean.mocks;

import com.opencsv.CSVReader;
import com.opencsv.bean.BeanField;
import com.opencsv.bean.MappingStrategy;
import java.beans.PropertyDescriptor;
import java.io.IOException;

public class ErrorLineMappingStrategy implements MappingStrategy {
    @Override
    public PropertyDescriptor findDescriptor(int col) {
       return null;
    }

    @Override
    public BeanField findField(int col) {
       return null;
    }

    @Override
    public Object createBean() throws InstantiationException, IllegalAccessException {
       throw new InstantiationException("this is a test Exception");
    }

    @Override
    public void captureHeader(CSVReader reader) throws IOException {
    }

    @Override
    public Integer getColumnIndex(String name) {
       return null;
    }

    @Override
    public boolean isAnnotationDriven() {
       return false;
    }
    
    @Override
    public String[] generateHeader() {
        return new String[0];
    }
    
    @Override
    public int findMaxFieldIndex() {
        return -1;
    }
    
    @Override
    public void verifyLineLength(int numberOfFields) {}
}