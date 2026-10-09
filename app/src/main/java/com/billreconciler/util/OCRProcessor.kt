package com.billreconciler.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object BillOCRProcessor {

    private val recognizer by lazy {
        TextRecognition.getClient(
            ChineseTextRecognizerOptions.Builder().build()
        )
    }

    suspend fun recognizeFromBitmap(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        return recognize(image)
    }

    suspend fun recognizeFromUri(context: Context, uri: Uri): String {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("无法打开图片: $uri")
        val bitmap = BitmapFactory.decodeStream(inputStream)
            ?: throw IllegalArgumentException("无法解码图片: $uri")
        inputStream.close()
        return recognizeFromBitmap(bitmap)
    }

    suspend fun recognizeFromFile(file: File): String {
        val bitmap = BitmapFactory.decodeFile(file.absolutePath)
            ?: throw IllegalArgumentException("无法解码图片: ${file.absolutePath}")
        return recognizeFromBitmap(bitmap)
    }

    private suspend fun recognize(image: InputImage): String =
        suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    continuation.resume(visionText.text)
                }
                .addOnFailureListener { e ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }
        }

    fun close() {
        recognizer.close()
    }
}
