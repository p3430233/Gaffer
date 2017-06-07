/*
 * Copyright 2017. Crown Copyright
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

package uk.gov.gchq.gaffer.parquetstore.operation.getelements.impl;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.parquet.avro.AvroParquetReader;
import org.apache.parquet.avro.AvroReadSupport;
import org.apache.parquet.filter2.compat.FilterCompat;
import org.apache.parquet.filter2.predicate.FilterPredicate;
import org.apache.parquet.hadoop.ParquetReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Tuple3;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterable;
import uk.gov.gchq.gaffer.commonutil.iterable.CloseableIterator;
import uk.gov.gchq.gaffer.data.element.Edge;
import uk.gov.gchq.gaffer.data.element.Element;
import uk.gov.gchq.gaffer.data.element.Entity;
import uk.gov.gchq.gaffer.data.element.function.ElementFilter;
import uk.gov.gchq.gaffer.data.element.id.DirectedType;
import uk.gov.gchq.gaffer.data.element.id.ElementId;
import uk.gov.gchq.gaffer.data.elementdefinition.view.View;
import uk.gov.gchq.gaffer.exception.SerialisationException;
import uk.gov.gchq.gaffer.operation.OperationException;
import uk.gov.gchq.gaffer.operation.SeedMatching;
import uk.gov.gchq.gaffer.operation.graph.SeededGraphFilters;
import uk.gov.gchq.gaffer.parquetstore.ParquetStore;
import uk.gov.gchq.gaffer.parquetstore.utils.Constants;
import uk.gov.gchq.gaffer.parquetstore.utils.GafferGroupObjectConverter;
import uk.gov.gchq.gaffer.parquetstore.utils.ParquetFileIterator;
import uk.gov.gchq.gaffer.parquetstore.utils.ParquetFilterUtils;
import uk.gov.gchq.gaffer.parquetstore.utils.SchemaUtils;
import uk.gov.gchq.gaffer.store.StoreException;
import uk.gov.gchq.koryphe.tuple.n.Tuple2;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 *
 */
