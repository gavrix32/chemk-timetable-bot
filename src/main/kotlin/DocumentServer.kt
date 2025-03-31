package net.gavrix32

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.slf4j.LoggerFactory
import java.io.IOException

object DocumentServer {
    val logger = LoggerFactory.getLogger(DocumentServer.javaClass)
    val tables = arrayOfNulls<Document>(10) as Array<Document>

    fun update() {
        for (building in 0..4) {
            for (day in 0..1) {
                val dayTxt = if (day == 0) "today" else "tomorrow"
                try {
                    tables[2 * building + day] = Jsoup.connect("https://rsp.chemk.org/${building + 1}korp/${dayTxt}.htm").get()
                } catch (e: IOException) {
                    return logger.error("Failed to get data from server (building = ${building + 1}, day = $dayTxt):\n$e")
                }
            }
        }
    }

    fun get(building: Int, day: String): Document {
        val dayNum = if (day == "today") 0 else 1
        return tables[2 * building + dayNum - 2]
    }
}