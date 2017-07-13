package uk.co.flax.biosolr.ontology.loaders;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEntityVisitor;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLNamedObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleIRIMapper;
import org.slf4j.Logger;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxOWLObjectRendererImpl;
import uk.co.flax.biosolr.ontology.config.OntologyConfiguration;
import uk.co.flax.biosolr.ontology.loaders.OntologyLoader;
import uk.co.flax.biosolr.ontology.loaders.OntologyLoadingException;
import uk.co.flax.biosolr.ontology.utils.Namespaces;

/**
 * @author Tony Burdett
 * @author Simon Jupp
 * @date 03/02/2015
 *
 * This Abstract class provies an OWL API based implementation of an ontology loader. Ontologies are loaded
 * and various caches are created for extracting  common slices out of the ontology.
 * This class has being doing the round in various guises for a while now and had become a bit unwieldy
 * todo refactor do include individual processors for various aspects of the ontology we want to extract
 *
 * Samples, Phenotypes and Ontologies Team, EMBL-EBI
 */
public abstract class AbstractOWLOntologyLoader implements OntologyLoader {

    private static final Pattern oboIdFragmentPattern = Pattern.compile("(([A-Za-z_]*)_(\\d+))");
    
    private IRI ontologyIRI;
    private String ontologyName;
    private URI ontologyResource;
    private Map<IRI, IRI> ontologyImportMappings;

    private Collection<IRI> classes  = new HashSet<IRI>();
    private Collection<IRI> individuals  = new HashSet<IRI>();
    private Collection<IRI> objectProperties  = new HashSet<IRI>();
    private Collection<IRI> dataProperties  = new HashSet<IRI>();
    private Collection<IRI> annotationProperties  = new HashSet<IRI>();
    private Collection<IRI> owlVocabulary  = new HashSet<IRI>();

    private IRI labelIRI = Namespaces.RDFS.createIRI("label");
    private Collection<IRI> synonymIRIs = new HashSet<IRI>();
    private Collection<IRI> definitionIRIs  = new HashSet<IRI>();
//    private Collection<IRI> hiddenIRIs  = new HashSet<IRI>();

    private IRI exclusionClassIRI;
    private IRI exclusionAnnotationIRI;

    private OWLOntologyManager manager;
    private OWLDataFactory factory;
    private OWLOntology ontology;

    private Collection<String> baseIRIs = new HashSet<>();

    private Map<IRI, String> ontologyAccessions = new HashMap<>();
    private Map<IRI, String> ontologyLabels = new HashMap<>();
    private Map<IRI, Collection<String>> ontologySynonyms = new HashMap<>();
    private Map<IRI, Collection<String>> ontologyDefinitions = new HashMap<>();
    private Map<IRI, Map<IRI,Collection<String>>> termAnnotations = new HashMap<>();
    private Collection<IRI> obsoleteTerms = new HashSet<>();
    private Map<IRI, Collection<String>> slims = new HashMap<>();
    private Collection<IRI> localTerms = new HashSet<>();
    private Collection<IRI> rootTerms = new HashSet<>();

    private Map<IRI, Collection<IRI>> directParentTerms = new HashMap<>();
    private Map<IRI, Collection<IRI>> allParentTerms = new HashMap<>();
    private Map<IRI, Collection<IRI>> directChildTerms = new HashMap<>();
    private Map<IRI, Collection<IRI>> allChildTerms = new HashMap<>();
    private Map<IRI, Collection<IRI>> equivalentTerms = new HashMap<>();
    private Map<IRI, Map<IRI,Collection<IRI>>> relatedTerms = new HashMap<>();
    private Map<IRI, Map<IRI,Collection<IRI>>> allRelatedTerms = new HashMap<>();

    private ShortFormProvider provider;
    private ManchesterOWLSyntaxOWLObjectRendererImpl manSyntaxRenderer;

    private Map<IRI, Collection<String>> equivalentClassExpressionsAsString = new HashMap<>();
    private Map<IRI, Collection<String>> superclassExpressionsAsString = new HashMap<>();

