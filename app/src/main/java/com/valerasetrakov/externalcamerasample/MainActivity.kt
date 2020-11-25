package com.valerasetrakov.externalcamerasample

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import kotlinx.android.synthetic.main.activity_main.*

private const val CAMERA_RESULT_CODE = 10

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        start_camera_btn.setOnClickListener {
            startCamera()
        }
    }

    private fun startCamera() {
        startActivityForResult(
                Intent.createChooser(
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                        applicationInfo.name
                ),
                CAMERA_RESULT_CODE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK) return
        when(requestCode) {
            CAMERA_RESULT_CODE -> {
                Log.d(MainActivity::class.simpleName, data?.data?.toString() ?: "")
            }
        }
    }
}