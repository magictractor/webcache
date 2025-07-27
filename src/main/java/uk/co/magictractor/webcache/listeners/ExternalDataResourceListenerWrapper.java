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
package uk.co.magictractor.webcache.listeners;

import uk.co.magictractor.webcache.ExternalDataResource;

/**
 * A wrapper for {@code ExternalDataResourceListener}s. This is used to allow
 * the implementation to be changed after an {@code ExternalDataResource} has
 * been created, typically used for switching the expiry listener to force a
 * cache update.
 */
public class ExternalDataResourceListenerWrapper implements ExternalDataResourceListener {

    private ExternalDataResourceListener implementation;

    public ExternalDataResourceListenerWrapper(ExternalDataResourceListener wrapped) {
        this.implementation = wrapped;
    }

    public void setImplementation(ExternalDataResourceListener implementation) {
        this.implementation = implementation;
    }

    @Override
    public void preSaveBody(ExternalDataResource dataResource) {
        implementation.preSaveBody(dataResource);
    }

    @Override
    public void postSaveBody(ExternalDataResource dataResource) {
        implementation.postSaveBody(dataResource);
    }

    @Override
    public Boolean isExpired(ExternalDataResource dataResource) {
        return implementation.isExpired(dataResource);
    }

    // ah... need to do something for the logger class too...

}