    public AbstractOWLOntologyLoader(OntologyConfiguration config) throws OntologyLoadingException {
        // read from config
        setOntologyIRI(IRI.create(config.getAccessURI()));
        setOntologyName(config.getNamespace());
        setSynonymIRIs(config.getSynonymAnnotationURI().stream()
                .map(IRI::create)
                .collect(Collectors.toSet()));

//        setHiddenIRIs(
//                config.getHiddenProperties()
//                        .stream()
//                        .map(IRI::create)
//                        .collect(Collectors.toSet()));

        setLabelIRI(IRI.create(config.getLabelURI()));

        setDefinitionIRIs(
                config.getDefinitionAnnotationURI().stream()
                        .map(IRI::create).
                        collect(Collectors.toSet()));
        setBaseIRI(config.getBaseURI());

        try {
            setOntologyResource(new URI(config.getAccessURI()));
        } catch (URISyntaxException e) {
            throw new OntologyLoadingException("URI syntax exception for " + config.getAccessURI(), e);
		}
    }


	@Override
	public Collection<String> getAccessions(IRI ontologyTermIRI) {
		Set<String> accessions = new HashSet<>();
		accessions.add(getOntologyTermAccessions().get(ontologyTermIRI));

		Optional<String> oboId = getOBOid(getOntologyTermAccessions().get(ontologyTermIRI));
		if (oboId.isPresent()) {
			accessions.add(oboId.get());
		}
		return accessions;
	}
	
	@Override
	public void initializeOntology() throws OntologyLoadingException {
		try {
			doInitialization();
		} catch (OWLOntologyCreationException e) {
			throw new OntologyLoadingException(e);
		}
	}
	
    protected void doInitialization() throws OWLOntologyCreationException {
        // init owl fields
        this.manager = OWLManager.createOWLOntologyManager();
        if (getOntologyResource() != null) {
            getLog().info("Mapping ontology IRI from {} to {}", getOntologyIRI(), getOntologyResource());
            this.manager.addIRIMapper(new SimpleIRIMapper(getOntologyIRI(),
                    IRI.create(getOntologyResource())));
        }
        if (getOntologyImportMappings() != null) {
            for (IRI from : getOntologyImportMappings().keySet()) {
                IRI to = getOntologyImportMappings().get(from);
                getLog().info("Mapping imported ontology IRI from " + from + " to " + to);
                this.manager.addIRIMapper(new SimpleIRIMapper(from, to));
            }
        }
        this.factory = manager.getOWLDataFactory();

        // collect things we want to ignore form OWL vocab
        owlVocabulary.add(factory.getOWLThing().getIRI());
        owlVocabulary.add(factory.getOWLNothing().getIRI());
        owlVocabulary.add(factory.getOWLTopObjectProperty().getIRI());
        owlVocabulary.add(factory.getOWLBottomObjectProperty().getIRI());

        // load the ontology
        this.ontology = loadOntology();
    }

    /**
     * Extracts and loads into memory all the class labels and corresponding IRIs.  This class makes the assumption that
     * one primary label per class exists. If any classes contain multiple rdfs:labels, these classes are ignored.
     * <p>
     * Once loaded, this method must set the IRI of the ontology, and should add class labels, class types (however you
     * chose to implement the concept of a "type") and synonyms, where they exist.
     * <p>
     * Implementations do not need to concern themselves with resolving imports or physical/logical mappings as this is
     * done in initialisation at the abstract level.  Subclasses can simply do <code>OWLOntology ontology =
     * getManager().loadOntology(IRI.create(getOntologyURI()));</code> as a basic implementation before populating the
     * various required caches
     */
    protected OWLOntology loadOntology() throws OWLOntologyCreationException {
        try {
            getLog().debug("Loading ontology...");
            this.ontology = getManager().loadOntology(getOntologyIRI());
            IRI ontologyIRI = ontology.getOntologyID().getOntologyIRI();

            if (getOntologyName() == null) {
                String name = getShortForm(ontologyIRI).get();
                if (name == null) {
                    getLog().warn("Can't shorten the name for " + ontologyIRI.toString());
                    name = ontologyIRI.toString();
                }
                setOntologyName(name);
            }
            getLog().debug("Successfully loaded ontology " + ontologyIRI);

            this.provider = new AnnotationValueShortFormProvider(
                    Collections.singletonList(factory.getOWLAnnotationProperty(getLabelIRI())),
                    Collections.<OWLAnnotationProperty, List<String>>emptyMap(),
                    manager);
            this.manSyntaxRenderer = new ManchesterOWLSyntaxOWLObjectRendererImpl();
            manSyntaxRenderer.setShortFormProvider(provider);

            // this call will initialise the reasoner
            getOWLReasoner(ontology);

            // cache all URIs for classes, properties and individuals
            getLog().debug("Computing indexes...");

            Collection<OWLEntity> allEntities = new HashSet<>();
            for (OWLOntology ontology1 : manager.getOntologies()) {
                allEntities.addAll(ontology1.getSignature());
            }
            indexTerms(allEntities);
            return ontology;
        }
        finally {
            getLog().debug("Done loading/indexing");
        }
    }

