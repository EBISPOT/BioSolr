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

package uk.co.flax.biosolr;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.PluginInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager class for facet tree parameters.
 *
 * @author mlp
 */
public class FacetTreeParameters {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(FacetTreeParameters.class);
	
	public static final String LOCAL_PARAM_TYPE = "ftree";
	public static final String CHILD_FIELD_PARAM = "childField";
	public static final String PARENT_FIELD_PARAM = "parentField";
	public static final String COLLECTION_PARAM = "collection";
	public static final String NODE_FIELD_PARAM = "nodeField";
	public static final String LABEL_FIELD_PARAM = "labelField";
	public static final String LEVELS_PARAM = "levels";
	public static final String STRATEGY_PARAM = "strategy";
	public static final String PRUNE_PARAM = "prune";
	public static final String DATAPOINTS_PARAM = "datapoints";
	public static final String DATAPOINTS_MORELABEL_PARAM = "datapoints.moreLabel";
	
	private final Map<String, String> defaults = new LinkedHashMap<>();

	/**
	 * Construct the tree parameters from the arguments passed to the
	 * component.
	 * @param args the arguments passed to the component.
	 */
	public FacetTreeParameters(NamedList<? extends Object> args) {
		// Find "defaults" list
		@SuppressWarnings({ "unchecked", "rawtypes" })
		NamedList<? extends Object> defaultArgs = (NamedList) args.get(PluginInfo.DEFAULTS);
		
		if (defaultArgs != null) {
			// Assume all of the args are single strings for now
			defaultArgs.forEach(entry -> {
				defaults.put(entry.getKey(), (String)entry.getValue());
			});
		}
	}
	
	public String getDefault(String name) {
		return getDefault(name, null);
	}
	
	public String getDefault(String name, String defaultVal) {
		if (defaults.containsKey(name)) {
			return defaults.get(name);
		}
		return defaultVal;
	}
	
	public int getIntDefault(String name) {
		int ret = 0;
		if (defaults.containsKey(name)) {
			try {
				ret = Integer.valueOf(defaults.get(name));
			} catch (NumberFormatException nfe) {
				LOGGER.error("{} is not an integer argument: {}", name, nfe.getMessage());
			}
		}
		return ret;
	}
	
}
