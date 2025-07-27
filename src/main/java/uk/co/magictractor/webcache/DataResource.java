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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 *
 */
public interface DataResource {

    /**
     * May return null if {@link #openInputStream()} or
     * {@link #openReader(Charset)} is used and never {@link #openReader()}.
     */
    Charset getCharset();

    /** Caller is responsible for closing the stream. */
    InputStream openInputStream();

    default Reader openReader() {
        Charset charset = getCharset();
        if (getCharset() == null) {
            throw new IllegalStateException(
                "Charset unknown. Specify a default using getCharset() or use openReader(Charset).");
        }

        return openReader(charset);
    }

    /** Caller is responsible for closing the reader. */
    default Reader openReader(Charset charset) {
        try {
            return new InputStreamReader(openInputStream(), charset);
        }
        catch (RuntimeException e) {
            // Can get Json errors from properties files when properties structure changes.
            throw new IllegalStateException("Error reading " + this, e);
        }
    }

}
