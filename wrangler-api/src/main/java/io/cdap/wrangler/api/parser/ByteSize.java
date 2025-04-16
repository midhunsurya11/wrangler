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
 * This class represents a byte size value with units (KB, MB, GB, etc.).
 * It parses string values like "5KB", "10MB" into their byte equivalents.
 */
@PublicEvolving
public class ByteSize implements Token {
    private final String originalValue;
    private final long bytes;

    /**
     * Creates a new ByteSize instance from a string value.
     * @param value String representation of byte size (e.g. "5KB", "10MB")
     */
    public ByteSize(String value) {
        this.originalValue = value;
        this.bytes = parseBytes(value);
    }

    /**
     * Parses the byte size string into actual bytes.
     * @param value String representation of byte size
     * @return Number of bytes
     */
    private long parseBytes(String value) {
        String number = value.replaceAll("[^0-9]", "");
        String unit = value.replaceAll("[0-9]", "").toUpperCase();
        long size = Long.parseLong(number);
        
        switch(unit) {
            case "B": return size;
            case "KB": return size * 1024L;
            case "MB": return size * 1024L * 1024L;
            case "GB": return size * 1024L * 1024L * 1024L;
            case "TB": return size * 1024L * 1024L * 1024L * 1024L;
            case "PB": return size * 1024L * 1024L * 1024L * 1024L * 1024L;
            default: return size;
        }
    }

    @Override
    public Long value() {
        return bytes;
    }

    @Override
    public TokenType type() {
        return TokenType.BYTE_SIZE;
    }

    /**
     * Gets the byte value.
     * @return Number of bytes
     */
    public long getBytes() {
        return bytes;
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
        object.addProperty("type", TokenType.BYTE_SIZE.name());
        object.addProperty("value", originalValue);
        object.addProperty("bytes", bytes);
        return object;
    }
}