    protected void indexTerms(Collection<OWLEntity> entities) {

        for (OWLEntity entity: entities) {

            if (entity.getIRI().toString().contains(Namespaces.OWL.toString())) {
                continue;
            }
            // get all the annotation properties
            evaluateAllAnnotationsValues(entity);

            // add the class accession for this entity
            Optional<String> shortForm = getShortForm(entity.getIRI());
            if (shortForm.isPresent()) {
                addClassAccession(entity.getIRI(), shortForm.get());
                // of no label, create one form shortform
                if (ontologyLabels.get(entity.getIRI()) == null) {
                    addClassLabel(entity.getIRI(), shortForm.get() );

                }

            }
            // find out if this term is local to the ontology based on the base URIs
			for (String base : getBaseIRI()) {
				if (entity.getIRI().toString().startsWith(base)) {
					addLocalTerms(entity.getIRI());
				}
			}
            
            // index the different types of entity
            entity.accept(new OWLEntityVisitor() {
                @Override
                public void visit(OWLClass cls) {
                    try {
                        classes.add(cls.getIRI());
                        indexSubclassRelations(cls);
                        indexEquivalentRelations(cls);
                    } catch (OWLOntologyCreationException e) {
                        getLog().error("unable to index classes, unable to create reasoner");
                    }

                }

                @Override
                public void visit(OWLObjectProperty property) {
                    objectProperties.add(property.getIRI());
                    try {
                        indexSubPropertyRelations(property);
                    } catch (OWLOntologyCreationException e) {
                        getLog().error("unable to index properties, unable to create reasoner");
                    }
                }

                @Override
                public void visit(OWLDataProperty property) {
                    dataProperties.add(property.getIRI());
                }

                @Override
                public void visit(OWLNamedIndividual individual) {
                    individuals.add(individual.getIRI());
                }

                @Override
                public void visit(OWLDatatype datatype) {
                    //ignore datatypes
                }

                @Override
                public void visit(OWLAnnotationProperty property) {
                    annotationProperties.add(property.getIRI());
                }
            });
        }
    }


    private void indexSubPropertyRelations(OWLObjectProperty property) throws OWLOntologyCreationException {

        // todo this didn't work with Elk, need to think about it
//        OWLReasoner reasoner = getOWLReasoner(ontology);

//        Set<IRI> superProperties = new HashSet<>();
//        // get direct children
//        for (OWLObjectPropertyExpression exp : reasoner.getSubperObjectProperties(property, true).getFlattened()) {
//            if (!exp.isAnonymous()) {
//                superProperties.add(exp.asOWLObjectProperty().getIRI());
//            }
//        }
//        addDirectParents(property.getIRI(), superProperties);
    }

