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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;

import com.google.common.base.MoreObjects;

import uk.co.magictractor.webcache.listeners.ExternalDataResourceListener;

/**
 *
 */
// https://www.taransworld.com/Spoilers/?d=weapons&showAll=1
public class WebCache extends AbstractExternalDataResource {

    private static final String HEADERS_FILE = "headers.txt";

    public static WebCache of(String resourceName) {
        return new WebCache(resourceName);
    }

    private final URL externalUrl;

    private WebCache(String externalUrlSpec) {
        try {
            externalUrl = new URL(externalUrlSpec);
        }
        catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL spec: " + externalUrlSpec, e);
        }

        String protocol = externalUrl.getProtocol();
        if (!"http".equals(protocol) && !"https".equals(protocol)) {
            throw new IllegalStateException(
                getClass().getSimpleName()
                        + " should only be used for http and https protocols, not suitable for "
                        + externalUrl);
        }

    }

    @Override
    public String name() {
        return externalUrl.toExternalForm();
    }

    @Override
    protected boolean isExpired() {
        for (ExternalDataResourceListener listener : getListeners()) {
            Boolean listenerExpiry = listener.isExpired(this);
            if (listenerExpiry != null) {
                return listenerExpiry;
            }
        }

        // If this is intended then the message can be avoided by adding EventListeners.never().
        getLogger().warn("Resource will never expire because no listeners return a value for isExpired()");

        return false;
    }

    @Override
    public String getCacheDir() {
        StringBuilder copyNameBuilder = new StringBuilder();

        copyNameBuilder.append(externalUrl.getHost());
        copyNameBuilder.append(externalUrl.getPath());
        if (externalUrl.getQuery() != null) {
            copyNameBuilder.append("?");
            copyNameBuilder.append(externalUrl.getQuery());
        }

        return copyNameBuilder.toString();
    }

    // TODO! also "Expires" etc?
    @Override
    public ResourceStreamSupplier fetchResource() throws IOException {
        CacheProperties properties = getProperties();

        HttpURLConnection httpConnection = (HttpURLConnection) externalUrl.openConnection();
        if (properties.getLastModified() != null) {
            httpConnection.setRequestProperty("If-Modified-Since", properties.getLastModified());
        }
        if (properties.getEtag() != null) {
            // Weak validation ("W/" prefix) is fine, it indicates that we only care about the content.
            httpConnection.setRequestProperty("If-None-Match", "W/" + properties.getEtag());
        }

        StringBuilder headersBuilder = new StringBuilder();
        int headerIndex = 0;
        while (true) {
            String headerValue = httpConnection.getHeaderField(headerIndex);
            if (headerValue == null) {
                break;
            }
            String headerName = httpConnection.getHeaderFieldKey(headerIndex);
            if (headerName != null) {
                headersBuilder.append(headerName);
                headersBuilder.append(": ");
                updateProperties(headerName, headerValue);
            }
            else {
                if (headerIndex != 0) {
                    // Should only be for the first line like
                    // HTTP/1.1 200 OK
                    throw new IllegalStateException();
                }
            }
            headersBuilder.append(headerValue);
            headersBuilder.append('\n');

            headerIndex++;

        }
        getCacheDataResource(HEADERS_FILE).setContent(headersBuilder.toString(), StandardCharsets.UTF_8);
        getProperties().setTimestamp(ZonedDateTime.now());

        int statusCode = httpConnection.getResponseCode();
        if (statusCode == 200) {
            return ResourceStreamSupplier.forStream(httpConnection.getInputStream());
        }
        else if (statusCode == 304) {
            return ResourceStreamSupplier.notModified();
        }

        throw new IllegalStateException("Unexpected response: " + statusCode + " " + httpConnection.getResponseMessage());
    }

    private void updateProperties(String headerName, String headerValue) {
        if ("Content-Type".equalsIgnoreCase(headerName)) {
            String contentType = headerValue;
            // Content-Type: text/html;charset=UTF-8
            int splitIndex = contentType.indexOf(";");
            if (splitIndex != -1) {
                int equalsIndex = contentType.indexOf("=", splitIndex + 1);
                if (!"charset".equals(contentType.substring(splitIndex + 1, equalsIndex).trim())) {
                    throw new IllegalStateException();
                }
                getProperties().setCharsetName(contentType.substring(equalsIndex + 1).trim());
                contentType = contentType.substring(0, splitIndex).trim();
            }
            getProperties().setContentType(contentType);
        }
        else if ("Last-Modified".equalsIgnoreCase(headerName)) {
            getProperties().setLastModified(headerValue);
        }
        else if ("ETag".equalsIgnoreCase(headerName)) {
            getProperties().setEtag(headerValue);
        }

    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("externalUrl", externalUrl)
                .toString();
    }

}
