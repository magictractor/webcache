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
package uk.co.magictractor.gowbuddy.parse;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import uk.co.magictractor.webcache.WebCache;

/**
 *
 */
public class XPathReader {

    private final XPath xpath;
    private final Document xml;

    public XPathReader(WebCache webCache) {
        xpath = XPathFactory.newInstance().newXPath();

        DocumentBuilder builder;
        try {
            builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException e) {
            throw new IllegalStateException(e);
        }

        // return builder.parse(HttpMessageUtil.createBodyInputStream(httpMessage));
        try (InputStream xmlStream = webCache.openInputStream()) {
            xml = builder.parse(xmlStream);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        catch (SAXException e) {
            throw new IllegalStateException(e);
        }
    }

}
