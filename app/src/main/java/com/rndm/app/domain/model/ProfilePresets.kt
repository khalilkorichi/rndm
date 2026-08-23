package com.rndm.app.domain.model

object ProfilePresets {

    /**
     * قائمة الأندية الثمانية الأساسية
     */
    val DEFAULT_TOP_CLUBS = listOf(
        "ريال مدريد",
        "برشلونة",
        "مانشستر سيتي",
        "باريس سان جيرمان",
        "ليفربول",
        "آرسنال",
        "إنتر ميلان",
        "أتلتيكو مدريد"
    )

    /**
     * قائمة اقتراحات لأندية عالمية وعربية إضافية
     */
    val SUGGESTED_CLUBS = listOf(
        "بايرن ميونخ",
        "يوفنتوس",
        "تشيلسي",
        "ميلان",
        "مانشستر يونايتد",
        "بوروسيا دورتموند",
        "باير ليفركوزن",
        "توتنهام",
        "نابولي",
        "روما",
        "نيوكاسل",
        "أستون فيلا",
        "إشبيلية",
        "بنفيكا",
        "سبورتينغ لشبونة",
        "بورتو",
        "أياكس",
        "الهلال",
        "النصر",
        "الاتحاد",
        "الأهلي المصري",
        "الترجي التونسي",
        "الوداد المغربي"
    )

    /**
     * قائمة أقوى 10 منتخبات في العالم
     */
    val DEFAULT_TOP_NATIONAL_TEAMS = listOf(
        "فرنسا",
        "الأرجنتين",
        "إسبانيا",
        "إنجلترا",
        "البرتغال",
        "ألمانيا",
        "بلجيكا",
        "هولندا",
        "البرازيل",
        "إيطاليا"
    )

    /**
     * قائمة اقتراحات لمنتخبات دولية وعربية إضافية
     */
    val SUGGESTED_NATIONAL_TEAMS = listOf(
        "كرواتيا",
        "المغرب",
        "السعودية",
        "الجزائر",
        "مصر",
        "تونس",
        "اليابان",
        "كوريا الجنوبية",
        "أوروغواي",
        "كولومبيا",
        "سويسرا",
        "الدنمارك",
        "السنغال",
        "قطر",
        "المكسيك",
        "الولايات المتحدة",
        "تركيا",
        "ساحل العاج"
    )

    /**
     * قائمة الأشخاص / اللاعبين الأساسية
     */
    val DEFAULT_PLAYERS = listOf(
        "خليل",
        "عبدو",
        "ديدو",
        "عزيز",
        "قويدر",
        "ربيح",
        "سيف",
        "انور",
        "ريان",
        "اسامة"
    )

    /**
     * توليد أسماء افتراضية للاعبين
     */
    fun generateSamplePlayers(count: Int = 10): List<String> {
        if (count <= DEFAULT_PLAYERS.size) {
            return DEFAULT_PLAYERS.take(count)
        }
        return DEFAULT_PLAYERS + (DEFAULT_PLAYERS.size + 1..count).map { "لاعب $it" }
    }

    /**
     * إنشاء البروفايلات الافتراضية الجاهزة للتطبيق
     */
    fun createDefaultInitialProfiles(): List<Profile> {
        val now = System.currentTimeMillis()
        return listOf(
            Profile(
                id = 0L,
                name = "أقوى الأندية الأوروبية",
                type = ProfileType.CLUBS,
                items = DEFAULT_TOP_CLUBS.mapIndexed { index, name ->
                    ProfileItem(id = 0L, label = name, order = index)
                },
                createdAt = now
            ),
            Profile(
                id = 0L,
                name = "أقوى 10 منتخبات عالمية",
                type = ProfileType.NATIONAL_TEAMS,
                items = DEFAULT_TOP_NATIONAL_TEAMS.mapIndexed { index, name ->
                    ProfileItem(id = 0L, label = name, order = index)
                },
                createdAt = now + 1
            ),
            Profile(
                id = 0L,
                name = "دوري الأصدقاء (أشخاص)",
                type = ProfileType.PLAYERS,
                items = DEFAULT_PLAYERS.mapIndexed { index, name ->
                    ProfileItem(id = 0L, label = name, order = index)
                },
                createdAt = now + 2
            )
        )
    }
}
