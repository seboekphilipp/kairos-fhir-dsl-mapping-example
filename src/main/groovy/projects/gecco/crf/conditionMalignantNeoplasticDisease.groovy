package projects.gecco.crf


import de.kairos.fhir.centraxx.metamodel.CatalogEntry
import de.kairos.fhir.centraxx.metamodel.CrfItem
import de.kairos.fhir.centraxx.metamodel.CrfTemplateField
import de.kairos.fhir.centraxx.metamodel.LaborValue

//import javax.xml.catalog.Catalog

import static de.kairos.fhir.centraxx.metamodel.RootEntities.studyVisitItem

/**
 * Represented by a CXX StudyVisitItem
 * Specified by https://simplifier.net/forschungsnetzcovid-19/malignantneoplasticdisease
 * @author Lukas Reinert, Mike Wähnert
 * @since KAIROS-FHIR-DSL.v.1.8.0, CXX.v.3.18.1
 *
 * NOTE: Due to the Cardinality-restraint (1..1) for "code", multiple selections in CXX for this parameter
 *       will be added as additional codings.
 */


condition {
  final def crfName = context.source[studyVisitItem().template().crfTemplate().name()]
  final def studyVisitStatus = context.source[studyVisitItem().status()]
  if (crfName != "ANAMNESE / RISIKOFAKTOREN" || studyVisitStatus == "OPEN") {
    return //no export
  }
  final def crfItemCancer = context.source[studyVisitItem().crf().items()].find {
    "COV_GECCO_TUMORERKRANKUNG" == it[CrfItem.TEMPLATE]?.getAt(CrfTemplateField.LABOR_VALUE)?.getAt(LaborValue.CODE)
  }
  if (crfItemCancer[CrfItem.CATALOG_ENTRY_VALUE] != []) {
    id = "MalignantNeoplasticDisease/" + context.source[studyVisitItem().crf().id()]

    meta {
      profile "https://www.netzwerk-universitaetsmedizin.de/fhir/StructureDefinition/malignant-neoplastic-disease"
    }

    extension {
      url = "https://simplifier.net/forschungsnetzcovid-19/uncertaintyofpresence"
      valueCodeableConcept {
        coding {
          system = "http://snomed.info/sct"
          code = "261665006"
        }
      }
    }
    category {
      coding {
        system = "http://snomed.info/sct"
        code = "394593009"
      }
    }

    subject {
      reference = "Patient/" + context.source[studyVisitItem().studyMember().patientContainer().id()]
    }

    code {
      final def ICDcode = matchResponseToICD(crfItemCancer[CrfItem.CATALOG_ENTRY_VALUE][CatalogEntry.CODE] as String)
      if (ICDcode) {
        coding {
          system = "http://fhir.de/CodeSystem/dimdi/icd-10-gm"
          version = "2020"
          code = ICDcode
        }
      }
      final def SNOMEDcode = matchResponseToSNOMED(crfItemCancer[CrfItem.CATALOG_ENTRY_VALUE][CatalogEntry.CODE] as String)
      if (SNOMEDcode) {
        coding {
          system = "http://snomed.info/sct"
          code = SNOMEDcode
        }
      }
    }

    recordedDate {
      recordedDate = crfItemCancer[CrfItem.CREATIONDATE]
    }
  }
}


static String matchResponseToICD(final String resp) {
  switch (resp) {
    case ("[COV_AKTIV]"):
      return ""
    case ("[COV_REMISSION]"):
      return ""
    case ("[COV_KEINE_ERKRANKUNG]"):
      return ""
    default: null
  }
}

static String matchResponseToSNOMED(final String resp) {
  switch (resp) {
    case ("[COV_AKTIV]"):
      return "363346000"
    case ("[COV_REMISSION]"):
      return "363346000"
    default: null
  }
}