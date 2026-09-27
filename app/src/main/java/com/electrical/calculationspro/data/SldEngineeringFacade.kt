package com.electrical.calculationspro.data

/**

* ================================================================

* SLD ENGINEERING FACADE

* ================================================================

* 

* Single orchestration point between the SLD/UI layer and the

* engineering calculation engines.

* 

* UI

* ↓

* SldEngineeringFacade

* ↓

* SldEngineeringContext

* ↓

* Validation

* ↓

* Upstream Engineering

* ↓

* Short Circuit

* ↓

* Cable Sizing

* ↓

* Protection Coordination

* ↓

* Panel Schedule

* 

* This class contains orchestration only.

* 

* No engineering formulas are implemented here.

* 

* ================================================================
  */
  object SldEngineeringFacade {
  
  // ============================================================
  // VALIDATION
  // ============================================================
  
  fun validate(
  network: SldNetwork
  ): SldDesignValidator.Result {
  
   return SldDesignValidator.validate(
     network
 )
  
  }
  
  // ============================================================
  // UPSTREAM ENGINEERING
  // ============================================================
  
  fun calculateUpstream(
  network: SldNetwork
  ): SldUpstreamEngineering.Result {
  
   validateNetwork(network)

 return SldUpstreamEngineering.calculate(
     network
 )
  
  }
  
  /**
  
  * Context-aware upstream entry point.
  
  * 
  
  * Upstream calculations currently do not require standard
  
  * specific inputs, but the context is validated so that the
  
  * complete engineering cycle uses one consistent configuration.
    */
    fun calculateUpstream(
    network: SldNetwork,
    engineeringContext: SldEngineeringContext
    ): SldUpstreamEngineering.Result {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    return SldUpstreamEngineering.calculate(
    network
    )
    }
  
  // ============================================================
  // SHORT CIRCUIT
  // ============================================================
  
  fun calculateShortCircuit(
  network: SldNetwork,
  voltageFactor: Double = 1.05
  ): SldShortCircuitStudy {
  
   validateNetwork(network)

 require(
     voltageFactor > 0.0
 ) {
     "Voltage factor must be greater than zero."
 }

 return SldShortCircuitEngine.calculate(
     network = network,
     voltageFactor = voltageFactor
 )
  
  }
  
  /**
  
  * Context-aware short-circuit calculation.
    */
    fun calculateShortCircuit(
    network: SldNetwork,
    engineeringContext: SldEngineeringContext
    ): SldShortCircuitStudy {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    return SldShortCircuitEngine.calculate(
    network = network,
    voltageFactor = engineeringContext.voltageFactor
    )
    }
  
  // ============================================================
  // CABLE SIZING
  // ============================================================
  
  fun calculateCableSizing(
  network: SldNetwork,
  shortCircuitStudy: SldShortCircuitStudy? = null,
  voltageDropLimitPercent: Double = 3.0,
  shortCircuitTimeSeconds: Double = 1.0,
  upstreamEngineering:
  SldUpstreamEngineering.Result? = null
  ): SldCableSizingStudy {
  
   validateNetwork(network)

 require(
     voltageDropLimitPercent > 0.0
 ) {
     "Voltage drop limit must be greater than zero."
 }

 require(
     shortCircuitTimeSeconds > 0.0
 ) {
     "Short-circuit clearing time must be greater than zero."
 }

 val upstream =
     upstreamEngineering
         ?: SldUpstreamEngineering.calculate(
             network
         )

 val shortCircuit =
     shortCircuitStudy
         ?: SldShortCircuitEngine.calculate(
             network = network
         )

 return SldCableSizingEngine.calculate(
     network = network,
     shortCircuitStudy = shortCircuit,
     voltageDropLimitPercent =
         voltageDropLimitPercent,
     shortCircuitTimeSeconds =
         shortCircuitTimeSeconds,
     upstreamEngineering =
         upstream
 )
  
  }
  
  /**
  
  * Context-aware cable sizing.
  
  * 
  
  * The voltage-drop limit comes from the selected
  
  * StandardEngine when available.
    */
    fun calculateCableSizing(
    network: SldNetwork,
    engineeringContext: SldEngineeringContext,
    shortCircuitStudy: SldShortCircuitStudy? = null,
    upstreamEngineering:
    SldUpstreamEngineering.Result? = null
    ): SldCableSizingStudy {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    val upstream =
    upstreamEngineering
    ?: SldUpstreamEngineering.calculate(
    network
    )
    
    val shortCircuit =
    shortCircuitStudy
    ?: SldShortCircuitEngine.calculate(
    network = network,
    voltageFactor =
    engineeringContext.voltageFactor
    )
    
    return SldCableSizingEngine.calculate(
    network = network,
    shortCircuitStudy = shortCircuit,
    voltageDropLimitPercent =
    engineeringContext
    .effectiveVoltageDropLimitPercent(),
    shortCircuitTimeSeconds =
    engineeringContext.shortCircuitTimeSeconds,
    upstreamEngineering =
    upstream
    )
    }
  
  // ============================================================
  // PANEL SCHEDULE
  // ============================================================
  
  fun calculatePanelSchedule(
  network: SldNetwork,
  panelNodeId: String,
  cableSizingStudy: SldCableSizingStudy? = null,
  shortCircuitStudy: SldShortCircuitStudy? = null
  ): SldPanelSchedule {
  
   validateNetwork(network)

 require(
     network.nodes.any {
         it.id == panelNodeId &&
             it.type == SldNodeType.PANEL
     }
 ) {
     "Panel node '$panelNodeId' was not found."
 }

 return SldPanelScheduleEngine.calculate(
     network = network,
     panelNodeId = panelNodeId,
     cableSizingStudy =
         cableSizingStudy,
     shortCircuitStudy =
         shortCircuitStudy
 )
  
  }
  
  /**
  
  * Context-aware panel schedule.
  
  * 
  
  * The current panel schedule engine does not yet consume
  
  * standard-specific values directly. The context is therefore
  
  * validated and retained at the complete-study level without
  
  * duplicating calculations here.
    */
    fun calculatePanelSchedule(
    network: SldNetwork,
    panelNodeId: String,
    engineeringContext: SldEngineeringContext,
    cableSizingStudy: SldCableSizingStudy? = null,
    shortCircuitStudy: SldShortCircuitStudy? = null
    ): SldPanelSchedule {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    require(
    network.nodes.any {
    it.id == panelNodeId &&
    it.type == SldNodeType.PANEL
    }
    ) {
    "Panel node '$panelNodeId' was not found."
    }
    
    return SldPanelScheduleEngine.calculate(
    network = network,
    panelNodeId = panelNodeId,
    cableSizingStudy =
    cableSizingStudy,
    shortCircuitStudy =
    shortCircuitStudy
    )
    }
  
  // ============================================================
  // PROTECTION COORDINATION
  // ============================================================
  
  fun calculateProtectionCoordination(
  network: SldNetwork,
  shortCircuitStudy: SldShortCircuitStudy? = null,
  cableSizingStudy: SldCableSizingStudy? = null,
  upstreamEngineering:
  SldUpstreamEngineering.Result? = null
  ): SldProtectionCoordinationResult {
  
   validateNetwork(network)

 val upstream =
     upstreamEngineering
         ?: SldUpstreamEngineering.calculate(
             network
         )

 val shortCircuit =
     shortCircuitStudy
         ?: SldShortCircuitEngine.calculate(
             network = network
         )

 val cableSizing =
     cableSizingStudy
         ?: SldCableSizingEngine.calculate(
             network = network,
             shortCircuitStudy =
                 shortCircuit,
             upstreamEngineering =
                 upstream
         )

 return SldProtectionCoordinationEngine.calculate(
     network = network,
     shortCircuitStudy =
         shortCircuit,
     cableSizingStudy =
         cableSizing,
     upstreamEngineering =
         upstream
 )
  
  }
  
  /**
  
  * Context-aware protection coordination.
  
  * 
  
  * Protection coordination currently consumes the engineering
  
  * results produced upstream rather than calculating standard
  
  * data inside this facade.
    */
    fun calculateProtectionCoordination(
    network: SldNetwork,
    engineeringContext: SldEngineeringContext,
    shortCircuitStudy: SldShortCircuitStudy? = null,
    cableSizingStudy: SldCableSizingStudy? = null,
    upstreamEngineering:
    SldUpstreamEngineering.Result? = null
    ): SldProtectionCoordinationResult {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    val upstream =
    upstreamEngineering
    ?: SldUpstreamEngineering.calculate(
    network
    )
    
    val shortCircuit =
    shortCircuitStudy
    ?: SldShortCircuitEngine.calculate(
    network = network,
    voltageFactor =
    engineeringContext.voltageFactor
    )
    
    val cableSizing =
    cableSizingStudy
    ?: SldCableSizingEngine.calculate(
    network = network,
    shortCircuitStudy =
    shortCircuit,
    voltageDropLimitPercent =
    engineeringContext
    .effectiveVoltageDropLimitPercent(),
    shortCircuitTimeSeconds =
    engineeringContext.shortCircuitTimeSeconds,
    upstreamEngineering =
    upstream
    )
    
    return SldProtectionCoordinationEngine.calculate(
    network = network,
    shortCircuitStudy =
    shortCircuit,
    cableSizingStudy =
    cableSizing,
    upstreamEngineering =
    upstream
    )
    }
  
  // ============================================================
  // COMPLETE ENGINEERING STUDY
  // ============================================================
  
  /**
  
  * Backward-compatible complete engineering entry point.
  
  * 
  
  * Existing callers can continue using the original API.
  
  * 
  
  * Internally a single SldEngineeringContext is created so all
  
  * context-aware calculations use the same engineering inputs.
    */
    fun calculateComplete(
    network: SldNetwork,
    panelNodeId: String? = null,
    voltageFactor: Double = 1.05,
    voltageDropLimitPercent: Double = 3.0,
    shortCircuitTimeSeconds: Double = 1.0
    ): SldEngineeringPackage {
    
    val context =
    SldEngineeringContext(
    voltageFactor =
    voltageFactor,
    voltageDropLimitPercent =
    voltageDropLimitPercent,
    shortCircuitTimeSeconds =
    shortCircuitTimeSeconds
    )
    
    return calculateComplete(
    network = network,
    panelNodeId = panelNodeId,
    engineeringContext = context
    )
    }
  
  /**
  
  * Complete context-aware engineering study.
  
  * 
  
  * Calculation order:
  
  * 
  
  * 1. Network validation
  
  * 2. Engineering-context validation
  
  * 3. Upstream engineering
  
  * 4. Short circuit
  
  * 5. Cable sizing
  
  * 6. Protection coordination
  
  * 7. Panel schedule
  
  * 
  
  * The upstream result is calculated exactly once and shared
  
  * with downstream studies.
    */
    fun calculateComplete(
    network: SldNetwork,
    panelNodeId: String? = null,
    engineeringContext: SldEngineeringContext
    ): SldEngineeringPackage {
    
    validateNetwork(network)
    
    validateContext(
    engineeringContext
    )
    
    // --------------------------------------------------------
    // 1. UPSTREAM ENGINEERING
    // --------------------------------------------------------
    
    val upstream =
    SldUpstreamEngineering.calculate(
    network
    )
    
    // --------------------------------------------------------
    // 2. SHORT CIRCUIT
    // --------------------------------------------------------
    
    val shortCircuit =
    SldShortCircuitEngine.calculate(
    network = network,
    voltageFactor =
    engineeringContext.voltageFactor
    )
    
    // --------------------------------------------------------
    // 3. CABLE SIZING
    // --------------------------------------------------------
    
    val cableSizing =
    SldCableSizingEngine.calculate(
    network = network,
    shortCircuitStudy =
    shortCircuit,
    voltageDropLimitPercent =
    engineeringContext
    .effectiveVoltageDropLimitPercent(),
    shortCircuitTimeSeconds =
    engineeringContext
    .shortCircuitTimeSeconds,
    upstreamEngineering =
    upstream
    )
    
    // --------------------------------------------------------
    // 4. PROTECTION COORDINATION
    // --------------------------------------------------------
    
    val protection =
    SldProtectionCoordinationEngine.calculate(
    network = network,
    shortCircuitStudy =
    shortCircuit,
    cableSizingStudy =
    cableSizing,
    upstreamEngineering =
    upstream
    )
    
    // --------------------------------------------------------
    // 5. PANEL SCHEDULE
    // --------------------------------------------------------
    
    val panelSchedule =
    panelNodeId?.let { id ->
    
         calculatePanelSchedule(
         network = network,
         panelNodeId = id,
         engineeringContext =
             engineeringContext,
         cableSizingStudy =
             cableSizing,
         shortCircuitStudy =
             shortCircuit
     )
 }
    
    // --------------------------------------------------------
    // 6. FINAL ENGINEERING PACKAGE
    // --------------------------------------------------------
    
    return SldEngineeringPackage(
    upstream =
    upstream,
    shortCircuit =
    shortCircuit,
    cableSizing =
    cableSizing,
    protectionCoordination =
    protection,
    panelSchedule =
    panelSchedule,
    engineeringContext =
    engineeringContext
    )
    }
  
  // ============================================================
  // CONTEXT VALIDATION
  // ============================================================
  
  private fun validateContext(
  engineeringContext: SldEngineeringContext
  ) {
  
   /*
  * The project currently contains active Egyptian,
  * IEC and NEC engines, while the Egyptian engine explicitly
  * reports that its complete verified dataset is not yet
  * populated.
  *
  * We therefore validate the engineering inputs here without
  * forcing incomplete standards to abort an otherwise valid
  * calculation cycle.
  *
  * The implementation status remains available through the
  * context and can be surfaced by the UI/report layer.
  */
 engineeringContext
     .copy(
         requireImplementedStandard = false
     )
     .validate()
  
  }
  
  // ============================================================
  // NETWORK VALIDATION
  // ============================================================
  
  private fun validateNetwork(
  network: SldNetwork
  ) {
  
   require(
     network.nodes.isNotEmpty()
 ) {
     "SLD network is empty."
 }

 val validation =
     SldDesignValidator.validate(
         network
     )

 require(
     validation.valid
 ) {
     buildValidationMessage(
         validation
     )
 }
  
  }
  
  // ============================================================
  // VALIDATION MESSAGE
  // ============================================================
  
  private fun buildValidationMessage(
  validation:
  SldDesignValidator.Result
  ): String {
  
   return buildString {

     appendLine(
         "SLD topology validation failed."
     )

     if (
         validation.errors.isNotEmpty()
     ) {

         appendLine()

         appendLine(
             "Errors:"
         )

         validation.errors.forEach {

             appendLine(
                 "- [${it.code}] ${it.message}"
             )
         }
     }

     if (
         validation.warnings.isNotEmpty()
     ) {

         appendLine()

         appendLine(
             "Warnings:"
         )

         validation.warnings.forEach {

             appendLine(
                 "- [${it.code}] ${it.message}"
             )
         }
     }
 }
  
  }
  }

/**

* ================================================================

* COMPLETE SLD ENGINEERING RESULT

* ================================================================

* 

* Single engineering package consumed by:

* 

* - SLD UI

* - Engineering overlay

* - Reports

* - Project bridge

* 

* ================================================================
  */
  data class SldEngineeringPackage(
  
  val upstream:
  SldUpstreamEngineering.Result,
  
  val shortCircuit:
  SldShortCircuitStudy,
  
  val cableSizing:
  SldCableSizingStudy,
  
  val protectionCoordination:
  SldProtectionCoordinationResult,
  
  val panelSchedule:
  SldPanelSchedule?,
  
  val engineeringContext:
  SldEngineeringContext =
  SldEngineeringContext.default()
  )
