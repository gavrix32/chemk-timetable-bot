package net.gavrix32

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.io.IOException

object Parser {
    val logger = LoggerFactory.getLogger(Parser.javaClass)

    private fun parse(building: Int, group: String, day: String): Pair<Status, String> {
        if (building !in 1..5) {
            logger.error("$building building not found")
            return Pair(Status.BUILDING_NOT_FOUND, "$building корпус не найден.")
        }
        if (day != "today" && day != "tomorrow") {
            logger.error("Unknown day $day")
            return Pair(Status.UNKNOWN_DAY, "Неизвестный день $day.")
        }
        var document = Document("")
        try {
            document = Jsoup.connect("https://rsp.chemk.org/${building}korp/${day}.htm").get()
        } catch (e: IOException) {
            return Pair(Status.CONNECTION_ERROR, "Не удалось получить данные с сайта:\n\n$e")
        }
        val table = document.selectFirst("table") ?: run {
            logger.error("Table not found")
            return Pair(Status.TABLE_NOT_FOUND, "Таблица не найдена.")
        }

        var dayOfWeek = ""
        var timetable = ""
        var groupRow = -1
        var groupColumn = -1
        var groupFound = false

        val rows = table.select("tr")
        for (i in rows.indices) {
            val columns = rows[i].select("td")
            for (j in columns.indices) {
                var text = columns[j].text().lowercase()
                if (dayOfWeek.isEmpty()) {
                    dayOfWeek = when {
                        text.contains("понедельник") -> "понедельник"
                        text.contains("вторник") -> "вторник"
                        text.contains("среда") -> "среду"
                        text.contains("четверг") -> "четверг"
                        text.contains("пятница") -> "пятницу"
                        text.contains("суббота") -> "субботу"
                        else -> ""
                    }
                }
                if (columns[j].text().contains(group)) {
                    groupRow = i
                    groupColumn = j
                }
            }
            for (k in rows.indices) {
                if (k == groupRow && columns.size >= groupColumn + 2) {
                    val groupName = columns[groupColumn].text()
                    val pairNum = columns[groupColumn + 1].text()
                    val pairName = columns[groupColumn + 2].text()
                    if (groupName.equals(group)) {
                        groupFound = true
                    }
                    if (groupFound && !pairNum.isEmpty() && (groupName.equals(group) || groupName.isEmpty())) {
                        timetable += "$pairNum. $pairName\n"
                    } else {
                        groupFound = false
                    }
                }
            }
        }
        // Удаление пар содержащих "нет"
        val filteredTimetableLines = timetable.lines().filter { !it.contains("нет") }
        timetable = filteredTimetableLines.joinToString("\n")

        // Если группа Ип5-23 и её нет в таблице то вывести постоянное расписание
        if (groupRow == -1 && group == "Ип5-23") {
            // удаление путсых пар из постоянной таблицы
            timetable = IP523.getStaticTimetable(dayOfWeek).lines()
                .filter { it.isNotBlank() && !it.trim().endsWith(".") }
                .joinToString("\n")
            return Pair(Status.SUCCESS_STATIC, "Расписание на <b>$dayOfWeek</b> (постоянное):\n\n$timetable")
        }
        if (groupRow == -1) {
            logger.error("Group $group not found on day $day")
            return Pair(Status.GROUP_NOT_FOUND, "Группа $group не найдена.")
        }

        // 1,2,3 -> 1\n2\n3
        val timetableFlatMap = timetable.lines().flatMap { line ->
            if (line.contains(".")) {
                val (pairNum, info) = line.split(". ", limit = 2)
                pairNum.split(",").map { "$it. $info" }
            } else {
                listOf(line)
            }
        }
        timetable = timetableFlatMap.joinToString("\n")

        // Замена "по расписанию" на постоянное расписание
        if (group == "Ип5-23") {
            val staticTimetableLines = IP523.getStaticTimetable(dayOfWeek).lines()
            val timetableLines = timetable.lines().toMutableList()
            for (i in timetableLines.indices) {
                if (timetableLines[i].lowercase().contains("по расписанию")) {
                    if (i < staticTimetableLines.size) {
                        timetableLines[i] = staticTimetableLines[timetableLines[i][0].digitToInt()-1]
                    }
                }
            }
            timetable = timetableLines.joinToString("\n")
        }

        // Сортировка по номеру пары
        timetable = timetable.lines()
            .sortedBy { line ->
                line.substringBefore('.').toIntOrNull() ?: Int.MAX_VALUE
            }
            .joinToString("\n")

        return Pair(Status.SUCCESS, "Расписание на <b>$dayOfWeek</b> (по замене):\n\n$timetable")
    }

    fun today(building: Int, group: String): Pair<Status, String> {
        return parse(building, group, "today")
    }

    fun tomorrow(building: Int, group: String): Pair<Status, String> {
        return parse(building, group, "tomorrow")
    }
}