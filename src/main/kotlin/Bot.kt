package net.gavrix32

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.message.Message
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow
import java.io.File
import java.util.Calendar
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class Bot : LongPollingSingleThreadUpdateConsumer {
    private val logger = LoggerFactory.getLogger("Bot")
    private val telegramClient = OkHttpTelegramClient(System.getenv("BOT_TOKEN"))
    private lateinit var message: Message
    private var chatId = 0L
    private var database = mutableMapOf<Long, Userdata>()
    private var dataFile = File("database.json")

    init {
        loadData()
        var timer = Executors.newScheduledThreadPool(1)
        val task = Runnable {
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (currentHour in 12..17) {
                checkAndSendTimetableChanges()
            }
        }
        timer.scheduleAtFixedRate(task, 0, 30, TimeUnit.MINUTES)
    }

    private fun checkAndSendTimetableChanges() {
        logger.info("Checking the timetable for changes")
        database.forEach { (id, _) ->
            if (getOrPutData(id).notifyChanges) {
                var parserData = Parser.tomorrow(getOrPutData(id).building, getOrPutData(id).group)
                if (getOrPutData(id).tomorrowTimetable != parserData.second) {
                    getOrPutData(id).tomorrowTimetable = parserData.second
                    if (parserData.first == Status.SUCCESS) {
                        var msg = parserData.second.replace("Расписание", "Изменение в расписании")
                        val sendMessage = SendMessage.builder().chatId(id).text(msg).parseMode("HTML").build()
                        telegramClient.execute(sendMessage)
                        logCommands("", msg, false)
                    }
                }
            }
        }
        saveData()
    }

    private fun loadData() {
        if (dataFile.exists()) {
            val jsonString = dataFile.readText()
            val loadedDatabase: Map<Long, Userdata> = Json.decodeFromString(jsonString)
            database.putAll(loadedDatabase)
        }
    }

    private fun saveData() {
        val json = Json {
            prettyPrint = true
        }
        val jsonString = json.encodeToString(database)
        dataFile.writeText(jsonString)
    }

    private fun getOrPutData(chatId: Long): Userdata {
        return database.getOrPut(chatId) {
            Userdata()
        }
    }

    private fun startCmd(): String {
        val msg = "Напишите /setup чтобы выбрать корпус и группу."
        val sendMessage = SendMessage.builder().chatId(chatId).text(msg).parseMode("HTML").build()
        telegramClient.execute(sendMessage)
        return msg
    }

    private fun saveAndSend(parserData: Pair<Status, String>): String {
        if (parserData.first == Status.BUILDING_NOT_FOUND) return setupCmd()
        val msg = parserData.second
        getOrPutData(chatId).todayTimetable = msg
        val sendMessage = SendMessage.builder().chatId(chatId).text(msg).parseMode("HTML").build()
        telegramClient.execute(sendMessage)
        return msg
    }

    private fun todayCmd(): String {
        val parserData = Parser.today(getOrPutData(chatId).building, getOrPutData(chatId).group)
        return saveAndSend(parserData)
    }

    private fun tomorrowCmd(): String {
        val parserData = Parser.tomorrow(getOrPutData(chatId).building, getOrPutData(chatId).group)
        return saveAndSend(parserData)
    }

    private fun notifyChangesCmd(): String {
        getOrPutData(chatId).notifyChanges = !getOrPutData(chatId).notifyChanges
        val action = if (getOrPutData(chatId).notifyChanges) "включили" else "выключили"
        val msg = "Вы <b>$action</b> уведомления об изменениях в расписании."
        val sendMessage = SendMessage.builder().chatId(chatId).text(msg).parseMode("HTML").build()
        telegramClient.execute(sendMessage)
        return msg
    }

    private fun unknownCmd(): String {
        val msg = "Нет такой команды."
        val sendMessage = SendMessage(chatId.toString(), msg)
        telegramClient.execute(sendMessage)
        return msg
    }

    private fun logCommands(cmd: String, msg: String, printName: Boolean) {
        logger.info("----------------------------")
        if (printName) {
            logger.info("Message from ${message.chat.firstName} ${message.chat.lastName} (id = ${chatId})")
        } else {
            logger.info("Message from NO_NAME (id = ${chatId})")
        }
        logger.info("Message text: $cmd")
        logger.info("Bot answer:\n$msg")
    }

    private fun buildingMenuKeyboardRow(): InlineKeyboardRow {
        return InlineKeyboardRow(
            InlineKeyboardButton.builder().text("1").callbackData("building_1").build(),
            InlineKeyboardButton.builder().text("2").callbackData("building_2").build(),
            InlineKeyboardButton.builder().text("3").callbackData("building_3").build(),
            InlineKeyboardButton.builder().text("4").callbackData("building_4").build(),
            InlineKeyboardButton.builder().text("5").callbackData("building_5").build()
        )
    }

    private fun setupCmd(): String {
        val msg = "Выберите корпус:"
        val sendMessage = SendMessage.builder().chatId(chatId).text(msg).replyMarkup(
            InlineKeyboardMarkup.builder().keyboardRow(buildingMenuKeyboardRow()).build()
        ).build()
        telegramClient.execute(sendMessage)
        return msg
    }

    private fun currentGroupCmd(): String {
        val msg = "Текущая группа: <b>${getOrPutData(chatId).group}</b>."
        val sendMessage = SendMessage.builder().chatId(chatId).text(msg).parseMode("HTML").build()
        telegramClient.execute(sendMessage)
        getOrPutData(chatId).waitingGroupMsg = false
        return msg
    }

    private fun sourceCmd(): String {
        val msg = "Исходный код на <b>GitHub</b>: https://github.com/gavrix32/chemk-timetable-bot"
        val sendMessage = SendMessage.builder().chatId(chatId).text(msg).parseMode("HTML").build()
        telegramClient.execute(sendMessage)
        return msg
    }

    override fun consume(up: Update) {
        message = up.message
        if (up.hasMessage() && message.hasText()) {
            chatId = message.chatId
            val cmd = message.text.split("@")[0]
            var msg = when (cmd) {
                "/start" -> startCmd()
                "/setup" -> setupCmd()
                "/today" -> todayCmd()
                "/tomorrow" -> tomorrowCmd()
                "/notify_changes" -> notifyChangesCmd()
                "/source" -> sourceCmd()
                else -> {
                    if (getOrPutData(chatId).waitingGroupMsg) {
                        getOrPutData(chatId).group = cmd
                        getOrPutData(chatId).todayTimetable = Parser.today(getOrPutData(chatId).building, getOrPutData(chatId).group).second
                        getOrPutData(chatId).tomorrowTimetable = Parser.tomorrow(getOrPutData(chatId).building, getOrPutData(chatId).group).second
                        currentGroupCmd()
                    } else {
                        unknownCmd()
                    }
                }
            }
            logCommands(cmd, msg, true)
        } else if (up.hasCallbackQuery()) {
            message = up.callbackQuery.message as Message
            chatId = up.callbackQuery.message.chatId
            var callbackData = up.callbackQuery.data

            if (callbackData.startsWith("building_")) {
                getOrPutData(chatId).building = callbackData.substringAfter("building_").toInt()
                val msg = "Отправьте название группы в ответ на это сообщение, соблюдая регистр.\n\nНапример: Ип5-23"
                val editMsgText = EditMessageText
                    .builder()
                    .chatId(chatId)
                    .messageId(message.messageId)
                    .text(msg)
                    .build()
                telegramClient.execute(editMsgText)
                getOrPutData(chatId).waitingGroupMsg = true
                logCommands("*Pressed button $callbackData*", msg, true)
            }
        }
        saveData()
    }
}

fun main() {
    val botsApplication = TelegramBotsLongPollingApplication()
    botsApplication.registerBot(System.getenv("BOT_TOKEN"), Bot())
}