    protected  void indexSubclassRelations(OWLClass owlClass) throws OWLOntologyCreationException {

        OWLReasoner reasoner = getOWLReasoner(ontology);

        // use reasoner to check if root
        if (reasoner.getSubClasses(getFactory().getOWLThing(), true).getFlattened().contains(owlClass)) {
            addRootsTerms(owlClass.getIRI());
        }

        if (reasoner.getSubClasses(getFactory().getOWLClass(Namespaces.OBOINOWL.createIRI("ObsoleteClass")), false).getFlattened().contains(owlClass)) {
            addObsoleteTerms(owlClass.getIRI());
        }

        // get direct children

        Set<IRI> ct = removeExcludedIRI(
                reasoner.getSubClasses(owlClass, true).getFlattened().stream()
                        .map(OWLNamedObject::getIRI)
                        .collect(Collectors.toSet()),
                        owlVocabulary);
        if (ct.size() >0) addDirectChildren(owlClass.getIRI(), ct) ;

        // get all children
        Set<IRI> act = removeExcludedIRI(
                reasoner.getSubClasses(owlClass, false).getFlattened().stream()
                        .map(OWLNamedObject::getIRI)
                        .collect(Collectors.toSet()),
                owlVocabulary);
        if (act.size() >0) addAllChildren(owlClass.getIRI(), act);

        // get parents
        Set<IRI> dp =
                removeExcludedIRI(
                        reasoner.getSuperClasses(owlClass, true).getFlattened().stream()
                                .map(OWLNamedObject::getIRI)
                                .collect(Collectors.toSet()),
                        owlVocabulary);
        if (dp.size()>0) addDirectParents(owlClass.getIRI(), dp);

        // get all parents
        Set<IRI> ap =
                removeExcludedIRI(
                        reasoner.getSuperClasses(owlClass, false).getFlattened().stream()
                                .map(OWLNamedObject::getIRI)
                                .collect(Collectors.toSet()),
                        owlVocabulary);
        if (ap.size()>0) addAllParents(owlClass.getIRI(), ap);


        // find direct related terms
        Map<IRI, Collection<IRI>> relatedTerms = new HashMap<>();
        Set<String> relatedDescriptions = new HashSet<>();
        for (OWLClassExpression expression : owlClass.getSuperClasses(getManager().getOntologies())) {
            // only want existential with named class as filler
            if (expression.isAnonymous()) {

                if (expression instanceof OWLObjectSomeValuesFrom) {

                    OWLObjectSomeValuesFrom someValuesFrom = (OWLObjectSomeValuesFrom) expression;

                    if (!someValuesFrom.getFiller().isAnonymous()) {
                        IRI propertyIRI = someValuesFrom.getProperty().asOWLObjectProperty().getIRI();
                        IRI relatedTerm = someValuesFrom.getFiller().asOWLClass().getIRI();
                        if (!relatedTerms.containsKey(propertyIRI)) {
                            relatedTerms.put(propertyIRI, new HashSet<>());
                        }
                        relatedTerms.get(propertyIRI).add(relatedTerm);
                    }
                }

                // store stringified form of class description
                relatedDescriptions.add(manSyntaxRenderer.render(expression));
            }
        }
        if (!relatedTerms.isEmpty()) {
            addRelatedTerms(owlClass.getIRI(), relatedTerms );
        }

        if (!relatedDescriptions.isEmpty()) {

            addSuperClassDescriptions(owlClass.getIRI(), relatedDescriptions);

        }
        // todo find transitive closure of related terms
    }

    private void indexEquivalentRelations(OWLClass owlClass) throws OWLOntologyCreationException {
        OWLReasoner reasoner = getOWLReasoner(ontology);

        // get direct children
        addEquivalentTerms(owlClass.getIRI(),
                reasoner.getEquivalentClasses(owlClass).getEntities().stream()
                        .map(OWLNamedObject::getIRI)
                        .collect(Collectors.toSet()));

        Set<String> relatedDescriptions = new HashSet<>();

        for (OWLClassExpression expression : owlClass.getEquivalentClasses(getManager().getOntologies())) {
            if (expression.isAnonymous()) {
                relatedDescriptions.add(manSyntaxRenderer.render(expression));
            }
        }

        if (!relatedDescriptions.isEmpty()) {
            addEquivalentClassDescriptions(owlClass.getIRI(), relatedDescriptions);
        }

    }


