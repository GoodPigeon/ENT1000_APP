package com.example.intoleranser.ui.graph

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intoleranser.DTO.IntoleranseDTO
import com.example.intoleranser.R
import com.example.intoleranser.div.DelMeny
import com.example.intoleranser.risiko.KalkulerTotalRisiko
import kotlin.math.roundToInt


class GraphFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_graph, container, false)
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        val resultater = KalkulerTotalRisiko(prefs)

        // Setter tittel i action-bar og setter opp tilbake- og deleknapp
        val actionbar = (activity as AppCompatActivity?)!!.supportActionBar!!
        actionbar.title = "Graf"
        actionbar.setDisplayHomeAsUpEnabled(false)
        setHasOptionsMenu(true)

        // Sjekker at nok måltider har blitt spist
        if (resultater.antallMaaltider < 1) {
            Toast.makeText(context, "Du må minst ha spist ti måltider som inneholder FODMaps før resultatet vises", Toast.LENGTH_LONG).show()
            val oligos = IntoleranseDTO(navn ="Fructan og GOS", beskrivelse = "Fructan og GOS (Oligos) finnes i matvarer som hvete, rug og løk, mens kål og belggrønnsaker er kilder til GOS.",  sannsynlighet = 0)
            val fructose = IntoleranseDTO(navn ="Fruktose", beskrivelse = "Fruktose er en sukkerart som forekommer i søte frukter, bær, honning og grønnsaker.",  sannsynlighet = 0)
            val polyols = IntoleranseDTO(navn ="Polyoler", beskrivelse = "Polyoler brukes som søtstoff, for eksempel sorbitol og mannitol i sukkerfri tyggegummi og pastiller. Naturlige kilder er blant annet bjørnebær, sopp og blomkål.",  sannsynlighet = 0)
            val lactose = IntoleranseDTO(navn ="Laktose", beskrivelse = "Laktose, også kalt melkesukker, er et karbohydrat man finner naturlig i melken til alle pattedyr.",  sannsynlighet = 0)

            val registeredList = arrayListOf(oligos, fructose, polyols, lactose)
            val rvliste = root.findViewById<RecyclerView>(R.id.rv_liste2)
            rvliste.layoutManager = LinearLayoutManager(context)
            rvliste.adapter = GraphlistAdapter(context!!, registeredList, activity)
        } else {
            //Henter lista med registreringer
            val oligos = IntoleranseDTO(navn ="Fructan og GOS",
                beskrivelse = "Fructan og GOS (Oligos) finnes i matvarer som hvete, rug og løk, mens kål og belggrønnsaker er kilder til GOS.",
                sannsynlighet = resultater.sOligos.roundToInt())
            val fructose = IntoleranseDTO(navn ="Fruktose",
                beskrivelse = "Fruktose er en sukkerart som forekommer i søte frukter, bær, honning og grønnsaker.",
                sannsynlighet = resultater.sFructose.roundToInt())
            val polyols = IntoleranseDTO(navn ="Polyoler",
                beskrivelse = "Polyoler brukes som søtstoff, for eksempel sorbitol og mannitol i sukkerfri tyggegummi og pastiller. Naturlige kilder er blant annet bjørnebær, sopp og blomkål.",
                sannsynlighet = resultater.sPolyols.roundToInt())
            val lactose = IntoleranseDTO(navn ="Laktose",
                beskrivelse = "Laktose, også kalt melkesukker, er et karbohydrat man finner naturlig i melken til alle pattedyr.",
                sannsynlighet = resultater.sLactose.roundToInt())

            val registeredList = arrayListOf(oligos, fructose, polyols, lactose)
            val sortedList = registeredList.sortedWith(compareBy {it.sannsynlighet}).reversed()
            val rvliste = root.findViewById<RecyclerView>(R.id.rv_liste2)
            rvliste.layoutManager = LinearLayoutManager(context)
            rvliste.adapter = GraphlistAdapter(context!!, sortedList, activity)
        }

        // Dersom backstack-teller = 1 settes tittel på nytt
        // Gjøres for å sette riktig tittel og fjerne tilbakeknapp når bruker går vekk fra historiesiden
        val transactionNav = activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment)
        transactionNav?.childFragmentManager?.addOnBackStackChangedListener {
            val fm: FragmentManager = transactionNav.childFragmentManager
            val backStackCount: Int = fm.backStackEntryCount
            if (backStackCount == 1) {
                actionbar.title = "Graf"
                actionbar.setDisplayHomeAsUpEnabled(false)
            }
        }

        return root
    }

    // Viser deleknapp
    override fun onPrepareOptionsMenu(menu: Menu) {
        val item: MenuItem = menu.findItem(R.id.delbtn)
        item.isVisible = true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.delbtn -> {
                DelMeny(context, view)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
