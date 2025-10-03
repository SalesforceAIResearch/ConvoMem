package com.salesforce.crmmembench.questions.evidence.runners

import com.salesforce.crmmembench.questions.evidence.generators.ImplicitConnectionEvidenceGenerator

/**
 * Production runners for ImplicitConnectionEvidenceGenerator.
 * Creates runners for evidence counts 1-6 as required by the framework.
 */

object GenerateImplicitConnection1Evidence {
  def main(args: Array[String]): Unit = {
    new ImplicitConnectionEvidenceGenerator(1){
      override val targetPeopleCount: Int = 10
    }.generateEvidence()
  }
}

object GenerateImplicitConnection2Evidence {
  def main(args: Array[String]): Unit = {
    new ImplicitConnectionEvidenceGenerator(2).generateEvidence()
  }
}

object GenerateImplicitConnection3Evidence {
  def main(args: Array[String]): Unit = {
    new ImplicitConnectionEvidenceGenerator(3).generateEvidence()
  }
}

object GenerateImplicitConnection4Evidence {
  def main(args: Array[String]): Unit = {
    new ImplicitConnectionEvidenceGenerator(4).generateEvidence()
  }
}

object GenerateImplicitConnection5Evidence {
  def main(args: Array[String]): Unit = {
    new ImplicitConnectionEvidenceGenerator(5).generateEvidence()
  }
}

object GenerateImplicitConnection6Evidence {
  def main(args: Array[String]): Unit = {
    new ImplicitConnectionEvidenceGenerator(6).generateEvidence()
  }
}