    protected Optional<String> getShortForm(IRI entityIRI) {

        getLog().trace("Attempting to extract fragment name of URI '" + entityIRI + "'");
        String termURI = entityIRI.toString();
        URI entUri = entityIRI.toURI();

        // we want the "final part" of the URI...
        if (!StringUtils.isEmpty(entUri.getFragment())) {
            // a uri with a non-null fragment, so use this...
            getLog().trace("Extracting fragment name using URI fragment (" + entUri.getFragment() + ")");
            return Optional.of(entUri.getFragment());
        }
        else if (entityIRI.toURI().getPath() != null) {
            // no fragment, but there is a path so try and extract the final part...
            if (entityIRI.toURI().getPath().contains("/")) {
                getLog().trace("Extracting fragment name using final part of the path of the URI");
                return Optional.of(entityIRI.toURI().getPath().substring(entityIRI.toURI().getPath().lastIndexOf('/') + 1));
            }
            else {
                // no final path part, so just return whole path
                getLog().trace("Extracting fragment name using the path of the URI");
                return Optional.of(entityIRI.toURI().getPath());
            }
        }
        else {
            // no fragment, path is null, we've run out of rules so don't shorten
            getLog().trace("No rules to shorten this URI could be found (" + termURI + ")");
            return Optional.empty();
        }
    }


	private Optional<String> getOBOid(String fragment) {
        Matcher matcher = oboIdFragmentPattern.matcher(fragment);
        if (matcher.find()) {
            String newId = matcher.group(2) + ":" + matcher.group(3);
            return Optional.of(newId);
        }
        return Optional.empty();
    }


    protected Optional<String> evaluateLabelAnnotationValue(OWLEntity entity, OWLAnnotationValue value) {
        // get label annotations
        Optional<String> label = getOWLAnnotationValueAsString(value);
        if (!label.isPresent()) {
            // try and get the URI fragment and use that as label
            Optional<String> fragment = getShortForm(entity.getIRI());
            if (fragment.isPresent()) {
                return Optional.of(fragment.get());
            }
            else {
                getLog().warn("OWLEntity " + entity + " contains no label. " +
                        "No labels for this class will be loaded.");
                return  Optional.of(entity.toStringID());
            }
        }
        return label;
    }


    protected void evaluateAllAnnotationsValues(OWLEntity owlEntity) {

        IRI owlEntityIRI = owlEntity.getIRI();
        Set<String> synonyms = new HashSet<>();
        Set<String> definitions = new HashSet<>();
        Set<String> slims = new HashSet<>();

        // loop through other annotations in the imports closure
        for (OWLOntology ontology1 : getManager().getOntologies()) {
            for (OWLAnnotation annotation : owlEntity.getAnnotations(ontology1)) {
                OWLAnnotationProperty property = annotation.getProperty();
                IRI propertyIRI = property.getIRI();

                if (getLabelIRI().equals(propertyIRI)) {
                    addClassLabel(owlEntityIRI, evaluateLabelAnnotationValue(owlEntity, annotation.getValue()).get());
                }
                else if (getSynonymIRIs().contains(propertyIRI)) {
                    synonyms.add(getOWLAnnotationValueAsString(annotation.getValue()).get());
                }
                else if (getDefinitionIRIs().contains(propertyIRI)) {
                    definitions.add(getOWLAnnotationValueAsString(annotation.getValue()).get());
                }
                else if (propertyIRI.equals(Namespaces.OBOINOWL.createIRI("subset_property"))) {
                    slims.add(getOWLAnnotationValueAsString(annotation.getValue()).get());
                }
                else if (propertyIRI.equals(Namespaces.OWL.createIRI("deprecated"))) {
                    addObsoleteTerms(owlEntityIRI);
                }
                else {
                    if (getOWLAnnotationValueAsString(annotation.getValue()).isPresent()) {
                        // initialise maps if first time
                        if (!termAnnotations.containsKey(owlEntityIRI)) {
                            HashMap<IRI, Collection<String>> newMap = new HashMap<>();
                            newMap.put(propertyIRI, new HashSet<>());
                            termAnnotations.put(owlEntityIRI, newMap);
                        }

                        if (!termAnnotations.get(owlEntityIRI).containsKey(propertyIRI)) {
                            termAnnotations.get(owlEntityIRI).put(propertyIRI, new HashSet<>());
                        }

                        termAnnotations.get(owlEntityIRI).get(propertyIRI).add(getOWLAnnotationValueAsString(annotation.getValue()).get());
                    }
                }

            }
        }

        if (synonyms.size() > 0) {
            addSynonyms(owlEntityIRI, synonyms);
        }
        if (definitions.size() >0) {
            addDefinitions(owlEntityIRI, definitions);
        }
        if (slims.size() >0) {
            addSlims(owlEntityIRI, slims);
        }
    }


