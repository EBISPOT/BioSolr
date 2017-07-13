/**
 * Copyright (c) 2014 Lemur Consulting Ltd.
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
package uk.co.flax.biosolr.ontology.api;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A request from the SPARQL search form.
 * 
 * @author Matt Pearce
 */
public class JenaRequest {
	
	@JsonProperty("prefix")
	private final String prefix;
	
	@JsonProperty("query")
	private final String query;
	
	@JsonProperty("rows")
	private final int rows;

	public JenaRequest(@JsonProperty("prefix") String prefix, @JsonProperty("query") String query,
			@JsonProperty("rows") int rows) {
		this.prefix = prefix;
		this.query = query;
		this.rows = rows;
	}

	/**
	 * @return the prefix
	 */
	public String getPrefix() {
		return prefix;
	}

	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * @return the rows
	 */
	public int getRows() {
		return rows;
	}

}
