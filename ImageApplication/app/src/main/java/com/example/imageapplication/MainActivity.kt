package com.example.imageapplication

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.CAMERA
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
import androidx.navigation.NavController
import androidx.navigation.Navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.imageapplication.float.showToast
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavController
    private lateinit var navHostFragment: NavHostFragment
    private lateinit var loadImage: ActivityResultLauncher<String>
    private lateinit var cropImageLuncher: ActivityResultLauncher<Intent>
    private var uri: Uri? = null
    private lateinit var currentImagePath: String
    private var splitImage: Button? = null
    private var btnGallery: Button? = null

    private var sourceImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.activity_main)

//        val transaction = supportFragmentManager.beginTransaction()
//            .add(R.id.MainView,SecondFragment())
//        transaction.addToBackStack("")
//        transaction.commit()

        navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        //navController.navigate(R.id.action_MainActivity_to_secondFragment)


        btnGallery = findViewById(R.id.btn_image)
        sourceImage = findViewById(R.id.iv_souceImage)
        //splitImage = findViewById(R.id.btn_splitImage)

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

        loadImage = registerForActivityResult(ActivityResultContracts.GetContent()){
            if(it != null){
                Log.d("MainActivity","image data : $it and ${it.path}")

                if (getImageSize(it)!! <200){
                    sourceImage?.setImageURI(it)
                    cropImage(it)
                }else{
                    showMessageOKCancel("Your image is above 200kb",null)
                }
            }
        }

        btnGallery?.setOnClickListener {
            pickImageFromGallery()
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
    CAMERA
    )
    private fun hasExternalWritePermission() = ContextCompat.checkSelfPermission(this,
    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun hasExternalReadPermission() = ContextCompat.checkSelfPermission(this,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults.isNotEmpty()){
            val cameraAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED
            val readStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
            val writeExternalAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED

            if(cameraAccepted && writeExternalAccepted){
                Log.d("Permission","all permission are accepted")
                takePicture()
            }else if(readStorageAccepted){
                loadImage.launch("image/*")
            }else{
                //Snackbar.make(this,"Permission denied,",Snackbar.LENGTH_LONG)
                showToast("Permission denied")

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (shouldShowRequestPermissionRationale(ACCESS_FINE_LOCATION)) {
                        showMessageOKCancel("You need to allow access to both the permissions"
                        ) { _, _ ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(
                                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                        android.Manifest.permission.READ_EXTERNAL_STORAGE, CAMERA),
                                    1
                                )
                            }
                        }
                        return
                    }
                }

            }
        }
    }

    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener?) {
        AlertDialog.Builder(this@MainActivity)
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

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
            if (getImageSize(uri!!)!! <200){
                sourceImage?.setImageURI(uri)
                cropImage(uri)
            }else{
                showToast("Your image is above 200kb")
            }
            // The image was saved into the given Uri -> do something with it

        }else{
            Log.d("CameraImage","Image is not saved")

        }
    }

    private fun getImageSize(uri: Uri):Int? {
        return try {
            val aaa: InputStream? = this.contentResolver.openInputStream(uri)
            val byteSize: Int? = aaa?.available()
            val kbSize = byteSize?.div(1024)
            Log.d("CameraImage", "Image size :$kbSize")
            kbSize
        } catch (e: java.lang.Exception) {
            // here you can handle exception here
            null
        }
    }

    private fun cropImage(uri: Uri?) {
        val intent = Intent(this,MergedImage::class.java)
        intent.putExtra("DATA",uri?.toString())
        setResult(101,intent)
        cropImageLuncher.launch(intent)
    }

    private fun pickImageFromGallery() {


      val alertDialog = AlertDialog.Builder(this)
        alertDialog.setTitle("Select ")
        alertDialog.setPositiveButton("Pick Image"){ _, _ ->
            if(hasExternalReadPermission() != PackageManager.PERMISSION_GRANTED||
                hasExternalWritePermission() != PackageManager.PERMISSION_GRANTED ||
                hasExternalReadPermission() != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(
                    CAMERA,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE),2)
            }else{
                loadImage.launch("image/*")

            }
        }

        alertDialog.setNegativeButton("Camera"){ _, _ ->
            if(hasCameraPermission() != PackageManager.PERMISSION_GRANTED ||
                hasExternalWritePermission() != PackageManager.PERMISSION_GRANTED ||
                    hasExternalReadPermission() != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, arrayOf(CAMERA,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    android.Manifest.permission.READ_EXTERNAL_STORAGE),1)
            }else{

                takePicture()
            }
        }

        alertDialog.setNeutralButton("Cancel"){ _, _ ->

        }

        val builder = alertDialog.create()
        builder.show()

    }

}