    private Optional<String> getOWLAnnotationValueAsString (OWLAnnotationValue value) {

        if (value instanceof IRI) {
            Optional<String> shortForm= getShortForm((IRI) value);
            if ( shortForm.isPresent()) {
                return Optional.of(shortForm.get());
            }
        }
        else if (value instanceof OWLLiteral) {
            return Optional.of(((OWLLiteral) value).getLiteral());
        }
        return Optional.empty();

    }

    protected abstract OWLReasoner getOWLReasoner(OWLOntology ontology) throws OWLOntologyCreationException;

    // bunch of getters and setters

    protected void addDirectParents(IRI termIRI, Set<IRI> parents) {
        this.directParentTerms.put(termIRI, parents);
    }
    protected void addAllParents(IRI termIRI, Set<IRI> allParents) {
        this.allParentTerms.put(termIRI, allParents);
    }
    protected void addDirectChildren(IRI termIRI, Set<IRI> children) {
        this.directChildTerms.put(termIRI, children);
    }
    protected void addAllChildren(IRI termIRI, Set<IRI> allChildren) {
        this.allChildTerms.put(termIRI, allChildren);
    }
    protected void addEquivalentTerms(IRI termIRI, Set<IRI> equivalent) {
        this.equivalentTerms.put(termIRI, equivalent);
    }
    protected void addLocalTerms(IRI termIRI) {
        this.localTerms.add(termIRI);
    }

    protected void addRootsTerms(IRI termIRI) {
        this.rootTerms.add(termIRI);
    }
    protected void addObsoleteTerms(IRI termIRI) {
        this.obsoleteTerms.add(termIRI);
    }
    protected void addRelatedTerms(IRI termIRI, Map<IRI, Collection<IRI>> relatedTerms) {
        this.relatedTerms.put(termIRI, relatedTerms);
    }
    protected void addAllRelatedTerms(IRI termIRI, Map<IRI, Collection<IRI>> relatedTerms) {
        this.allRelatedTerms.put(termIRI, relatedTerms);
    }
    protected void addSuperClassDescriptions(IRI termIRI, Set<String> relatedSuperDescriptions) {
        this.superclassExpressionsAsString.put(termIRI, relatedSuperDescriptions);
    }
    protected void addEquivalentClassDescriptions(IRI termIRI, Set<String> relatedEquivalentDescriptions) {
        this.equivalentClassExpressionsAsString.put(termIRI, relatedEquivalentDescriptions);
    }
    public Collection<String> getBaseIRI() {
        return baseIRIs;
    }

    @Override
	public Map<IRI, Collection<IRI>> getAllRelatedTerms(IRI entityIRI) {
        return allRelatedTerms.get(entityIRI);
    }

    @Override
	public Map<IRI, Collection<IRI>> getRelatedTerms(IRI entityIRI) {
        if (relatedTerms.containsKey(entityIRI)) {
            return relatedTerms.get(entityIRI);
        }
        return Collections.emptyMap();
    }
    @Override
    public Map<IRI, Collection<IRI>> getDirectParentTerms() {
        return directParentTerms;
    }

    @Override
    public Map<IRI, Collection<IRI>> getAllParentTerms() {
        return allParentTerms;
    }

