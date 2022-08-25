package com.example.imageapplication

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.imageapplication.databinding.FragmentSendOtpBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class SendOTPFragment : Fragment() {

    private val TAG = SendOTPFragment::class.java.simpleName
    private var _binding: FragmentSendOtpBinding? = null
    private lateinit var mAuth:FirebaseAuth
    private lateinit var callbacks : PhoneAuthProvider.OnVerificationStateChangedCallbacks

    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSendOtpBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAuth = FirebaseAuth.getInstance()

        binding.btnSend.setOnClickListener {
            if(binding.etPhone.text.toString().trim().isEmpty()){
                Log.d(TAG,"Invalid Phone Number")
                Toast.makeText(requireContext(),"Invalid Phone Number", Toast.LENGTH_LONG).show()
            }else if(binding.etPhone.text.toString().trim().length != 10) {
                Log.d(TAG,"Invalid Phone Number ${binding.etPhone.text.toString().trim().length}")
                Toast.makeText(requireContext(),"Type valid Phone Number", Toast.LENGTH_LONG).show()
            }else
            {
                sendOTP()
            }
        }
    }

    private fun sendOTP() {

        binding.progressBar.visibility = View.VISIBLE
        binding.btnSend.visibility =  View.INVISIBLE
        callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                // This callback will be invoked in two situations:
                // 1 - Instant verification. In some cases the phone number can be instantly
                //     verified without needing to send or enter a verification code.
                // 2 - Auto-retrieval. On some devices Google Play services can automatically
                //     detect the incoming verification SMS and perform verification without
                //     user action.
                Log.d(TAG, "onVerificationCompleted:$credential")
                //signInWithPhoneAuthCredential(credential)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                // This callback is invoked in an invalid request for verification is made,
                // for instance if the the phone number format is not valid.
                Log.w(TAG, "onVerificationFailed", e)

                binding.progressBar.visibility = View.GONE
                binding.btnSend.visibility = View.VISIBLE
                Toast.makeText(requireContext(), e.localizedMessage, Toast.LENGTH_SHORT).show()


                // Show a message and update the UI
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                // The SMS verification code has been sent to the provided phone number, we
                // now need to ask the user to enter the code and then construct a credential
                // by combining the code with a verification ID.
                Log.d(TAG, "onCodeSent:$verificationId")

                // Save verification ID and resending token so we can use them later
                binding.progressBar.visibility = View.GONE
                binding.btnSend.visibility = View.VISIBLE
                Toast.makeText(
                    requireContext(),
                    "OTP is successfully send.",
                    Toast.LENGTH_SHORT
                ).show()

                val bundle = bundleOf("phoneNumber" to binding.etPhone.text.toString().trim(),
                    "verificationId" to verificationId)
                findNavController().navigate(R.id.action_SecondFragment_to_FirstFragment,bundle)


            }
        }
        val phoneNumber = "+880${binding.etPhone.text.toString().trim()}"
        Log.d(TAG,"phone number : $phoneNumber")

        val options = PhoneAuthOptions.newBuilder(mAuth)
            .setPhoneNumber(phoneNumber)       // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(requireActivity())                 // Activity (for callback binding)
            .setCallbacks(callbacks)          // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}