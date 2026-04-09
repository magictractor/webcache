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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;
import com.jayway.jsonpath.spi.json.JsonProvider;
import com.jayway.jsonpath.spi.mapper.GsonMappingProvider;
import com.jayway.jsonpath.spi.mapper.MappingProvider;

import uk.co.magictractor.json.JsonWriter;
import uk.co.magictractor.webcache.CacheDataResource;
import uk.co.magictractor.webcache.ExternalDataResource;
import uk.co.magictractor.webcache.WebCacheConstants;

/**
 *
 */
public class PrettyPrinterDataResourceListener extends AbstractExternalDataResourceListener {

    @Override
    public void postSaveBody(ExternalDataResource dataResource) {

        String contentType = dataResource.getProperties().getContentType();
        if (!WebCacheConstants.CONTENT_TYPE_JSON.equals(contentType)) {
            return;
        }

        String ppName = dataResource.getProperties().getBodyName("pretty");
        CacheDataResource ppResource = dataResource.getCacheDataResource(ppName);

        // ctx = dataResource.openJsonReader(config).
        DocumentContext ctx;
        try (InputStream jsonStream = dataResource.openInputStream()) {
            ctx = JsonPath.parse(jsonStream, createConfiguration());
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        JsonWriter ppWriter = new JsonWriter(ctx);
        try (OutputStream out = ppResource.openOutputStream()) {
            ppWriter.write(out);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private Configuration createConfiguration() {
        GsonBuilder gsonBuilder = new GsonBuilder();

        Gson gson = gsonBuilder.setPrettyPrinting().create();
        // TODO! also set formatting style

        JsonProvider jsonProvider = new GsonJsonProvider(gson);
        MappingProvider mappingProvider = new GsonMappingProvider(gson);

        return new Configuration.ConfigurationBuilder()
                .jsonProvider(jsonProvider)
                .mappingProvider(mappingProvider)
                .build();
    }

    //    @Override
    //    public void postSaveBody(ExternalDataResource dataResource) {
    //
    //        String contentType = dataResource.getProperties().getContentType();
    //        if (!WebCacheConstants.CONTENT_TYPE_JSON.equals(contentType)) {
    //            return;
    //        }
    //
    //        String ppName = dataResource.getProperties().getBodyName("pretty");
    //        CacheDataResource ppResource = dataResource.getCacheDataResource(ppName);
    //
    //        JsonWriter ppWriter = new JsonWriter(dataResource, new PrettyPrinterJsonConfig());
    //        try (OutputStream out = ppResource.openOutputStream()) {
    //            ppWriter.write(out);
    //        }
    //        catch (IOException e) {
    //            throw new UncheckedIOException(e);
    //        }
    //    }

}
