# Electrical Calculations Pro | حاسبة الحسابات الكهربائية برو

Professional Android application for electrical calculations  
تطبيق أندرويد احترافي للحسابات الكهربائية

## Supported Standards | المعايير المدعومة

- **IEC** 60364-5-52
- **الكود المصري** (Egyptian Code) – مبني على IEC
- CEI
- NEC
- CEC

## Features | المميزات

- تحديد مقطع الموصل + تنسيق أجهزة الحماية
- حساب هبوط الجهد
- حساب التيار / الجهد / القدرة
- واجهة داكنة احترافية
- **دعم اللغتين**: العربية + الإنجليزية (مع إمكانية التبديل من الواجهة)
- حسابات أوفلاين

## Language Support | دعم اللغة

يوجد زر تغيير اللغة في الشريط العلوي (أيقونة اللغة).  
يمكنك التبديل بين:

- العربية
- English

## Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Min SDK 26

## How to run

1. Clone the repository
2. Open in **Android Studio** (latest version recommended)
3. Sync Gradle
4. Run on emulator or real device

```bash
git clone https://github.com/Elghaty/ElectricalCalculationsPro.git
cd ElectricalCalculationsPro
```

## Project Structure

```
app/src/main/java/com/electrical/calculationspro/
├── MainActivity.kt
├── data/
│   ├── Models.kt
│   ├── Calculations.kt
│   └── Language.kt          ← الترجمة (عربي/إنجليزي)
├── ui/
│   ├── theme/
│   ├── components/
│   └── screens/
│       └── ConductorSizingScreen.kt
```

## Current Status

- ✅ UI مطابقة للتصميم المطلوب
- ✅ دعم اللغتين + تبديل اللغة
- ✅ IEC + الكود المصري
- ✅ حساب مقطع الكابل + هبوط الجهد + جهاز الحماية
- ⏳ جداول كاملة أكثر دقة
- ⏳ باقي الشاشات (Voltage Drop, Power, ...)

## Author

Created for Egyptian electrical engineers and technicians.
