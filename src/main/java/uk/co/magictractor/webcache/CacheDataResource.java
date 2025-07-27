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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.Charset;

import com.google.common.io.ByteStreams;

/**
 * Common interface for local cache data resources. These are writable. For each
 * ExternalDataResource there are likely to be several CacheDataResources
 * containing a body of the resource, properties associated with the resource,
 * HTTP response headers etc.
 */
public interface CacheDataResource extends DataResource {

    boolean exists();

    int size();

    OutputStream openOutputStream();

    default public Writer openWriter(Charset charset) {
        return new OutputStreamWriter(openOutputStream(), charset);
    }

    default void setContent(String text, Charset charset) {
        setContent(text.toCharArray(), charset);
    }

    default void setContent(char[] chars, Charset charset) {
        try (OutputStreamWriter out = new OutputStreamWriter(openOutputStream())) {
            out.write(chars);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    default void setContent(byte[] bytes) {
        try (OutputStream out = openOutputStream()) {
            out.write(bytes);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    default long setContent(InputStream remoteSource) {
        try (OutputStream out = openOutputStream()) {
            return ByteStreams.copy(remoteSource, out);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
