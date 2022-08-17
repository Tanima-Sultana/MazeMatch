package com.example.imageapplication

import android.Manifest
import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.imageapplication.databinding.FragmentImageUploadBinding
import java.io.File
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*


class ImageUploadFragment : Fragment() {

    private lateinit var binding: FragmentImageUploadBinding

    private lateinit var loadImage: ActivityResultLauncher<String>
    private lateinit var cropImageLuncher: ActivityResultLauncher<Intent>
    private var uri: Uri? = null
    private lateinit var currentImagePath: String
    private var btnGallery: Button? = null

    private var sourceImage: ImageView? = null


    companion object {
        var PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentImageUploadBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnGallery = binding.btnImage
        sourceImage = binding.ivSouceImage

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


    private fun cropImage(uri: Uri?) {
        val intent = Intent(requireContext(),MergedImage::class.java)
        intent.putExtra("DATA",uri?.toString())
        requireActivity().setResult(101,intent)
        //setResult(101,intent)
        cropImageLuncher.launch(intent)
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            Log.d("CameraImage","Image location :$uri")
            if (getImageSize(uri!!)!! <200){
                sourceImage?.setImageURI(uri)
                cropImage(uri)
            }else{
                Toast.makeText(requireContext(),"Your image is above 200kb",
                Toast.LENGTH_SHORT).show()
                //showToast("Your image is above 200kb")
            }
            // The image was saved into the given Uri -> do something with it

        }else{
            Log.d("CameraImage","Image is not saved")

        }
    }



    private fun takePicture(){

        createImageFile().also {
            try {
                uri = FileProvider.getUriForFile(
                    requireContext(),BuildConfig.APPLICATION_ID+".provider",it)
            }
            catch (e:Exception){
                Log.e("CameraImage","Error: ${e.message}")
            }
        }

        Log.d("CameraImage", "uri is :$uri")
        takePicture.launch(uri)

    }

    val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()) { permission ->
        val granted = permission.entries.all {
            it.value
        }
        if(granted) {
            loadImage.launch("image/*")

            //pickImageFromGallery()
        }
    }

    val requestSinglePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it){
            takePicture()
        }
    }



        private fun pickImageFromGallery() {


            val alertDialog = AlertDialog.Builder(requireContext())
            alertDialog.setTitle("Select ")
            alertDialog.setPositiveButton("Pick Image"){ _, _ ->
                if( hasExternalReadPermission() != PackageManager.PERMISSION_GRANTED){
//                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(
//                        Manifest.permission.CAMERA,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE),2)
                    requestSinglePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

                }else{
                    loadImage.launch("image/*")

                }
            }

            alertDialog.setNegativeButton("Camera"){ _, _ ->
                if(hasCameraPermission() != PackageManager.PERMISSION_GRANTED ||
                    hasExternalWritePermission() != PackageManager.PERMISSION_GRANTED ){
                    requestMultiplePermissions.launch(PERMISSIONS)

//                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(
//                        Manifest.permission.CAMERA,
//                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                        Manifest.permission.READ_EXTERNAL_STORAGE),1)
                }else{

                    takePicture()
                }
            }

            alertDialog.setNeutralButton("Cancel"){ _, _ ->

            }

            val builder = alertDialog.create()
            builder.show()

        }



    private fun showMessageOKCancel(message: String, okListener: DialogInterface.OnClickListener?) {
        AlertDialog.Builder(requireContext())
            .setMessage(message)
            .setPositiveButton("OK", okListener)
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(requireContext(),
        Manifest.permission.CAMERA
    )
    private fun hasExternalWritePermission() = ContextCompat.checkSelfPermission(requireContext(),
        Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun hasExternalReadPermission() = ContextCompat.checkSelfPermission(requireContext(),
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageDirectory = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("Specimen_${timestamp}",".jpg",imageDirectory).apply {
            currentImagePath = absolutePath
        }
    }

//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if(grantResults.isNotEmpty()){
//            val cameraAccepted = grantResults[0]== PackageManager.PERMISSION_GRANTED
//            val readStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
//            val writeExternalAccepted = grantResults[2] == PackageManager.PERMISSION_GRANTED
//
//            if(cameraAccepted && writeExternalAccepted){
//                Log.d("Permission","all permission are accepted")
//                takePicture()
//            }else if(readStorageAccepted){
//                loadImage.launch("image/*")
//            }else{
//                Snackbar.make(requireView(),"Permission denied,",Snackbar.LENGTH_LONG).show()
//                //showToast("Permission denied")
//
//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                    if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
//                        showMessageOKCancel("You need to allow access to both the permissions"
//                        ) { _, _ ->
//                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                                requestPermissions(
//                                    arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
//                                        Manifest.permission.CAMERA
//                                    ),
//                                    1
//                                )
//                            }
//                        }
//                        return
//                    }
//                }
//
//            }
//        }
//
//    }

    private fun getImageSize(uri: Uri):Int? {
        return try {
            val aaa:InputStream? = this@ImageUploadFragment.context?.contentResolver?.openInputStream(uri)
            //val aaa: InputStream = requireContext().contentResolver.openInputStream(uri)
            val byteSize: Int? = aaa?.available()
            val kbSize = byteSize?.div(1024)
            Log.d("CameraImage", "Image size :$kbSize")
            kbSize
        } catch (e: java.lang.Exception) {
            // here you can handle exception here
            null
        }
    }

}



