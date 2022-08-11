package com.example.imageapplication

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Point
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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

    private lateinit var floatImage: ActivityResultLauncher<String>
    private lateinit var simpleFloatingWindow: SimpleFloatingWindow
    private var uri: Uri? = null
    private lateinit var currentImagePath: String
    private var split_image: Button? = null
    private var btnGallery: Button? = null

    private var sourceImage: ImageView? = null
    var selectedImage: Uri? = null
    private val RESULT_LOAD_IMAGE = 1
    var chunkSideLength = 50

    var chunkedImage: java.util.ArrayList<Bitmap>? = null

    // Number of rows and columns in chunked image
    var rows = 0
    var cols = 0 // Number of rows and columns in chunked image

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main)
        //alertDialogForCameraImage();
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


    fun createImageFile():File{
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

    private fun takepicture(){

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
        startActivityForResult(intent,101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if( resultCode == -1 && requestCode == 101){
            val result = data?.getStringExtra("RESULT")
            var resultUri:Uri? = null
            if (result != null){
                resultUri = Uri.parse(result)
            }

            sourceImage?.setImageURI(resultUri)
        }
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

                takepicture()
            }
        }

        alertDialog.setNeutralButton("Cancel"){dialogueInterface, which ->

        }

        val builder = alertDialog.create()
        builder.show()

    }


    private fun splitImage(image: ImageView, chunkNumbers: Int) {

        //For the number of rows and columns of the grid to be displayed
        val rows: Int
        val cols: Int

        //For height and width of the small image chunks
        val chunkHeight: Int
        val chunkWidth: Int

        //To store all the small image chunks in bitmap format in this list
        val chunkedImages = ArrayList<Bitmap>(chunkNumbers)

        //Getting the scaled bitmap of the source image
        val drawable = image.drawable as BitmapDrawable
        val bitmap = drawable.bitmap
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmap.width, bitmap.height, true)
        cols = Math.sqrt(chunkNumbers.toDouble()).toInt()
        rows = cols
        chunkHeight = bitmap.height / rows
        chunkWidth = bitmap.width / cols

        //xCoord and yCoord are the pixel positions of the image chunks
        var yCoord = 0
        for (x in 0 until rows) {
            var xCoord = 0
            for (y in 0 until cols) {
                chunkedImages.add(
                    Bitmap.createBitmap(
                        scaledBitmap,
                        xCoord,
                        yCoord,
                        chunkWidth,
                        chunkHeight
                    )
                )
                xCoord += chunkWidth
            }
            yCoord += chunkHeight
        }

        sourceImage?.setImageBitmap(chunkedImage?.get(0))

        /* Now the chunkedImages has all the small image chunks in the form of Bitmap class.
         * You can do what ever you want with this chunkedImages as per your requirement.
         * I pass it to a new Activity to show all small chunks in a grid for demo.
         * You can get the source code of this activity from my Google Drive Account.
         */
        //Start a new activity to show these chunks into a grid
