package com.electrical.calculationspro.data

enum class AppLanguage {
    ENGLISH,
    ARABIC
}

object Strings {

    private val english =
        mapOf(

            "app_name" to
                "Electrical Calculations Pro",

            "home" to
                "Home",

            "sld" to
                "Professional SLD",

            "conductor_sizing" to
                "Conductor Sizing",

            "conductor_sizing_protection" to
                "Conductor Sizing",

            "protection_coordination" to
                "Protection Coordination",

            "current_type" to
                "Current Type",

            "direct_current" to
                "DC",

            "alternating_single" to
                "AC Single Phase",

            "alternating_two" to
                "AC Two Phase",

            "alternating_three" to
                "AC Three Phase",

            "voltage" to
                "Voltage",

            "load" to
                "Load",

            "power_factor_label" to
                "Power Factor",

            "line_length" to
                "Line Length",

            "ambient_temp" to
                "Ambient Temperature",

            "circuits_conduit" to
                "Circuits in Conduit",

            "max_voltage_drop" to
                "Maximum Voltage Drop",

            "conductor" to
                "Conductor",

            "copper" to
                "Copper",

            "aluminum" to
                "Aluminum",

            "insulation" to
                "Insulation",

            "pvc" to
                "PVC",

            "xlpe" to
                "XLPE",

            "epr" to
                "EPR",

            "rubber" to
                "Rubber",

            "method_installation" to
                "Installation Method",

            "calculate" to
                "Calculate",

            "recommended_section" to
                "Recommended Section",

            "selected_section" to
                "Selected Section",

            "design_current" to
                "Design Current",

            "voltage_drop_result" to
                "Voltage Drop",

            "voltage_drop_percent" to
                "Voltage Drop %",

            "current_capacity" to
                "Current Capacity",

            "breaker_rating" to
                "Breaker Rating",

            "status" to
                "Status",

            "pass" to
                "PASS",

            "warning" to
                "WARNING",

            "fail" to
                "FAIL",

            "active_power" to
                "Active Power",

            "apparent_power" to
                "Apparent Power",

            "reactive_power" to
                "Reactive Power",

            "power_factor" to
                "Power Factor",

            "resistance" to
                "Resistance",

            "impedance" to
                "Impedance",

            "voltage_drop" to
                "Voltage Drop",

            "current" to
                "Current"
        )

    private val arabic =
        mapOf(

            "app_name" to
                "الحسابات الكهربائية الاحترافية",

            "home" to
                "الرئيسية",

            "sld" to
                "SLD الاحترافي",

            "conductor_sizing" to
                "اختيار مقطع الموصل",

            "conductor_sizing_protection" to
                "اختيار مقطع الموصل",

            "protection_coordination" to
                "تنسيق الحماية",

            "current_type" to
                "نوع التيار",

            "direct_current" to
                "تيار مستمر DC",

            "alternating_single" to
                "تيار متردد أحادي الوجه",

            "alternating_two" to
                "تيار متردد ثنائي الوجه",

            "alternating_three" to
                "تيار متردد ثلاثي الأوجه",

            "voltage" to
                "الجهد",

            "load" to
                "الحمل",

            "power_factor_label" to
                "معامل القدرة",

            "line_length" to
                "طول الخط",

            "ambient_temp" to
                "درجة حرارة الوسط المحيط",

            "circuits_conduit" to
                "عدد الدوائر داخل الماسورة",

            "max_voltage_drop" to
                "أقصى هبوط جهد",

            "conductor" to
                "الموصل",

            "copper" to
                "نحاس",

            "aluminum" to
                "ألومنيوم",

            "insulation" to
                "نوع العزل",

            "pvc" to
                "PVC",

            "xlpe" to
                "XLPE",

            "epr" to
                "EPR",

            "rubber" to
                "مطاط",

            "method_installation" to
                "طريقة التركيب",

            "calculate" to
                "احسب",

            "recommended_section" to
                "المقطع المقترح",

            "selected_section" to
                "المقطع المختار",

            "design_current" to
                "تيار التصميم",

            "voltage_drop_result" to
                "هبوط الجهد",

            "voltage_drop_percent" to
                "نسبة هبوط الجهد",

            "current_capacity" to
                "سعة تحمل التيار",

            "breaker_rating" to
                "مقاس القاطع",

            "status" to
                "الحالة",

            "pass" to
                "ناجح",

            "warning" to
                "تحذير",

            "fail" to
                "فشل",

            "active_power" to
                "القدرة الفعالة",

            "apparent_power" to
                "القدرة الظاهرية",

            "reactive_power" to
                "القدرة غير الفعالة",

            "power_factor" to
                "معامل القدرة",

            "resistance" to
                "المقاومة",

            "impedance" to
                "الممانعة",

            "voltage_drop" to
                "هبوط الجهد",

            "current" to
                "التيار"
        )

    fun get(
        key: String,
        language: AppLanguage
    ): String {

        val map =
            if (
                language ==
                AppLanguage.ARABIC
            ) {
                arabic
            } else {
                english
            }

        return map[key]
            ?: key
    }
}
