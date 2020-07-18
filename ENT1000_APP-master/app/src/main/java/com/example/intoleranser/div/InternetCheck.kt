package com.example.intoleranser.div

import android.os.AsyncTask
import java.lang.Exception
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

// Klasse som returnerer true hvis enheten er koblet til internett, ellers false
class InternetCheck(private val consumer:Consumer):AsyncTask<Void, Void, Boolean>() {
    init {
        execute() // Kjøres automatisk når klassen kalles
    }

    override fun doInBackground(vararg p0: Void?): Boolean {
        return try {
            // Oppretter socket til tilkobling
            val sokkel = Socket()
            val address = InetAddress.getByName("www.google.com")
            val socketAddress = InetSocketAddress(address, 80)
            // Forsøker å koble til Google
            sokkel.connect(socketAddress, 1500)
            sokkel.close()
            true
        } catch (e:Exception) {
            false
        }
    }

    // Etter at metoden er kjørt
    override fun onPostExecute(result: Boolean?) {
        super.onPostExecute(result)
        consumer.accept(result)
    }

    interface Consumer {
        fun accept(isConnected:Boolean?)
    }
}

