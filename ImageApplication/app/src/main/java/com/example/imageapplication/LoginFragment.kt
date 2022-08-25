package com.example.imageapplication

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.imageapplication.data.UserCredentials
import com.example.imageapplication.databinding.FragmentLoginBinding
import com.example.imageapplication.security.utils
import com.google.firebase.database.*

class LoginFragment : Fragment() {

    private lateinit var query: Query
    private var value: UserCredentials? = null
    private lateinit var binding : FragmentLoginBinding
    private val TAG = LoginFragment::class.java.simpleName
    private lateinit var mDatabaseReference: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.d(TAG,"phone number : ${arguments?.getString("userphonenumber")}")
        mDatabaseReference = FirebaseDatabase.getInstance().getReference("Users")
        if(arguments?.getString("userphonenumber") != null){
            binding.tvLogin.text = "Please set your Password"
        }

        configureUI(arguments?.getString("userphonenumber") == null)

    }

    private fun configureUI(isStartView: Boolean) {

        if(!isStartView){
            binding.tvRegister.visibility =  View.GONE
            binding.tilRepassword.visibility = View.VISIBLE
        }

        binding.tvRegister.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_SendOtpFragment)
        }

        binding.btnLogin.setOnClickListener {
            checkCredentials(isStartView)
        }

        binding.btnContinue.setOnClickListener {
            findNavController().navigate(R.id.action_LoginFragment_to_ImageUploadFragment)
        }
    }

    private val valueEventListener = object : ValueEventListener {
        override fun onDataChange(snapshot: DataSnapshot) {
            Log.d(TAG,"on data change ${snapshot.value} and ${snapshot.hasChildren()}")

           if(snapshot.exists()){
               if(arguments?.getString("userphonenumber") == null){
                   login()
               }else{
                   Toast.makeText(requireContext(),"User already exists !",Toast.LENGTH_LONG).show()
               }
           }else{
               if(arguments?.getString("userphonenumber") == null){
                   Toast.makeText(requireContext(),"User does not exist !",Toast.LENGTH_LONG).show()
               }else{
                   signUp()
               }
           }
        }

        override fun onCancelled(error: DatabaseError) {
            Log.d(TAG,"Failed to read value : ${error.message}")
        }

    }


    private val childEventListener = object :ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            Log.d(TAG,"child credentials onChildAdded1 $snapshot")

            val value = snapshot.getValue(UserCredentials::class.java)
            if(value != null){
                if(utils.isPasswordMatched(binding.etPassword.text.toString().trim(),
                        value.password!!)){
                    Toast.makeText(requireContext(), "Successfully logged in", Toast.LENGTH_LONG)
                        .show()
                    findNavController().navigate(R.id.action_LoginFragment_to_ImageUploadFragment)
                }else{
                    Toast.makeText(requireContext(), "Login Attempt unsuccessful", Toast.LENGTH_LONG)
                        .show()
                }
            }

            binding.pbLogin.visibility = View.INVISIBLE
            Log.d(TAG,"child credentials onChildAdded $value")
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
            val value = snapshot.getValue(UserCredentials::class.java)
            Log.d(TAG,"child credentials onChildChanged $value")
        }

        override fun onChildRemoved(snapshot: DataSnapshot) {
            val value = snapshot.getValue(UserCredentials::class.java)
            Log.d(TAG,"child credentials onChildRemoved $value")
        }

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            val value = snapshot.getValue(UserCredentials::class.java)
            Log.d(TAG,"child credentials onChildMoved $value")
        }

        override fun onCancelled(error: DatabaseError) {
            Log.d(TAG,"child credentials ${error.message}")
        }

    }

    private fun checkCredentials(isStartView: Boolean) {
        if (binding.etPassword.text?.isEmpty() == true || binding.etUsername.text?.isBlank() == true) {
            Toast.makeText(
                requireContext(),
                "Please enter both phone number and password",
                Toast.LENGTH_LONG
            ).show()
        }else if(!isStartView && binding.etRepassword.text?.isBlank() == true){
            Toast.makeText(
                requireContext(),
                "Please enter password once more!",
                Toast.LENGTH_LONG
            ).show()
        }else if((!isStartView) && (binding.etRepassword.text.toString().trim() != binding.etPassword.text.toString().trim())){
            Toast.makeText(
                requireContext(),
                "Please confirm your password!",
                Toast.LENGTH_LONG
            ).show()
        } else if (binding.etUsername.text?.length != 10) {
            Toast.makeText(requireContext(), "Please enter correct phone number", Toast.LENGTH_LONG)
                .show()
        }
//        else if(isStartView) {
//            login()
//        }
        else{
            Log.d(TAG,"etUsername text = +880${binding.etUsername.text.toString().trim()}")
            //mDatabaseReference.addValueEventListener(valueEventListener)
            val phone = "+880${binding.etUsername.text.toString().trim()}"
            Log.d(TAG,"p n :$phone")
            query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("phoneNumber")
                .equalTo(phone)
            query.addListenerForSingleValueEvent(valueEventListener)
        }
    }

    private fun signUp() {

        val userCredentials = UserCredentials(
            arguments?.getString("userphonenumber"),
            utils.encrypt(binding.etPassword.text.toString().trim()), false
        )
        val uploadId = mDatabaseReference.push().key
        if (uploadId != null) {
            mDatabaseReference.child(uploadId).setValue(userCredentials)
            binding.pbLogin.visibility = View.INVISIBLE
        }
        //userCredentials.phoneNumber?.let { mDatabaseReference.push().child(it).setValue(userCredentials) }
        Toast.makeText(
            requireContext(),
            "Password is set for ${userCredentials.phoneNumber} !",
            Toast.LENGTH_LONG
        )
            .show()
        binding.btnLogin.visibility = View.GONE
        binding.btnContinue.visibility = View.VISIBLE
    }

    private fun login() {
        binding.pbLogin.visibility = View.VISIBLE
        Log.d(TAG,"etUsername text = +880${binding.etUsername.text.toString().trim()}")
        val phone = "+880${binding.etUsername.text.toString().trim()}"
        Log.d(TAG,"p n :$phone")
        query = FirebaseDatabase.getInstance().getReference("Users").orderByChild("phoneNumber")
            .equalTo(phone)
        query.addChildEventListener(childEventListener)

    }


}