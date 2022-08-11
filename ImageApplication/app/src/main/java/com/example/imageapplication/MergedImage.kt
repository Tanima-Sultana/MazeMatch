package com.example.imageapplication

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.material.snackbar.Snackbar
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.imageapplication.databinding.ActivityMergedImageBinding
import com.yalantis.ucrop.UCrop
import java.io.File
import java.lang.StringBuilder
import java.util.*

class MergedImage : AppCompatActivity() {
    var result: String? = null
    var fileUri: Uri? = null
    lateinit var binding: ActivityMergedImageBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMergedImageBinding.inflate(layoutInflater)
        setContentView(binding.root)
        readIntent()

        val destinationUri = StringBuilder(UUID.randomUUID().toString())
            .append(".jpg").toString()

        val options:UCrop.Options = UCrop.Options()

        fileUri?.let {
            UCrop.of(it,Uri.fromFile(File(cacheDir,destinationUri)))
                .withOptions(options)
                .withAspectRatio(0F, 0F)
                .useSourceImageAspectRatio()
                .withMaxResultSize(2000,2000)
                .start(this)
        }

    }

    private fun readIntent() {
        val intent = intent
        if(intent.extras != null){
            result = intent.getStringExtra("DATA")
            Log.d("MergedImage","intent result : $result")
            fileUri = Uri.parse(result)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP){
            val resultUri = data?.let { UCrop.getOutput(it) }
            val resultIntent = Intent()
            resultIntent.putExtra("RESULT","$resultUri")
            setResult(-1,resultIntent)
            finish()
        }else if(resultCode == UCrop.RESULT_ERROR){
            val cropError = data?.let { UCrop.getError(it) }
        }
    }
}