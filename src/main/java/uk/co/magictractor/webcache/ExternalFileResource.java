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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TimeZone;

import com.google.common.base.MoreObjects;

/**
 *
 */
public class ExternalFileResource extends AbstractExternalDataResource {

    private static final ZoneId DEFAULT_ZONE_ID = TimeZone.getDefault().toZoneId();

    // TODO! flexible locations (relative to user, different operating systems)

    private final File externalFile;

    public static ExternalFileResource of(String fileName) {
        return new ExternalFileResource(fileName);
    }

    private ExternalFileResource(String fileName) {
        externalFile = new File(fileName);
        if (!externalFile.exists()) {
            throw new IllegalArgumentException("File does not exist: " + fileName);
        }
    }

    @Override
    public String name() {
        return externalFile.toString();
    }

    @Override
    protected boolean isExpired() {
        // TODO! verify that listeners do not indicate expiry. Could do it on add?
        return hasExternalFileChanged();
    }

    private boolean hasExternalFileChanged() {
        // Millis are not preserved in the properties, so convert to seconds for comparison.
        long previousTimestamp = getProperties().getTimestamp().toInstant().toEpochMilli() / 1000;
        long currentTimestamp = externalFile.lastModified() / 1000;
        if (previousTimestamp < currentTimestamp) {
            getLogger().info("External file has changed {}", name());
            return true;
        }
        else if (previousTimestamp == currentTimestamp) {
            getLogger().debug("External file is unchanged {}", name());
            return false;
        }
        else {
            getLogger().warn("External file has unexpected timestamp (earlier than before), treating as changed {}", name());
            return true;
        }
    }

    @Override
    public ResourceStreamSupplier fetchResource() throws IOException {

        Instant lastModified = Instant.ofEpochMilli(externalFile.lastModified());
        ZonedDateTime timestamp = ZonedDateTime.ofInstant(lastModified, DEFAULT_ZONE_ID);
        getProperties().setTimestamp(timestamp);

        String fileName = externalFile.getName();
        int dotIndex = fileName.lastIndexOf(".");
        if (dotIndex > 0) {
            String extension = fileName.substring(dotIndex);
            getProperties().setBodyExtension(extension);
        }

        return ResourceStreamSupplier.forStream(new FileInputStream(externalFile));
    }

    @Override
    public String getCacheDir() {
        // TODO! some of path too?
        return externalFile.getName();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("externalFile", externalFile)
                .toString();
    }

}
