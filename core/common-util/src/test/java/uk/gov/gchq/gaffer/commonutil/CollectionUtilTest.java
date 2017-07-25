/*
 * Copyright 2016 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.gaffer.commonutil;

import org.junit.Test;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CollectionUtilTest {
    @Test
    public void shouldReturnTreeSetWithProvidedItem() {
        // Given
        final String item = "test item";

        // When
        final TreeSet<String> treeSet = CollectionUtil.treeSet(item);

        // Then
        assertEquals(1, treeSet.size());
        assertTrue(treeSet.contains(item));
    }

    @Test
    public void shouldReturnTreeSetWithWithOutNullItem() {
        // Given
        final String item = null;

        // When
        final TreeSet<String> treeSet = CollectionUtil.treeSet(item);

        // Then
        assertEquals(0, treeSet.size());
    }

    @Test
    public void shouldReturnTreeSetWithProvidedItems() {
        // Given
        final String[] items = {"test item 1", "test item 2", null};

        // When
        final TreeSet<String> treeSet = CollectionUtil.treeSet(items);

        // Then
        assertEquals(2, treeSet.size());
        for (final String item : items) {
            if (null != item) {
                assertTrue(treeSet.contains(item));
            }
        }
    }

    @Test
    public void shouldReturnTreeSetWithNoItemsForNullArray() {
        // Given
        final String[] items = null;

        // When
        final TreeSet<String> treeSet = CollectionUtil.treeSet(items);

        // Then
        assertEquals(0, treeSet.size());
    }

    @Test
    public void shouldConvertMapToStringKeys() {
        // Given
        final Map<Class<? extends Number>, String> map = new HashMap<>();
        map.put(Integer.class, "integer");
        map.put(Double.class, "double");
        map.put(Long.class, "long");

        // When
        final Map<String, String> result = CollectionUtil.toMapWithStringKeys(map);

        // Then
        final Map<String, String> expectedResult = new HashMap<>();
        expectedResult.put(Integer.class.getName(), "integer");
        expectedResult.put(Double.class.getName(), "double");
        expectedResult.put(Long.class.getName(), "long");
        assertEquals(expectedResult, result);
    }

    @Test
    public void shouldConvertMapToStringKeysWithProvidedMap() {
        // Given
        final Map<Class<? extends Number>, String> map = new HashMap<>();
        map.put(Integer.class, "integer");
        map.put(Double.class, "double");
        map.put(Long.class, "long");

        final Map<String, String> result = new LinkedHashMap<>();

        // When
        CollectionUtil.toMapWithStringKeys(map, result);

        // Then
        final Map<String, String> expectedResult = new LinkedHashMap<>();
        expectedResult.put(Integer.class.getName(), "integer");
        expectedResult.put(Double.class.getName(), "double");
        expectedResult.put(Long.class.getName(), "long");
        assertEquals(expectedResult, result);
    }

    @Test
    public void shouldConvertMapToClassKeys() throws ClassNotFoundException {
        // Given
        final Map<String, String> map = new HashMap<>();
        map.put(Integer.class.getName(), "integer");
        map.put(Double.class.getName(), "double");
        map.put(Long.class.getName(), "long");

        // When
        final Map<Class<? extends Number>, String> result = CollectionUtil.toMapWithClassKeys(map);

        // Then
        final Map<Class<? extends Number>, String> expectedResult = new HashMap<>();
        expectedResult.put(Integer.class, "integer");
        expectedResult.put(Double.class, "double");
        expectedResult.put(Long.class, "long");
        assertEquals(expectedResult, result);
    }

    @Test
    public void shouldConvertMapToClassKeysWithProvidedMap() throws ClassNotFoundException {
        // Given
        final Map<String, String> map = new HashMap<>();
        map.put(Integer.class.getName(), "integer");
        map.put(Double.class.getName(), "double");
        map.put(Long.class.getName(), "long");

        final Map<Class<? extends Number>, String> result = new LinkedHashMap<>();


        // When
        CollectionUtil.toMapWithClassKeys(map, result);

        // Then
        final Map<Class<? extends Number>, String> expectedResult = new LinkedHashMap<>();
        expectedResult.put(Integer.class, "integer");
        expectedResult.put(Double.class, "double");
        expectedResult.put(Long.class, "long");
        assertEquals(expectedResult, result);
    }
}
