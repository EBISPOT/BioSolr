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
package uk.co.flax.biosolr.solr.ontology;

import javax.validation.constraints.NotNull;
import java.util.Collection;
import java.util.Map;

/**
 * Interface defining ontology helper functionality.
 *
 * Created by mlp on 20/10/15.
 */
public interface OntologyHelper {

    /**
     * Update the last time this helper was called. This method
     * should be called every time the helper is used.
     */
    void updateLastCallTime();

    /**
     * Return the last time this helper was called.
     */
    long getLastCallTime();

    /**
     * Explicitly dispose of the helper class, closing down any resources in
     * use.
     */
    void dispose();

	/**
	 * Check whether the IRI exists in the ontology (or ontologies) represented
	 * by this helper.
	 * @param iri
	 * @return <code>true</code> if the class corresponding to this IRI can be found,
	 * <code>false</code> if not.
	 */
	boolean isIriInOntology(String iri);

    /**
     * Find the labels for a single OWL class.
     *
     * @param iri the IRI of the class whose labels are required.
     * @return a collection of labels for the class. Never <code>null</code>.
     */
    Collection<String> findLabels(String iri);

    /**
     * Find all of the labels for a collection of OWL class IRIs.
     *
     * @param iris the IRIs whose labels should be looked up.
     * @return a collection of labels. Never <code>null</code>.
     */
    Collection<String> findLabelsForIRIs(Collection<String> iris);

    /**
     * Find the synonyms for a class.
     *
     * @param iri the IRI of the class whose synonyms are required.
     * @return the collection of synonyms. Never <code>null</code>.
     */
    Collection<String> findSynonyms(String iri);

    /**
     * Find all of the definitions for a class.
     *
     * @param iri the IRI of the class whose definitions are required.
     * @return the definitions. Never <code>null</code>.
     */
    Collection<String> findDefinitions(String iri);

    /**
     * Get the direct child IRIs for a class.
     *
     * @param iri
     * @return the child IRIs, as strings. Never <code>null</code>.
     */
    Collection<String> getChildIris(@NotNull String iri);

    /**
     * Get all descendant IRIs for a class, including direct children.
     *
     * @param iri
     * @return the descendant IRIs, as strings. Never <code>null</code>.
     */
    Collection<String> getDescendantIris(String iri);

    /**
     * Get the direct parent IRIs for a class.
     *
     * @param iri
     * @return the parent IRIs, as strings. Never <code>null</code>.
     */
    Collection<String> getParentIris(String iri);

    /**
     * Get all ancestor IRIs for a class, including direct children.
     *
     * @param iri
     * @return the ancestor IRIs, as strings. Never <code>null</code>.
     */
    Collection<String> getAncestorIris(String iri);

    /**
     * Retrieve a map of related classes for a particular class.
     *
     * @param iri
     * @return a map of relation type to a list of IRIs for nodes with that
     * relationship.
     */
    Map<String, Collection<String>> getRelations(String iri);

}
