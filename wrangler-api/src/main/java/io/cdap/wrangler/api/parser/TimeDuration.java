/*
 * Copyright © 2017-2019 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package io.cdap.wrangler.api.parser;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.cdap.wrangler.api.annotations.PublicEvolving;

/**
 * This class represents a time duration value with units (ms, s, m, h, d).
 * It parses string values like "100ms", "5s" into their millisecond equivalents.
 */
@PublicEvolving
public class TimeDuration implements Token {
    private final String originalValue;
    private final long milliseconds;

    /**
     * Creates a new TimeDuration instance from a string value.
     * @param value String representation of time duration (e.g. "100ms", "5s")
     */
    public TimeDuration(String value) {
        this.originalValue = value;
        this.milliseconds = parseMillis(value);
    }

    /**
     * Parses the time duration string into milliseconds.
     * @param value String representation of time duration
     * @return Number of milliseconds
     */
    private long parseMillis(String value) {
        String number = value.replaceAll("[^0-9]", "");
        String unit = value.replaceAll("[0-9]", "").toLowerCase();
        long duration = Long.parseLong(number);
        
        switch(unit) {
            case "ms": return duration;
            case "s": return duration * 1000L;
            case "m": return duration * 60L * 1000L;
            case "h": return duration * 60L * 60L * 1000L;
            case "d": return duration * 24L * 60L * 60L * 1000L;
            default: return duration;
        }
    }

    @Override
    public Long value() {
        return milliseconds;
    }

    @Override
    public TokenType type() {
        return TokenType.TIME_DURATION;
    }

    /**
     * Gets the milliseconds value.
     * @return Number of milliseconds
     */
    public long getMilliseconds() {
        return milliseconds;
    }

    /**
     * Gets the original string value.
     * @return Original string representation
     */
    public String getOriginalValue() {
        return originalValue;
    }

    @Override
    public JsonElement toJson() {
        JsonObject object = new JsonObject();
        object.addProperty("type", TokenType.TIME_DURATION.name());
        object.addProperty("value", originalValue);
        object.addProperty("milliseconds", milliseconds);
        return object;
    }
}



