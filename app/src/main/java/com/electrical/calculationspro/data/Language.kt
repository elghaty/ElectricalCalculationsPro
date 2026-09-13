package com.electrical.calculationspro.data

enum class AppLanguage {
    ENGLISH,
    ARABIC
}

object Strings {

    fun get(
        key: String,
        lang: AppLanguage
    ): String {

        return when (lang) {

            AppLanguage.ENGLISH ->
                english[key] ?: key

            AppLanguage.ARABIC ->
                arabic[key] ?: key
        }
    }

    private val english = mapOf(

        "app_name" to
            "Electrical Calculations Pro",

        "engineering_tools" to
            "Engineering Tools",

        "update_program" to
            "Update Program",

        "update_program_description" to
            "Check latest version",

        "conductor_sizing_protection" to
            "Conductor sizing and protective device coordination",

        "conductor_sizing" to
            "Conductor sizing",

        "voltage_drop" to
            "Calculation of voltage drop",

        "current" to
            "Calculation of current",

        "voltage" to
            "Calculation of voltage",

        "active_power" to
            "Calculation of active power",

        "apparent_power" to
            "Calculation of apparent power",

        "reactive_power" to
            "Calculation of reactive power",

        "power_factor" to
            "Calculation of power factor",

        "resistance" to
            "Calculation of resistance",

        "impedance" to
            "Calculation of impedance",

        "sld_editor" to
            "SLD System Designer",

        "sld_editor_short" to
            "SLD",

        "current_type" to
            "Current type:",

        "direct_current" to
            "Direct current",

        "alternating_single" to
            "Alternating single-phase",

        "alternating_two" to
            "Alternating two-phase",

        "alternating_three" to
            "Alternating three-phase",

        "load" to
            "Load:",

        "power_factor_label" to
            "Power factor:",

        "line_length" to
            "Line length:",

        "method_installation" to
            "Installation method:",

        "ambient_temp" to
            "Ambient temperature:",

        "conductor" to
            "Conductor:",

        "insulation" to
            "Insulation:",

        "circuits_conduit" to
            "Circuits in the same conduit:",

        "max_voltage_drop" to
            "Max voltage drop:",

        "calculate" to
            "Calculate",

        "results" to
            "Results",

        "design_current" to
            "Design current (Ib)",

        "recommended_section" to
            "Recommended section",

        "selected_section" to
            "Selected section",

        "ampacity" to
            "Ampacity (Iz)",

        "voltage_drop_result" to
            "Voltage drop",

        "protective_device" to
            "Protective device",

        "copper" to
            "Copper",

        "aluminum" to
            "Aluminum",

        "pvc" to
            "PVC",

        "xlpe" to
            "XLPE",

        "epr" to
            "EPR",

        "rubber" to
            "Rubber",

        "coming_soon" to
            "Coming soon...",

        "language" to
            "Language",

        "english" to
            "English",

        "arabic" to
            "العربية",

        "standard" to
            "Standard",

        "egyptian_code" to
            "Egyptian Code",

        "single_core" to
            "Single-core",

        "multi_core" to
            "Multi-core",

        "notes" to
            "Notes",

        "functions" to
            "Engineering Functions",

        "functions_description" to
            "Electrical calculations, conductor sizing, voltage drop, current, voltage, active power, apparent power, reactive power, power factor, resistance and impedance.",

        "about" to
            "About",

        "about_description" to
            "Electrical Calculations Pro\nProfessional electrical engineering calculation application.",

        "close" to
            "Close"
    )

    private val arabic = mapOf(

        "app_name" to
            "حاسبة الحسابات الكهربائية برو",

        "engineering_tools" to
            "الأدوات الهندسية",

        "update_program" to
            "تحديث البرنامج",

        "update_program_description" to
            "التحقق من أحدث إصدار",

        "conductor_sizing_protection" to
            "تحديد مقطع الموصل وتنسيق أجهزة الحماية",

        "conductor_sizing" to
            "تحديد مقطع الموصل",

        "voltage_drop" to
            "حساب هبوط الجهد",

        "current" to
            "حساب التيار",

        "voltage" to
            "حساب الجهد",

        "active_power" to
            "حساب القدرة الفعالة",

        "apparent_power" to
            "حساب القدرة الظاهرية",

        "reactive_power" to
            "حساب القدرة غير الفعالة",

        "power_factor" to
            "حساب معامل القدرة",

        "resistance" to
            "حساب المقاومة",

        "impedance" to
            "حساب المعاوقة",

        "sld_editor" to
            "مصمم مخطط SLD",

        "sld_editor_short" to
            "SLD",

        "current_type" to
            "نوع التيار:",

        "direct_current" to
            "تيار مستمر",

        "alternating_single" to
            "تيار متردد أحادي الطور",

        "alternating_two" to
            "تيار متردد ثنائي الطور",

        "alternating_three" to
            "تيار متردد ثلاثي الطور",

        "load" to
            "الحمل:",

        "power_factor_label" to
            "معامل القدرة:",

        "line_length" to
            "طول الخط:",

        "method_installation" to
            "طريقة التركيب:",

        "ambient_temp" to
            "درجة الحرارة المحيطة:",

        "conductor" to
            "الموصل:",

        "insulation" to
            "العزل:",

        "circuits_conduit" to
            "عدد الدوائر في نفس الماسورة:",

        "max_voltage_drop" to
            "أقصى هبوط جهد:",

        "calculate" to
            "احسب",

        "results" to
            "النتائج",

        "design_current" to
            "تيار التصميم (Ib)",

        "recommended_section" to
            "المقطع الموصى به",

        "selected_section" to
            "المقطع المختار",

        "ampacity" to
            "سعة التيار (Iz)",

        "voltage_drop_result" to
            "هبوط الجهد",

        "protective_device" to
            "جهاز الحماية",

        "copper" to
            "نحاس",

        "aluminum" to
            "ألومنيوم",

        "pvc" to
            "PVC",

        "xlpe" to
            "XLPE",

        "epr" to
            "EPR",

        "rubber" to
            "مطاط",

        "coming_soon" to
            "قريباً...",

        "language" to
            "اللغة",

        "english" to
            "English",

        "arabic" to
            "العربية",

        "standard" to
            "المعيار",

        "egyptian_code" to
            "الكود المصري",

        "single_core" to
            "أحادي النواة",

        "multi_core" to
            "متعدد النواة",

        "notes" to
            "ملاحظات",

        "functions" to
            "الوظائف الهندسية",

        "functions_description" to
            "حسابات كهربائية، تحديد مقطع الموصل، هبوط الجهد، التيار، الجهد، القدرة الفعالة، القدرة الظاهرية، القدرة غير الفعالة، معامل القدرة، المقاومة والمعاوقة.",

        "about" to
            "عن البرنامج",

        "about_description" to
            "حاسبة الحسابات الكهربائية برو\nبرنامج احترافي للحسابات والتصميمات الهندسية الكهربائية.",

        "close" to
            "إغلاق"
    )
}
