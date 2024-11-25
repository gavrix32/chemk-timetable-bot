package net.gavrix32

import kotlinx.serialization.Serializable

@Serializable
data class Userdata(
    var building: Int = 0,
    var group: String = "",
    var waitingGroupMsg: Boolean = false,
    var todayTimetable: String = "",
    var tomorrowTimetable: String = "",
    var notifyChanges: Boolean = false
)