package org.phenoscape.annotations.demo

import scala.collection.JavaConverters._

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.rdf.model.Model
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.sys.JenaSystem
import org.phenoscape.scowl._
import org.semanticweb.elk.owlapi.ElkReasonerFactory
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.model.AxiomType
import org.semanticweb.owlapi.model.IRI
import org.semanticweb.owlapi.model.OWLAxiom
import org.semanticweb.owlapi.model.OWLClass
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom
import org.semanticweb.owlapi.model.OWLNamedIndividual
import org.semanticweb.owlapi.model.OWLOntology
import org.semanticweb.owlapi.model.parameters.Imports

object Main extends App {

  JenaSystem.init()
  val manager = OWLManager.createOWLOntologyManager()
  val factory = manager.getOWLDataFactory
  val uberon = manager.loadOntology(IRI.create("http://purl.obolibrary.org/obo/uberon/ext.owl"))
  val zfa = manager.loadOntology(IRI.create("http://purl.obolibrary.org/obo/zfa.owl"))
  val zfaUberonBridge = manager.loadOntology(IRI.create("http://purl.obolibrary.org/obo/uberon/bridge/uberon-bridge-to-zfa.owl"))
  val zp = manager.loadOntology(IRI.create("http://compbio.charite.de/jenkins/job/zp-owl/lastSuccessfulBuild/artifact/zp.owl"))
  val mp = manager.loadOntology(IRI.create("http://purl.obolibrary.org/obo/mp.owl"))
  val zfinData = RDFDataMgr.loadModel("https://data.monarchinitiative.org/ttl/zfin_slim.ttl")
  val mgiData = RDFDataMgr.loadModel("https://data.monarchinitiative.org/ttl/mgi_slim.ttl")
  val HasPhenotype = ObjectProperty("http://purl.obolibrary.org/obo/RO_0002200")
  val HasPart = ObjectProperty("http://purl.obolibrary.org/obo/BFO_0000051")
  val InheresIn = ObjectProperty("http://purl.obolibrary.org/obo/RO_0000052")
  val Towards = ObjectProperty("http://purl.obolibrary.org/obo/RO_0002503")
  val PhenotypeOf = ObjectProperty("http://example.org/phenotype_of")
  val propertyAxioms = Set(InheresIn SubPropertyOf PhenotypeOf, Towards SubPropertyOf PhenotypeOf)
  val phenotypeAssociationQuery = QueryFactory.create(s"""
    PREFIX oban: <http://purl.org/oban/> 
    SELECT DISTINCT ?gene ?phenotype
    WHERE {
      ?association a oban:association ;
                   oban:association_has_predicate <${HasPhenotype.getIRI}> ;
                   oban:association_has_subject ?gene ;
                   oban:association_has_object ?phenotype .
    }
    """)
  val Dummy = Class("http://example.org/dummy")
  def createAnnotations(data: Model): Set[OWLAxiom] = {
    val qe = QueryExecutionFactory.create(phenotypeAssociationQuery, data)
    val annotations = (for {
      result <- qe.execSelect().asScala
    } yield {
      val gene = Individual(result.getResource("gene").getURI)
      val phenotype = Class(result.getResource("phenotype").getURI)
      gene Type (Dummy and phenotype)
    }).toSet[OWLAxiom]
    qe.close()
    annotations
  }
  val zfinAnnotations = createAnnotations(zfinData)
  val mgiAnnotations = createAnnotations(mgiData)
  def termToDummy(term: OWLClass): OWLClass = Class(term.getIRI.toString + "DUMMY")
  val (mapping, phenotypeAxioms) = (for {
    term <- uberon.getClassesInSignature(Imports.EXCLUDED).asScala
  } yield {
    val dummyTerm = termToDummy(term)
    (dummyTerm -> term, dummyTerm EquivalentTo (Dummy and (HasPart some (PhenotypeOf some term))))
  }).unzip
  val dummiesToTerms = mapping.toMap
  def filterDisjointAxioms(axioms: Set[OWLAxiom]): Set[OWLAxiom] = axioms
    .filterNot { _.getAxiomType == AxiomType.DISJOINT_CLASSES }
    .filterNot {
      case axiom: OWLEquivalentClassesAxiom => axiom.getNamedClasses.contains(factory.getOWLNothing) || axiom.getClassExpressions.contains(factory.getOWLNothing)
      case _                                => false
    }
  val fishAxioms = filterDisjointAxioms(uberon.getAxioms().asScala.toSet ++ zfa.getAxioms().asScala.toSet ++ zfaUberonBridge.getAxioms().asScala.toSet ++ zp.getAxioms().asScala.toSet ++ zfinAnnotations ++ phenotypeAxioms ++ propertyAxioms)
  val mouseAxioms = filterDisjointAxioms(uberon.getAxioms().asScala.toSet ++ mp.getAxioms().asScala.toSet ++ mgiAnnotations ++ phenotypeAxioms ++ propertyAxioms)
  val fishOnt = manager.createOntology(fishAxioms.asJava)
  val mouseOnt = manager.createOntology(mouseAxioms.asJava)
  def liftAnnotations(ont: OWLOntology): Set[(OWLNamedIndividual, OWLClass)] = {
    val reasoner = new ElkReasonerFactory().createReasoner(ont)
    val results = (for {
      ind <- ont.getIndividualsInSignature(Imports.EXCLUDED).asScala
      term <- reasoner.getTypes(ind, true).getFlattened.asScala
      realTerm <- dummiesToTerms.get(term)
    } yield ind -> realTerm).toSet
    reasoner.dispose()
    results
  }
  val fishUberon = liftAnnotations(fishOnt)
  val mouseUberon = liftAnnotations(mouseOnt)
  for {
    (gene, structure) <- fishUberon.toSeq ++ mouseUberon.toSeq
  } println(s"${gene.getIRI}\t${structure.getIRI}")

}
