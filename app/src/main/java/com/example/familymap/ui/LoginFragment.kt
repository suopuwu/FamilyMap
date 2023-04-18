package com.example.familymap.ui

import Exchange.Response
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.familymap.R
import com.example.familymap.data.*
import com.example.familymap.data.serverProxy.FamilyMembers
import com.example.familymap.data.serverProxy.Login
import com.example.familymap.data.serverProxy.Register
import com.example.familymap.data.serverProxy.RelatedEvents
import com.example.familymap.databinding.FragmentLoginBinding
import kotlinx.coroutines.*

class LoginFragment : Fragment() {
    private lateinit var navController: NavController
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val view = binding.root
        navController = this.findNavController()

        fun addEventListeners() {
            //listen to both buttons
            binding.loginButton.run {
                setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch { login() }
                }
                isEnabled = false
            }
            binding.registerButton.run {
                setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch { register() }
                }
                isEnabled = false
            }

            //handle any input into a field
            fun setFieldListener(vararg field: EditText) {
                field.forEach {
                    it.doAfterTextChanged { updateButtons() }
                }
            }
            setFieldListener(
                binding.host,
                binding.port,
                binding.username,
                binding.firstName,
                binding.lastName,
                binding.email
            )


            //handle input from radio buttons
            binding.genderRadio.setOnCheckedChangeListener { _, _ -> updateButtons() }
        }
        addEventListeners()
        updateButtons()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    //enables and disables buttons as needed.
    private fun updateButtons() {
        fun oneIfEmpty(element: EditText): Int {
            return if (element.text.toString().isEmpty()) 1 else 0
        }
        //disable both buttons if host, port, username, or password are empty
        if (oneIfEmpty(binding.host) + oneIfEmpty(binding.port) + oneIfEmpty(binding.username) + oneIfEmpty(
                binding.password
            ) != 0
        ) {
            binding.loginButton.isEnabled = false
            binding.registerButton.isEnabled = false
            return
        }
        //if we got past that, enable the login button
        binding.loginButton.isEnabled = true
        //if any remaining fields are empty, disable only the register button
        if (oneIfEmpty(binding.firstName) + oneIfEmpty(binding.lastName) + oneIfEmpty(binding.email) != 0 ||
            binding.genderRadio.checkedRadioButtonId == -1
        ) {
            binding.registerButton.isEnabled = false
            return
        }
        //otherwise, enable the register button
        binding.registerButton.isEnabled = true
    }

    private fun getText(textBox: EditText): String {
        return textBox.text.toString()
    }

    private suspend fun login() = coroutineScope {
        val response = Login.login(
            getText(binding.username),
            getText(binding.password),
            getText(binding.host),
            getText(binding.port)
        )
        if (!response.success) {
            launchToast("Login failed: ${response.message}")
        } else {
            handleSuccessfulLoginRegisterResponse(response)
        }
    }

    private suspend fun register() = coroutineScope {
        val response = Register.register(
            getText(binding.username),
            getText(binding.password),
            getText(binding.firstName),
            getText(binding.lastName),
            getText(binding.email),
            if (binding.maleRadio.isActivated) "m" else "f",
            binding.host.text.toString(),
            binding.port.text.toString()
        )
        if (!response.success) {
            launchToast("Registration failed: ${response.message}")
        } else {
            handleSuccessfulLoginRegisterResponse(response)
        }

    }

    private fun launchToast(text: String) {
        CoroutineScope(Dispatchers.Main).launch {
            Toast.makeText(
                context,
                text,
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun handleSuccessfulLoginRegisterResponse(response: Response) {
        Cache.setLoginInfo(
            response.authtoken,
            response.personID,
            getText(binding.host),
            getText(binding.port),
            getText(binding.username)
        )

        //Get related events, error if get failed, add to cache if successful
        val relatedEventsResponse = RelatedEvents.getRelatedEvents()
        if (!relatedEventsResponse.success) {
            launchToast("Error: ${relatedEventsResponse.message}")
            return
        }
        Cache.eventList = relatedEventsResponse.data

        //get family members, error if get failed, add to cache if successful
        val familyMembersResponse = FamilyMembers.getFamilyMembers()
        if (!familyMembersResponse.success) {
            launchToast("Error: ${familyMembersResponse.message}")
            return
        }
        Cache.personList = familyMembersResponse.data

        Cache.initSecondaryValues()
        //navigate to the map fragment
        CoroutineScope(Dispatchers.Main).launch {
            navController.navigate(R.id.action_loginFragment_to_mapsFragment)
        }
    }
}