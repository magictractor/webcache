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

import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper for input streams from external data resources that contains a flag
 * that can indicate that content has not changed (for 304 status code from HTTP
 * requests).
 */
public class ResourceStreamSupplier implements AutoCloseable {

    public static ResourceStreamSupplier notModified() {
        return new ResourceStreamSupplier(false, null);
    }

    public static ResourceStreamSupplier forStream(InputStream inputStream) {
        return new ResourceStreamSupplier(true, inputStream);
    }

    private final boolean isModified;
    private final InputStream inputStream;

    private ResourceStreamSupplier(boolean isModified, InputStream inputStream) {
        this.isModified = isModified;
        this.inputStream = inputStream;
    }

    public boolean isModified() {
        return isModified;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) {
            inputStream.close();
        }
    }
}
