package com.example.intoleranser.div

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.View
import android.widget.Toast
import java.io.File
import java.io.FileOutputStream

// Klasse som tar skjermbilde av aktiviteten og deler det
class DelMeny(
    context: Context?,
    view: View?
) {
    init {
        // Tar skjermbilde
        val bitmap = view?.let { skjermbilde(it) }
        // Lagrer filen
        val fil = lagreBitmap(context, bitmap!!, "mineIntoleranser.png")
        val uriTilBilde: Uri = Uri.fromFile(File(fil!!.absolutePath))
        // Gjør klar til deling
        val shareIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uriTilBilde)
            putExtra(Intent.EXTRA_TITLE, "Se hva MAPP sier jeg ikke burde spise:")
            type = "image/png"
        }
        // Deler
        context?.startActivity(Intent.createChooser(shareIntent, "Del med"))
    }

    // Metode som tar skjermbilde av aktiviteten
    private fun skjermbilde(view: View): Bitmap? {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }

    // Metode som lagrer skjermbilde som bitmap
    private fun lagreBitmap(
        context: Context?,
        bitmap: Bitmap,
        filnavn: String
    ): File? {
        val path = context?.getExternalFilesDir(null)?.absolutePath + "/Screenshots"
        val mappe = File(path)
        if (!mappe.exists()) mappe.mkdirs()
        val fil = File(mappe, filnavn)
        try {
            val streamUt = FileOutputStream(fil)
            bitmap.compress(Bitmap.CompressFormat.PNG, 90, streamUt)
            streamUt.flush()
            streamUt.close()
        } catch (ex: Exception) {
            Toast.makeText(context, "En feil skjedde under lagring: $ex, prøv igjen.", Toast.LENGTH_LONG).show()
        }
        return fil
    }
}