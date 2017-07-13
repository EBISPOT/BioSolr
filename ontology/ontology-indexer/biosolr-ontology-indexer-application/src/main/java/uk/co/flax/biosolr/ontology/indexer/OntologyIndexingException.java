package uk.co.flax.biosolr.ontology.indexer;

/**
 * Copyright ${year} EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 * /
 * <p/>
 * <p/>
 * /**
 *
 * @author Simon Jupp
 * @date 23/10/2012
 * Functional Genomics Group EMBL-EBI
 */
public class OntologyIndexingException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public OntologyIndexingException() {
    }

    public OntologyIndexingException(String message) {
        super(message);
    }

    public OntologyIndexingException(String message, Throwable cause) {
        super(message, cause);
    }

    public OntologyIndexingException(Throwable cause) {
        super(cause);
    }
}
