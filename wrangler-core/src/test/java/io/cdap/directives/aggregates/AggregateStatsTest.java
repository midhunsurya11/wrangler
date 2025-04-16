/*
 *  Copyright © 2017-2019 Cask Data, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy of
 *  the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations under
 *  the License.
 */
package io.cdap.directives.aggregates;

import io.cdap.wrangler.TestingRig;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.TimeDuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.when;

/**
 * Tests for the AggregateStats directive.
 */
@RunWith(MockitoJUnitRunner.class)
public class AggregateStatsTest {

    @Mock
    private ExecutorContext context;
    
    @Mock
    private TransientStore store;

    @Before
    public void setup() {
        when(context.getTransientStore()).thenReturn(store);
    }

    @Test
    public void testSizeAndTimeAggregation() throws Exception {
        String[] directives = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        List<Row> rows = Arrays.asList(
            new Row("data_transfer_size", new ByteSize("2.5MB"))
                .add("response_time", new TimeDuration("500ms")),
            new Row("data_transfer_size", new ByteSize("7.5MB"))
                .add("response_time", new TimeDuration("1.5s")),
            new Row("data_transfer_size", new ByteSize("5MB"))
                .add("response_time", new TimeDuration("750ms"))
        );

        when(store.get("total_bytes")).thenReturn(null);
        when(store.get("total_nanos")).thenReturn(null);
        when(store.get("row_count")).thenReturn(null);

        List<Row> results = TestingRig.execute(directives, rows, context);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(15.0, ((Double) results.get(0).getValue("total_size_mb")).doubleValue(), 0.001);
        Assert.assertEquals(2.75, ((Double) results.get(0).getValue("total_time_sec")).doubleValue(), 0.001);
    }

    @Test
    public void testDifferentUnits() throws Exception {
        String[] directives = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size_gb total_time_min 'GB' 'm'"
        };

        List<Row> rows = Arrays.asList(
            new Row("data_transfer_size", new ByteSize("512MB"))
                .add("response_time", new TimeDuration("30s")),
            new Row("data_transfer_size", new ByteSize("1.5GB"))
                .add("response_time", new TimeDuration("2m")),
            new Row("data_transfer_size", new ByteSize("2GB"))
                .add("response_time", new TimeDuration("2.5m"))
        );

        when(store.get("total_bytes")).thenReturn(null);
        when(store.get("total_nanos")).thenReturn(null);
        when(store.get("row_count")).thenReturn(null);

        List<Row> results = TestingRig.execute(directives, rows, context);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(4.0, ((Double) results.get(0).getValue("total_size_gb")).doubleValue(), 0.001);
        Assert.assertEquals(5.0, ((Double) results.get(0).getValue("total_time_min")).doubleValue(), 0.001);
    }

    @Test
    public void testEmptyInput() throws Exception {
        String[] directives = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size_mb total_time_sec"
        };

        // Replace List.of() with Collections.emptyList()
        List<Row> rows = Collections.emptyList();

        when(store.get("total_bytes")).thenReturn(null);
        when(store.get("total_nanos")).thenReturn(null);
        when(store.get("row_count")).thenReturn(null);

        List<Row> results = TestingRig.execute(directives, rows, context);

        Assert.assertEquals(1, results.size());
        Assert.assertEquals(0.0, ((Double) results.get(0).getValue("total_size_mb")).doubleValue(), 0.001);
        Assert.assertEquals(0.0, ((Double) results.get(0).getValue("total_time_sec")).doubleValue(), 0.001);
    }

    @Test(expected = DirectiveParseException.class)
    public void testInvalidSizeUnit() throws Exception {
        String[] directives = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size total_time 'XB' 's'"
        };
        TestingRig.execute(directives, Collections.singletonList(new Row()));
    }

    @Test(expected = DirectiveParseException.class) 
    public void testInvalidTimeUnit() throws Exception {
        String[] directives = new String[] {
            "aggregate-stats :data_transfer_size :response_time total_size total_time 'MB' 'x'"
        };
        TestingRig.execute(directives, Collections.singletonList(new Row()));
    }

    @Test(expected = DirectiveParseException.class)
    public void testMissingColumns() throws Exception {
        String[] directives = new String[] {
            "aggregate-stats :missing_size :missing_time total_size total_time"
        };
        List<Row> rows = Arrays.asList(
            new Row("data_transfer_size", new ByteSize("1MB"))
                .add("response_time", new TimeDuration("1s"))
        );
        TestingRig.execute(directives, rows);
    }
}