    @Override
    public Map<IRI, Collection<IRI>> getDirectChildTerms() {
        return directChildTerms;
    }

    @Override
    public Map<IRI, Collection<IRI>> getAllChildTerms() {
        return allChildTerms;
    }

    @Override
    public Map<IRI, Collection<String>> getLogicalSuperClassDescriptions() {
        return this.superclassExpressionsAsString;
    }

    @Override
    public Map<IRI, Collection<String>> getLogicalEquivalentClassDescriptions() {
        return this.equivalentClassExpressionsAsString;
    }

    @Override
    public Map<IRI, Collection<IRI>> getEquivalentTerms() {
        return equivalentTerms;
    }

    @Override
    public boolean isObsoleteTerm(IRI entityIRI) {
        return this.obsoleteTerms.contains(entityIRI);
    }

    @Override
    public boolean isLocalTerm(IRI entityIRI) {
        return this.localTerms.contains(entityIRI);
    }
    
    public void setBaseIRI(Collection<String> baseIRIs) {
        this.baseIRIs = baseIRIs;
    }

    @Override
    public Collection<IRI> getAllClasses() {
        return classes;
    }

    @Override
    public Collection<IRI> getAllObjectPropertyIRIs() {
        return objectProperties;
    }

    @Override
    public Collection<IRI> getAllDataPropertyIRIs() {
        return dataProperties;
    }

    @Override
    public Collection<IRI> getAllIndividualIRIs() {
        return individuals;
    }

    @Override
    public Collection<IRI> getAllAnnotationPropertyIRIs() {
        return annotationProperties;
    }

    @Override
	public void setOntologyResource(URI ontologyResource) {
        this.ontologyResource = ontologyResource;
    }

    @Override
	public void setOntologyImportMappings(Map<IRI, IRI> ontologyImportMappings) {
        this.ontologyImportMappings = ontologyImportMappings;
    }

    public void setSynonymIRIs(Collection<IRI> synonymIRI) {
        this.synonymIRIs = synonymIRI;
    }

    public void setLabelIRI(IRI labelIRI) {
        this.labelIRI = labelIRI;
    }

    public IRI getLabelIRI() {
        return labelIRI;
    }

    @Override
	public Collection<IRI> getDefinitionIRIs() {
        return definitionIRIs;
    }

    public void setExclusionClassIRI(IRI exclusionClassIRI) {
        this.exclusionClassIRI = exclusionClassIRI;
    }

    public void setExclusionAnnotationIRI(IRI exclusionAnnotationIRI) {
        this.exclusionAnnotationIRI = exclusionAnnotationIRI;
    }

    public void setOntologyName(String ontologyName) {
        this.ontologyName = ontologyName;
    }

    /**
     * Returns the short name of the ontology
     *
     * @return the short name of the ontology
     */
    @Override
	public String getOntologyName() {
        return ontologyName;
    }

    /**
     * Returns the location from which the ontology (specified by the <code>ontologyIRI</code> property) will be loaded
     * from
     *
     * @return a spring Resource representing this ontology
     */
    public URI getOntologyResource() {
        return ontologyResource;
    }

    /**
     * Returns a series of mappings between two IRIs to describe where to load any imported ontologies, declared by the
     * ontology being loaded, should be acquired from.  In the returned map, each key is a logical name of an imported
     * ontology, and each value is the physical location the ontology should be loaded from.  In other words, if this
     * <code>OntologyLoader</code> loads an ontology <code>http://www.test.com/ontology_A</code> and ontology A
     * declares
     * <pre><owl:imports rdf:resource="http://www.test.com/ontology_B" /></pre>, if no import mappings are set then
     * ontology B will be loaded from <code>http://www.test.com/ontology_B</code>.  Declaring a mapping
     * {http://www.test.com/ontology_B, file://tmp/ontologyB.owl}, though, will cause ontology B to be loaded from a
     * local copy of the file.
     *
     * @return the ontology import mappings, logical IRI -> physical location IRI
     */
    public Map<IRI, IRI> getOntologyImportMappings() {
        return ontologyImportMappings;
    }

