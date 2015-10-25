/**
 * Copyright (c) 2015 Lemur Consulting Ltd.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.flax.biosolr.solr.ontology.ols;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import uk.co.flax.biosolr.solr.ontology.OntologyHelper;
import uk.co.flax.biosolr.solr.ontology.OntologyHelperFactoryTest;

import java.io.File;
import static org.junit.Assert.*;

/**
 * Unit tests for the OntologyTerms object.
 *
 * Created by mlp on 21/10/15.
 * @author mlp
 */
public class OntologyTermsTest {

	static final String TERMS_FILE = "ols/ols_terms.json";

	@Test
	public void deserialize_fromFile() throws Exception {
		ObjectMapper mapper = new ObjectMapper();
		mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
		OntologyTerms terms = mapper.readValue(
				new File(OntologyHelperFactoryTest.getFilePath(TERMS_FILE)),
				OntologyTerms.class);
		assertNotNull(terms);
		assertNotNull(terms.getIri());
		assertNotNull(terms.getLinks());
		assertNotNull(terms.getLinks().get(LinkType.SELF));
	}

}
