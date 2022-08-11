package com.example.imageapplication

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.imageapplication.float.SimpleFloatingWindow
import com.example.imageapplication.float.canDrawOverlays
import com.example.imageapplication.float.showToast
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var cropImageLuncher: ActivityResultLauncher<Intent>
    private lateinit var floatImage: ActivityResultLauncher<String>
    private lateinit var simpleFloatingWindow: SimpleFloatingWindow
    private var uri: Uri? = null
    private lateinit var currentImagePath: String
    private var split_image: Button? = null
    private var btnGallery: Button? = null

    private var sourceImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)
        btnGallery = findViewById(R.id.btn_image)
        sourceImage = findViewById(R.id.iv_souceImage)
        split_image = findViewById(R.id.btn_splitImage)


        simpleFloatingWindow = SimpleFloatingWindow(applicationContext)
//        if ()
//
//        split_image?.setOnClickListener {
//           cropImage()
//
//        }


        cropImageLuncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            if(it.resultCode == Activity.RESULT_OK ){
                val result = it.data?.getStringExtra("RESULT")
                var resultUri:Uri? = null
                if (result != null){
                    resultUri = Uri.parse(result)
                }

                sourceImage?.setImageURI(resultUri)
            }
        }

        val loadImage = registerForActivityResult(ActivityResultContracts.GetContent()){
            if(it != null){
                Log.d("MainActivity","image data : $it and ${it.path}")
                sourceImage?.setImageURI(it)
                cropImage(it)
            }
        }

        floatImage = registerForActivityResult(ActivityResultContracts.GetContent()){
            if(it != null){
                if(canDrawOverlays){
                    simpleFloatingWindow.show()
                }else{
                    showToast("Permission is required")
                }
            }
        }

        btnGallery?.setOnClickListener {
            pickImageFromGallery(loadImage)
        }
    }


    private fun createImageFile():File{
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageDirectory = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("Specimen_${timestamp}",".jpg",imageDirectory).apply {
            currentImagePath = absolutePath
        }
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(this,
    android.Manifest.permission.CAMERA)
    private fun hasExternalWritePermission() = ContextCompat.checkSelfPermission(this,
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun hasExternalReadPermission() = ContextCompat.checkSelfPermission(this,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private fun takePicture(){

        createImageFile().also {
            try {
                uri = FileProvider.getUriForFile(
                    applicationContext,BuildConfig.APPLICATION_ID+".provider",it)
            }
            catch (e:Exception){
                Log.e("CameraImage","Error: ${e.message}")
            }
        }

        Log.d("CameraImage", "uri is :$uri")
        takePicture.launch(uri)

    }


    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            Log.d("CameraImage","Image location :$uri")
            // The image was saved into the given Uri -> do something with it
            sourceImage?.setImageURI(uri)
            cropImage(uri)
            //Picasso.get().load(viewModel.profileImageUri).resize(800,800).into(registerImgAvatar)
        }else{
            Log.d("CameraImage","Image is not saved")

        }
    }

    private fun cropImage(uri: Uri?) {
        val intent = Intent(this,MergedImage::class.java)
        intent.putExtra("DATA",uri?.toString())
        setResult(101,intent)
        cropImageLuncher.launch(intent)
        //startActivityForResult(intent,101)
    }

    private fun pickImageFromGallery(loadImage: ActivityResultLauncher<String>) {


      val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Select ")
        alertDialog.setPositiveButton("Pick Image"){dialogueInterface, which ->
            if(hasExternalReadPermission() != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE),2)
            }else{
                loadImage.launch("image/*")

            }
        }

        alertDialog.setNegativeButton("Camera"){dialogueInterface, which ->
            if(hasCameraPermission() != PackageManager.PERMISSION_GRANTED ||
                hasExternalWritePermission() != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE),1)
            }else{

                takePicture()
            }
        }

        alertDialog.setNeutralButton("Cancel"){dialogueInterface, which ->

        }

        val builder = alertDialog.create()
        builder.show()

    }

}