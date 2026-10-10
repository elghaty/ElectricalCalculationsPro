package com.electrical.calculationspro.data.catalog

/**

* Code/catalog compliance evaluation.

* 

* Final compliance requires:

* - Valid engineering requirements.

* - Sufficient equipment ratings.

* - Exact verified commercial product data.

* - Official manufacturer source information.

* 

* A preliminary candidate must never be reported as compliant.
  */
  object CodeCompliance {
  
  data class Result(
  val compliant: Boolean,
  val standard: String,
  val product: String?,
  val reasons: List<String>
  )
  
  fun evaluate(
  requirement: TechnicalRequirement.Cable,
  item: CableCatalogItem
  ): Result {
  val reasons = mutableListOf<String>()
  
   validateVerifiedProductSource(
     source = item.source,
     itemManufacturer = item.manufacturer,
     reasons = reasons
 )

 if (!requirement.requiredSectionMm2.isFinite() ||
     requirement.requiredSectionMm2 <= 0.0
 ) {
     reasons += "Required cable section must be finite and greater than zero."
 }

 if (requirement.requiredVoltageV <= 0) {
     reasons += "Required cable voltage must be greater than zero."
 }

 if (requirement.cores <= 0) {
     reasons += "Required cable core count must be greater than zero."
 }

 if (!item.sectionMm2.isFinite() || item.sectionMm2 <= 0.0) {
     reasons += "Catalog cable section is invalid."
 } else if (item.sectionMm2 < requirement.requiredSectionMm2) {
     reasons += "Cable section is below the calculated required section."
 }

 if (item.voltageClassV <= 0 ||
     item.voltageClassV < requirement.requiredVoltageV
 ) {
     reasons += "Cable voltage class is invalid or below the required system voltage."
 }

 if (item.cores != requirement.cores) {
     reasons += "Cable core count does not match the engineering requirement."
 }

 if (!item.conductorMaterial.equals(
         requirement.material.name,
         ignoreCase = true
     )
 ) {
     reasons += "Cable conductor material does not match the engineering requirement."
 }

 if (!item.insulation.equals(
         requirement.insulation.name,
         ignoreCase = true
     )
 ) {
     reasons += "Cable insulation does not match the engineering requirement."
 }

 return result(requirement.standard.name, item.model, reasons)
  
  }
  
  fun evaluate(
  requirement: TechnicalRequirement.Breaker,
  item: BreakerCatalogItem
  ): Result {
  val reasons = mutableListOf<String>()
  
   validateVerifiedProductSource(
     source = item.source,
     itemManufacturer = item.manufacturer,
     reasons = reasons
 )

 if (!requirement.requiredCurrentA.isFinite() ||
     requirement.requiredCurrentA <= 0.0
 ) {
     reasons += "Required breaker current must be finite and greater than zero."
 }

 if (!requirement.requiredVoltageV.isFinite() ||
     requirement.requiredVoltageV <= 0.0
 ) {
     reasons += "Required breaker voltage must be finite and greater than zero."
 }

 if (requirement.poles <= 0) {
     reasons += "Required breaker pole count must be greater than zero."
 }

 if (!requirement.requiredBreakingCapacityKA.isFinite() ||
     requirement.requiredBreakingCapacityKA <= 0.0
 ) {
     reasons += "A positive verified prospective fault-current requirement is mandatory for final breaker compliance."
 }

 if (!item.ratedCurrentA.isFinite() ||
     item.ratedCurrentA <= 0.0
 ) {
     reasons += "Catalog breaker rated current is invalid."
 } else if (item.ratedCurrentA < requirement.requiredCurrentA) {
     reasons += "Breaker rated current is below the calculated design current."
 }

 if (!item.voltageV.isFinite() ||
     item.voltageV <= 0.0 ||
     item.voltageV < requirement.requiredVoltageV
 ) {
     reasons += "Breaker rated voltage is invalid or below the required system voltage."
 }

 if (item.poles != requirement.poles) {
     reasons += "Breaker pole count does not match the engineering requirement."
 }

 val breakingCapacity = item.breakingCapacityKA

 if (breakingCapacity == null ||
     !breakingCapacity.isFinite() ||
     breakingCapacity <= 0.0
 ) {
     reasons += "Verified positive breaker breaking-capacity data is unavailable."
 } else if (
     requirement.requiredBreakingCapacityKA.isFinite() &&
     requirement.requiredBreakingCapacityKA > 0.0 &&
     breakingCapacity < requirement.requiredBreakingCapacityKA
 ) {
     reasons += "Breaker breaking capacity is below the calculated prospective fault current."
 }

 return result(requirement.standard.name, item.model, reasons)
  
  }
  
  fun evaluate(
  requirement: TechnicalRequirement.Transformer,
  item: TransformerCatalogItem
  ): Result {
  val reasons = mutableListOf<String>()
  
   validateVerifiedProductSource(
     source = item.source,
     itemManufacturer = item.manufacturer,
     reasons = reasons
 )

 if (!requirement.requiredKva.isFinite() ||
     requirement.requiredKva <= 0.0
 ) {
     reasons += "Required transformer rating must be finite and greater than zero."
 }

 if (!requirement.primaryVoltageV.isFinite() ||
     requirement.primaryVoltageV <= 0.0
 ) {
     reasons += "Required transformer primary voltage is invalid."
 }

 if (!requirement.secondaryVoltageV.isFinite() ||
     requirement.secondaryVoltageV <= 0.0
 ) {
     reasons += "Required transformer secondary voltage is invalid."
 }

 if (!requirement.frequencyHz.isFinite() ||
     requirement.frequencyHz <= 0.0
 ) {
     reasons += "Required transformer frequency is invalid."
 }

 if (!item.ratedPowerKva.isFinite() ||
     item.ratedPowerKva <= 0.0
 ) {
     reasons += "Catalog transformer rating is invalid."
 } else if (item.ratedPowerKva < requirement.requiredKva) {
     reasons += "Transformer rated power is below the calculated requirement."
 }

 if (!sameRating(item.primaryVoltageV, requirement.primaryVoltageV)) {
     reasons += "Transformer primary voltage does not match the engineering requirement."
 }

 if (!sameRating(item.secondaryVoltageV, requirement.secondaryVoltageV)) {
     reasons += "Transformer secondary voltage does not match the engineering requirement."
 }

 if (!sameRating(item.frequencyHz, requirement.frequencyHz)) {
     reasons += "Transformer frequency does not match the engineering requirement."
 }

 val impedance = item.impedancePercent
 if (impedance == null ||
     !impedance.isFinite() ||
     impedance <= 0.0
 ) {
     reasons += "Verified positive transformer impedance is unavailable."
 }

 return result(requirement.standard.name, item.model, reasons)
  
  }
  
  fun evaluate(
  requirement: TechnicalRequirement.Generator,
  item: GeneratorCatalogItem
  ): Result {
  val reasons = mutableListOf<String>()
  
   validateVerifiedProductSource(
     source = item.source,
     itemManufacturer = item.manufacturer,
     reasons = reasons
 )

 if (!requirement.requiredKva.isFinite() ||
     requirement.requiredKva <= 0.0
 ) {
     reasons += "Required generator rating must be finite and greater than zero."
 }

 if (!requirement.requiredVoltageV.isFinite() ||
     requirement.requiredVoltageV <= 0.0
 ) {
     reasons += "Required generator voltage is invalid."
 }

 if (!requirement.frequencyHz.isFinite() ||
     requirement.frequencyHz <= 0.0
 ) {
     reasons += "Required generator frequency is invalid."
 }

 if (!requirement.powerFactor.isFinite() ||
     requirement.powerFactor <= 0.0 ||
     requirement.powerFactor > 1.0
 ) {
     reasons += "Required generator power factor must be greater than zero and no greater than one."
 }

 if (!item.ratedPowerKva.isFinite() ||
     item.ratedPowerKva <= 0.0
 ) {
     reasons += "Catalog generator rating is invalid."
 } else if (item.ratedPowerKva < requirement.requiredKva) {
     reasons += "Generator rated power is below the calculated requirement."
 }

 if (!item.voltageV.isFinite() ||
     item.voltageV <= 0.0 ||
     item.voltageV < requirement.requiredVoltageV
 ) {
     reasons += "Generator voltage is invalid or below the required system voltage."
 }

 if (!sameRating(item.frequencyHz, requirement.frequencyHz)) {
     reasons += "Generator frequency does not match the engineering requirement."
 }

 if (!item.powerFactor.isFinite() ||
     item.powerFactor <= 0.0 ||
     item.powerFactor > 1.0 ||
     item.powerFactor < requirement.powerFactor
 ) {
     reasons += "Generator power factor is invalid or below the required value."
 }

 return result(requirement.standard.name, item.model, reasons)
  
  }
  
  fun evaluate(
  requirement: TechnicalRequirement.Busbar,
  item: BusbarCatalogItem
  ): Result {
  val reasons = mutableListOf<String>()
  
   validateVerifiedProductSource(
     source = item.source,
     itemManufacturer = item.manufacturer,
     reasons = reasons
 )

 if (!requirement.requiredCurrentA.isFinite() ||
     requirement.requiredCurrentA <= 0.0
 ) {
     reasons += "Required busbar current must be finite and greater than zero."
 }

 if (!requirement.requiredVoltageV.isFinite() ||
     requirement.requiredVoltageV <= 0.0
 ) {
     reasons += "Required busbar voltage must be finite and greater than zero."
 }

 if (!requirement.requiredShortCircuitKA.isFinite() ||
     requirement.requiredShortCircuitKA <= 0.0
 ) {
     reasons += "A positive prospective fault-current requirement is mandatory for final busbar compliance."
 }

 if (requirement.poles <= 0) {
     reasons += "Required busbar pole count must be greater than zero."
 }

 if (!item.ratedCurrentA.isFinite() ||
     item.ratedCurrentA <= 0.0
 ) {
     reasons += "Catalog busbar rated current is invalid."
 } else if (item.ratedCurrentA < requirement.requiredCurrentA) {
     reasons += "Busbar rated current is below the calculated requirement."
 }

 if (!item.voltageV.isFinite() ||
     item.voltageV <= 0.0 ||
     item.voltageV < requirement.requiredVoltageV
 ) {
     reasons += "Busbar rated voltage is invalid or below the required system voltage."
 }

 if (item.poles != requirement.poles) {
     reasons += "Busbar pole count does not match the engineering requirement."
 }

 val withstand = item.shortCircuitKA

 if (withstand == null ||
     !withstand.isFinite() ||
     withstand <= 0.0
 ) {
     reasons += "Verified positive busbar short-circuit withstand data is unavailable."
 } else if (
     requirement.requiredShortCircuitKA.isFinite() &&
     requirement.requiredShortCircuitKA > 0.0 &&
     withstand < requirement.requiredShortCircuitKA
 ) {
     reasons += "Busbar short-circuit withstand is below the calculated fault current."
 }

 return result(requirement.standard.name, item.model, reasons)
  
  }
  
  fun evaluate(
  requirement: TechnicalRequirement.Contactor,
  item: ContactorCatalogItem
  ): Result {
  val reasons = mutableListOf<String>()
  
   validateVerifiedProductSource(
     source = item.source,
     itemManufacturer = item.manufacturer,
     reasons = reasons
 )

 if (!requirement.requiredCurrentA.isFinite() ||
     requirement.requiredCurrentA <= 0.0
 ) {
     reasons += "Required contactor current must be finite and greater than zero."
 }

 if (!requirement.requiredVoltageV.isFinite() ||
     requirement.requiredVoltageV <= 0.0
 ) {
     reasons += "Required contactor voltage must be finite and greater than zero."
 }

 if (requirement.utilizationCategory.isBlank()) {
     reasons += "A contactor utilization category is required."
 }

 if (!item.ratedCurrentA.isFinite() ||
     item.ratedCurrentA <= 0.0
 ) {
     reasons += "Catalog contactor rated current is invalid."
 } else if (item.ratedCurrentA < requirement.requiredCurrentA) {
     reasons += "Contactor rated current is below the calculated motor current."
 }

 if (!item.voltageV.isFinite() ||
     item.voltageV <= 0.0 ||
     item.voltageV < requirement.requiredVoltageV
 ) {
     reasons += "Contactor rated voltage is invalid or below the required voltage."
 }

 if (!item.utilizationCategory.equals(
         requirement.utilizationCategory,
         ignoreCase = true
     )
 ) {
     reasons += "Contactor utilization category does not match the requirement."
 }

 return result(requirement.standard.name, item.model, reasons)
  
  }
  
  fun evaluate(
  requirement: TechnicalRequirement.Panel,
  item: PanelCatalogItem
  ): Result {
  val reasons = mutableListOf<String>()
  
   validateVerifiedProductSource(
     source = item.source,
     itemManufacturer = item.manufacturer,
     reasons = reasons
 )

 if (!requirement.requiredCurrentA.isFinite() ||
     requirement.requiredCurrentA <= 0.0
 ) {
     reasons += "Required panel current must be finite and greater than zero."
 }

 if (!requirement.requiredVoltageV.isFinite() ||
     requirement.requiredVoltageV <= 0.0
 ) {
     reasons += "Required panel voltage must be finite and greater than zero."
 }

 if (requirement.poles <= 0) {
     reasons += "Required panel pole count must be greater than zero."
 }

 if (!item.ratedCurrentA.isFinite() ||
     item.ratedCurrentA <= 0.0
 ) {
     reasons += "Catalog panel rated current is invalid."
 } else if (item.ratedCurrentA < requirement.requiredCurrentA) {
     reasons += "Panel rated current is below the required current."
 }

 if (!item.voltageV.isFinite() ||
     item.voltageV <= 0.0 ||
     item.voltageV < requirement.requiredVoltageV
 ) {
     reasons += "Panel rated voltage is invalid or below the required voltage."
 }

 if (item.poles != requirement.poles) {
     reasons += "Panel pole count does not match the engineering requirement."
 }

 return result(requirement.standard.name, item.model, reasons)
  
  }
  
  /**
  
  * Family-level and generic records cannot pass final compliance.
    */
    private fun validateVerifiedProductSource(
    source: CatalogSource,
    itemManufacturer: Manufacturer,
    reasons: MutableList<String>
    ) {
    if (source.status != ProductFamilyStatus.VERIFIED_PRODUCT) {
    reasons +=
    "Catalogue record is not an exact verified commercial product; family/generic records cannot pass final compliance."
    }
    
    if (source.manufacturer != itemManufacturer) {
    reasons +=
    "Catalog source manufacturer does not match the equipment manufacturer."
    }
    
    if (source.officialUrl.isBlank()) {
    reasons +=
    "Official manufacturer source URL is missing; product data cannot be treated as verified."
    }
    
    if (
    source.sourceReference.isBlank() ||
    source.sourceReference.equals(
    "NO COMMERCIAL PRODUCT",
    ignoreCase = true
    )
    ) {
    reasons += "Exact commercial product reference is missing."
    }
    }
  
  private fun sameRating(
  actual: Double,
  required: Double
  ): Boolean =
  actual.isFinite() &&
  required.isFinite() &&
  kotlin.math.abs(actual - required) <=
  maxOf(1.0e-6, kotlin.math.abs(required) * 1.0e-6)
  
  private fun result(
  standard: String,
  product: String,
  reasons: List<String>
  ): Result =
  Result(
  compliant = reasons.isEmpty(),
  standard = standard,
  product = product,
  reasons = reasons
  )
  }
