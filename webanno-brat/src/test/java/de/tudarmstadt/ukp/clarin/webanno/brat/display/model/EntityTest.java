/*******************************************************************************
 * Copyright 2015
 * Ubiquitous Knowledge Processing (UKP) Lab and FG Language Technology
 * Technische Universität Darmstadt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package de.tudarmstadt.ukp.clarin.webanno.brat.display.model;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import de.tudarmstadt.ukp.clarin.webanno.support.JSONUtil;

public class EntityTest
{
    @Test
    public void toJsonTest()
        throws IOException
    {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();

        String json = JSONUtil.toPrettyJsonString(jsonConverter, new Entity(new VID(1, 2), "type",
                new Offsets(1, 2), "label", "color"));
        
        assertEquals("[ \"1.2\", \"type\", [ [ 1, 2 ] ], \"label\", \"color\" ]", json);
    }
}
