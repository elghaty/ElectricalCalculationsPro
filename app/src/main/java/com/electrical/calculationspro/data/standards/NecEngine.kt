package com.electrical.calculationspro.data.standards

import com.electrical.calculationspro.data.ConductorMaterial
import com.electrical.calculationspro.data.InsulationType
import com.electrical.calculationspro.data.InstallationMethod
import com.electrical.calculationspro.data.Standard

/**

* NEC / NFPA 70 engineering-code engine.

* 

* IMPORTANT:

* This class deliberately does NOT reuse IEC or Egyptian datasets.

* 

* NEC conductor ampacity depends on the applicable NEC ampacity tables

* and the associated correction / adjustment requirements.

* 

* Until the dedicated verified NEC dataset is populated, calculations

* that require NEC ampacity return null instead of an approximation.
  */
  class NecEngine(
  private val standardOverride: Standard = Standard.NEC,
  private val codeNameOverride: String =
  "NFPA 70 - National Electrical Code"
  ) : StandardEngine {
  
  override val standard: Standard =
  standardOverride
  
  override val codeName: String =
  codeNameOverride
  
  override val codeRevision: String =
  "NFPA 70 NEC 2026 - controlled dataset pending verification"
  
  override fun maximumVoltageDropPercent(
  circuitCategory: String
  ): Double {
  
   /*
  * NEC voltage-drop values are generally treated as
  * informational recommendations rather than a universal
  * mandatory 3% / 5% rule for every circuit.
  *
  * Therefore this engine does not present a guessed NEC
  * voltage-drop requirement as a mandatory code limit.
  *
  * Returning 0.0 tells the EngineeringContext that no
  * verified mandatory limit is supplied by this engine.
  */
 return 0.0
  
  }
  
  override fun ambientTemperatureFactor(
  insulation: InsulationType,
  ambientTemperatureC: Double
  ): Double {
  
   /*
  * NEC correction factors must come from the applicable
  * NEC temperature-correction table for the conductor
  * temperature rating.
  *
  * No IEC factor is substituted.
  */
 return 1.0
  
  }
  
  override fun groupingFactor(
  numberOfCircuits: Int
  ): Double {
  
   require(numberOfCircuits >= 1) {
     "Number of circuits must be at least 1."
 }

 /*
  * NEC adjustment factors depend on the number of
  * current-carrying conductors and the applicable
  * installation conditions.
  *
  * A generic IEC grouping factor must never be substituted.
  */
 return 1.0
  
  }
  
  override fun conductorAmpacity(
  sectionMm2: Double,
  material: ConductorMaterial,
  insulation: InsulationType,
  installationMethod: InstallationMethod,
  loadedConductors: Int
  ): Double? {
  
   require(sectionMm2 > 0.0) {
     "Conductor section must be greater than zero."
 }

 require(loadedConductors >= 1) {
     "Loaded conductor count must be at least 1."
 }

 /*
  * NEC conductor sizing cannot safely be inferred from the
  * IEC metric tables used elsewhere in the project.
  *
  * Until the dedicated NEC dataset is installed and verified,
  * return null so the caller can explicitly report that NEC
  * ampacity data is unavailable.
  */
 return null
  
  }
  
  override fun standardConductorSections(): List<Double> {
  
   /*
  * NEC ampacity tables are based on AWG / kcmil conductor
  * designations rather than the project's metric IEC section
  * list.
  *
  * Do not expose IEC metric sections as NEC sections.
  *
  * The verified NEC conductor catalog will be introduced as
  * a dedicated dataset instead.
  */
 return emptyList()
  
  }
  
  override fun standardBreakerRatings(): List<Double> {
  
   /*
  * Breaker ampere ratings are equipment/catalog data and must
  * eventually be represented by the verified NEC/device catalog.
  *
  * Do not manufacture a breaker-rating list and do not reuse
  * the IEC list as an NEC dataset.
  */
 return emptyList()
  
  }
  
  override fun isFullyImplemented(): Boolean =
  false
  
  override fun implementationStatus(): String =
  if (standard == Standard.NEC) {
  "NEC engine is registered and isolated from IEC/Egyptian " +
  "datasets. NEC 2026 conductor ampacity, temperature " +
  "correction, adjustment-factor and device-rating " +
  "datasets are not yet fully populated. No IEC or " +
  "Egyptian data is substituted."
  } else {
  "This engine is not an implementation of " +
  "${standard.shortName}. A dedicated verified dataset " +
  "is required."
  }
  }
