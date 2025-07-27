/**
 * Copyright 2019 Ken Dobson
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
package uk.co.magictractor.webcache;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 *
 */
public final class CacheProperties {

    private static final DateTimeFormatter ZONED_DATE_TIME_FORMATTER = DateTimeFormatter.RFC_1123_DATE_TIME;

    private static final String BODY_BASE_KEY = "Body-Base";

    /**
     * Extension which may be appended to cache files to associate an
     * application for reading the file. Expected to start with a dot, like
     * ".json".
     */
    private static final String BODY_EXTENSION_KEY = "Body-Extension";

    private static final String CONTENT_TYPE_KEY = "Content-Type";

    private static final String CHARSET_KEY = "Charset";

    /**
     * The timestamp of an external resource. Used to determine when external
     * files have changed.
     */
    private static final String TIMESTAMP_KEY = "Timestamp";

    /**
     * The last modification time of a web resource. Found in the HTTP response
     * header and should be included in subsequent requests so that the server
     * may indicate that the resource has not changed by returning a 304 Not
     * Modified status code.
     */
    private static final String LAST_MODIFIED_KEY = "Last-Modified";

    private static final String ETAG_KEY = "ETag";

    private String bodyBase;
    private String bodyExtension;
    private String contentType;
    private Charset charset;
    private String lastModified;
    private ZonedDateTime timestamp;
    private String etag;

    public static final CacheProperties newWithDefaults() {
        CacheProperties properties = new CacheProperties();
        properties.setBodyBase("body");
        // Set null to create a placeholder, giving consistent ordering for the property across resources.
        properties.setBodyExtension(null);

        return properties;
    }

    public String getBodyBase() {
        return bodyBase;
    }

    public void setBodyBase(String bodyBase) {
        this.bodyBase = bodyBase;
    }

    public String getBodyExtension() {
        return bodyExtension;
    }

    public void setBodyExtension(String bodyExtension) {
        if (bodyExtension != null && !bodyExtension.startsWith(".")) {
            throw new IllegalArgumentException("extension should start with a dot");
        }

        this.bodyExtension = bodyExtension;
    }

    public String getBodyName() {
        return getBodyName(null);
    }

    public String getBodyName(String suffix) {
        StringBuilder nameBuilder = new StringBuilder();
        nameBuilder.append(getBodyBase());
        if (suffix != null) {
            nameBuilder.append('_');
            nameBuilder.append(suffix);
        }
        String extension = getBodyExtension();
        if (extension != null) {
            nameBuilder.append(extension);
        }

        return nameBuilder.toString();
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getCharsetName() {
        return charset == null ? null : charset.name();
    }

    // TODO! change getter and setter to Charset rather than String?
    public void setCharsetName(String charsetName) {
        this.charset = Charset.forName(charsetName);
    }

    public Charset getCharset() {
        return charset;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public void read(InputStream in) throws IOException {
        InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8);
        read(reader);
    }

    public void read(Reader reader) throws IOException {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);

        int token = tokenizer.nextToken();
        if (token != '{') {
            throw new IllegalStateException("Expected '{', but was " + token);
        }

        while (true) {
            token = tokenizer.nextToken();
            if (token != '"') {
                throw new IllegalStateException("Expected '\"', but was " + token);
            }
            String key = tokenizer.sval;

            token = tokenizer.nextToken();
            if (token != ':') {
                throw new IllegalStateException("Expected ':', but was " + token);
            }

            token = tokenizer.nextToken();
            if (token != '"') {
                throw new IllegalStateException("Expected '\"', but was " + token);
            }
            String value = tokenizer.sval;

            read(key, value);

            token = tokenizer.nextToken();
            if (token == '}') {
                break;
            }
            if (token != ',') {
                throw new IllegalStateException("Expected ',', but was " + token);
            }
        }

        token = tokenizer.nextToken();
        if (token != StreamTokenizer.TT_EOF) {
            throw new IllegalStateException("Expected TT_EOF, but was " + token);
        }
    }

    private void read(String key, String value) {
        switch (key) {
            case BODY_BASE_KEY:
                setBodyBase(value);
                break;
            case BODY_EXTENSION_KEY:
                setBodyExtension(value);
                break;
            case CONTENT_TYPE_KEY:
                setContentType(value);
                break;
            case CHARSET_KEY:
                setCharsetName(value);
                break;
            case TIMESTAMP_KEY:
                setTimestamp(ZONED_DATE_TIME_FORMATTER.parse(value, ZonedDateTime::from));
                break;
            case LAST_MODIFIED_KEY:
                setLastModified(value);
                break;
            case ETAG_KEY:
                setEtag(value);
                break;
            default:
                throw new IllegalStateException("Code needs modification to handle ");
        }
    }

    public void write(OutputStream out) throws IOException {
        Writer writer = new BufferedWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8));
        write(writer);
    }

    public void write(Writer writer) throws IOException {
        writer.write("{\n");
        boolean isFirst = true;
        isFirst = write(writer, BODY_BASE_KEY, getBodyBase(), isFirst);
        isFirst = write(writer, BODY_EXTENSION_KEY, getBodyExtension(), isFirst);
        isFirst = write(writer, CONTENT_TYPE_KEY, getContentType(), isFirst);
        isFirst = write(writer, CHARSET_KEY, getCharsetName(), isFirst);
        isFirst = write(writer, LAST_MODIFIED_KEY, getLastModified(), isFirst);
        isFirst = write(writer, ETAG_KEY, getEtag(), isFirst);
        isFirst = write(writer, TIMESTAMP_KEY, getTimestamp(), isFirst);
        writer.write("\n}\n");
        writer.flush();
    }

    private boolean write(Writer writer, String key, ZonedDateTime value, boolean isFirst) throws IOException {
        String valueString = value == null ? null : ZONED_DATE_TIME_FORMATTER.format(value);
        return write(writer, key, valueString, isFirst);
    }

    private boolean write(Writer writer, String key, String value, boolean isFirst) throws IOException {
        if (value == null) {
            return isFirst;
        }

        if (!isFirst) {
            writer.write(",\n");
        }
        writer.write("  \"");
        writer.write(key);
        writer.write("\": \"");
        if (ETAG_KEY.equals(key)) {
            // Escape quotes in ETags
            writer.write(value.replace("\"", "\\\""));
        }
        else {
            writer.write(value);
        }
        writer.write("\"");

        return false;
    }

}
