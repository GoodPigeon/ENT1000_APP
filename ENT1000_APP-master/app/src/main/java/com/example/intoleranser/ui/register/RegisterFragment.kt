package com.example.intoleranser.ui.register

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import androidx.preference.PreferenceManager
import com.example.intoleranser.DTO.EngFoodDTO
import com.example.intoleranser.DTO.FoodDTO
import com.example.intoleranser.DTO.RegisteredDTO
import com.example.intoleranser.GetCalls
import com.example.intoleranser.MainActivity
import com.example.intoleranser.R
import com.example.intoleranser.RetrofitClientInstance
import com.example.intoleranser.div.InternetCheck
import com.example.intoleranser.risiko.KalkulerMatvareRisiko
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionCloudImageLabelerOptions
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceImageLabelerOptions
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.wonderkiln.camerakit.*
import com.wonderkiln.camerakit.CameraKit.Constants.METHOD_STILL
import kotlinx.android.synthetic.main.choosealert.view.*
import kotlinx.android.synthetic.main.fragment_register.*
import kotlinx.android.synthetic.main.registrer_alertdialog.*
import kotlinx.android.synthetic.main.registrer_alertdialog.view.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.String.CASE_INSENSITIVE_ORDER
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// Bør splittes opp i flere klasser
class RegisterFragment : Fragment() {

    private lateinit var registerViewModel: RegisterViewModel
    private val gson = Gson()
    val matnavn = ArrayList<String>()
    val matDTO = ArrayList<FoodDTO>()
    val engelskMatDTO = ArrayList<EngFoodDTO>()
    var utvalg = ArrayList<String>()
    //Lokale variabler
    private val symptomer = listOf("Ingen symptomer", "Kløe", "Diaré", "Utslett", "Magekramper", "Pustevansker", "Brystsmerter", "Hovne luftveier")
    private val alvorlighetsgrader = listOf("Ingen alvorlighet", "Svært alvorlig", "Litt alvorlig", "Litt mild", "Svært mild")

    // Vekker kamera etter pause
    override fun onResume() {
        super.onResume()
        camera_view.start()
    }

    // Lukker kamera i pausemodus
    override fun onPause() {
        super.onPause()
        camera_view.stop()
    }

