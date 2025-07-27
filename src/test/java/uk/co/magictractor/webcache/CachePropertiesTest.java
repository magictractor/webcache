/**
 * Copyright 2025 Ken Dobson
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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import org.junit.jupiter.api.Test;

public class CachePropertiesTest {

    @Test
    public void testRead_gemologica() throws IOException, ClassNotFoundException {
        CacheProperties props = read("properties_gemologica.json");

        assertThat(props.getBodyBase()).isEqualTo("body");
        assertThat(props.getBodyExtension()).isEqualTo(".json");
        assertThat(props.getCharset()).isEqualTo(StandardCharsets.UTF_8);
        assertThat(props.getContentType()).isEqualTo("application/json");
        assertThat(props.getEtag()).isEqualTo("W/\"424a25-Ie0CoPkr9tV7mpas7QYK1BcjBrs\"");
        assertThat(props.getTimestamp()).isEqualTo(ZonedDateTime.of(2025, 7, 22, 14, 36, 37, 0, ZoneId.of("+0100")));
        assertThat(props.getLastModified()).isNull();
    }

    @Test
    public void testRead_taranData() throws IOException, ClassNotFoundException {
        CacheProperties props = read("properties_tarandata.json");

        assertThat(props.getBodyBase()).isEqualTo("body");
        assertThat(props.getBodyExtension()).isEqualTo(".json");
        assertThat(props.getCharset()).isNull();
        assertThat(props.getContentType()).isEqualTo("application/json");
        assertThat(props.getEtag()).isEqualTo("\"261d93-63abb88f029c0\"");
        assertThat(props.getTimestamp()).isEqualTo(ZonedDateTime.of(2025, 7, 26, 10, 4, 40, 0, ZoneId.of("+0100")));
        assertThat(props.getLastModified()).isEqualTo("Fri, 25 Jul 2025 07:03:11 GMT");
    }

    @Test
    public void testWrite() throws IOException, ClassNotFoundException {
        CacheProperties props = new CacheProperties();

        props.setBodyBase("body");
        props.setBodyExtension(".xml");
        props.setCharsetName("ISO-8859-1");
        props.setContentType("application/xml");
        props.setLastModified("Fri, 25 Jul 2025 07:03:11 GMT");
        props.setEtag("W/\"424a25-Ie0CoPkr9tV7mpas7QYK1BcjBrs\"");
        props.setTimestamp(ZonedDateTime.of(2025, 7, 26, 10, 4, 40, 0, ZoneId.of("-0500")));

        StringWriter writer = new StringWriter();
        props.write(writer);

        writer.toString();
        BufferedReader reader = new BufferedReader(new StringReader(writer.toString()));
        assertThat(reader.readLine()).isEqualTo("{");
        assertThat(reader.readLine()).isEqualTo("  \"Body-Base\": \"body\",");
        assertThat(reader.readLine()).isEqualTo("  \"Body-Extension\": \".xml\",");
        assertThat(reader.readLine()).isEqualTo("  \"Content-Type\": \"application/xml\",");
        assertThat(reader.readLine()).isEqualTo("  \"Charset\": \"ISO-8859-1\",");
        assertThat(reader.readLine()).isEqualTo("  \"Last-Modified\": \"Fri, 25 Jul 2025 07:03:11 GMT\",");
        assertThat(reader.readLine()).isEqualTo("  \"ETag\": \"W/\\\"424a25-Ie0CoPkr9tV7mpas7QYK1BcjBrs\\\"\",");
        assertThat(reader.readLine()).isEqualTo("  \"Timestamp\": \"Sat, 26 Jul 2025 10:04:40 -0500\"");
        assertThat(reader.readLine()).isEqualTo("}");
        assertThat(reader.readLine()).isEqualTo(null);
    }

    private CacheProperties read(String resourceName) throws IOException, ClassNotFoundException {
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IllegalArgumentException("No resource with name " + resourceName);
            }

            return read(is);
        }
    }

    private CacheProperties read(InputStream is) throws IOException, ClassNotFoundException {
        CacheProperties props = new CacheProperties();
        props.read(is);

        return props;
    }

}
