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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 *
 */
public class WebCacheTest {

    @Test
    public void testCopyName() {
        // This URL was used with the late Taransworld.
        WebCache webCache = WebCache.of("https://www.taransworld.com/SoulForge/weapons.pl");

        assertThat(webCache.getCacheDir()).isEqualTo("www.taransworld.com/SoulForge/weapons.pl");
    }

    @Test
    public void testCopyName_questionMark() {
        // This URL was used with the late Taransworld.
        WebCache webCache = WebCache.of("https://www.taransworld.com/Spoilers/?d=troops");

        assertThat(webCache.getCacheDir()).isEqualTo("www.taransworld.com/Spoilers/?d=troops");
    }

}
