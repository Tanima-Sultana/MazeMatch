package com.example.imageapplication

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.imageapplication.databinding.ActivityMergedImageBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.util.*

class MergedImage : AppCompatActivity() {
    private var result: String? = null
    private var fileUri: Uri? = null
    private lateinit var binding: ActivityMergedImageBinding


    override fun onDestroy() {
        super.onDestroy()
        Log.d("MergedImage","destroy")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ActivityMergedImageBinding.inflate(layoutInflater).also { binding = it }
        setContentView(binding.root)
        readIntent()

        val destinationUri = StringBuilder(UUID.randomUUID().toString())
            .append(".jpg").toString()

        val options:UCrop.Options = UCrop.Options()
        //options.setCircleDimmedLayer(true)

        val croppedIntent = fileUri?.let {
            UCrop.of(it,Uri.fromFile(File(cacheDir,destinationUri)))
                .withOptions(options)
                .withAspectRatio(0F, 0F)
                .useSourceImageAspectRatio()
                .withMaxResultSize(2000,2000)
                .getIntent(this)
        }

        val cropLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result1 ->
            when (result1.resultCode) {
                Activity.RESULT_OK -> {
                    val resultUri = result1.data?.let { UCrop.getOutput(it) }
                    val resultIntent = Intent()
                    resultIntent.putExtra("RESULT","$resultUri")
                    setResult(-1,resultIntent)
                    finish()
                }
                UCrop.RESULT_ERROR -> {
                    val cropError = result1.data?.let { UCrop.getError(it) }
                    finish()
                }
                else -> {
                    finish()
                    Log.d("MergedImage","error, ${result1.resultCode}")
                }
            }
        }

        cropLauncher.launch(croppedIntent)

    }

    private fun readIntent() {
        val intent = intent
        if(intent.extras != null){
            result = intent.getStringExtra("DATA")
            Log.d("MergedImage","intent result : $result")
            fileUri = Uri.parse(result)
        }
    }

}