public class ParquetElementRetriever implements CloseableIterable<Element> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParquetElementRetriever.class);

    private final SchemaUtils schemaUtils;
    private final View view;
    private final DirectedType directedType;
    private final SeededGraphFilters.IncludeIncomingOutgoingType includeIncomingOutgoingType;
    private final SeedMatching.SeedMatchingType seedMachingType;
    private final Iterable<? extends ElementId> seeds;
    private final String dataDir;
    private final HashMap<String, ArrayList<Tuple3<Object[], Object[], String>>> indices;
    private FileSystem fs;


    public ParquetElementRetriever(final View view, final ParquetStore store, final DirectedType directedType,
                                   final SeededGraphFilters.IncludeIncomingOutgoingType includeIncomingOutgoingType,
                                   final SeedMatching.SeedMatchingType seedMachingType, final Iterable<? extends ElementId> seeds) throws OperationException, StoreException {
        this.view = view;
        this.schemaUtils = store.getSchemaUtils();
        this.directedType = directedType;
        this.includeIncomingOutgoingType = includeIncomingOutgoingType;
        this.seedMachingType = seedMachingType;
        this.seeds = seeds;
        this.indices = store.getIndex();
        this.dataDir = store.getProperties().getDataDir() + "/" + store.getCurrentSnapshot();
        this.fs = store.getFS();
    }

    @Override
    public void close() {

    }

    @Override
    public CloseableIterator<Element> iterator() {
        return new ParquetIterator(this.schemaUtils, this.view, this.directedType, this.includeIncomingOutgoingType,
                this.seedMachingType, this.seeds, this.dataDir, this.indices, this.fs);
    }



    protected static class ParquetIterator implements CloseableIterator<Element> {
        private Element currentElement = null;
        private ParquetReader<GenericRecord> reader;
        private SchemaUtils schemaUtils;
        private HashMap<String, GafferGroupObjectConverter> groupToObjectConverter;
        private HashMap<Path, FilterPredicate> pathToFilterMap;
        private Path currentPath;
        private Iterator<Path> paths;
        private ParquetFileIterator fileIterator;
        private FileSystem fs;
        private Boolean needsValidation;
        private View view;


        protected ParquetIterator(final SchemaUtils schemaUtils, final View view,
                                  final DirectedType directedType,
                                  final SeededGraphFilters.IncludeIncomingOutgoingType includeIncomingOutgoingType,
                                  final SeedMatching.SeedMatchingType seedMachingType,
                                  final Iterable<? extends ElementId> seeds,
                                  final String dataDir, final HashMap<String, ArrayList<Tuple3<Object[], Object[], String>>> indices,
                                  final FileSystem fs) {
            try {
                Tuple2<HashMap<Path, FilterPredicate>, Boolean> results = ParquetFilterUtils.buildPathToFilterMap(schemaUtils, view, directedType, includeIncomingOutgoingType, seedMachingType, seeds, dataDir, indices);
                this.pathToFilterMap = results.get0();
                this.needsValidation = results.get1();
                LOGGER.debug("pathToFilterMap: " + pathToFilterMap.toString());
                if (!pathToFilterMap.isEmpty()) {
                    this.fs = fs;
                    this.view = view;
                    this.paths = pathToFilterMap.keySet().stream().sorted().iterator();
                    LOGGER.debug("Created new ParquetElementRetriever for paths: " + this.paths.toString());
                    this.schemaUtils = schemaUtils;
                    this.groupToObjectConverter = new HashMap<>();
                    //find all the parquet files
                    this.currentPath = this.paths.next();
                    try {
                        this.fileIterator = new ParquetFileIterator(this.currentPath, this.fs);
                        this.reader = openParquetReader();
                    } catch (IOException e) {
                        LOGGER.error("Path does not exist");
                    }
                } else {
                    LOGGER.info("There are no results for this query");
                }
            } catch (OperationException | SerialisationException e) {
                LOGGER.error("Error while creating the mapping of file paths to Parquet filters: " + e.getMessage());
            }
        }

        private ParquetReader<GenericRecord> openParquetReader() throws IOException {
            if (this.fileIterator.hasNext()) {
                Path file = this.fileIterator.next();
                LOGGER.debug("Opening a new Parquet reader for file: " + file.toString());
                FilterPredicate filter = this.pathToFilterMap.get(this.currentPath);
                if (filter != null) {
                    return AvroParquetReader.builder(new AvroReadSupport<GenericRecord>(), file).withFilter(FilterCompat.get(filter)).build();
                } else {
                    return AvroParquetReader.builder(new AvroReadSupport<GenericRecord>(), file).build();
                }
            } else {
                if (this.paths.hasNext()) {
                    this.currentPath = this.paths.next();
                    this.fileIterator = new ParquetFileIterator(this.currentPath, this.fs);
                    return openParquetReader();
                }
            }
            return null;
        }

        @Override
        public boolean hasNext() {
            if (this.currentElement == null) {
                try {
                    this.currentElement = next();
                } catch (NoSuchElementException e) {
                    return false;
                }
            }
            return true;
        }


        @Override
        public Element next() throws NoSuchElementException {
            Element e = getNextElement();
            if (this.needsValidation) {
                final ElementFilter preAggFilter = view.getElement(e.getGroup()).getPreAggregationFilter();
                if (preAggFilter != null) {
                    while (!preAggFilter.test(e)) {
                        e = getNextElement();
                    }
                }
            }
            return e;
        }

        private Element getNextElement() {
            Element e;
            try {
                if (this.currentElement != null) {
                    LOGGER.debug("Current element: " + this.currentElement);
                    e = this.currentElement;
                    this.currentElement = null;
                } else {
                    if (this.reader != null) {
                        GenericRecord record = this.reader.read();
                        if (record != null) {
                            e = convertGenericRecordToElement(record);
                        } else {
                            LOGGER.debug("Closing Parquet reader");
                            this.reader.close();
                            this.reader = openParquetReader();
                            if (this.reader != null) {
                                record = this.reader.read();
                                if (record != null) {
                                    e = convertGenericRecordToElement(record);
                                } else {
                                    LOGGER.debug("This file has no data");
                                    e = next();
                                }
                            } else {
                                LOGGER.debug("Reached the end of all the files of data");
                                throw new NoSuchElementException();
                            }
                        }
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            } catch (IOException | OperationException ex) {
                throw new NoSuchElementException();
            }
            if (e instanceof Edge && this.currentPath.toString().contains("reverseEdges")) {
                while (((Edge) e).getSource().equals(((Edge) e).getDestination())) {
                    e = next();
                }
            }
            return e;
        }

        private Element convertGenericRecordToElement(final GenericRecord record) throws OperationException, SerialisationException {
            String group = (String) record.get(Constants.GROUP);
            GafferGroupObjectConverter converter = getConverter(group);
            Element e;
            if (this.schemaUtils.getEntityGroups().contains(group)) {
                final String[] paths = this.schemaUtils.getPaths(group, Constants.VERTEX);
                final Object[] parquetObjects = new Object[paths.length];
                for (int i = 0; i < paths.length; i++) {
                    parquetObjects[i] = recursivlyGetObjectFromRecord(paths[i], (GenericData.Record) record);
                }
                e = new Entity(group, converter.parquetObjectsToGafferObject(Constants.VERTEX, parquetObjects));
            } else {
                String[] paths = this.schemaUtils.getPaths(group, Constants.SOURCE);
                final Object[] srcParquetObjects = new Object[paths.length];
                for (int i = 0; i < paths.length; i++) {
                    srcParquetObjects[i] = recursivlyGetObjectFromRecord(paths[i], (GenericData.Record) record);
                }
                paths = this.schemaUtils.getPaths(group, Constants.DESTINATION);
                final Object[] dstParquetObjects = new Object[paths.length];
                for (int i = 0; i < paths.length; i++) {
                    dstParquetObjects[i] = recursivlyGetObjectFromRecord(paths[i], (GenericData.Record) record);
                }
                e = new Edge(group, converter.parquetObjectsToGafferObject(Constants.SOURCE, srcParquetObjects),
                        converter.parquetObjectsToGafferObject(Constants.DESTINATION, dstParquetObjects),
                        (boolean) record.get(Constants.DIRECTED));
            }

            for (final String column : this.schemaUtils.getGafferSchema().getElement(group).getProperties()) {
                final String[] paths = this.schemaUtils.getPaths(group, column);
                final Object[] parquetObjects = new Object[paths.length];
                for (int i = 0; i < paths.length; i++) {
                    final String path = paths[i];
                    parquetObjects[i] = recursivlyGetObjectFromRecord(path, (GenericData.Record) record);
                }
                e.putProperty(column, this.getConverter(group).parquetObjectsToGafferObject(column, parquetObjects));
            }
            return e;
        }

        private Object recursivlyGetObjectFromRecord(final String path, final GenericData.Record record) {
            if (path.contains(".")) {
                return recursivlyGetObjectFromRecord(path.substring(path.indexOf(".") + 1), (GenericData.Record) record.get(path.substring(0, path.indexOf("."))));
            } else {
                if (record != null) {
                    Object result = record.get(path);
                    if (result instanceof ByteBuffer) {
                        result = ((ByteBuffer) result).array();
                    }
                    return result;
                } else {
                    return null;
                }
            }
        }

        private GafferGroupObjectConverter getConverter(final String group) throws SerialisationException {
            if (this.groupToObjectConverter.containsKey(group)) {
                return this.groupToObjectConverter.get(group);
            } else {
                GafferGroupObjectConverter converter = this.schemaUtils.getConverter(group);
                this.groupToObjectConverter.put(group, converter);
                return converter;
            }
        }

        @Override
        public void close() {
            try {
                if (this.reader != null) {
                    LOGGER.debug("Closing ParquetReader", this.reader);
                    this.reader.close();
                }
            } catch (IOException e) {
                LOGGER.warn("Failed to close " + this.getClass().getCanonicalName());
            }
        }
    }
}