    /**
     * Gets the IRI used to denote synonym annotations in this ontology.  As there is no convention for this (i.e. no
     * rdfs:synonym), ontologies tend to define their own.
     *
     * @return the synonym annotation IRI
     */
    public Collection<IRI> getSynonymIRIs() {
        return synonymIRIs;
    }

    /**
     * Gets the IRI used to denote definition annotations in this ontology.  As there is no convention for this (i.e. no
     * rdfs:definition), ontologies tend to define their own.
     *
     * @return the definition annotation IRI
     */
    public void setDefinitionIRIs(Collection<IRI> definitionIRIs) {
        this.definitionIRIs = definitionIRIs;
    }

//    public void setHiddenIRIs(Collection<IRI> hiddenIRIs) {
//        this.hiddenIRIs = hiddenIRIs;
//    }

    /**
     * Gets the IRI used to denote a class which represents the superclass of all classes to exclude in this ontology.
     * When this ontology is loaded, all subclasses of the class with this IRI will be excluded.  This is to support the
     * case where an ontology has declared an "Obsolete" class and favours moving classes under this heirarchy as
     * opposed to deleting classes.
     *
     * @return the IRI representing the class in the hierarchy that denotes classes to exclude during loading
     */
    public IRI getExclusionClassIRI() {
        return exclusionClassIRI;
    }

    /**
     * Gets the IRI of an annotation property that is used to exclude classes during loading.  This is to support the
     * case where an ontology used an annotation property to act as a flag indicating that classes should not be shown
     * or else are deprecated.  Any classes with an annotation with this IRI will be excluded from loading
     *
     * @return the IRI representing the annotation that denotes an exclusion flag
     */
    public IRI getExclusionAnnotationIRI() {
        return exclusionAnnotationIRI;
    }

    public OWLOntologyManager getManager() {
        return manager;
    }

    public OWLDataFactory getFactory() {
        return factory;
    }

    @Override public IRI getOntologyIRI() {
        return ontologyIRI;
    }

    public OWLOntology getOntology() {
        return ontology;
    }

    public Map<IRI, String> getOntologyTermAccessions() {
        return ontologyAccessions;
    }

	@Override
	public Map<IRI, String> getTermLabels() {
		return ontologyLabels;
	}

	@Override
	public Map<IRI, Collection<String>> getTermSynonyms() {
		return ontologySynonyms;
	}

	@Override
	public Map<IRI, Collection<String>> getTermDefinitions() {
		return ontologyDefinitions;
	}

    @Override
    public Map<IRI, Collection<String>> getAnnotations(IRI entityIRI) {
        if (termAnnotations.containsKey(entityIRI)) {
            return termAnnotations.get(entityIRI);
        }
        return Collections.emptyMap();
    }

    public void setOntologyIRI(IRI ontologyIRI) {
        this.ontologyIRI = ontologyIRI;
    }

    protected void addClassAccession(IRI clsIri, String accession) {
        this.ontologyAccessions.put(clsIri, accession);
    }

    protected void addClassLabel(IRI clsIri, String label) {
        this.ontologyLabels.put(clsIri, label);
    }

    protected void addSynonyms(IRI clsIri, Set<String> synonyms) {
        this.ontologySynonyms.put(clsIri, synonyms);
    }

    protected void addDefinitions(IRI clsIri, Set<String> definitions) {
        this.ontologyDefinitions.put(clsIri, definitions);
    }

    protected void addSlims(IRI clsIri, Set<String> slims) {
        this.slims.put(clsIri, slims);
    }

    @Override public Collection<String> getSubsets(IRI termIri) {
        if (this.slims.containsKey(termIri)) {
            return slims.get(termIri);
        }
        return Collections.emptySet();
    }

    protected Set<IRI> removeExcludedIRI(
            Set<IRI> allIris,
            Collection<IRI> iris) {
        iris.forEach(allIris::remove);
        // and return
        return allIris;
    }

	protected abstract Logger getLog();

}
