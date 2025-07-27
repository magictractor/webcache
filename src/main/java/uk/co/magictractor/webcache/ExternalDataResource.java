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

import java.util.List;

import uk.co.magictractor.webcache.listeners.ExternalDataResourceListener;

/**
 * Common interface for external data resources. These are read-only and should
 * be cached locally when possible (notably for web sites).
 */
public interface ExternalDataResource extends DataResource {

    /**
     * A simple name for a resource, to be used for logging. For example
     * {@code "C:\MyFolder\MyFile.foo"} or
     * {@code "https://site.com/path/resource.json"}.
     */
    String name();

    CacheProperties getProperties();

    default CacheDataResource getBodyCacheDataResource() {
        return getCacheDataResource(getProperties().getBodyName());
    }

    CacheDataResource getCacheDataResource(String cacheName);

    ExternalDataResource addListener(ExternalDataResourceListener listener);

    ExternalDataResource addListeners(ExternalDataResourceListener... listeners);

    List<ExternalDataResourceListener> getListeners();

}
