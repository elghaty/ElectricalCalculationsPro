fun recalculateEngineering() {

    val currentNetwork =
        network()

    /*
     * ============================================================
     * EDITOR STATE != ENGINEERING STATE
     * ============================================================
     *
     * A drawing is allowed to be incomplete.
     *
     * Examples:
     *
     * SOURCE
     *   |
     * PANEL
     *   |
     * BREAKER
     *
     * and then the user adds:
     *
     * LOAD-2
     *
     * LOAD-2 is temporarily disconnected.
     *
     * That is NOT an application error.
     *
     * It is a normal editor state.
     *
     * Therefore:
     *
     * 1. Never delete the new element.
     * 2. Never navigate away.
     * 3. Never call engineering engines.
     * 4. Show the topology problem in the SLD.
     */

    try {

        /*
         * ========================================================
         * HARD TOPOLOGY CHECK
         * ========================================================
         *
         * SldDesignValidator is diagnostic.
         *
         * SldTopologyEngine is authoritative for engineering
         * readiness.
         */
        val topologyResult =
            SldEngineeringFacade.checkEngineeringTopology(
                currentNetwork
            )

        if (
            !topologyResult.valid
        ) {

            /*
             * IMPORTANT:
             *
             * Keep the complete drawing exactly as it is.
             */
            state.engineeringPackage =
                null

            state.engineeringError =
                topologyResult.errorMessage
                    ?.takeIf {
                        it.isNotBlank()
                    }
                    ?: if (arabic) {
                        "المخطط غير متصل بالمصدر أو غير مكتمل للدراسة الهندسية."
                    } else {
                        "The SLD is not connected to the source or is not ready for engineering."
                    }

            return
        }

        /*
         * ========================================================
         * OPTIONAL DIAGNOSTIC VALIDATION
         * ========================================================
         *
         * We keep the existing design validator for warnings and
         * engineering-input diagnostics.
         *
         * Its warnings must NOT prevent drawing.
         *
         * The topology gate above is what decides whether
         * engineering is allowed.
         */
        val validation =
            SldEngineeringFacade.validate(
                currentNetwork
            )

        if (
            !validation.valid
        ) {

            state.engineeringPackage =
                null

            val messages =
                validation.errors
                    .map {
                        it.message
                    }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()

            state.engineeringError =
                if (
                    messages.isNotEmpty()
                ) {

                    messages.joinToString(
                        " • "
                    )

                } else if (arabic) {

                    "المخطط غير مكتمل للدراسة الهندسية."

                } else {

                    "The SLD is not ready for engineering study."
                }

            return
        }

        /*
         * ========================================================
         * ENGINEERING CALCULATION
         * ========================================================
         *
         * At this point SldTopologyEngine has already accepted
         * the graph.
         *
         * calculateComplete() contains its own hard gate as a
         * second safety layer.
         */
        state.engineeringPackage =
            SldEngineeringFacade.calculateComplete(
                network =
                    currentNetwork
            )

        state.engineeringError =
            null

    } catch (
        error: Throwable
    ) {

        /*
         * ========================================================
         * FINAL SAFETY BARRIER
         * ========================================================
         *
         * Even if a lower engineering engine throws an exception:
         *
         * - DO NOT delete nodes
         * - DO NOT delete connections
         * - DO NOT leave SLD screen
         * - DO NOT crash the application
         *
         * Keep the drawing intact and expose the error in the SLD.
         */
        state.engineeringPackage =
            null

        state.engineeringError =
            error.message
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: if (arabic) {
                    "تعذر تحديث الدراسة الهندسية. المخطط ما زال محفوظًا وقابلًا للتعديل."
                } else {
                    "Engineering study could not be updated. The drawing remains intact and editable."
                }
    }
}
