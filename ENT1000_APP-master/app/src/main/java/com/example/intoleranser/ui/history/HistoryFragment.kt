package com.example.intoleranser.ui.history

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intoleranser.DTO.RegisteredDTO
import com.example.intoleranser.R
import com.example.intoleranser.ui.calendar.ListAdapter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class HistoryFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_history, container, false)
        val gson = Gson()
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Henter argumenter sendt med når fragment var byttet
        val arguments = arguments
        val sukkerart: String? = arguments?.getString("sukkerart")

        // Setter tittel i action bar basert på valgt FODMap
        val actionbar = (activity as AppCompatActivity?)!!.supportActionBar!!
        actionbar.title = sukkerart
        // Tilbakeknapp på action bar
        actionbar.setDisplayHomeAsUpEnabled(true)

        fun fetchArrayList(): ArrayList<RegisteredDTO> {
            val yourArrayList: ArrayList<RegisteredDTO>
            val json = prefs.getString("food", "")

            yourArrayList = when {
                json.isNullOrEmpty() -> ArrayList()
                else -> gson.fromJson(json, object : TypeToken<List<RegisteredDTO>>() {}.type)
            }
            return yourArrayList
        }

        //Henter lista med registreringer
        val registeredList = fetchArrayList()
        // Fjerner de som ikke inneholder den valgte sukkerarten
        val removeList = mutableListOf<Any>()
        for (matvare in registeredList) {
            if (sukkerart == "Fructan og GOS") {
                if (matvare.foodDTO.details.fructose == 0) {
                    removeList.add(matvare)
                }
            } else if (sukkerart == "Fruktose") {
                if (matvare.foodDTO.details.oligos == 0) {
                    removeList.add(matvare)
                }
            } else if (sukkerart == "Polyoler") {
                if (matvare.foodDTO.details.polyols == 0) {
                    removeList.add(matvare)
                }
            } else if (sukkerart == "Laktose") {
                if (matvare.foodDTO.details.lactose == 0) {
                    removeList.add(matvare)
                }
            }
        }
        registeredList.removeAll(removeList)
        val rvliste = root.findViewById<RecyclerView>(R.id.rv_liste)
        rvliste.layoutManager = LinearLayoutManager(context)
        rvliste.adapter = ListAdapter(context!!, registeredList)

        return root
    }
}
