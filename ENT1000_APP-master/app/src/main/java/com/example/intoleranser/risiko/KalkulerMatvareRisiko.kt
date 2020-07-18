package com.example.intoleranser.risiko

import android.content.SharedPreferences
import com.example.intoleranser.DTO.FoodDTO
import kotlin.math.roundToInt

// Klasse som brukes til å kalkulere brukerens risiko for å spise en matvare
class KalkulerMatvareRisiko(
    p: SharedPreferences
) {
    // Matvarens FODMaps 0-2
    var sOligos = 0
    var sFructose = 0
    var sPolyols = 0
    var sLactose = 0

    private val prefs = p
    private val totalRisiko = KalkulerTotalRisiko(prefs)

    // Brukerens risiko per FODMap i prosent
    private val pOligos = totalRisiko.sOligos
    private val pFructose = totalRisiko.sFructose
    private val pPolyols = totalRisiko.sPolyols
    private val pLactose = totalRisiko.sLactose

    // Metode som henter ut matvarens info fra json-filen
    fun finnMatvare(
        valgtMatvare: String,
        matDTO: ArrayList<FoodDTO>
    ) {
        // Finner matvaren i json-filen og lagrer unna dens FODMap
        for (mat in matDTO) {
            if (valgtMatvare == mat.name) {
                sOligos = mat.details.oligos
                sFructose = mat.details.fructose
                sPolyols = mat.details.polyols
                sLactose = mat.details.lactose
                return
            }
        }
    }

    // Metode som kalkulerer brukerens risiko for å spise en matvare
    fun kalkulerRisiko(): Int {
        val risiko: Double
        // Multipliserer matvarens FODMap 0-4 med brukerens risiko for dette
        val rOligos = sOligos * pOligos
        val rFructose = sFructose * pFructose
        val rPolyols = sPolyols * pPolyols
        val rLactose = sLactose * pLactose

        // Finner den sukkerarten med høyest risiko
        var rList = listOf(rOligos, rFructose, rPolyols, rLactose)
        rList = rList.sorted()
        // Deler med to for å få prosent
        risiko = rList[3] / 2
        return risiko.roundToInt()
    }

    // Metode som returnerer en streng som sier alle typer FODMaps matvaren inneholder
    fun matvareFODMaps(): String {
        var returStreng = ""
        var satt = false
        if (sOligos != 0) {
            returStreng += " Fructan og GOS"
            satt = true
        }
        if (sFructose != 0) {
            returStreng += " Fruktose"
            satt = true
        }
        if (sPolyols != 0) {
            returStreng += " Polyoler"
            satt = true
        }
        if (sLactose != 0) {
            returStreng += " Laktose"
            satt = true
        }

        return if (satt) {
            returStreng
        } else {
            " Ingen sukkerarter."
        }
    }
}