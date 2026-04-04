package com.example.speakerapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DeviceRegistration : Screen("device_registration")
    object ParentDashboard : Screen("parent_dashboard")
    object AlertsFeed : Screen("alerts_feed")
    object ChildMonitoring : Screen("child_monitoring")
    object SpeakerList : Screen("speaker_list")
    object SpeakerEnrollment : Screen("speaker_enrollment")
    object Settings : Screen("settings")
}
