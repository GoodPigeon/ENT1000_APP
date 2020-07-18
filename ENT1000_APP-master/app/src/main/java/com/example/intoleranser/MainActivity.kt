package com.example.intoleranser

import android.os.Bundle
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navView: BottomNavigationView = findViewById(R.id.nav_view)

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, R.id.navigation_register, R.id.navigation_graph
            )
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.action_bar, menu)
        return true
    }

    // Når tilbakeknappen er trykket
    override fun onBackPressed() {
        // Henter navbar sin fragment-manager
        val transaction = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        // Sjekker antall transactions
        val count = transaction?.childFragmentManager?.backStackEntryCount
        if (count == 0) {
            // Bruker standard tilbakeknapp-behaviour
            super.onBackPressed()
        } else {
            // Går tilbake til forrige side
            supportFragmentManager.popBackStackImmediate()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        supportFragmentManager.popBackStackImmediate()
        return true
    }


}