/*******************************************************************************
 * Copyright 2012
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

package de.tudarmstadt.ukp.clarin.webanno.brat.util;

import de.tudarmstadt.ukp.clarin.webanno.brat.controller.BratAnnotationException;

/**
 * Throw an exception if either a target or orgin span annotation is not merged before the arc
 * annotation merging is attempted
 *
 */
public class NoOriginOrTargetAnnotationSelectedException
    extends BratAnnotationException
{
    private static final long serialVersionUID = 1280015349963924638L;

    public NoOriginOrTargetAnnotationSelectedException(String message)
    {
        super(message);
    }

}
