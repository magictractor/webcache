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
package uk.co.magictractor.webcache.listeners;

import uk.co.magictractor.webcache.ExternalDataResource;

/**
 *
 */
public interface ExternalDataResourceListener {

    void preSaveBody(ExternalDataResource dataResource);

    void postSaveBody(ExternalDataResource dataResource);

    /**
     * The first listener to return a non-null value determines whether the data
     * resource is expired. If all return null, the resource is not expired.
     */
    Boolean isExpired(ExternalDataResource dataResource);

}
