package com.valerasetrakov.externalcamerasample

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Environment.MEDIA_MOUNTED
import android.provider.MediaStore
import androidx.annotation.CallSuper
import androidx.annotation.RequiresApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Интерфейс для предоставления расположения нового изображения
 */
interface PictureDestinationProvider {
    /**
     * @return content [Uri], который будет указывать на данные о фото в таблице
     */
    fun providePictureDestination(): Uri
}

/**
 * Реализация [PictureDestinationProvider], которая будет предоставлять [Uri] нового изображения в
 * таблице, предоставляемой через [pictureTableProvider] и с параметрами, генерируемыми через
 * [contentValuesGenerator]
 */
open class ContentResolverUriProvider(
    private val activityProvider: ActivityProvider,
    private val pictureTableProvider: PictureTableProvider = PictureTableProvider,
    private val contentValuesGenerator: PictureContentValuesGenerator = PictureContentValuesGenerator
): PictureDestinationProvider {

    private val currentActivity get() = activityProvider.get()

    override fun providePictureDestination(): Uri {
        val values = createContentValues()
        return currentActivity.contentResolver.insert(
            pictureTableProvider.providePictureTable(),
            values
        ) ?: error("Failed generate image uri for new photo")
    }

    /**
     * @return [ContentValues] для создания [Uri] для нового изображения
     */
    @CallSuper
    protected open fun createContentValues(): ContentValues {
        return contentValuesGenerator.generateContentValues()
    }
}

/**
 * Генератор значений для столбцов в таблице
 */
interface PictureContentValuesGenerator {

    @CallSuper
    fun generateContentValues(): ContentValues {
        return ContentValues()
    }

    companion object: PictureContentValuesGenerator
}

/**
 * Реализация [PictureContentValuesGenerator] для версий Android либо равных [Build.VERSION_CODES.Q],
 * либо последующих
 */
@RequiresApi(Build.VERSION_CODES.Q)
open class AfterQPictureContentValuesGenerator(
    private val pictureNameGenerator: PictureNameGenerator = PictureNameGenerator,
    private val pictureFolderGenerator: PictureFolderGenerator = PictureFolderGenerator
): PictureContentValuesGenerator {

    override fun generateContentValues(): ContentValues {
        return super.generateContentValues().apply {
            pictureNameGenerator.generatePictureName().takeIf { pictureName ->
                pictureName.isNotEmpty()
            }?.let {  pictureName ->
                put(MediaStore.Images.Media.DISPLAY_NAME, pictureName)
            }
            pictureFolderGenerator.generatePictureFolderPath().takeIf { pictureRelativePath ->
                pictureRelativePath.isNotEmpty()
            }?.let { pictureRelativePath ->
                put(MediaStore.Images.Media.RELATIVE_PATH, pictureRelativePath)
            }
        }
    }
}

/**
 * Реализация [BeforeQPictureContentValuesGenerator] для версий Android до [Build.VERSION_CODES.Q],
 * для обратной совместимости
 */
open class BeforeQPictureContentValuesGenerator(
    private val pictureNameGenerator: PictureNameGenerator = PictureNameGenerator,
    private val pictureFolderGenerator: PictureFolderGenerator = PictureFolderGenerator,
    protected val pictureBaseDirectoryProvider: PictureBaseDirectoryProvider = PictureBaseDirectoryProvider
): PictureContentValuesGenerator {

    override fun generateContentValues(): ContentValues {
        return super.generateContentValues().apply {
            val absolutePathToPicture = generateAbsolutePathToPicture()
            put(MediaStore.Images.Media.DATA, absolutePathToPicture)
        }
    }

    protected open fun generateAbsolutePathToPicture(): String {
        val pictureBaseDirectory = getBaseDirectory()
        val pictureRelativePath = pictureFolderGenerator.generatePictureFolderPath()
        val pictureName = pictureNameGenerator.generatePictureName()
        val pictureDirection = File(pictureBaseDirectory, pictureRelativePath)
        val pictureFile = File(pictureDirection, pictureName)
        return pictureFile.absolutePath
    }

    private fun getBaseDirectory(): File {
        val baseDirectory = pictureBaseDirectoryProvider.provideBaseDirectory()
        val externalStorageState = Environment.getExternalStorageState(baseDirectory)
        if (externalStorageState != MEDIA_MOUNTED) {
            error("Base directory for saving picture need to be external storage")
        }
        return baseDirectory
    }
}



/**
 * Интерфейс для генерации имени нового изображения
 */
interface PictureNameGenerator {
    /**
     * @return имя нового изображения
     */
    fun generatePictureName(): String {
        val date = Date()
        date.time = System.currentTimeMillis() + Random().nextInt(1000) + 1
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(date)
        return "IMG_$timeStamp.jpg"
    }

    companion object: PictureNameGenerator
}

/**
 * Интерфейс для генерации относительного пути до директории изображения
 */
interface PictureFolderGenerator {
    /**
     * @return относительный путь для нового изображения
     */
    fun generatePictureFolderPath(): String {
        return ""
    }

    companion object: PictureFolderGenerator
}

/**
 * Интерфейс для предоставления базой директории для нового изображения
 */
interface PictureBaseDirectoryProvider {

    fun provideBaseDirectory(): File {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    }

    companion object: PictureBaseDirectoryProvider
}

/**
 * Интерфейс для предоставления таблицы, в которой будет произведена запись о новом изображении
 */
interface PictureTableProvider {
    fun providePictureTable(): Uri {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    companion object: PictureTableProvider
}