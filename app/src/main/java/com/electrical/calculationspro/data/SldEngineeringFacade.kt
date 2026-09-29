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
     network = network
 )
  
  }
  
  /**
  
  * Context-aware upstream entry point.
  
  * 
  
  * The selected EngineeringContext is passed directly to the
  
  * authoritative Upstream engine.
  
  * 
  
  * This ensures that the selected engineering standard controls
  
  * the breaker rating dataset and voltage-drop requirements.
    */
    fun calculateUpstream(
    network: SldNetwork,
    engineeringContext: SldEngineeringContext
    ): SldUpstreamEngineering.Result {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    return SldUpstreamEngineering.calculate(
    network = network,
    engineeringContext = engineeringContext
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
    voltageFactor =
    engineeringContext.voltageFactor
    )
    }
  
  // ============================================================
  // CABLE SIZING
  // ============================================================
  
  fun calculateCableSizing(
  network: SldNetwork,
  shortCircuitStudy:
  SldShortCircuitStudy? = null,
  voltageDropLimitPercent:
  Double = 3.0,
  shortCircuitTimeSeconds:
  Double = 1.0,
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
             network = network
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
  
  * The selected StandardEngine controls:
  
  * 
  
  * - conductor sections
  
  * - conductor ampacity
  
  * - ambient correction
  
  * - grouping correction
  
  * - voltage-drop limit
      */
      fun calculateCableSizing(
      network: SldNetwork,
      engineeringContext:
      SldEngineeringContext,
      shortCircuitStudy:
      SldShortCircuitStudy? = null,
      upstreamEngineering:
      SldUpstreamEngineering.Result? = null
      ): SldCableSizingStudy {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    val upstream =
    upstreamEngineering
    ?: SldUpstreamEngineering.calculate(
    network = network,
    engineeringContext =
    engineeringContext
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
    shortCircuitStudy =
    shortCircuit,
    voltageDropLimitPercent =
    engineeringContext
    .effectiveVoltageDropLimitPercent(),
    shortCircuitTimeSeconds =
    engineeringContext
    .shortCircuitTimeSeconds,
    upstreamEngineering =
    upstream,
    engineeringContext =
    engineeringContext
    )
    }
  
  // ============================================================
  // PANEL SCHEDULE
  // ============================================================
  
  fun calculatePanelSchedule(
  network: SldNetwork,
  panelNodeId: String,
  cableSizingStudy:
  SldCableSizingStudy? = null,
  shortCircuitStudy:
  SldShortCircuitStudy? = null
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
  
  * The panel schedule engine currently receives the already
  
  * calculated engineering studies. Standard-specific data is
  
  * therefore maintained in the engineering context and the
  
  * upstream/cable/protection studies.
    */
    fun calculatePanelSchedule(
    network: SldNetwork,
    panelNodeId: String,
    engineeringContext:
    SldEngineeringContext,
    cableSizingStudy:
    SldCableSizingStudy? = null,
    shortCircuitStudy:
    SldShortCircuitStudy? = null
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
  shortCircuitStudy:
  SldShortCircuitStudy? = null,
  cableSizingStudy:
  SldCableSizingStudy? = null,
  upstreamEngineering:
  SldUpstreamEngineering.Result? = null
  ): SldProtectionCoordinationResult {
  
   validateNetwork(network)

 val upstream =
     upstreamEngineering
         ?: SldUpstreamEngineering.calculate(
             network = network
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
  
  * Breaker ratings and cable engineering data are supplied
  
  * through the selected EngineeringContext.
    */
    fun calculateProtectionCoordination(
    network: SldNetwork,
    engineeringContext:
    SldEngineeringContext,
    shortCircuitStudy:
    SldShortCircuitStudy? = null,
    cableSizingStudy:
    SldCableSizingStudy? = null,
    upstreamEngineering:
    SldUpstreamEngineering.Result? = null
    ): SldProtectionCoordinationResult {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    val upstream =
    upstreamEngineering
    ?: SldUpstreamEngineering.calculate(
    network = network,
    engineeringContext =
    engineeringContext
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
    engineeringContext
    .shortCircuitTimeSeconds,
    upstreamEngineering =
    upstream,
    engineeringContext =
    engineeringContext
    )
    
    return SldProtectionCoordinationEngine.calculate(
    network = network,
    shortCircuitStudy =
    shortCircuit,
    cableSizingStudy =
    cableSizing,
    upstreamEngineering =
    upstream,
    engineeringContext =
    engineeringContext
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
  
  * A single engineering context is created internally and
  
  * passed through the complete calculation chain.
    */
    fun calculateComplete(
    network: SldNetwork,
    panelNodeId: String? = null,
    voltageFactor: Double = 1.05,
    voltageDropLimitPercent:
    Double = 3.0,
    shortCircuitTimeSeconds:
    Double = 1.0
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
  
  * IMPORTANT:
  
  * 
  
  * The upstream result is calculated once and then passed to
  
  * the downstream engineering engines. This keeps all SLD
  
  * calculations synchronized.
    */
    fun calculateComplete(
    network: SldNetwork,
    panelNodeId: String? = null,
    engineeringContext:
    SldEngineeringContext
    ): SldEngineeringPackage {
    
    validateNetwork(network)
    validateContext(engineeringContext)
    
    // --------------------------------------------------------
    // 1. UPSTREAM ENGINEERING
    // --------------------------------------------------------
    
    val upstream =
    SldUpstreamEngineering.calculate(
    network = network,
    engineeringContext =
    engineeringContext
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
    upstream,
    engineeringContext =
    engineeringContext
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
    upstream,
    engineeringContext =
    engineeringContext
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
  engineeringContext:
  SldEngineeringContext
  ) {
  
   /*
  * The project contains active Egyptian, IEC and NEC
  * engines. Some standards can explicitly report that
  * their complete verified datasets are not yet populated.
  *
  * We therefore validate engineering inputs without
  * forcing an incomplete standard to abort the SLD study.
  *
  * The implementation status remains available to the UI
  * and report layers.
  *
  * No fallback to another standard is performed here.
  */
 engineeringContext
     .copy(
         requireImplementedStandard =
             false
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
