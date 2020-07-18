package com.example.intoleranser.ui.calendar

import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.intoleranser.DTO.RegisteredDTO
import com.example.intoleranser.R
import com.example.intoleranser.div.DelMeny
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class CalendarFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_calendar, container, false)
        val gson = Gson()
        val prefs: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

        // Setter tittel i action bar og setter opp deleknapp
        val actionbar = (activity as AppCompatActivity?)!!.supportActionBar!!
        actionbar.title = "Kalender"
        actionbar.setDisplayHomeAsUpEnabled(false)
        setHasOptionsMenu(true);

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
        val rvliste = root.findViewById<RecyclerView>(R.id.rv_liste)
        rvliste.layoutManager = LinearLayoutManager(context)
        rvliste.adapter = ListAdapter(context!!, registeredList)

        // Dersom backstack-teller = 1 settes tittel på nytt
        // Gjøres for å sette riktig tittel og fjerne tilbakeknapp når bruker går vekk fra historiesiden
        val transactionNav = activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment)
        transactionNav?.childFragmentManager?.addOnBackStackChangedListener {
            val fm: FragmentManager = transactionNav.childFragmentManager
            val backStackCount: Int = fm.backStackEntryCount
            if (backStackCount == 1) {
                actionbar.title = "Kalender"
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