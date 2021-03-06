/*
 * Copyright 2020 Crown Copyright
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
package uk.gov.gchq.gaffer.accumulostore.operation.hdfs.mapper;

import org.apache.hadoop.conf.Configuration;

import uk.gov.gchq.gaffer.accumulostore.key.core.impl.byteEntity.ByteEntityAccumuloElementConverter;
import uk.gov.gchq.gaffer.accumulostore.operation.hdfs.handler.job.factory.AccumuloSampleDataForSplitPointsJobFactory;
import uk.gov.gchq.gaffer.accumulostore.utils.AccumuloStoreConstants;
import uk.gov.gchq.gaffer.hdfs.operation.mapper.AbstractGafferMapperTest;
import uk.gov.gchq.gaffer.hdfs.operation.mapper.GafferMapper;

public class SampleDataForSplitPointsMapperTest extends AbstractGafferMapperTest {
    @Override
    protected void applyMapperSpecificConfiguration(final Configuration configuration) {
        configuration.setFloat(AccumuloSampleDataForSplitPointsJobFactory.PROPORTION_TO_SAMPLE, 0.001F);
        configuration.set(AccumuloStoreConstants.ACCUMULO_ELEMENT_CONVERTER_CLASS, ByteEntityAccumuloElementConverter.class.getName());
    }

    @Override
    protected GafferMapper getGafferMapper() {
        return new SampleDataForSplitPointsMapper();
    }
}
