package com.example.imageapplication

import android.Manifest
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentValues
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.imageapplication.data.Upload
import com.example.imageapplication.databinding.FragmentImageUploadBinding
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*


class ImageUploadFragment : Fragment() {

    private var mUploadTask: StorageTask<UploadTask.TaskSnapshot>? = null
    private lateinit var resultUri: Uri
    private lateinit var binding: FragmentImageUploadBinding

    private lateinit var loadImage: ActivityResultLauncher<String>
    private lateinit var cropImageLuncher: ActivityResultLauncher<Intent>
    private var uri: Uri? = null
    private lateinit var currentImagePath: String


    private lateinit var mStorageRef:StorageReference
    private lateinit var mDatabaseRef:DatabaseReference

    private val TAG = ImageUploadFragment::class.java.simpleName

    private var isImageSaved = false


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
        binding = FragmentImageUploadBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mStorageRef = FirebaseStorage.getInstance().getReference("uploads")
        mDatabaseRef = FirebaseDatabase.getInstance().getReference("uploads")


        cropImageLuncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    val result = it.data?.getStringExtra("RESULT")
                    if (result != null) {
                        resultUri = Uri.parse(result)
                    }

                    binding.ivSouceImage.setImageURI(resultUri)
                    binding.btnUpload.visibility = View.VISIBLE
                    binding.pbUpload.visibility = View.VISIBLE
                    binding.btnSave.visibility = View.VISIBLE
                }
            }

        loadImage = registerForActivityResult(ActivityResultContracts.GetContent()) {
            if (it != null) {
                Log.d("MainActivity", "image data : $it and ${it.path}")

                if (getImageSize(it)!! < 200) {
                    binding.ivSouceImage.setImageURI(it)
                    cropImage(it)
                } else {
                    showMessageOKCancel("Your image is above 200kb", null)
                }
            }
        }

        binding.btnImage.setOnClickListener {
            pickImageFromGallery()
        }

        binding.btnUpload.setOnClickListener {
            if (mUploadTask != null && mUploadTask?.isInProgress == true){
                Toast.makeText(requireContext(),"Upload is in progress",Toast.LENGTH_LONG).show()
            }else{
                //binding.btnUpload.visibility = View.GONE
                uploadFile()

            }
        }



        binding.btnSave.setOnClickListener {
            saveImage()
        }

        binding.btnShowImage.setOnClickListener {
            findNavController().navigate(R.id.action_UploadImage_to_seeImage)
        }
    }


    private fun cropImage(uri: Uri?) {
        val intent = Intent(requireContext(), MergedImage::class.java)
        intent.putExtra("DATA", uri?.toString())
        requireActivity().setResult(101, intent)
        cropImageLuncher.launch(intent)
    }

    private val takePicture =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
            if (success) {
                Log.d("CameraImage", "Image location :$uri")
                if (getImageSize(uri!!)!! < 200) {
                    binding.ivSouceImage.setImageURI(uri)
                    cropImage(uri)
                } else {
//                    Toast.makeText(
//                        requireContext(), "Your image is above 200kb",
//                        Toast.LENGTH_SHORT
//                    ).show()
                    showMessageOKCancel("Your image is above 200kb",null)
                }
                // The image was saved into the given Uri -> do something with it

            } else {
                Log.d("CameraImage", "Image is not saved")

            }
        }


    private fun saveImage():Boolean{
        var bitmap:Bitmap? = null
        val imageUri = if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }else{
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }


        try {
            bitmap = if (Build.VERSION.SDK_INT <= 28) {
                MediaStore.Images.Media.getBitmap(requireContext().contentResolver, resultUri)
            } else {
                val source: ImageDecoder.Source = ImageDecoder.createSource(requireContext().contentResolver, resultUri)
                ImageDecoder.decodeBitmap(source)
            }
        }catch (e:Exception){
            Log.e(TAG,"bitmap conversation exception : ${e.message}")
        }



        val contentValues = ContentValues()
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, System.currentTimeMillis().toString())
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        val uri = requireContext().contentResolver.insert(imageUri,contentValues)

        try {

            val outputStream : OutputStream? = uri?.let {
                requireContext().contentResolver.openOutputStream(
                    it
                )
            }

            val cc = BitmapFactory.decodeFile(createImageFile().absolutePath)
            Log.d(TAG,"image stream : $outputStream")

            Log.d(TAG,"image bitmap : $cc")
            bitmap?.compress(Bitmap.CompressFormat.JPEG,100,outputStream)
            Toast.makeText(requireContext(),"Saved the image!",Toast.LENGTH_LONG).show()

            return true
        }catch (e:Exception){
            Log.d(TAG,"image is not saved ${e.message}\n ${e}")
            Toast.makeText(requireContext(),"Image is not saved!",Toast.LENGTH_LONG).show()
            return false
        }
    }


    private fun takePicture() {

        createImageFile().also {
            try {
                uri = FileProvider.getUriForFile(
                    requireContext(), BuildConfig.APPLICATION_ID + ".provider", it
                )
            } catch (e: Exception) {
                Log.e("CameraImage", "Error: ${e.message}")
            }
        }

        Log.d("CameraImage", "uri is :$uri")
        takePicture.launch(uri)

    }

    private val requestMultiplePermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permission ->
        val granted = permission.entries.all {
            it.value
        }
        if (granted) {
            takePicture()
            //pickImageFromGallery()
        }
    }

    private val requestSinglePermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        if (it) {
            loadImage.launch("image/*")
        }
    }


    private fun pickImageFromGallery() {


        val alertDialog = AlertDialog.Builder(requireContext())
        alertDialog.setTitle("Select ")
        alertDialog.setPositiveButton("Pick Image") { _, _ ->
            if (hasExternalReadPermission() != PackageManager.PERMISSION_GRANTED) {
                requestSinglePermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)

            } else {
                loadImage.launch("image/*")

            }
        }

        alertDialog.setNegativeButton("Camera") { _, _ ->
            if (hasCameraPermission() != PackageManager.PERMISSION_GRANTED ||
                hasExternalWritePermission() != PackageManager.PERMISSION_GRANTED
            ) {
                requestMultiplePermissions.launch(PERMISSIONS)
            } else {

                takePicture()
            }
        }

        alertDialog.setNeutralButton("Cancel") { _, _ ->

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

    private fun hasCameraPermission() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.CAMERA
    )

    private fun hasExternalWritePermission() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private fun hasExternalReadPermission() = ContextCompat.checkSelfPermission(
        requireContext(),
        Manifest.permission.READ_EXTERNAL_STORAGE
    )

    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageDirectory = requireActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(timestamp, ".jpg", imageDirectory).apply {
            currentImagePath = absolutePath
        }
    }

    private fun getImageSize(uri: Uri): Int? {
        return try {
            val aaa: InputStream? =
                this@ImageUploadFragment.context?.contentResolver?.openInputStream(uri)
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

    private fun getFileExtension(uri:Uri):String?{

        val extension = if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            //If scheme is a content
            val mime = MimeTypeMap.getSingleton()
            mime.getExtensionFromMimeType(requireContext().contentResolver.getType(uri))
        } else {
            //If scheme is a File
            //This will replace white spaces with %20 and also other special characters. This will avoid returning null values on file name with spaces and special characters.
            MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(uri.path?.let { File(it) }).toString())
        }

        return extension
    }

    private fun uploadFile(){

        val fileReference = mStorageRef.child(System.currentTimeMillis().toString()
                +"."+ getFileExtension(resultUri)
        )

        Log.d(TAG,"resultUri : $resultUri and fileReference name : ${System.currentTimeMillis().toString()
                +"."+ getFileExtension(resultUri)}")

       mUploadTask =  fileReference.putFile(resultUri)
            .addOnSuccessListener {
                Handler(Looper.getMainLooper()).postDelayed({
                    binding.pbUpload.progress = 100
                }, 10000)

                if (it.metadata?.reference != null) {
                    val result: Task<Uri> = it.storage.downloadUrl
                    result.addOnSuccessListener { p0 ->
                        val upload = Upload(createImageFile().nameWithoutExtension + ".jpg", p0.toString())

                        val uploadId = mDatabaseRef.push().key

                        if (uploadId != null) {
                            mDatabaseRef.child(uploadId).setValue(upload)
                        }
                        binding.btnUpload.visibility = View.GONE
                        binding.btnShowImage.visibility = View.VISIBLE

                    }
                }

            }.addOnFailureListener{
                Toast.makeText(requireContext(),"${it.message}",Toast.LENGTH_SHORT).show()

            }.addOnProgressListener {
                val progress = (it.bytesTransferred / it.totalByteCount) * 100.0
                binding.pbUpload.progress = progress.toInt()

            }

    }



}



