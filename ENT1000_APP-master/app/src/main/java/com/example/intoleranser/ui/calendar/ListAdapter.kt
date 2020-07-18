package com.example.intoleranser.ui.calendar

import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intoleranser.DTO.RegisteredDTO
import com.example.intoleranser.R
import com.example.intoleranser.risiko.KalkulerMatvareRisiko
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.android.synthetic.main.edit_alertdialog.*
import kotlinx.android.synthetic.main.layout_listitem.view.*
import kotlinx.android.synthetic.main.registrer_alertdialog.alvorlighetsinput
import kotlinx.android.synthetic.main.registrer_alertdialog.registrerButton
import kotlinx.android.synthetic.main.registrer_alertdialog.symptominput
import java.text.SimpleDateFormat
import java.util.*
import java.util.Collections.swap
import kotlin.collections.ArrayList

class ListAdapter(private val context: Context, private val elements: ArrayList<RegisteredDTO>) : RecyclerView.Adapter<ListAdapter.MinViewHolder>() {
    private val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
    private val gson = Gson()
    private val symptomer = listOf("Ingen symptomer", "Kløe", "Diaré", "Utslett", "Magekramper", "Pustevansker", "Brystsmerter", "Hovne luftveier")
    private val alvorlighetsgrader = listOf("Ingen alvorlighet", "Svært alvorlig", "Litt alvorlig", "Litt mild", "Svært mild")

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MinViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_listitem, parent, false)
        return MinViewHolder(view)
    }

    override fun onBindViewHolder(holder: MinViewHolder, position: Int) {
        holder.listname.text = elements[position].foodDTO.name
        holder.listdate.text = elements[position].date
        holder.listsymptom.text = elements[position].symptom
        holder.listalvorlighet.text = elements[position].alvorlighet

        // Kalkuler risiko
        elements[position].foodDTO.name
        val risiko = KalkulerMatvareRisiko(prefs)
        risiko.sFructose = elements[position].foodDTO.details.fructose
        risiko.sPolyols = elements[position].foodDTO.details.polyols
        risiko.sOligos = elements[position].foodDTO.details.oligos
        risiko.sLactose = elements[position].foodDTO.details.lactose
        val kalkulertRisiko = risiko.kalkulerRisiko()
        val listRisikoTekst = "$kalkulertRisiko%"
        holder.listRisiko.text = listRisikoTekst

        // Setter tekstens farge etter hvor høy risikoen er
        if (kalkulertRisiko >= 75) {
            holder.listRisiko.setTextColor(Color.RED)
        } else if ((25 < kalkulertRisiko) && (kalkulertRisiko < 75)){
            holder.listRisiko.setTextColor(Color.parseColor("#E86826"))
        } else {
            holder.listRisiko.setTextColor(Color.GREEN)
        }
    }

    override fun getItemCount() = elements.size

    inner class MinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.listnavn
        val listdate: TextView = itemView.listdato
        val listname: TextView = itemView.listnavn
        val listRisiko: TextView = itemView.listrisiko
        val listsymptom: TextView = itemView.listsymptom
        val listalvorlighet: TextView = itemView.listalvorlighet
        init {
            val slettknapp = itemView.slettknapp
            val redigerknapp = itemView.redigerknapp
            slettknapp.setOnClickListener{
                removeItem(adapterPosition)
            }

            redigerknapp.setOnClickListener{
                changeItem(adapterPosition)
            }
        }
    }

    fun removeItem(position: Int){
        elements.removeAt(position)
        removeObjectFromArrayList(position)
        notifyItemRemoved(position)
    }

    fun changeItem(position: Int) {
        val chosen = elements[position]

        //Knapp for å registrere matvarer manuelt, åpner en alertdialog
        try {
            val alertDialogView =
                LayoutInflater.from(context).inflate(R.layout.edit_alertdialog, null)
            val builder = AlertDialog.Builder(context)
                .setView(alertDialogView)
                .setTitle("Endre verdier")
            val tmpalertdialog = builder.show()

            val navntekst = tmpalertdialog.navntekst
            navntekst.text = chosen.foodDTO.name
            var j = 0
            for (i in symptomer){
                if(i == chosen.symptom){
                    break
                }
                j++
            }
            swap(symptomer, 0, j)
            val symptomspinner = tmpalertdialog.symptominput
            val symptomadapter = ArrayAdapter(
                tmpalertdialog.context,
                android.R.layout.simple_spinner_dropdown_item,
                symptomer
            )
            tmpalertdialog.symptominput.adapter = symptomadapter

            var k = 0
            for (i in alvorlighetsgrader){
                if(i == chosen.alvorlighet){
                    break
                }
                k++
            }
            swap(alvorlighetsgrader, 0, k)

            val alvorlighetsspinner = tmpalertdialog.alvorlighetsinput
            val alvorlighetsadapter = ArrayAdapter(
                tmpalertdialog.context,
                android.R.layout.simple_spinner_dropdown_item,
                alvorlighetsgrader
            )
            tmpalertdialog.alvorlighetsinput.adapter = alvorlighetsadapter

            //Lagrer verdiene som dataobjekt RegisteredDTO i liste og lagrer det internt på mobilen
            val lagreknapp = tmpalertdialog.registrerButton
            lagreknapp.setOnClickListener {
                try {
                    val symptomvalue = symptomspinner.selectedItem.toString()
                    val alvorlighetsvalue = alvorlighetsspinner.selectedItem.toString()
                    val df = SimpleDateFormat("dd-MMM-yyyy")
                    val date: String = df.format(Calendar.getInstance().time)

                    val newElement = editObjectInArrayList(elements[position], date, symptomvalue, alvorlighetsvalue)
                    elements.removeAt(position)
                    removeObjectFromArrayList(position)
                    notifyItemRemoved(position)
                    elements.add(position, newElement)
                    addObjectToArrayList(position, newElement)
                    notifyItemInserted(position)

                    Toast.makeText(context, "Verdier registert!", Toast.LENGTH_LONG).show()
                    tmpalertdialog.cancel()
                } catch (ex: Exception) {
                    Toast.makeText(
                        context,
                        "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                        Toast.LENGTH_LONG
                    ).show()
                }
                tmpalertdialog.cancel()
            }
        } catch (ex: Exception) {
            Toast.makeText(
                context,
                "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    //Metode for å lagre registreringer til arraylist
    private fun removeObjectFromArrayList(position: Int) {
        val bookmarks = fetchArrayList()
        bookmarks.removeAt(position)
        val prefsEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        val json = gson.toJson(bookmarks)
        prefsEditor.putString("food", json)
        prefsEditor.apply()
    }

    //Metode for å lagre registreringer til arraylist
    private fun addObjectToArrayList(position: Int, registeredDTO: RegisteredDTO) {
        val bookmarks = fetchArrayList()
        bookmarks.add(position, registeredDTO)
        val prefsEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        val json = gson.toJson(bookmarks)
        prefsEditor.putString("food", json)
        prefsEditor.apply()
    }

    private fun editObjectInArrayList(element: RegisteredDTO, date: String, newSymptom: String, newAlvorlighet: String): RegisteredDTO {
        element.date = date
        element.symptom = newSymptom
        element.alvorlighet = newAlvorlighet
        return element
    }

    //Metode for å hente arraylist av registreringer
    private fun fetchArrayList(): ArrayList<RegisteredDTO> {
        val yourArrayList: ArrayList<RegisteredDTO>
        val json = PreferenceManager.getDefaultSharedPreferences(context).getString("food", "")

        yourArrayList = when {
            json.isNullOrEmpty() -> ArrayList()
            else -> gson.fromJson(json, object : TypeToken<List<RegisteredDTO>>() {}.type)
        }

        return yourArrayList
    }
}
