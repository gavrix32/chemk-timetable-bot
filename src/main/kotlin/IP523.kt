package net.gavrix32

import java.util.Calendar

object IP523 {
    fun getStaticTimetable(dayOfWeek: String): String {
        val alternatingPair = if (Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 2 == 0) {
            "4. История, Кузьмина Л.Г., 101О"
        } else {
            "4. ОСиС, Ильина Н.В., 104М"
        }
        when (dayOfWeek) {
            "понедельник" -> return """
                1. Дискретная математика, Савинова Е.А., 210М
                2. ИТ, Васильева К.В., 205М
            """.trimIndent()
            "вторник" -> return """
                1. Дискретная математика, Савинова Е.А., 210М
                2. ОСиС, Ильина Н.В., 104М
                3. Иностранный язык, Федорова Т.В., 103У
                $alternatingPair
            """.trimIndent()
            "среду" -> return """
                1.
                2.
                3. ЭВМ, Баранова О.Б., 2 корпус, 220
                4. ОСиС, Ильина Н.В., 2 корпус, 227
            """.trimIndent()
            "четверг" -> return """
                1.
                2.
                3. ОАиП, Мелешкина Е.В., 206М
                4. ИТ, Васильева К.В., 206М
                5. ИТ, Васильева К.В., 206М
            """.trimIndent()
            "пятницу" -> return """
                1. История, Кузьмина Л.Г., 101О
                2. ОАиП, Мелешкина Е.В., 206М
            """.trimIndent()
            "субботу" -> return """
                1. ЭВМ, Баранова О.Б., 304У
                2. Русский язык, Клементьева С.В., 202У
                3. Физкультура, Морозов А.В., с/з
            """.trimIndent()
            else -> return "Неизвестный день недели: $dayOfWeek"
        }
    }
}