    //Metode for å lagre registreringer til arraylist
    private fun saveObjectToArrayList(yourObject: RegisteredDTO) {
        val bookmarks = fetchArrayList()
        bookmarks.add(0, yourObject)
        val prefsEditor = PreferenceManager.getDefaultSharedPreferences(context).edit()

        val json = gson.toJson(bookmarks)
        prefsEditor.putString("food", json)
        prefsEditor.apply()
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

    override fun onCreateView(

        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val service = RetrofitClientInstance.retrofitInstanse!!.create(GetCalls::class.java)
        val call = service.getAllFoods()
        val engCall = service.getAllFoodsEng()

        // Setter tittel i action bar
        (activity as AppCompatActivity?)!!.supportActionBar!!.title = "Registrer"
        (activity as AppCompatActivity?)!!.supportActionBar!!.setDisplayHomeAsUpEnabled(false);

        // Kobler til NORSK database gjennom internett
        call.enqueue(object : Callback<List<FoodDTO>> {
            override fun onFailure(call: Call<List<FoodDTO>>, t: Throwable) {
                Toast.makeText(context, "Error reading JSON!", Toast.LENGTH_LONG).show()
            }

            //If its connected to the API successfully
            override fun onResponse(call: Call<List<FoodDTO>>, response: Response<List<FoodDTO>>) {
                val foodElements = response.body()
                for (mat in foodElements!!) {
                    matnavn.add(mat.name)
                    matDTO.add(mat)
                }
            }
        })

        // Kobler til ENGELSK database gjennom internett
        engCall.enqueue(object : Callback<List<EngFoodDTO>> {
            override fun onFailure(call: Call<List<EngFoodDTO>>, t: Throwable) {
                Toast.makeText(context, "Error reading JSON!", Toast.LENGTH_LONG).show()
            }

            //If its connected to the API successfully
            override fun onResponse(call: Call<List<EngFoodDTO>>, response: Response<List<EngFoodDTO>>) {
                val foodElements = response.body()
                for (mat in foodElements!!) {
                    engelskMatDTO.add(mat)
                }
            }
        })

        registerViewModel =
            ViewModelProviders.of(this).get(RegisterViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_register, container, false)
        val btn2: FloatingActionButton = root.findViewById(R.id.manualbutton)
        val btnDetect = root.findViewById<FloatingActionButton>(R.id.btn_detect)
        val cameraView =root.findViewById<CameraView>(R.id.camera_view)

        btnDetect.setOnClickListener {
            cameraView.start()
            cameraView.setMethod(METHOD_STILL)
            cameraView.captureImage()
        }

        cameraView.addCameraKitListener(object: CameraKitEventListener {
            override fun onVideo(p0: CameraKitVideo?) {
                // Ikke brukt
            }

            override fun onEvent(p0: CameraKitEvent?) {
                // Ikke brukt
            }

            // Når et bilde blir tatt
            override fun onImage(p0: CameraKitImage?) {
                // Skriver "laster"
                Toast.makeText(context, "Laster...", Toast.LENGTH_SHORT).show()

                // Lagrer bildet til bitmap og lukker kamera
                var bitmapBilde = p0!!.bitmap

                bitmapBilde = Bitmap.createScaledBitmap(bitmapBilde, cameraView.width, cameraView.height, false)
                cameraView.stop()

                kjorDetektering(bitmapBilde)
            }

            override fun onError(p0: CameraKitError?) {
                // Ikke brukt
            }
        })

        //Knapp for å registrere matvarer manuelt, åpner en alertdialog
        btn2.setOnClickListener{
            try {
                val alertDialogView = LayoutInflater.from(context).inflate(R.layout.registrer_alertdialog, null)
                val builder = AlertDialog.Builder(context)
                    .setView(alertDialogView)
                    .setTitle("Registrer matvare manuelt")
                val tmpalertdialog = builder.show()

                val navninput = alertDialogView.matinput
                val navneforslagAdapter = ArrayAdapter(tmpalertdialog.context, android.R.layout.simple_list_item_1, matnavn)
                navninput.setAdapter(navneforslagAdapter)
                navninput.threshold = 1

                val symptomspinner = tmpalertdialog.symptominput
                val symptomadapter = ArrayAdapter(tmpalertdialog.context, android.R.layout.simple_spinner_dropdown_item, symptomer)
                tmpalertdialog.symptominput.adapter = symptomadapter

                val alvorlighetsspinner = tmpalertdialog.alvorlighetsinput
                val alvorlighetsadapter = ArrayAdapter(tmpalertdialog.context, android.R.layout.simple_spinner_dropdown_item, alvorlighetsgrader)
                tmpalertdialog.alvorlighetsinput.adapter = alvorlighetsadapter

                // Kalkuler risiko
                navninput.setOnItemClickListener { parent, _, position, _ ->
                    try {
                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                        val risikoTekst = tmpalertdialog.risikoView
                        val innholdTekst = tmpalertdialog.innholdView

                        val valgtMatvare = parent.getItemAtPosition(position)
                        val risiko = KalkulerMatvareRisiko(prefs)
                        risiko.finnMatvare(valgtMatvare as String, matDTO)
                        val kalkulertRisiko = risiko.kalkulerRisiko()

                        val risikoFarge: Int
                        // Setter tekstens farge etter hvor høy risikoen er
                        risikoFarge = if (kalkulertRisiko >= 75) {
                            Color.RED
                        } else if ((25 < kalkulertRisiko) && (kalkulertRisiko < 75)){
                            Color.parseColor("#E86826")
                        } else {
                            Color.GREEN
                        }

                        val risikoSpan: Spannable = SpannableString("Risiko: $kalkulertRisiko%")
                        risikoSpan.setSpan(
                            ForegroundColorSpan(risikoFarge), 8, risikoSpan.length,
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        )
                        risikoTekst.text = risikoSpan

                        innholdTekst.text = "Inneholder:" + risiko.matvareFODMaps()
                    } catch (ex: Exception) {
                        Toast.makeText(context, "Kalkulering av risiko feilet: $ex. Prøv igjen", Toast.LENGTH_LONG).show()
                    }
                }

                //Lagrer verdiene som dataobjekt RegisteredDTO i liste og lagrer det internt på mobilen
                 val lagreknapp = tmpalertdialog.registrerButton
                lagreknapp.setOnClickListener {
                    try{
                        val navnString = navninput!!.text.toString()

                        var varenFinnes = false
                        for (mat in matDTO){
                            if(mat.name == navnString && !varenFinnes){
                                varenFinnes = true
                                val symptomvalue = symptomspinner.selectedItem.toString()
                                val alvorlighetsvalue = alvorlighetsspinner.selectedItem.toString()
                                val df = SimpleDateFormat("dd-MMM-yyyy")
                                val date: String = df.format(Calendar.getInstance().time)

                                val registeredDTO = RegisteredDTO(mat, date, symptomvalue, alvorlighetsvalue)
                                saveObjectToArrayList(registeredDTO)
                            }
                        }
                        if (!varenFinnes){
                            Toast.makeText(context, "Noe gikk galt med å lagre dine verdier. Prøv igjen", Toast.LENGTH_LONG).show()
                            tmpalertdialog.cancel()
                            val intent = Intent(context, MainActivity::class.java)
                            startActivity(intent)
                        }
                        else {
                            Toast.makeText(context, "Verdier registert!", Toast.LENGTH_LONG).show()
                            tmpalertdialog.cancel()
                            val intent = Intent(context, MainActivity::class.java)
                            startActivity(intent)

                        }
                    }
                    catch (ex: Exception){
                        Toast.makeText(context, "Noe gikk galt med å lagre dine verdier. Prøv igjen", Toast.LENGTH_LONG).show()
                        val intent = Intent(context, MainActivity::class.java)
                        startActivity(intent)
                    }
                    tmpalertdialog.cancel()
                }
            }
            catch (ex: Exception){
                Toast.makeText(context, "Noe gikk galt", Toast.LENGTH_LONG).show()
                val intent = Intent(context, MainActivity::class.java)
                startActivity(intent)
            }
        }


        return root
    }

    private fun kjorDetektering(bitmap: Bitmap?) {
        // Gjør bitmap klar til Vision API
        val visionBilde = FirebaseVisionImage.fromBitmap(bitmap!!)

        // Sjekker at enheten har internett
        InternetCheck(object: InternetCheck.Consumer {
            override fun accept(isConnected: Boolean?) {
                if (isConnected!!) {
                    // Detekterer med Cloud Detect
                    val visionInstillinger = FirebaseVisionCloudImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.8f) // Hent resultater med over 80% sikkerhet
                        .build()

                    // Lager Vision API instans
                    val detektor = FirebaseVision.getInstance().getCloudImageLabeler(visionInstillinger)

                    // Prosesserer bildet, mottar labels og kjører metoden som går gjennom resultatet
                    detektor.processImage(visionBilde)
                        .addOnFailureListener{ e -> Log.d("EDMTERROR", e.message!!) }
                        .addOnSuccessListener { result -> prosesserSkyResultat(result) }
                } else {
                    // Detekterer lokalt på enheten
                    val visionInstillinger = FirebaseVisionOnDeviceImageLabelerOptions.Builder()
                        .setConfidenceThreshold(0.8f) // Hent resultater med over 80% sikkerhet
                        .build()

                    val detektor = FirebaseVision.getInstance().getOnDeviceImageLabeler(visionInstillinger)

                    detektor.processImage(visionBilde)
                        .addOnFailureListener{ e -> Log.d("EDMTERROR", e.message!!) }
                        .addOnSuccessListener { result -> prosesserEnhetResultat(result) }
                }
            }
        })
    }
    private fun prosesserSkyResultat(resultat: List<FirebaseVisionImageLabel>) {
        var foundResult = false
        var norsknavn: String
        utvalg.clear()
        for (label in resultat) {
            if (!foundResult) {
             for (navn in engelskMatDTO) {
                    if (navn.name.contains(label.text) || navn.category.contains((label.text))) {
                        val id = navn.id
                        foundResult = true
                        if(navn.name == label.text) {
                            for (mat in matDTO) {
                                if (mat.id == id) {
                                    norsknavn = mat.name
                                    utvalg.add(norsknavn)
                                }
                            }
                        }
                        else if(navn.category.contains(label.text)){
                            for (mat in matDTO) {
                                if (mat.id == id) {
                                    norsknavn = mat.name
                                    utvalg.add(norsknavn)
                                }
                            }
                        }
                        else{
                            for (mat in matDTO) {
                                if (mat.id == id) {
                                    norsknavn = mat.name
                                    utvalg.add(norsknavn)
                                }
                            }
                        }
                        //Dersom utvalget er større enn en verdi
                        if (utvalg.size > 1) {
                            try {
                                val valgAlertDialogView = LayoutInflater.from(context)
                                    .inflate(R.layout.choosealert, null)

                                val builder = AlertDialog.Builder(context)
                                    .setView(valgAlertDialogView)
                                    .setTitle("Velg matvare")
                                val tmpValgAlertDialog = builder.show()


                                val valgspinner = valgAlertDialogView.valgspinner
                                val valgadapter = ArrayAdapter(
                                    valgAlertDialogView.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    utvalg
                                )
                                valgAlertDialogView.valgspinner.adapter = valgadapter

                                val velgknapp = valgAlertDialogView.velgButton
                                val ingenknapp = valgAlertDialogView.ingenButton

                                velgknapp.setOnClickListener {
                                    val valgtmat = valgspinner.selectedItem.toString()
                                    tmpValgAlertDialog.dismiss()

                                    try {
                                        val alertDialogView = LayoutInflater.from(context)
                                            .inflate(R.layout.registrer_alertdialog, null)
                                        val builder = AlertDialog.Builder(context)
                                            .setView(alertDialogView)
                                            .setTitle("Registrer matvare manuelt")
                                        val tmpalertdialog = builder.show()

                                        val navninput = alertDialogView.matinput
                                        val navneforslagAdapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_list_item_1,
                                            matnavn
                                        )
                                        navninput.setAdapter(navneforslagAdapter)
                                        navninput.threshold = 1
                                        navninput.setText(valgtmat)

                                        // Kalkuler risiko
                                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                        val risikoTekst = tmpalertdialog.risikoView
                                        val innholdTekst = tmpalertdialog.innholdView

                                        val risiko = KalkulerMatvareRisiko(prefs)
                                        risiko.finnMatvare(valgtmat, matDTO)
                                        val kalkulertRisiko = risiko.kalkulerRisiko()

                                        val risikoFarge: Int
                                        // Setter tekstens farge etter hvor høy risikoen er
                                        risikoFarge = if (kalkulertRisiko >= 75) {
                                            Color.RED
                                        } else if ((25 < kalkulertRisiko) && (kalkulertRisiko < 75)){
                                            Color.parseColor("#E86826")
                                        } else {
                                            Color.GREEN
                                        }

                                        val risikoSpan: Spannable = SpannableString("Risiko: $kalkulertRisiko%")
                                        risikoSpan.setSpan(
                                            ForegroundColorSpan(risikoFarge), 8, risikoSpan.length,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                        risikoTekst.text = risikoSpan

                                        innholdTekst.text = "Inneholder:" + risiko.matvareFODMaps()

                                        val symptomspinner = tmpalertdialog.symptominput
                                        val symptomadapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_spinner_dropdown_item,
                                            symptomer
                                        )
                                        tmpalertdialog.symptominput.adapter = symptomadapter

                                        val alvorlighetsspinner = tmpalertdialog.alvorlighetsinput
                                        val alvorlighetsadapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_spinner_dropdown_item,
                                            alvorlighetsgrader
                                        )
                                        tmpalertdialog.alvorlighetsinput.adapter =
                                            alvorlighetsadapter

                                        //Lagrer verdiene som dataobjekt RegisteredDTO i liste og lagrer det internt på mobilen
                                        val lagreknapp = tmpalertdialog.registrerButton
                                        lagreknapp.setOnClickListener {
                                            try {
                                                val navnString = navninput!!.text.toString()
                                                var varenFinnes = false
                                                for (mat in matDTO) {
                                                    if (mat.name == navnString && !varenFinnes) {
                                                        varenFinnes = true
                                                        val symptomvalue =
                                                            symptomspinner.selectedItem.toString()
                                                        val alvorlighetsvalue =
                                                            alvorlighetsspinner.selectedItem.toString()
                                                        val df = SimpleDateFormat("dd-MMM-yyyy")
                                                        val date: String =
                                                            df.format(Calendar.getInstance().time)

                                                        val registeredDTO = RegisteredDTO(
                                                            mat,
                                                            date,
                                                            symptomvalue,
                                                            alvorlighetsvalue
                                                        )
                                                        saveObjectToArrayList(registeredDTO)
                                                    }
                                                }
                                                if (!varenFinnes) {
                                                    Toast.makeText(
                                                        context,
                                                        "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Verdier registert!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }

                                            } catch (ex: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            val intent = Intent(context, MainActivity::class.java)
                                            startActivity(intent)
                                        }

                                    } catch (ex: Exception) {
                                    }
                                }
                                ingenknapp.setOnClickListener {
                                    try {
                                        val alertDialogView = LayoutInflater.from(context)
                                            .inflate(R.layout.registrer_alertdialog, null)
                                        val builder =
                                            AlertDialog.Builder(context).setView(alertDialogView)
                                                .setTitle("Registrer matvare")
                                        val tmpalertdialog = builder.show()
                                        val navninput = alertDialogView.matinput
                                        val navneforslagAdapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_list_item_1,
                                            matnavn
                                        )
                                        navninput.setAdapter(navneforslagAdapter)
                                        navninput.threshold = 1

                                        // Kalkuler risiko
                                        navninput.setOnItemClickListener { parent, _, position, _ ->
                                            try {
                                                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                                val risikoTekst = tmpalertdialog.risikoView
                                                val innholdTekst = tmpalertdialog.innholdView

                                                val valgtMatvare = parent.getItemAtPosition(position)
                                                val risiko = KalkulerMatvareRisiko(prefs)
                                                risiko.finnMatvare(valgtMatvare as String, matDTO)
                                                val kalkulertRisiko = risiko.kalkulerRisiko()

                                                val risikoFarge: Int
                                                // Setter tekstens farge etter hvor høy risikoen er
                                                if (kalkulertRisiko >= 75) {
                                                    risikoFarge = Color.RED
                                                } else if ((25 < kalkulertRisiko) && (kalkulertRisiko < 75)){
                                                    risikoFarge = Color.parseColor("#E86826")
                                                } else {
                                                    risikoFarge = Color.GREEN
                                                }

                                                val risikoSpan: Spannable = SpannableString("Risiko: $kalkulertRisiko%")
                                                risikoSpan.setSpan(
                                                    ForegroundColorSpan(risikoFarge), 8, risikoSpan.length,
                                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                                )
                                                risikoTekst.text = risikoSpan

                                                innholdTekst.text = "Inneholder:" + risiko.matvareFODMaps()
                                            } catch (ex: Exception) {
                                                Toast.makeText(context, "Kalkulering av risiko feilet: $ex. Prøv igjen", Toast.LENGTH_LONG).show()
                                            }
                                        }

                                        val symptomspinner = tmpalertdialog.symptominput
                                        val symptomadapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_spinner_dropdown_item,
                                            symptomer
                                        )
                                        tmpalertdialog.symptominput.adapter = symptomadapter

                                        val alvorlighetsspinner = tmpalertdialog.alvorlighetsinput
                                        val alvorlighetsadapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_spinner_dropdown_item,
                                            alvorlighetsgrader
                                        )
                                        tmpalertdialog.alvorlighetsinput.adapter =
                                            alvorlighetsadapter

                                        //Lagrer verdiene som dataobjekt RegisteredDTO i liste og lagrer det internt på mobilen
                                        val lagreknapp = tmpalertdialog.registrerButton
                                        lagreknapp.setOnClickListener {
                                            try {
                                                val navnString = navninput!!.text.toString()
                                                var varenFinnes = false
                                                for (mat in matDTO) {
                                                    if (mat.name == navnString && !varenFinnes) {
                                                        varenFinnes = true
                                                        val foodDTOvalue = mat
                                                        val symptomvalue =
                                                            symptomspinner.selectedItem.toString()
                                                        val alvorlighetsvalue =
                                                            alvorlighetsspinner.selectedItem.toString()
                                                        val df = SimpleDateFormat("dd-MMM-yyyy")
                                                        val date: String =
                                                            df.format(Calendar.getInstance().time)

                                                        val registeredDTO = RegisteredDTO(
                                                            foodDTOvalue,
                                                            date,
                                                            symptomvalue,
                                                            alvorlighetsvalue
                                                        )
                                                        saveObjectToArrayList(registeredDTO)
                                                    }
                                                }
                                                if (!varenFinnes) {
                                                    Toast.makeText(
                                                        context,
                                                        "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Verdier registert!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }

                                            } catch (ex: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            val intent = Intent(context, MainActivity::class.java)
                                            startActivity(intent)
                                        }

                                    } catch (ex: Exception) {
                                    }
                                }


                            }catch (ex: Exception){

                            }
                        }
                        //Dersom utvalget bare er en verdi
                        else {
                            try {
                                val alertDialogView = LayoutInflater.from(context)
                                    .inflate(R.layout.registrer_alertdialog, null)
                                val builder = AlertDialog.Builder(context).setView(alertDialogView)
                                    .setTitle("Registrer matvare")
                                val tmpalertdialog = builder.show()
                                val navninput = alertDialogView.matinput
                                val navneforslagAdapter = ArrayAdapter(
                                    tmpalertdialog.context,
                                    android.R.layout.simple_list_item_1,
                                    matnavn
                                )
                                navninput.setAdapter(navneforslagAdapter)
                                navninput.threshold = 1
                                navninput.setText(utvalg[0])

                                // Kalkuler risiko
                                navninput.setOnItemClickListener { _, _, _, _ ->
                                    try {
                                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                        val risikoTekst = tmpalertdialog.risikoView
                                        val innholdTekst = tmpalertdialog.innholdView

                                        val risiko = KalkulerMatvareRisiko(prefs)
                                        val kalkulertRisiko = risiko.kalkulerRisiko()

                                        val risikoFarge: Int
                                        // Setter tekstens farge etter hvor høy risikoen er
                                        risikoFarge = if (kalkulertRisiko >= 75) {
                                            Color.RED
                                        } else if ((25 < kalkulertRisiko) && (kalkulertRisiko < 75)){
                                            Color.parseColor("#E86826")
                                        } else {
                                            Color.GREEN
                                        }

                                        val risikoSpan: Spannable = SpannableString("Risiko: $kalkulertRisiko%")
                                        risikoSpan.setSpan(
                                            ForegroundColorSpan(risikoFarge), 8, risikoSpan.length,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                        risikoTekst.text = risikoSpan

                                        innholdTekst.text = "Inneholder:" + risiko.matvareFODMaps()
                                    } catch (ex: Exception) {
                                        Toast.makeText(context, "Kalkulering av risiko feilet: $ex. Prøv igjen", Toast.LENGTH_LONG).show()
                                    }
                                }

                                val symptomspinner = tmpalertdialog.symptominput
                                val symptomadapter = ArrayAdapter(
                                    tmpalertdialog.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    symptomer
                                )
                                tmpalertdialog.symptominput.adapter = symptomadapter

                                val alvorlighetsspinner = tmpalertdialog.alvorlighetsinput
                                val alvorlighetsadapter = ArrayAdapter(
                                    tmpalertdialog.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    alvorlighetsgrader
                                )
                                tmpalertdialog.alvorlighetsinput.adapter =
                                    alvorlighetsadapter

                                //Lagrer verdiene som dataobjekt RegisteredDTO i liste og lagrer det internt på mobilen
                                val lagreknapp = tmpalertdialog.registrerButton
                                lagreknapp.setOnClickListener {
                                    try {
                                        val navnString = navninput!!.text.toString()
                                        var varenFinnes = false
                                        for (mat in matDTO) {
                                            if (mat.name == navnString && !varenFinnes) {
                                                varenFinnes = true
                                                val symptomvalue =
                                                    symptomspinner.selectedItem.toString()
                                                val alvorlighetsvalue =
                                                    alvorlighetsspinner.selectedItem.toString()
                                                val df = SimpleDateFormat("dd-MMM-yyyy")
                                                val date: String =
                                                    df.format(Calendar.getInstance().time)

                                                val registeredDTO = RegisteredDTO(
                                                    mat,
                                                    date,
                                                    symptomvalue,
                                                    alvorlighetsvalue
                                                )
                                                saveObjectToArrayList(registeredDTO)
                                            }
                                        }
                                        if (!varenFinnes) {
                                            Toast.makeText(
                                                context,
                                                "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Verdier registert!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                    } catch (ex: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    val intent = Intent(context, MainActivity::class.java)
                                    startActivity(intent)
                                }

                            } catch (ex: Exception) {
                            }
                        }
                    }
                }
            }
        }
        if (!foundResult) {
            Toast.makeText(context, "Bildet ga ingen resultater, prøv igjen eller skriv inn manuelt", Toast.LENGTH_LONG).show()
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
            }
        }

    private fun prosesserEnhetResultat(resultat: List<FirebaseVisionImageLabel>) {
        var foundResult = false
        var norsknavn: String
        utvalg.clear()
        for (label in resultat) {
            if (!foundResult) {
                for (navn in engelskMatDTO) {
                    if (navn.name.contains(label.text) || navn.category.contains((label.text))) {
                        val id = navn.id
                        foundResult = true
                        if(navn.name == label.text) {
                            for (mat in matDTO) {
                                if (mat.id == id) {
                                    norsknavn = mat.name
                                    utvalg.add(norsknavn)
                                }
                            }
                        }
                        else if(navn.category.contains(label.text)){
                            for (mat in matDTO) {
                                if (mat.id == id) {
                                    norsknavn = mat.name
                                    utvalg.add(norsknavn)
                                }
                            }
                            Collections.sort(utvalg, CASE_INSENSITIVE_ORDER)
                        }
                        else{
                            for (mat in matDTO) {
                                if (mat.id == id) {
                                    norsknavn = mat.name
                                    utvalg.add(norsknavn)
                                }
                            }
                        }
                        //Dersom utvalget er større enn en verdi
                        if (utvalg.size > 1) {
                            try {
                                val valgAlertDialogView = LayoutInflater.from(context)
                                    .inflate(R.layout.choosealert, null)

                                val builder = AlertDialog.Builder(context)
                                    .setView(valgAlertDialogView)
                                    .setTitle("Velg matvare")
                                val tmpValgAlertDialog = builder.show()

                                val valgspinner = valgAlertDialogView.valgspinner
                                val valgadapter = ArrayAdapter(
                                    valgAlertDialogView.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    utvalg
                                )
                                valgAlertDialogView.valgspinner.adapter = valgadapter

                                val velgknapp = valgAlertDialogView.velgButton
                                val ingenknapp = valgAlertDialogView.ingenButton

                                velgknapp.setOnClickListener {
                                    val valgtmat = valgspinner.selectedItem.toString()
                                    tmpValgAlertDialog.dismiss()

                                    try {
                                        val alertDialogView = LayoutInflater.from(context)
                                            .inflate(R.layout.registrer_alertdialog, null)
                                        val builder = AlertDialog.Builder(context)
                                            .setView(alertDialogView)
                                            .setTitle("Registrer matvare manuelt")
                                        val tmpalertdialog = builder.show()

                                        val navninput = alertDialogView.matinput
                                        val navneforslagAdapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_list_item_1,
                                            matnavn
                                        )
                                        navninput.setAdapter(navneforslagAdapter)
                                        navninput.threshold = 1
                                        navninput.setText(valgtmat)

                                        // Kalkuler risiko
                                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                        val risikoTekst = tmpalertdialog.risikoView
                                        val innholdTekst = tmpalertdialog.innholdView

                                        val risiko = KalkulerMatvareRisiko(prefs)
                                        val kalkulertRisiko = risiko.kalkulerRisiko()

                                        val risikoFarge: Int
                                        // Setter tekstens farge etter hvor høy risikoen er
                                        if (kalkulertRisiko >= 75) {
                                            risikoFarge = Color.RED
                                        } else if ((25 < kalkulertRisiko) && (kalkulertRisiko < 75)){
                                            risikoFarge = Color.parseColor("#E86826")
                                        } else {
                                            risikoFarge = Color.GREEN
                                        }

                                        val risikoSpan: Spannable = SpannableString("Risiko: $kalkulertRisiko%")
                                        risikoSpan.setSpan(
                                            ForegroundColorSpan(risikoFarge), 8, risikoSpan.length,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                        risikoTekst.text = risikoSpan

                                        innholdTekst.text = "Inneholder:" + risiko.matvareFODMaps()

                                        val symptomspinner = tmpalertdialog.symptominput
                                        val symptomadapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_spinner_dropdown_item,
                                            symptomer
                                        )
                                        tmpalertdialog.symptominput.adapter = symptomadapter

                                        val alvorlighetsspinner = tmpalertdialog.alvorlighetsinput
                                        val alvorlighetsadapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_spinner_dropdown_item,
                                            alvorlighetsgrader
                                        )
                                        tmpalertdialog.alvorlighetsinput.adapter =
                                            alvorlighetsadapter

                                        //Lagrer verdiene som dataobjekt RegisteredDTO i liste og lagrer det internt på mobilen
                                        val lagreknapp = tmpalertdialog.registrerButton
                                        lagreknapp.setOnClickListener {
                                            try {
                                                val navnString = navninput!!.text.toString()
                                                var varenFinnes = false
                                                for (mat in matDTO) {
                                                    if (mat.name == navnString && !varenFinnes) {
                                                        varenFinnes = true
                                                        val foodDTOvalue = mat
                                                        val symptomvalue =
                                                            symptomspinner.selectedItem.toString()
                                                        val alvorlighetsvalue =
                                                            alvorlighetsspinner.selectedItem.toString()
                                                        val df = SimpleDateFormat("dd-MMM-yyyy")
                                                        val date: String =
                                                            df.format(Calendar.getInstance().time)

                                                        val registeredDTO = RegisteredDTO(
                                                            foodDTOvalue,
                                                            date,
                                                            symptomvalue,
                                                            alvorlighetsvalue
                                                        )
                                                        saveObjectToArrayList(registeredDTO)
                                                    }
                                                }
                                                if (!varenFinnes) {
                                                    Toast.makeText(
                                                        context,
                                                        "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Verdier registert!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }

                                            } catch (ex: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            val intent = Intent(context, MainActivity::class.java)
                                            startActivity(intent)
                                        }

                                    } catch (ex: Exception) {
                                    }
                                }
                                ingenknapp.setOnClickListener {
                                    try {
                                        val alertDialogView = LayoutInflater.from(context)
                                            .inflate(R.layout.registrer_alertdialog, null)
                                        val builder =
                                            AlertDialog.Builder(context).setView(alertDialogView)
                                                .setTitle("Registrer matvare")
                                        val tmpalertdialog = builder.show()
                                        val navninput = alertDialogView.matinput
                                        val navneforslagAdapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_list_item_1,
                                            matnavn
                                        )
                                        navninput.setAdapter(navneforslagAdapter)
                                        navninput.threshold = 1

                                        // Kalkuler risiko
                                        navninput.setOnItemClickListener { parent, _, position, _ ->
                                            try {
                                                val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                                val risikoTekst = tmpalertdialog.risikoView
                                                val innholdTekst = tmpalertdialog.innholdView

                                                val valgtMatvare = parent.getItemAtPosition(position)
                                                val risiko = KalkulerMatvareRisiko(prefs)
                                                risiko.finnMatvare(valgtMatvare as String, matDTO)
                                                val kalkulertRisiko = risiko.kalkulerRisiko()

                                                val risikoFarge: Int
                                                // Setter tekstens farge etter hvor høy risikoen er
                                                if (kalkulertRisiko >= 75) {
                                                    risikoFarge = Color.RED
                                                } else if ((25 < kalkulertRisiko) && (kalkulertRisiko < 75)){
                                                    risikoFarge = Color.parseColor("#E86826")
                                                } else {
                                                    risikoFarge = Color.GREEN
                                                }

                                                val risikoSpan: Spannable = SpannableString("Risiko: $kalkulertRisiko%")
                                                risikoSpan.setSpan(
                                                    ForegroundColorSpan(risikoFarge), 8, risikoSpan.length,
                                                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                                )
                                                risikoTekst.text = risikoSpan

                                                innholdTekst.text = "Inneholder:" + risiko.matvareFODMaps()
                                            } catch (ex: Exception) {
                                                Toast.makeText(context, "Kalkulering av risiko feilet: $ex. Prøv igjen", Toast.LENGTH_LONG).show()
                                            }
                                        }

                                        val symptomspinner = tmpalertdialog.symptominput
                                        val symptomadapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_spinner_dropdown_item,
                                            symptomer
                                        )
                                        tmpalertdialog.symptominput.adapter = symptomadapter

                                        val alvorlighetsspinner = tmpalertdialog.alvorlighetsinput
                                        val alvorlighetsadapter = ArrayAdapter(
                                            tmpalertdialog.context,
                                            android.R.layout.simple_spinner_dropdown_item,
                                            alvorlighetsgrader
                                        )
                                        tmpalertdialog.alvorlighetsinput.adapter =
                                            alvorlighetsadapter

                                        //Lagrer verdiene som dataobjekt RegisteredDTO i liste og lagrer det internt på mobilen
                                        val lagreknapp = tmpalertdialog.registrerButton
                                        lagreknapp.setOnClickListener {
                                            try {
                                                val navnString = navninput!!.text.toString()
                                                var varenFinnes = false
                                                for (mat in matDTO) {
                                                    if (mat.name == navnString && !varenFinnes) {
                                                        varenFinnes = true
                                                        val foodDTOvalue = mat
                                                        val symptomvalue =
                                                            symptomspinner.selectedItem.toString()
                                                        val alvorlighetsvalue =
                                                            alvorlighetsspinner.selectedItem.toString()
                                                        val df = SimpleDateFormat("dd-MMM-yyyy")
                                                        val date: String =
                                                            df.format(Calendar.getInstance().time)

                                                        val registeredDTO = RegisteredDTO(
                                                            foodDTOvalue,
                                                            date,
                                                            symptomvalue,
                                                            alvorlighetsvalue
                                                        )
                                                        saveObjectToArrayList(registeredDTO)
                                                    }
                                                }
                                                if (!varenFinnes) {
                                                    Toast.makeText(
                                                        context,
                                                        "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                } else {
                                                    Toast.makeText(
                                                        context,
                                                        "Verdier registert!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }

                                            } catch (ex: Exception) {
                                                Toast.makeText(
                                                    context,
                                                    "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            }
                                            val intent = Intent(context, MainActivity::class.java)
                                            startActivity(intent)
                                        }

                                    } catch (ex: Exception) {
                                    }
                                }


                            }catch (ex: Exception){

                            }
                        }
                        //Dersom utvalget bare er en verdi
                        else {
                            try {
                                val alertDialogView = LayoutInflater.from(context)
                                    .inflate(R.layout.registrer_alertdialog, null)
                                val builder = AlertDialog.Builder(context).setView(alertDialogView)
                                    .setTitle("Registrer matvare")
                                val tmpalertdialog = builder.show()
                                val navninput = alertDialogView.matinput
                                val navneforslagAdapter = ArrayAdapter(
                                    tmpalertdialog.context,
                                    android.R.layout.simple_list_item_1,
                                    matnavn
                                )
                                navninput.setAdapter(navneforslagAdapter)
                                navninput.threshold = 1
                                navninput.setText(utvalg.get(0))

                                // Kalkuler risiko
                                navninput.setOnItemClickListener { parent, _, position, _ ->
                                    try {
                                        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
                                        val risikoTekst = tmpalertdialog.risikoView
                                        val innholdTekst = tmpalertdialog.innholdView

                                        val valgtMatvare = parent.getItemAtPosition(position)
                                        val risiko = KalkulerMatvareRisiko(prefs)
                                        risiko.finnMatvare(valgtMatvare as String, matDTO)
                                        val kalkulertRisiko = risiko.kalkulerRisiko()

                                        val risikoFarge: Int
                                        // Setter tekstens farge etter hvor høy risikoen er
                                        risikoFarge = if (kalkulertRisiko >= 75) {
                                            Color.RED
                                        } else if ((25 < kalkulertRisiko) && (kalkulertRisiko < 75)){
                                            Color.parseColor("#E86826")
                                        } else {
                                            Color.GREEN
                                        }

                                        val risikoSpan: Spannable = SpannableString("Risiko: $kalkulertRisiko%")
                                        risikoSpan.setSpan(
                                            ForegroundColorSpan(risikoFarge), 8, risikoSpan.length,
                                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                                        )
                                        risikoTekst.text = risikoSpan

                                        innholdTekst.text = "Inneholder:" + risiko.matvareFODMaps()
                                    } catch (ex: Exception) {
                                        Toast.makeText(context, "Kalkulering av risiko feilet: $ex. Prøv igjen", Toast.LENGTH_LONG).show()
                                    }
                                }

                                val symptomspinner = tmpalertdialog.symptominput
                                val symptomadapter = ArrayAdapter(
                                    tmpalertdialog.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    symptomer
                                )
                                tmpalertdialog.symptominput.adapter = symptomadapter

                                val alvorlighetsspinner = tmpalertdialog.alvorlighetsinput
                                val alvorlighetsadapter = ArrayAdapter(
                                    tmpalertdialog.context,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    alvorlighetsgrader
                                )
                                tmpalertdialog.alvorlighetsinput.adapter =
                                    alvorlighetsadapter

                                //Lagrer verdiene som dataobjekt RegisteredDTO i liste og lagrer det internt på mobilen
                                val lagreknapp = tmpalertdialog.registrerButton
                                lagreknapp.setOnClickListener {
                                    try {
                                        val navnString = navninput!!.text.toString()
                                        var varenFinnes = false
                                        for (mat in matDTO) {
                                            if (mat.name == navnString && !varenFinnes) {
                                                varenFinnes = true
                                                val symptomvalue =
                                                    symptomspinner.selectedItem.toString()
                                                val alvorlighetsvalue =
                                                    alvorlighetsspinner.selectedItem.toString()
                                                val df = SimpleDateFormat("dd-MMM-yyyy")
                                                val date: String =
                                                    df.format(Calendar.getInstance().time)

                                                val registeredDTO = RegisteredDTO(
                                                    mat,
                                                    date,
                                                    symptomvalue,
                                                    alvorlighetsvalue
                                                )
                                                saveObjectToArrayList(registeredDTO)
                                            }
                                        }
                                        if (!varenFinnes) {
                                            Toast.makeText(
                                                context,
                                                "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "Verdier registert!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }

                                    } catch (ex: Exception) {
                                        Toast.makeText(
                                            context,
                                            "Noe gikk galt med å lagre dine verdier. Prøv igjen",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    val intent = Intent(context, MainActivity::class.java)
                                    startActivity(intent)
                                }

                            } catch (ex: Exception) {
                            }
                        }
                    }
                }
            }
        }
        if (!foundResult) {
            Toast.makeText(context, "Bildet ga ingen resultater, prøv igjen eller skriv inn manuelt", Toast.LENGTH_LONG).show()
            val intent = Intent(context, MainActivity::class.java)
            startActivity(intent)
        }
    }
}


