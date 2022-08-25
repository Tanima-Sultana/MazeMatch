package com.example.imageapplication.security

import java.security.MessageDigest
import kotlin.text.Charsets.UTF_8
object utils {
    private fun md5(str: String): ByteArray = MessageDigest.getInstance("MD5").digest(str.toByteArray(UTF_8))
    private fun ByteArray.toHex() = joinToString(separator = "") { byte -> "%02x".format(byte) }
    fun isPasswordMatched(newPassword:String,savedPassword:String):Boolean{
        return md5(newPassword).toHex() == savedPassword
        //return newPassword == savedPassword

    }

    fun encrypt(str:String):String{
        return md5(str).toHex()
    }
}