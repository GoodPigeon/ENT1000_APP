package com.example.intoleranser.ui.graph

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.intoleranser.DTO.IntoleranseDTO
import com.example.intoleranser.R
import com.example.intoleranser.ui.history.HistoryFragment
import kotlinx.android.synthetic.main.statistikk_listitem.view.*


class GraphlistAdapter(
    private val context: Context,
    private val elementer: List<IntoleranseDTO>,
    private val activity: FragmentActivity?
) : RecyclerView.Adapter<GraphlistAdapter.ViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.statistikk_listitem, parent, false)
        return ViewHolder(view, context, activity)
    }

    override fun getItemCount(): Int {
        return elementer.size
    }

    override fun onBindViewHolder(p0: ViewHolder, p1: Int) {
        val element = elementer[p1]
        p0.setData(element)
    }

    class ViewHolder(
        itemView: View,
        private val context: Context,
        private val activity: FragmentActivity?
    ) : RecyclerView.ViewHolder(itemView) {
        fun setData(element: IntoleranseDTO?){
            val arguments = Bundle()
            val sannsynlighetTekst = element?.sannsynlighet.toString() + "%"
            itemView.innholdnavn.text = element!!.navn
            itemView.beskrivelse.text = element.beskrivelse
            itemView.sannsynlighetverdi.text = sannsynlighetTekst

            // Setter tekstens farge etter hvor høy risikoen er
            if (element.sannsynlighet >= 75) {
                itemView.sannsynlighetverdi.setTextColor(Color.RED)
            } else if ((25 < element.sannsynlighet) && (element.sannsynlighet < 75)){
                itemView.sannsynlighetverdi.setTextColor(Color.parseColor("#E86826"))
            } else {
                itemView.sannsynlighetverdi.setTextColor(Color.GREEN)
            }

            // Setter bakgrunn på listeelementet og setter sender med hvilken type
            // det er hvis bruker går inn på historiesiden
            if(element.navn == "Fructan og GOS"){
                itemView.setBackgroundResource(R.drawable.oligos)
                arguments.putString("sukkerart", "Fructan og GOS")
            }
            if(element.navn == "Fruktose"){
                itemView.setBackgroundResource(R.drawable.fructose)
                arguments.putString("sukkerart", "Fruktose")
            }
            if(element.navn == "Polyoler"){
                itemView.setBackgroundResource(R.drawable.polyols)
                arguments.putString("sukkerart", "Polyoler")
            }
            if(element.navn == "Laktose"){
                itemView.setBackgroundResource(R.drawable.lactose)
                arguments.putString("sukkerart", "Laktose")
            }

            // Knapper for å se matvarene som har blitt spist av den typen FODMap
            itemView.historybtn.setOnClickListener {
                try {
                    val newFragment = HistoryFragment()
                    newFragment.arguments = arguments

                    // Henter navbar sine transactions
                    val transactionNav = activity?.supportFragmentManager?.findFragmentById(R.id.nav_host_fragment)
                    // Lager en child-transaction på dette
                    // Dette gjøres så tilbakeknapp fungerer riktig
                    val transaction = transactionNav?.childFragmentManager?.beginTransaction()
                    // Bytter til history side
                    if (transaction != null) {
                        transaction.replace(R.id.graph_container, newFragment)
                        transaction.addToBackStack(null)
                        transaction.commit()
                    }
                } catch (ex: Exception) {
                    Toast.makeText(context, "Noe gikk galt: $ex. Prøv igjen", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