//        val intent = Intent(this@ImageActivity, ChunkedImageActivity::class.java)
//        intent.putParcelableArrayListExtra("image chunks", chunkedImages)
//        startActivity(intent)
    }

    private fun splitImage1(image: ImageView, chunkSideLength: Int) {
        val random = Random(System.currentTimeMillis())

        // height and weight of higher|wider chunks if they would be
        val higherChunkSide: Int
        val widerChunkSide: Int

        // Getting the scaled bitmap of the source image
        val bitmap = (image.drawable as BitmapDrawable).bitmap
        rows = bitmap.height / chunkSideLength
        higherChunkSide = bitmap.height % chunkSideLength + chunkSideLength
        cols = bitmap.width / chunkSideLength
        widerChunkSide = bitmap.width % chunkSideLength + chunkSideLength

        // To store all the small image chunks in bitmap format in this list
        chunkedImage = ArrayList(rows * cols)
        if (higherChunkSide != chunkSideLength) {
            if (widerChunkSide != chunkSideLength) {
                // picture has both higher and wider chunks plus one big square chunk
                val widerChunks = ArrayList<Bitmap?>(rows - 1)
                val higherChunks = ArrayList<Bitmap?>(cols - 1)
                val squareChunk: Bitmap
                var yCoord = 0
                for (y in 0 until rows - 1) {
                    var xCoord = 0
                    for (x in 0 until cols - 1) {
                        chunkedImage!!.add(
                            Bitmap.createBitmap(
                                bitmap,
                                xCoord,
                                yCoord,
                                chunkSideLength,
                                chunkSideLength
                            )
                        )
                        xCoord += chunkSideLength
                    }
                    // add last chunk in a row to array of wider chunks
                    widerChunks.add(
                        Bitmap.createBitmap(
                            bitmap,
                            xCoord,
                            yCoord,
                            widerChunkSide,
                            chunkSideLength
                        )
                    )
                    yCoord += chunkSideLength
                }

                // add last row to array of higher chunks
                var xCoord = 0
                for (x in 0 until cols - 1) {
                    higherChunks.add(
                        Bitmap.createBitmap(
                            bitmap,
                            xCoord,
                            yCoord,
                            chunkSideLength,
                            higherChunkSide
                        )
                    )
                    xCoord += chunkSideLength
                }

                //save bottom-right big square chunk
                squareChunk =
                    Bitmap.createBitmap(bitmap, xCoord, yCoord, widerChunkSide, higherChunkSide)

                //shuffle arrays
                Collections.shuffle(chunkedImage)
                Collections.shuffle(higherChunks)
                Collections.shuffle(widerChunks)

                //determine random position of big square chunk
                val bigChunkX = random.nextInt(cols)
                val bigChunkY = random.nextInt(rows)

                //add wider and higher chunks into resulting array of chunks
                //all wider(higher) chunks should be in one column(row) to avoid collisions between chunks
                //We must insert it row by row because they will displace each other from their columns otherwise
                for (y in 0 until rows - 1) {
                    chunkedImage!!.add(cols * y + bigChunkX, widerChunks[y]!!)
                }

                //And then we insert the whole row of higher chunks
                for (x in 0 until cols - 1) {
                    chunkedImage!!.add(bigChunkY * cols + x, higherChunks[x]!!)
                }
                chunkedImage!!.add(bigChunkY * cols + bigChunkX, squareChunk)
            } else {
                // picture has only number of higher chunks
                val higherChunks = ArrayList<Bitmap?>(cols)
                var yCoord = 0
                for (y in 0 until rows - 1) {
                    var xCoord = 0
                    for (x in 0 until cols) {
                        chunkedImage!!.add(
                            Bitmap.createBitmap(
                                bitmap,
                                xCoord,
                                yCoord,
                                chunkSideLength,
                                chunkSideLength
                            )
                        )
                        xCoord += chunkSideLength
                    }
                    yCoord += chunkSideLength
                }

                // add last row to array of higher chunks
                var xCoord = 0
                for (x in 0 until cols) {
                    higherChunks.add(
                        Bitmap.createBitmap(
                            bitmap,
                            xCoord,
                            yCoord,
                            chunkSideLength,
                            higherChunkSide
                        )
                    )
                    xCoord += chunkSideLength
                }

                //shuffle arrays
                Collections.shuffle(chunkedImage)
                Collections.shuffle(higherChunks)

                //add higher chunks into resulting array of chunks
                //Each higher chunk should be in his own column to preserve original image size
                //We must insert it row by row because they will displace each other from their columns otherwise
                val higherChunksPositions: MutableList<Point> = ArrayList<Point>(cols)
                for (x in 0 until cols) {
                    higherChunksPositions.add(Point(x, random.nextInt(rows)))
                }

                //sort positions of higher chunks. THe upper-left elements should be first

                Collections.sort(higherChunksPositions, object : Comparator<Point> {
                    override fun compare(lhs: Point, rhs: Point): Int {
                        if (lhs.y != rhs.y) {
                            return if (lhs.y < rhs.y) -1 else 1
                        } else if (lhs.x != rhs.x) {
                            return if (lhs.x < rhs.x) -1 else 1
                        }
                        return 0
                    }
                })
//                Collections.sort(higherChunksPositions, Comparator<Any?> { lhs, rhs ->
//                    if (lhs.y !== rhs.y) {
//                        return@Comparator if (lhs.y < rhs.y) -1 else 1
//                    } else if (lhs.x !== rhs.x) {
//                        return@Comparator if (lhs.x < rhs.x) -1 else 1
//                    }
//                    0
//                })
                for (x in 0 until cols) {
                    val currentCoord: Point = higherChunksPositions[x]
                    chunkedImage!!.add(currentCoord.y * cols + currentCoord.x, higherChunks[x]!!)
                }
            }
        } else {
            if (widerChunkSide != chunkSideLength) {
                // picture has only number of wider chunks
                val widerChunks = ArrayList<Bitmap?>(rows)
                var yCoord = 0
                for (y in 0 until rows) {
                    var xCoord = 0
                    for (x in 0 until cols - 1) {
                        chunkedImage!!.add(
                            Bitmap.createBitmap(
                                bitmap,
                                xCoord,
                                yCoord,
                                chunkSideLength,
                                chunkSideLength
                            )
                        )
                        xCoord += chunkSideLength
                    }
                    // add last chunk in a row to array of wider chunks
                    widerChunks.add(
                        Bitmap.createBitmap(
                            bitmap,
                            xCoord,
                            yCoord,
                            widerChunkSide,
                            chunkSideLength
                        )
                    )
                    yCoord += chunkSideLength
                }

                //shuffle arrays
                Collections.shuffle(chunkedImage)
                Collections.shuffle(widerChunks)

                //add wider chunks into resulting array of chunks
                //Each wider chunk should be in his own row to preserve original image size
                for (y in 0 until rows) {
                    chunkedImage!!.add(cols * y + random.nextInt(cols), widerChunks[y]!!)
                }
            } else {
                // picture perfectly splits into square chunks
                var yCoord = 0
                for (y in 0 until rows) {
                    var xCoord = 0
                    for (x in 0 until cols) {
                        chunkedImage!!.add(
                            Bitmap.createBitmap(
                                bitmap,
                                xCoord,
                                yCoord,
                                chunkSideLength,
                                chunkSideLength
                            )
                        )
                        xCoord += chunkSideLength
                    }
                    yCoord += chunkSideLength
                }
                Collections.shuffle(chunkedImage)
            }
        }

        // Function of merge the chunks images(after image divided in pieces then i can call this function to combine
        // and merge the image as one)
        mergeImage(chunkedImage!!, bitmap.width, bitmap.height)
    }

    private fun mergeImage(imageChunks: ArrayList<Bitmap>, width: Int, height: Int) {

        // create a bitmap of a size which can hold the complete image after merging
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_4444)

        // create a canvas for drawing all those small images
        val canvas = Canvas(bitmap)
        var count = 0
        var currentChunk = imageChunks[0]

        //Array of previous row chunks bottom y coordinates
        val yCoordinates = IntArray(cols)
        Arrays.fill(yCoordinates, 0)
        for (y in 0 until rows) {
            var xCoord = 0
            for (x in 0 until cols) {
                currentChunk = imageChunks[count]
                canvas.drawBitmap(currentChunk,xCoord.toFloat(),yCoordinates[x].toFloat(),null)
                //canvas.drawBitmap(currentChunk, xCoord.toFloat(), yCoordinates[x], null)
                xCoord += currentChunk.width
                yCoordinates[x] += currentChunk.height
                count++
            }
        }

        /*
     * The result image is shown in a new Activity
     */
//        val intent = Intent(this@MainActivity, MergedImage::class.java)
//        intent.putExtra("merged_image", bitmap)
//        startActivity(intent)
//        finish()

        Log.d("MergedImage","merged image in bitmap :$bitmap")
        sourceImage?.setImageBitmap(chunkedImage?.get(1))
    }


    companion object {
        private const val REQUEST_CODE_DRAW_OVERLAY_PERMISSION = 5
    }
}