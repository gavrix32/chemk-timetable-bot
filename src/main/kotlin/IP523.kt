import kotlinx.serialization.json.Json
import java.io.File
import java.util.Calendar

object IP523 {
    fun getStaticTimetable(dayOfWeek: String): String {
        val isAlternative = Calendar.getInstance().get(Calendar.WEEK_OF_YEAR) % 2 == 0

        var mainFile = File("ip5-23.json")
        if (!mainFile.exists()) {
            return "Не найден файл с постоянным расписанием."
        }
        val mainRasp: Map<String, List<String>> = Json.decodeFromString(mainFile.readText())
        println(mainRasp[dayOfWeek]!!.joinToString("\n"))

        var altFile = File("ip5-23_alt.json")
        if (!altFile.exists()) {
                return "Не найден файл с чередующимся расписанием."
        }
        val altRasp: Map<String, List<String>> = Json.decodeFromString(altFile.readText())

        if (isAlternative && altRasp[dayOfWeek] != null) {
            return altRasp[dayOfWeek]!!.joinToString("\n")
        }
        return mainRasp[dayOfWeek]!!.joinToString("\n")
    }
}