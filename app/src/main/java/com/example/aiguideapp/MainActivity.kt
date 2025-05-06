package com.example.aiguideapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity(), TextToSpeech.OnInitListener {
    companion object {
        private const val REQUEST_RECORD_AUDIO = 200
    }

    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String = ""
    private lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
    }

    // TTS 초기화 완료 콜백
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.KOREAN
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String) {}
                override fun onError(utteranceId: String) {}
                override fun onDone(utteranceId: String) {
                    if (utteranceId == "INIT") {
                        runOnUiThread { checkPermissionsAndStart() }
                    }
                }
            })
            val params = Bundle().apply { putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "INIT") }
            tts.speak("앱이 시작되었습니다. 말씀해 주세요.", TextToSpeech.QUEUE_FLUSH, params, "INIT")
        } else {
            Toast.makeText(this, "TTS 초기화 실패", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkPermissionsAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO
                )
            } else {
                startRecording()
            }
        } else {
            startRecording()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(this, "녹음 권한을 허용해야 앱을 사용할 수 있습니다.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startRecording() {
        try {
            val outputDir = externalCacheDir?.absolutePath ?: cacheDir.absolutePath
            audioFilePath = "$outputDir/recorded_audio.3gp"
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            Toast.makeText(this, "녹음 시작", Toast.LENGTH_SHORT).show()
            Handler(Looper.getMainLooper()).postDelayed({ stopRecording() }, 5000) //5초 녹음 후 stopRecording() 자동 호출
        } catch (e: Exception) {
            Log.e("UPLOAD", "startRecording 실패", e)
        }
    }

    private fun stopRecording() { //서버에 전송
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            Toast.makeText(this, "녹음 완료. 서버에 전송합니다.", Toast.LENGTH_SHORT).show()
            Log.d("UPLOAD", "stopRecording() 호출됨. 파일: $audioFilePath")
            sendAudioFileToServer(audioFilePath)
        } catch (e: Exception) {
            Log.e("UPLOAD", "stopRecording 실패", e)
        }
    }

    private fun sendAudioFileToServer(filePath: String) {
        Log.d("UPLOAD", "서버 전송 시작")
        val audioFile = File(filePath)
        if (!audioFile.exists()) {
            Log.e("UPLOAD", "파일이 존재하지 않습니다: $filePath")
            return
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .build()
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                audioFile.name,
                RequestBody.create("audio/3gp".toMediaTypeOrNull(), audioFile)
            )
            .build()
        val request = Request.Builder()
            .url("http://192.168.1.4:5000/upload")
            .post(requestBody)
            .build()
        Thread {
            try {
                val response = client.newCall(request).execute()
                val raw = response.body?.string().orEmpty()
                Log.d("UPLOAD", "서버 원응답: $raw")
                if (response.isSuccessful) {
                    val json = JSONObject(raw)
                    val target = json.optString("target", "")
                    val destination = json.optString("destination", "")
                    runOnUiThread {
                        speak("타겟은: $target")
                        if (destination.isNotBlank()) speak("목적지는: $destination")
                    }
                } else {
                    runOnUiThread { speak("서버 에러가 발생했습니다.") }
                }
            } catch (e: IOException) {
                runOnUiThread { speak("서버와 통신에 실패했습니다.") }
            }
        }.start()
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_ADD, null, null)
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
    }
}
