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

import io.cdap.cdap.api.annotation.Description;
import io.cdap.cdap.api.annotation.Name;
import io.cdap.cdap.api.annotation.Plugin;
import io.cdap.wrangler.api.Arguments;
import io.cdap.wrangler.api.Directive;
import io.cdap.wrangler.api.DirectiveExecutionException;
import io.cdap.wrangler.api.DirectiveParseException;
import io.cdap.wrangler.api.ExecutorContext;
import io.cdap.wrangler.api.Row;
import io.cdap.wrangler.api.TransientStore;
import io.cdap.wrangler.api.TransientVariableScope;
import io.cdap.wrangler.api.parser.ByteSize;
import io.cdap.wrangler.api.parser.ColumnName;
import io.cdap.wrangler.api.parser.Text;
import io.cdap.wrangler.api.parser.TimeDuration;
import io.cdap.wrangler.api.parser.TokenType;
import io.cdap.wrangler.api.parser.UsageDefinition;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Directive for aggregating byte sizes and time durations across rows.
 */
@Plugin(type = Directive.TYPE)
@Name("aggregate-stats")
@Description("Aggregates byte sizes and time durations into totals")
public class AggregateStats implements Directive {

  public static final String NAME = "aggregate-stats";
  private static final String STORE_BYTES_KEY = "total_bytes";
  private static final String STORE_NANOS_KEY = "total_nanos";
  private static final String STORE_COUNT_KEY = "row_count";
  
  private static final List<String> VALID_SIZE_UNITS = Arrays.asList("B", "KB", "MB", "GB", "TB");
  private static final List<String> VALID_TIME_UNITS = Arrays.asList("ns", "ms", "s", "m", "h");

  private String sizeColumn;
  private String timeColumn;
  private String totalSizeColumn;
  private String totalTimeColumn;
  private String sizeUnit;
  private String timeUnit;

  @Override
  public UsageDefinition define() {
    UsageDefinition.Builder builder = UsageDefinition.builder(NAME);
    builder.define("size_column", TokenType.COLUMN_NAME);
    builder.define("time_column", TokenType.COLUMN_NAME);
    builder.define("total_size_column", TokenType.COLUMN_NAME);
    builder.define("total_time_column", TokenType.COLUMN_NAME);
    builder.define("size_unit", TokenType.TEXT, true);
    builder.define("time_unit", TokenType.TEXT, true);
    return builder.build();
  }

  @Override
  public void initialize(Arguments args) throws DirectiveParseException {
    this.sizeColumn = ((ColumnName) args.value("size_column")).value();
    this.timeColumn = ((ColumnName) args.value("time_column")).value();
    this.totalSizeColumn = ((ColumnName) args.value("total_size_column")).value();
    this.totalTimeColumn = ((ColumnName) args.value("total_time_column")).value();

    if (args.contains("size_unit")) {
      String unit = ((Text) args.value("size_unit")).value().toUpperCase();
      if (!VALID_SIZE_UNITS.contains(unit)) {
        throw new DirectiveParseException(
          String.format("Invalid size unit '%s'. Valid units are: %s", unit, VALID_SIZE_UNITS));
      }
      this.sizeUnit = unit;
    } else {
      this.sizeUnit = "MB";
    }

    if (args.contains("time_unit")) {
      String unit = ((Text) args.value("time_unit")).value().toLowerCase();
      if (!VALID_TIME_UNITS.contains(unit)) {
        throw new DirectiveParseException(
          String.format("Invalid time unit '%s'. Valid units are: %s", unit, VALID_TIME_UNITS));
      }
      this.timeUnit = unit;
    } else {
      this.timeUnit = "s";
    }
  }

  @Override
  public List<Row> execute(List<Row> rows, ExecutorContext context) throws DirectiveExecutionException {
    TransientStore store = context.getTransientStore();
    long totalBytes = store.get(STORE_BYTES_KEY) != null ? (Long) store.get(STORE_BYTES_KEY) : 0L;
    long totalNanos = store.get(STORE_NANOS_KEY) != null ? (Long) store.get(STORE_NANOS_KEY) : 0L;
    int rowCount = store.get(STORE_COUNT_KEY) != null ? (Integer) store.get(STORE_COUNT_KEY) : 0;

    for (Row row : rows) {
      Object sizeObj = row.getValue(sizeColumn);
      Object timeObj = row.getValue(timeColumn);

      if (sizeObj == null) {
        throw new DirectiveExecutionException(String.format("Column '%s' has null value", sizeColumn));
      }
      if (timeObj == null) {
        throw new DirectiveExecutionException(String.format("Column '%s' has null value", timeColumn));
      }

      if (!(sizeObj instanceof ByteSize)) {
        throw new DirectiveExecutionException(
          String.format("Column '%s' is not a ByteSize value: %s", sizeColumn, sizeObj.getClass().getName()));
      }
      if (!(timeObj instanceof TimeDuration)) {
        throw new DirectiveExecutionException(
          String.format("Column '%s' is not a TimeDuration value: %s", timeColumn, timeObj.getClass().getName()));
      }

      totalBytes += ((ByteSize) sizeObj).getBytes();
      totalNanos += ((TimeDuration) timeObj).getMilliseconds() * 1_000_000L;
      rowCount++;
    }

    store.set(TransientVariableScope.GLOBAL, STORE_BYTES_KEY, totalBytes);
    store.set(TransientVariableScope.GLOBAL, STORE_NANOS_KEY, totalNanos);
    store.set(TransientVariableScope.GLOBAL, STORE_COUNT_KEY, rowCount);

    Row result = new Row();
    result.add(totalSizeColumn, convertBytes(totalBytes, sizeUnit));
    result.add(totalTimeColumn, convertTime(totalNanos, timeUnit));

    // Changed List.of() to Collections.singletonList() for JDK 8 compatibility
    return Collections.singletonList(result);
  }

  private double convertBytes(long bytes, String unit) {
    switch (unit.toUpperCase()) {
      case "KB": return bytes / 1024.0;
      case "MB": return bytes / (1024.0 * 1024.0);
      case "GB": return bytes / (1024.0 * 1024.0 * 1024.0);
      case "TB": return bytes / (1024.0 * 1024.0 * 1024.0 * 1024.0);
      default: return bytes;
    }
  }

  private double convertTime(long nanos, String unit) {
    switch (unit.toLowerCase()) {
      case "ms": return nanos / 1_000_000.0;
      case "s": return nanos / 1_000_000_000.0;
      case "m": return nanos / (60.0 * 1_000_000_000.0);
      case "h": return nanos / (3600.0 * 1_000_000_000.0);
      default: return nanos;
    }
  }

  @Override
  public void destroy() {
    // No resources to clean up
  }
}




