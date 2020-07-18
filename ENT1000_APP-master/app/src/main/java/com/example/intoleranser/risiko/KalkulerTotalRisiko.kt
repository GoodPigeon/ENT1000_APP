package com.example.intoleranser.risiko

import android.content.SharedPreferences
import com.example.intoleranser.DTO.RegisteredDTO
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.Error

// Klasse som henter brukerens registrerte matvarer og kalkulerer en risiko per FODMap
class KalkulerTotalRisiko(prefs: SharedPreferences) {
    var antallMaaltider: Int = 0 // Brukes til grense før resultat vises
    // Risikoen per FODMap i prosent
    var sOligos: Double = 0.0
    var sFructose: Double = 0.0
    var sPolyols: Double = 0.0
    var sLactose: Double = 0.0

    init {
        // Henter de lagrede matvarene
        fun fetchArrayList(): ArrayList<RegisteredDTO> {
            val gson = Gson()
            val yourArrayList: ArrayList<RegisteredDTO>
            val json = prefs.getString("food", "")

            yourArrayList = when {
                json.isNullOrEmpty() -> ArrayList()
                else -> gson.fromJson(json, object : TypeToken<List<RegisteredDTO>>() {}.type)
            }

            return yourArrayList
        }
        val registeredList = fetchArrayList()
        kalkulerRisiko(registeredList)
    }

    // Metode som kalkulerer risikoen for hver FODMap
    // TO-DO: Veldig stygg kode, bør fikses
    private fun kalkulerRisiko(registeredList: ArrayList<RegisteredDTO>) {
        // Det som skal være den største mulige verdien per FODMap
        var mOligos = 0.0
        var mFructose = 0.0
        var mPolyols = 0.0
        var mLactose = 0.0

        // Går igjennom hver matvare som er registrert
        for (mat in registeredList) {
            // Multipliserer matvarens FODMap med alvorligheten brukeren legger inn
            sOligos += (mat.foodDTO.details.oligos * konverterAlvorlighetTall(mat.alvorlighet))
            // Multipliserer matvarens FODMap med maksimal mulig alvorlighet(4)
            mOligos += (mat.foodDTO.details.oligos * 4)
            // Repeteres for hver FODMap
            sFructose += (mat.foodDTO.details.fructose * konverterAlvorlighetTall(mat.alvorlighet))
            mFructose += (mat.foodDTO.details.fructose * 4)
            sPolyols += (mat.foodDTO.details.polyols * konverterAlvorlighetTall(mat.alvorlighet))
            mPolyols += (mat.foodDTO.details.polyols * 4)
            sLactose += (mat.foodDTO.details.lactose * konverterAlvorlighetTall(mat.alvorlighet))
            mLactose += (mat.foodDTO.details.lactose * 4)

            // Hvis alvorligheten er satt som null og matvaren ikke inneholder noen FODMaps
            // inkrementeres antallMåltider
            if ((mat.alvorlighet != "Ingen alvorlighet") &&
                    ((mat.foodDTO.details.oligos != 0) ||
                    (mat.foodDTO.details.fructose != 0) ||
                    (mat.foodDTO.details.polyols != 0) ||
                    (mat.foodDTO.details.lactose != 0))
            ) {
                antallMaaltider++
            }
        }

        // Dividerer summen av alle matvarers FODMaps verdi med den største mulige verdien.
        // Deler den største mulige verdien på 100 for å få dette i prosent.
        if (mOligos != 0.0) {
            sOligos /= (mOligos / 100)
        } else {
            sOligos = 0.0
        }
        if (mFructose != 0.0) {
            sFructose /= (mFructose / 100)
        } else {
            sFructose = 0.0
        }
        if (mPolyols != 0.0) {
            sPolyols /= (mPolyols / 100)
        } else {
            sPolyols = 0.0
        }
        if (mLactose != 0.0) {
            sLactose /= (mLactose / 100)
        } else {
            sLactose = 0.0
        }
    }

    // Metode som konverterer alvorligheten som legges inn til en int
    private fun konverterAlvorlighetTall(alvorlighet: String): Int {
        if (alvorlighet == "Ingen alvorlighet") {
            return 0
        }
        if (alvorlighet == "Litt mild") {
            return 1
        }
        if (alvorlighet == "Svært mild") {
            return 2
        }
        if (alvorlighet == "Litt alvorlig") {
            return 3
        }
        if (alvorlighet == "Svært alvorlig") {
            return 4
        }
        throw Error("Ukjent alvorlighet")
    }
}