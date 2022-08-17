package com.example.imageapplication

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import com.example.imageapplication.databinding.FragmentVerifyOtpBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthProvider

class VerifyOTPFragment : Fragment() {

    private lateinit var binding: FragmentVerifyOtpBinding
    private lateinit var verificationID:String
    private val TAG = VerifyOTPFragment::class.java.simpleName


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentVerifyOtpBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        //TODO FIXME getting data from fragment
        val phoneNumber = arguments?.getString("phoneNumber")
        val verificationID = arguments?.getString("verificationId")
        editTextInput()
        Log.d(TAG,"phoneNumber :  $phoneNumber and verificationID : $verificationID")
        //binding.tvMobile.setText()
        binding.tvMobile.text = String.format("+880-${phoneNumber}")

        binding.tvResendBtn.setOnClickListener {
            Toast.makeText(requireContext(),"OTP send Successfully",Toast.LENGTH_SHORT).show()
        }

        binding.btnVerify.setOnClickListener {

            binding.progressBarVerify.visibility = View.VISIBLE
            binding.btnVerify.visibility =  View.INVISIBLE


            if(binding.etC1.text.toString().trim().isEmpty() ||
                binding.etC2.text.toString().trim().isEmpty() ||
                binding.etC3.text.toString().trim().isEmpty() ||
                binding.etC4.text.toString().trim().isEmpty() ||
                binding.etC5.text.toString().trim().isEmpty() ||
                binding.etC6.text.toString().trim().isEmpty()){
                Toast.makeText(requireContext(),"OTP is not valid!",Toast.LENGTH_SHORT).show()
            }else if(verificationID != null){
                val code = binding.etC1.text.toString().trim()+
                        binding.etC2.text.toString().trim()+
                        binding.etC3.text.toString().trim()+
                        binding.etC4.text.toString().trim()+
                        binding.etC5.text.toString().trim()+
                        binding.etC6.text.toString().trim()

                val credential = PhoneAuthProvider.getCredential(verificationID, code)

                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            binding.progressBarVerify.visibility = View.VISIBLE
                            binding.btnVerify.visibility =  View.INVISIBLE
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success")

                            val user = task.result?.user
                            val userphonenumber = user?.phoneNumber
                            val username = user?.displayName

                            val bundle = bundleOf("userphonenumber" to userphonenumber,
                                "username" to username)
                            findNavController().navigate(R.id.action_verifyFragment_to_ImageUpload,bundle)
                        } else {
                            binding.progressBarVerify.visibility = View.GONE
                            binding.btnVerify.visibility =  View.VISIBLE
                            // Sign in failed, display a message and update the UI
                            Log.w(TAG, "signInWithCredential:failure", task.exception)
                            if (task.exception is FirebaseAuthInvalidCredentialsException) {
                                // The verification code entered was invalid
                            }
                            // Update UI
                        }
                    }

            }
        }
    }

    private fun editTextInput(){
        binding.etC1.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC2.requestFocus()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
        binding.etC2.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC3.requestFocus()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
        binding.etC3.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC4.requestFocus()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
        binding.etC4.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC5.requestFocus()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })
        binding.etC5.addTextChangedListener(object :TextWatcher{
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {

            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.etC6.requestFocus()
            }

            override fun afterTextChanged(s: Editable?) {
            }

        })


    }

}