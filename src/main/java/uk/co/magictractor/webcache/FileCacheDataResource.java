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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;

import com.google.common.base.MoreObjects;
import com.google.common.io.Files;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copy of a DataResource which is used for local caches of web files, decrypted
 * files and pretty printed Json.
 */
public class FileCacheDataResource implements CacheDataResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileCacheDataResource.class);

    private static final File CACHE_BASE_DIR = determineCacheBaseDir();

    // Path?
    private final File file;
    private final Charset charset;

    public FileCacheDataResource(String dirName, String fileName, Charset charset) {
        File dir = new File(CACHE_BASE_DIR, tidyDir(dirName));
        file = new File(dir, fileName);
        this.charset = charset;
    }

    private static final File determineCacheBaseDir() {
        File cacheBaseDir = sourceCacheBaseDir();
        if (cacheBaseDir == null) {
            // TODO! something better than this
            cacheBaseDir = Files.createTempDir();
        }

        return cacheBaseDir;
    }

    private static File sourceCacheBaseDir() {
        Class<?> baseClass = determineBaseClass();
        URL classUrl = baseClass.getResource(baseClass.getSimpleName() + ".class");

        if (!"file".contentEquals(classUrl.getProtocol())) {
            // Not running from source, most likely a jar file.
            // Not appropriate and not possible to write to src/main/resources
            LOGGER.debug("Not file protocol, cannot write to src/main/resources: {}", classUrl);
            return null;
        }

        // Use URI to tolerate spaces in path (seen with space in user name)
        URI classUri;
        try {
            classUri = classUrl.toURI();
        }
        catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
        File file = new File(classUri);
        if (!file.exists()) {
            throw new IllegalStateException("File does not exist: " + classUrl.getFile());
        }

        boolean targetDir = false;
        do {
            String fileName = file.getName();
            targetDir = "target".equals(fileName) || "bin".equals(fileName);
            file = file.getParentFile();
        } while (!targetDir);

        File baseDir = new File(file, "/src/main/resources/webcache/");
        LOGGER.info("Webcache base directory is {}", baseDir);

        return baseDir;
    }

    private static Class<?> determineBaseClass() {
        StackTraceElement[] stackTraceElements = new RuntimeException().getStackTrace();
        StackTraceElement lastStackTraceElement = stackTraceElements[stackTraceElements.length - 1];
        try {
            return Class.forName(lastStackTraceElement.getClassName());
        }
        catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    private String tidyDir(String dirName) {
        // Some characters cannot appear in local file names.
        return dirName
                .replace("?", "__")
                .replace(":", "");
    }

    @Override
    public Charset getCharset() {
        return charset;
    }

    private void touch() {
        try {
            touch0();
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void touch0() throws IOException {
        // Files.exists
        if (file.exists()) {
            return;
        }

        file.getParentFile().mkdirs();

        LOGGER.debug("Creating new file {}", file);

        file.createNewFile();
    }

    @Override
    public boolean exists() {
        return file.exists();
    }

    @Override
    public int size() {
        return (int) file.length();
    }

    @Override
    public InputStream openInputStream() {
        try {
            return new FileInputStream(file);
        }
        catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public OutputStream openOutputStream() {
        touch();
        try {
            return new FileOutputStream(file);
        }
        catch (FileNotFoundException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("file", file)
                .toString();
    }

}
