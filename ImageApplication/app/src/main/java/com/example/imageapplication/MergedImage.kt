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

        val a = fileUri?.let {
            UCrop.of(it,Uri.fromFile(File(cacheDir,destinationUri)))
                .withOptions(options)
                .withAspectRatio(0F, 0F)
                .useSourceImageAspectRatio()
                .withMaxResultSize(2000,2000)
                .getIntent(this)
                //.start(this)
        }

        val b = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ result1 ->
            if( result1.resultCode == Activity.RESULT_OK ){
                val resultUri = result1.data?.let { UCrop.getOutput(it) }
                val resultIntent = Intent()
                resultIntent.putExtra("RESULT","$resultUri")
                setResult(-1,resultIntent)
                finish()
            }else if(result1.resultCode == UCrop.RESULT_ERROR){
                val cropError = result1.data?.let { UCrop.getError(it) }
            }
        }

        b.launch(a)

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