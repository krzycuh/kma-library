package pl.kmazurek.library.application.service

interface NotificationService {
    fun notifyUserRegistered(
        userName: String,
        userEmail: String,
    )
}
