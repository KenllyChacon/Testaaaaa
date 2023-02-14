package com.example.test.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.lifecycleScope
import com.example.test.databinding.ActivityMainBinding
import com.example.test.userCase.oauth2.Oauth2UC
import com.example.test.userCase.pets.PetsUC
import com.example.test.utils.Test
import com.example.test.utils.Variables
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val Context.datastore by preferencesDataStore("testDB")

    /*private val lstPerms =
        arrayOf(
            READ
        )*/

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch { Dispatchers.IO{
            initClass()



            checkBiometic()
        }
        }

    }

    override fun onPause() {
        super.onPause()
        println("PAUSANDO APP")

    }

    override fun onResume() {
        super.onResume()
        println("REANUDANDO APP")
    }

    private suspend fun saveDataStore(access: Boolean){
        datastore.edit {
            it[booleanPreferencesKey("access")] = access
        }
    }

    private fun runBiometric(){

        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt =
            BiometricPrompt(this,executor,object :BiometricPrompt.AuthenticationCallback(){
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                binding.layoutLogin.visibility = View.VISIBLE
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                Snackbar.make(binding.imageView, "huella no reconocida", Snackbar.LENGTH_LONG).show()
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                val intent = Intent(this@MainActivity, PrincipalActivity::class.java)
      lifecycleScope.launch(Dispatchers.IO){
          saveDataStore(true)
      }
                startActivity(intent)
            }
        })

        val biometricPromptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Autenticacion de la huella")
            .setSubtitle("Ingrese su huella para autenticarse")
            .setNegativeButtonText("Usar el ussuario y contraseña")
            .build()

        binding.imageView2.setOnClickListener{
            biometricPrompt.authenticate(biometricPromptInfo)
        }

    }

    private fun checkBiometic(){
        val biometricManager = BiometricManager.from(this)
        when(biometricManager.canAuthenticate(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)){
            BiometricManager.BIOMETRIC_SUCCESS -> {
                binding.imageView2.visibility = View.VISIBLE
                runBiometric()
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE ->{
                binding.imageView2.visibility = View.GONE
                Snackbar.make(binding.imageView, "No existe sensor en el dispositivo", Snackbar.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE ->{
                binding.imageView2.visibility = View.GONE
                Snackbar.make(binding.imageView, "Existe un error con el biometrico", Snackbar.LENGTH_LONG).show()
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED ->{
              val intenEnroll = Intent(Settings.ACTION_BIOMETRIC_ENROLL).apply {
                  putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG or
                      DEVICE_CREDENTIAL)
              }
                startActivity(intenEnroll)
            }
        }
    }

    private suspend fun initClass() {
        val txt = binding.txtUser
        binding.buttonLogin.setOnClickListener {
            val txtUser = binding.txtUser.text.toString()
            val txtPass = binding.txtPass.text.toString()

            if (txtUser == ("admin") && txtPass == "admin") {
                var intent = Intent(this, PrincipalActivity::class.java)
                intent.putExtra(Variables.nombreUsuario, "Bienvenidos")
                startActivity(intent)
            } else {
                Snackbar.make(
                    txt, "Nombre de usuario o contraseña incorrectos",
                    Snackbar.LENGTH_SHORT
                ).show()
            }
        }
      /*  val value = datastore.data.map {
            stringPreferencesKey("access")
        }

        value.collect{
            it.name
        }*/

    }

    private fun testToken() {
        lifecycleScope.launch(Dispatchers.IO)
        {
            try {
                Log.d("UCE", "Ejecutando el Token")
                val c = Oauth2UC().getTokenPets()

                Log.d("UCE", "Ejecutando la consulta")
                if (c != null) {
                    val r = PetsUC().getAllPets(
                        1,
                        "dog",
                        "${c.token_type} ${c.access_token}"
                    )
                    Log.d("UCE", "Resultados totales son: ${r?.size}")
                }
            } catch (e: Exception) {
                Snackbar.make(binding.imageView, e.message.toString(), Snackbar.LENGTH_LONG).show()
            }
        }
    }
}