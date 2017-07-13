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
package uk.co.flax.biosolr.ontology;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.co.flax.biosolr.ontology.config.IndexerConfiguration;
import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.config.loaders.ConfigurationLoader;
import uk.co.flax.biosolr.ontology.config.loaders.ConfigurationLoaderFactory;
import uk.co.flax.biosolr.ontology.indexer.OWLOntologyIndexer;
import uk.co.flax.biosolr.ontology.indexer.OntologyIndexer;
import uk.co.flax.biosolr.ontology.indexer.OntologyIndexingException;
import uk.co.flax.biosolr.ontology.indexer.ReasonerFactory;
import uk.co.flax.biosolr.ontology.loaders.BasicOWLOntologyLoader;
import uk.co.flax.biosolr.ontology.loaders.OntologyLoader;
import uk.co.flax.biosolr.ontology.loaders.OntologyLoadingException;
import uk.co.flax.biosolr.ontology.plugins.PluginException;
import uk.co.flax.biosolr.ontology.plugins.PluginManager;
import uk.co.flax.biosolr.ontology.storage.StorageEngine;
import uk.co.flax.biosolr.ontology.storage.StorageEngineException;
import uk.co.flax.biosolr.ontology.storage.StorageManager;

/**
 * Main class for indexing one or more ontologies.
 * 
 * @author Matt Pearce
 */
public class OntologyIndexerApplication {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(OntologyIndexerApplication.class);
	
	private final IndexerConfiguration configuration;
	private final PluginManager pluginManager;
	
	public OntologyIndexerApplication(IndexerConfiguration config) throws Exception {
		this.configuration = config;
		PluginManager.initialisePluginManager(config.getPluginTypes());
		this.pluginManager = PluginManager.getInstance();
	}
	
	public void run() {
		StorageEngine storageEngine = new StorageManager(configuration.getStorage());
		try {
			storageEngine.initialise();
			if (!storageEngine.isReady()) {
				System.err.println("Storage engine is not ready - aborting!");
				return;
			}
		} catch (StorageEngineException e) {
			System.err.println("Could not build storage engine(s): " + e.getMessage());
			e.printStackTrace();
		}
		
		for (String source : configuration.getOntologies().keySet()) {
			try {
				OntologyConfiguration ontologyConfig = configuration.getOntologies().get(source);
				OntologyLoader loader = buildOntologyLoader(ontologyConfig);
				OntologyIndexer indexer = new OWLOntologyIndexer(source, ontologyConfig, storageEngine, pluginManager);
				indexer.indexOntology(loader);
				pluginManager.processOntologyPlugins(loader, source, ontologyConfig);
			} catch (OntologyIndexingException e) {
				LOGGER.error("Caught exception indexing {}: {}", source, e.getMessage());
				LOGGER.error("Exception detail:", e);
			} catch (PluginException e) {
				LOGGER.error("Caught plugin exception indexing {}: {}", source, e.getMessage());
				LOGGER.error("Exception detail:", e);
			}
		}
	}
	
	private OntologyLoader buildOntologyLoader(OntologyConfiguration ontConfig) throws OntologyIndexingException {
		OntologyLoader loader;
		
		try {
			loader = new BasicOWLOntologyLoader(ontConfig, new ReasonerFactory());
			loader.initializeOntology();
		} catch (OntologyLoadingException e) {
			throw new OntologyIndexingException(e);
		}
		
		return loader;
	}
	
	public static void main(String[] args) {
		if (args.length == 0) {
			usage();
			System.exit(1);
		}
		
        // Set property to make sure we can parse all of EFO
        System.setProperty("entityExpansionLimit", "1000000");
        
		ConfigurationLoader configLoader = ConfigurationLoaderFactory.buildConfigurationLoader(args[0]);
		if (configLoader == null) {
			System.err.println("Could not find configuration loader for " + args[0]);
			System.exit(1);
		}
		
		try {
			OntologyIndexerApplication indexer = new OntologyIndexerApplication(configLoader.loadConfiguration());
			indexer.run();
		} catch (IOException e) {
			System.err.println("Could not load configuration file " + args[0] + ": " + e.getMessage());
			System.exit(1);
		} catch (Exception e) {
			System.err.println("Error initialising indexer: " + e.getMessage());
            e.printStackTrace();
			System.exit(1);
		}
	}
	
	private static void usage() {
		System.out.println("Usage:");
		System.out.println("  java uk.co.flax.biosolr.ontology.OntologyIndexer configfile");
	}

}
