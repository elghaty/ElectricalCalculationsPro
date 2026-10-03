package com.electrical.calculationspro.data

enum class AppLanguage {
    ENGLISH,
    ARABIC
}

object Strings {

    private val english = mapOf(
        "app_name" to "Electrical Calculations Pro",
        "home" to "Home",
        "project" to "Project",
        "design" to "Design",
        "reports" to "Reports",
        "sld" to "SLD Designer",

        "electromechanical_design" to "Electromechanical Design",
        "engineering_project" to "Engineering Project",
        "engineering_reports" to "Engineering Reports",

        "project_management" to "Project Management",
        "active_project" to "Active Project",
        "new_project" to "New Project",
        "no_active_project" to "No Active Project",

        "designer" to "Designer",
        "consultant" to "Consultant",
        "project_number" to "Project No.",

        "loads" to "Loads",
        "load_schedule" to "Load Schedule",
        "current" to "Current",
        "design_current" to "Design Current",
        "cables" to "Cables",
        "conductor_sizing" to "Conductor Sizing",
        "voltage_drop" to "Voltage Drop",
        "breaker" to "Breaker",
        "breaker_selection" to "Breaker Selection",
        "short_circuit" to "Short Circuit",
        "protection" to "Protection",
        "protection_coordination" to "Protection Coordination",
        "transformer" to "Transformer",
        "generator" to "Generator",
        "panel" to "Panel",

        "water" to "Water",
        "water_design" to "Water Design",
        "sewage" to "Sewage",
        "sewage_design" to "Sewage Design",
        "pump" to "Pump",
        "pumps" to "Pumps",
        "report" to "Report",

        "calculate" to "Calculate",
        "calculate_and_save" to "Calculate & Save Design",
        "results" to "Results",
        "status" to "Status",
        "no_result" to "No Result Yet",

        "voltage" to "Voltage",
        "load" to "Load",
        "power_factor" to "Power Factor",
        "power_factor_label" to "Power Factor",
        "line_length" to "Line Length",
        "length" to "Length",
        "ambient_temp" to "Ambient Temperature",
        "circuits_conduit" to "Circuits in Conduit",
        "max_voltage_drop" to "Maximum Voltage Drop",

        "conductor" to "Conductor",
        "copper" to "Copper",
        "aluminum" to "Aluminum",
        "insulation" to "Insulation",
        "pvc" to "PVC",
        "xlpe" to "XLPE",
        "epr" to "EPR",
        "rubber" to "Rubber",
        "installation_method" to "Installation Method",

        "recommended_section" to "Recommended Section",
        "selected_section" to "Selected Section",
        "current_capacity" to "Current Capacity",
        "breaker_rating" to "Breaker Rating",

        "active_power" to "Active Power",
        "apparent_power" to "Apparent Power",
        "reactive_power" to "Reactive Power",
        "resistance" to "Resistance",
        "impedance" to "Impedance",

        "static_head" to "Static Head",
        "friction_loss" to "Friction Loss",
        "minor_losses" to "Minor Losses",
        "required_pressure_head" to "Required Pressure Head",
        "total_dynamic_head" to "Total Dynamic Head",
        "flow" to "Flow",
        "average_flow" to "Average Flow",
        "peak_flow" to "Peak Flow",
        "minimum_flow" to "Minimum Flow",
        "rising_main" to "Rising Main",
        "wet_well" to "Wet Well",
        "velocity" to "Velocity",
        "motor_power" to "Motor Power",
        "yearly_energy" to "Yearly Energy",

        "pass" to "PASS",
        "warning" to "WARNING",
        "fail" to "FAIL",
        "check" to "CHECK",

        "current_type" to "Current Type",
        "direct_current" to "DC",
        "alternating_single" to "AC Single Phase",
        "alternating_two" to "AC Two Phase",
        "alternating_three" to "AC Three Phase",

        "english" to "English",
        "arabic" to "العربية",
        "language" to "Language",
        "back" to "Back",

        "open_sld" to "Open SLD Designer",
        "engineering_workspace" to "Engineering Workspace",
        "select_workspace" to "Select the engineering workspace",
        "no_active_project_message" to "Create or select a project first."
    )

