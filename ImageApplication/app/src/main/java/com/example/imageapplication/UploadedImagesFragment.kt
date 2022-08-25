package com.example.imageapplication

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.imageapplication.adapter.ImageAdapter
import com.example.imageapplication.data.Upload
import com.example.imageapplication.databinding.FragmentUploadedImagesBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class UploadedImagesFragment : Fragment() {

    private lateinit var itemTouchHelperCallback: ItemTouchHelper.SimpleCallback
    private lateinit var binding: FragmentUploadedImagesBinding
    private lateinit var mUploads:MutableList<Upload>
    private lateinit var mDatabaseReference: DatabaseReference

    private lateinit var mStrorage:FirebaseStorage

    lateinit var mAdapter:ImageAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentUploadedImagesBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvUploadedImages.setHasFixedSize(true)
        binding.rvUploadedImages.layoutManager = LinearLayoutManager(requireContext())
        mUploads = mutableListOf()
        mAdapter = ImageAdapter(mUploads)
        binding.rvUploadedImages.adapter = mAdapter


        mStrorage = FirebaseStorage.getInstance()
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("uploads")

        mDatabaseReference.addValueEventListener( object: ValueEventListener{
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                mUploads.clear()
                for (i in snapshot.children){
                    val upload = i.getValue(Upload::class.java)
                    if (upload != null) {
                        upload.key = i.key
                        mUploads.add(upload)
                    }
                    mAdapter.notifyDataSetChanged()

                    binding.pbShowImage.visibility = View.INVISIBLE
                }


            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(),error.message,Toast.LENGTH_LONG).show()
                binding.pbShowImage.visibility = View.INVISIBLE

            }

        })


        itemTouchHelperCallback =
            object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {

                    return false
                }

                @SuppressLint("NotifyDataSetChanged")
                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {


                    val selectedItem = mUploads[viewHolder.adapterPosition]
                    val selectedKey = selectedItem.key
                    val imageRef = selectedItem.fileUrl?.let { mStrorage.getReferenceFromUrl(it) }
                    imageRef?.delete()?.addOnSuccessListener {
                        if (selectedKey != null) {
                            mDatabaseReference.child(selectedKey).removeValue()
                            mUploads.removeAt(viewHolder.layoutPosition)
                            mAdapter.notifyDataSetChanged()
                            Snackbar.make(requireView(),"Item is deleted", Snackbar.LENGTH_SHORT).show()

                        }
                    }?.addOnFailureListener {

                    }

                    //                   noteViewModel.delete(noteAdapter.getNoteAt(viewHolder.adapterPosition))
                }

            }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvUploadedImages)



    }


}