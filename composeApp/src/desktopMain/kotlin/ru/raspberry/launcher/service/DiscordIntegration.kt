package ru.raspberry.launcher.service

import de.jcm.discordgamesdk.Core
import de.jcm.discordgamesdk.CreateParams
import de.jcm.discordgamesdk.activity.Activity
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.util.Date
import kotlin.concurrent.thread


class DiscordIntegration {
    private var params: CreateParams = CreateParams()
    private var core: Core? = null
    private var activity: Activity? = null
    private var working = false

    constructor() {
        try {
            params.setClientID(1234567890123456789L)
            params.setFlags(CreateParams.getDefaultFlags())

            core = Core(params)
            activity = Activity()
            activity?.timestamps()?.start = Date().toInstant()
            activity?.assets()?.largeImage = "icon"
        } catch (_: Exception) {
            println("Discord SDK not found, rich presence will not be available.")

        }
    }

    fun enableRichPresence() {
        core?.activityManager()?.updateActivity(activity)
    }
    fun disableRichPresence() {
        core?.activityManager()?.clearActivity()
    }

    var details: String
        get() = activity?.details ?: ""
        set(value) {
            activity?.details = value
            core?.activityManager()?.updateActivity(activity)
        }
    var state: String
        get() = activity?.state ?: ""
        set(value) {
            activity?.state = value
            core?.activityManager()?.updateActivity(activity)
        }


    fun start() {
        working = true
        thread {
            runBlocking {
                while (working) run()
            }
        }
    }

    fun stop() {
        working = false
    }



    private suspend fun run() {
        try {
            core?.runCallbacks()
            delay(10)
        } catch (_: Exception) {}
    }
}