    private val arabic = mapOf(
        "app_name" to "الحسابات الكهروميكانيكية الاحترافية",
        "home" to "الرئيسية",
        "project" to "المشروع",
        "design" to "التصميم",
        "reports" to "التقارير",
        "sld" to "مصمم SLD",

        "electromechanical_design" to "التصميم الكهروميكانيكي",
        "engineering_project" to "المشروع الهندسي",
        "engineering_reports" to "التقارير الهندسية",

        "project_management" to "إدارة المشروعات",
        "active_project" to "المشروع النشط",
        "new_project" to "مشروع جديد",
        "no_active_project" to "لا يوجد مشروع نشط",

        "designer" to "المصمم",
        "consultant" to "الاستشاري",
        "project_number" to "رقم المشروع",

        "loads" to "الأحمال",
        "load_schedule" to "جدول الأحمال",
        "current" to "التيار",
        "design_current" to "تيار التصميم",
        "cables" to "الكابلات",
        "conductor_sizing" to "اختيار مقطع الموصل",
        "voltage_drop" to "هبوط الجهد",
        "breaker" to "القاطع",
        "breaker_selection" to "اختيار القاطع",
        "short_circuit" to "تيار القصر",
        "protection" to "الحماية",
        "protection_coordination" to "تنسيق الحماية",
        "transformer" to "المحول",
        "generator" to "المولد",
        "panel" to "اللوحة",

        "water" to "المياه",
        "water_design" to "تصميم المياه",
        "sewage" to "الصرف الصحي",
        "sewage_design" to "تصميم الصرف الصحي",
        "pump" to "المضخة",
        "pumps" to "المضخات",
        "report" to "التقرير",

        "calculate" to "احسب",
        "calculate_and_save" to "احسب واحفظ التصميم",
        "results" to "النتائج",
        "status" to "الحالة",
        "no_result" to "لا توجد نتيجة بعد",

        "voltage" to "الجهد",
        "load" to "الحمل",
        "power_factor" to "معامل القدرة",
        "power_factor_label" to "معامل القدرة",
        "line_length" to "طول الخط",
        "length" to "الطول",
        "ambient_temp" to "درجة حرارة الوسط المحيط",
        "circuits_conduit" to "عدد الدوائر داخل الماسورة",
        "max_voltage_drop" to "أقصى هبوط جهد",

        "conductor" to "الموصل",
        "copper" to "نحاس",
        "aluminum" to "ألومنيوم",
        "insulation" to "العزل",
        "pvc" to "PVC",
        "xlpe" to "XLPE",
        "epr" to "EPR",
        "rubber" to "مطاط",
        "installation_method" to "طريقة التركيب",

        "recommended_section" to "المقطع المقترح",
        "selected_section" to "المقطع المختار",
        "current_capacity" to "سعة تحمل التيار",
        "breaker_rating" to "مقاس القاطع",

        "active_power" to "القدرة الفعالة",
        "apparent_power" to "القدرة الظاهرية",
        "reactive_power" to "القدرة غير الفعالة",
        "resistance" to "المقاومة",
        "impedance" to "الممانعة",

        "static_head" to "الرأس الساكن",
        "friction_loss" to "فاقد الاحتكاك",
        "minor_losses" to "الفواقد الثانوية",
        "required_pressure_head" to "رأس الضغط المطلوب",
        "total_dynamic_head" to "الرأس الديناميكي الكلي",
        "flow" to "التدفق",
        "average_flow" to "التدفق المتوسط",
        "peak_flow" to "التدفق الأقصى",
        "minimum_flow" to "التدفق الأدنى",
        "rising_main" to "خط الطرد",
        "wet_well" to "حوض التجميع",
        "velocity" to "السرعة",
        "motor_power" to "قدرة المحرك",
        "yearly_energy" to "الطاقة السنوية",

        "pass" to "ناجح",
        "warning" to "تحذير",
        "fail" to "فشل",
        "check" to "مراجعة",

        "current_type" to "نوع التيار",
        "direct_current" to "تيار مستمر DC",
        "alternating_single" to "تيار متردد أحادي الوجه",
        "alternating_two" to "تيار متردد ثنائي الوجه",
        "alternating_three" to "تيار متردد ثلاثي الأوجه",

        "english" to "English",
        "arabic" to "العربية",
        "language" to "اللغة",
        "back" to "رجوع",

        "open_sld" to "فتح مصمم SLD",
        "engineering_workspace" to "مساحة العمل الهندسية",
        "select_workspace" to "اختر بيئة التصميم التي تريد العمل عليها",
        "no_active_project_message" to "أنشئ أو اختر مشروعًا أولًا."
    )

    fun get(
        key: String,
        language: AppLanguage
    ): String {
        val map =
            if (language == AppLanguage.ARABIC) {
                arabic
            } else {
                english
            }

        return map[key] ?: key
    }
}
