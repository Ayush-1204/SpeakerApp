package com.example.speakerapp.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object DeviceRegistration : Screen("device_registration")
    object ParentDashboard : Screen("parent_dashboard")
    object ChildMonitoring : Screen("child_monitoring")
    object SpeakerList : Screen("speaker_list")
    object SpeakerEnrollment : Screen("speaker_enrollment")
}
