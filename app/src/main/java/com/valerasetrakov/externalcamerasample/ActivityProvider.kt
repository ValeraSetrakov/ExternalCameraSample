package com.valerasetrakov.externalcamerasample

import androidx.appcompat.app.AppCompatActivity

class ActivityProvider (private val activity: AppCompatActivity) {
    fun get(): AppCompatActivity { return activity }
}