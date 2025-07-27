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
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.magictractor.webcache.listeners.ExternalDataResourceListener;

/**
 *
 */
public abstract class AbstractExternalDataResource implements ExternalDataResource {

    // Contains charset, content type and download date
    private static final String PROPERTIES_FILE = "properties.json";

    // Same logger as implementing classes.
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<ExternalDataResourceListener> listeners = new ArrayList<>();
    private boolean isFetching;

    private Map<String, FileCacheDataResource> localCopies = new HashMap<>();
    private CacheProperties properties;

    abstract protected boolean isExpired();

    @Override
    public final InputStream openInputStream() {
        if (isFetchRequired()) {
            isFetching = true;

            try (ResourceStreamSupplier rss = fetchResource()) {
                if (rss.isModified()) {
                    InputStream in = rss.getInputStream();

                    // fetchResource() may have explictly set properties, but if not, values can be inferred.
                    inferProperties();

                    preSaveBody();

                    // Save a local copy of the content.
                    CacheDataResource bodyResource = getBodyCacheDataResource();
                    bodyResource.setContent(in);

                    // hook for creating a pretty printed copy of the cached body
                    // hooks could also modify properties, so do this before writing properties
                    postSaveBody();

                    // TODO! maybe time elapsed too?
                    logger.info("Fetched {} bytes from {}", bodyResource.size(), name());
                }
                else {
                    logger.info("Confirmed that existing local cache already matches server data for {}", name());
                }

                // Properties are written even if the content was not modified
                // because properties includes a timestamp.
                writeProperties();
            }
            catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            finally {
                isFetching = false;
            }
        }

        return getBodyCacheDataResource().openInputStream();
    }

    private boolean isFetchRequired() {
        if (isFetching) {
            // Happens with postSaveBody hooks such as PrettyPrinterDataResourceListener
            logger.trace("Fetch not required, fetch is in progress (likely caused by a post-save hook {}", name());
            return false;
        }

        if (!hasProperties()) {
            logger.info("Fetch required due to no existing properties {}", name());
            return true;
        }

        CacheDataResource bodyCache = getBodyCacheDataResource();
        boolean isCached = bodyCache.exists();
        if (!isCached) {
            logger.info("Fetch required due to missing (deleted?) body file {}", name());
            // This is unusual. There's a properties file, but no local copy of the body.
            // Most likely the local copy has been manually deleted to force a download.
            // So we must not use If-Modified-Since or If-None-Match in the HTTP request.
            getProperties().setLastModified(null);
            getProperties().setEtag(null);

            return true;
        }

        return isExpired();
    }

    /**
     * Fetch an external resource. This might update properties
     * (properties.json) and header files for HTTP (headers.txt).
     */
    abstract ResourceStreamSupplier fetchResource() throws IOException;

    // The pre=save hook is used to modify the extension for encrypted files.
    private void preSaveBody() {
        for (ExternalDataResourceListener listener : listeners) {
            listener.preSaveBody(this);
        }
    }

    // The post-save hook is used to modify and save copies of the resource, specifically decrypting (World.json) and pretty printing (.json).
    private void postSaveBody() {
        for (ExternalDataResourceListener listener : listeners) {
            listener.postSaveBody(this);
        }
    }

    @Override
    public ExternalDataResource addListener(ExternalDataResourceListener listener) {
        listeners.add(listener);
        return this;
    }

    @Override
    public ExternalDataResource addListeners(ExternalDataResourceListener... listeners) {
        this.listeners.addAll(Arrays.asList(listeners));
        return this;
    }

    @Override
    public List<ExternalDataResourceListener> getListeners() {
        // Mutable seems OK here. Users may add, remove or reorder if they wish.
        return listeners;
    }

    private void inferProperties() {
        CacheProperties properties = getProperties();
        if (properties.getBodyExtension() == null) {
            properties.setBodyExtension(inferExtension());
        }
        if (properties.getContentType() == null) {
            properties.setContentType(inferContentType());
        }
    }

    private String inferExtension() {
        String contentType = getProperties().getContentType();
        if (contentType != null) {
            int slashIndex = contentType.indexOf("/");
            return "." + contentType.substring(slashIndex + 1);
        }

        return null;
    }

    private String inferContentType() {
        String extension = getProperties().getBodyExtension();
        if (".json".equals(extension)) {
            return WebCacheConstants.CONTENT_TYPE_JSON;
        }
        return WebCacheConstants.CONTENT_TYPE_OCTET_STREAM;
    }

    private boolean hasProperties() {
        return getCacheDataResource(PROPERTIES_FILE).exists();
    }

    private void writeProperties() {
        CacheDataResource propertiesCacheResource = getCacheDataResource(PROPERTIES_FILE);
        try (OutputStream out = propertiesCacheResource.openOutputStream()) {
            getProperties().write(out);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public final Charset getCharset() {
        return getProperties().getCharset();
    }

    @Override
    public CacheProperties getProperties() {
        if (properties == null) {
            CacheDataResource propertiesFile = getCacheDataResource(PROPERTIES_FILE);
            if (propertiesFile.exists()) {
                properties = new CacheProperties();
                try (InputStream in = propertiesFile.openInputStream()) {
                    properties.read(in);
                }
                catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
                logger.debug("Read properties for {}: {} ", name(), properties);
            }
            else {
                properties = CacheProperties.newWithDefaults();
                logger.debug("Created properties for {}: {} ", name(), properties);
            }
        }
        return properties;
    }

    @Override
    public FileCacheDataResource getCacheDataResource(String fileName) {
        FileCacheDataResource localCopy = localCopies.get(fileName);
        if (localCopy == null) {
            synchronized (localCopies) {
                localCopy = localCopies.get(fileName);
                if (localCopy == null) {
                    // TODO! charsets...
                    localCopy = new FileCacheDataResource(getCacheDir(), fileName, StandardCharsets.UTF_8);
                }
            }
        }
        return localCopy;
    }

    abstract String getCacheDir();

    protected Logger getLogger() {
        return logger;
    }

}
