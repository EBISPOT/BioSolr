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
package uk.co.flax.biosolr.ontology.config;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Configuration details for the storage engine.
 * 
 * @author Matt Pearce
 */
public class StorageConfiguration {
	
	private List<String> engineTypes;
	
	@JsonProperty("solr")
	private SolrConfiguration solr;
	
	@JsonProperty("other")
	private Map<String, StorageEngineConfiguration> other;
	
	/**
	 * @return the storageType
	 */
	public List<String> getEngineTypes() {
		return engineTypes;
	}

	/**
	 * @param storageType the storageType to set
	 */
	public void setEngineTypes(List<String> storageType) {
		this.engineTypes = storageType;
	}

	/**
	 * @return the solr
	 */
	public SolrConfiguration getSolr() {
		return solr;
	}

	/**
	 * @param solr the solr to set
	 */
	public void setSolr(SolrConfiguration solr) {
		this.solr = solr;
	}

	public Map<String, StorageEngineConfiguration> getAdditionalEngines() {
		return other;
	}

	public void setAdditionalEngines(Map<String, StorageEngineConfiguration> additionalEngines) {
		this.other = additionalEngines;
	}

}
