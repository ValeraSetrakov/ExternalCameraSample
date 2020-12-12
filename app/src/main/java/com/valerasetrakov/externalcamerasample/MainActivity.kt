package com.valerasetrakov.externalcamerasample

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*

private const val CAMERA_RESULT_CODE = 10
private const val WRITE_EXTERNAL_STORAGE_CODE = 10

class MainActivity : AppCompatActivity() {

    private var photoUri = Uri.EMPTY
    var pictureDestinationProvider: PictureDestinationProvider? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        pictureDestinationProvider = ContentResolverUriProvider(
            contentResolver = contentResolver
        )
        start_camera_btn.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_EXTERNAL_STORAGE_CODE)
            }
        }
        delete_photo_btn.setOnClickListener {
            deleteUri(photoUri)
        }
    }

    private fun startCamera() {
        photoUri = createImageUri()
        startActivityForResult(
                Intent.createChooser(
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                            putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        },
                        applicationInfo.name
                ),
                CAMERA_RESULT_CODE
        )
    }

    private fun createImageUri(): Uri? {
        val newImageUri = pictureDestinationProvider?.provideDestination()
        return newImageUri
    }

    private fun deleteUri(uri: Uri) {
        contentResolver.delete(uri, null)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            CAMERA_RESULT_CODE -> {
                when(resultCode) {
                    Activity.RESULT_OK -> {
                        contentResolver.query(photoUri, null, null, null, null)?.use {
                            it.moveToFirst()
                            val data = it.getString(
                                it.getColumnIndex(
                                    MediaStore.Images.ImageColumns.DATA
                                )
                            )
                            Log.d(MainActivity::class.simpleName, "file path $data")
                        }
                    }
                    else -> {
                        deleteUri(photoUri)
                    }
                }
            }
        }
        if (resultCode != Activity.RESULT_OK) return
    }
}