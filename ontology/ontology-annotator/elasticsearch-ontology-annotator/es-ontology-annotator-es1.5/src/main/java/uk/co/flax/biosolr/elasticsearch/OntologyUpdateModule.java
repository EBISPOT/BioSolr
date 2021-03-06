/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.co.flax.biosolr.elasticsearch;

import org.elasticsearch.common.inject.AbstractModule;
import uk.co.flax.biosolr.elasticsearch.mapper.ontology.RegisterOntologyType;

/**
 * Module binding the ontology annotator registration class.
 *
 * @author mlp
 */
public class OntologyUpdateModule extends AbstractModule {
	
	@Override
	protected void configure() {
		bind(RegisterOntologyType.class).asEagerSingleton();
	}

}
