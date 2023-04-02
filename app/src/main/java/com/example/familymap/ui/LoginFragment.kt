package com.example.familymap.ui

import Exchange.ExchangeTypes
import Exchange.Request
import Exchange.Response
import android.os.Bundle
import android.text.Editable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.core.widget.doOnTextChanged
import androidx.navigation.NavController
import androidx.navigation.fragment.findNavController
import com.example.familymap.R
import com.example.familymap.databinding.FragmentLoginBinding
import com.example.familymap.utils.Communicator
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
        fun setFieldListener(field: EditText) {
            field.doAfterTextChanged { updateButtons() }
        }
        setFieldListener(binding.host)
        setFieldListener(binding.port)
        setFieldListener(binding.username)
        setFieldListener(binding.password)
        setFieldListener(binding.firstName)
        setFieldListener(binding.lastName)
        setFieldListener(binding.email)

        //handle input from radio buttons
        binding.genderRadio.setOnCheckedChangeListener { _, _ -> updateButtons() }
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
        binding.loginButton.isEnabled = true
        //if any remaining fields are empty, disable only the register button
        if (oneIfEmpty(binding.firstName) + oneIfEmpty(binding.lastName) + oneIfEmpty(binding.email) != 0 ||
            binding.genderRadio.checkedRadioButtonId == -1
        ) {
            binding.registerButton.isEnabled = false
            return
        }
        binding.registerButton.isEnabled = true
    }

    //todo separate data and ui code
    private suspend fun login() = coroutineScope {
        val request = Request()
        request.run {
            username = binding.username.text.toString()
            password = binding.password.text.toString()
        }
        //todo rename when less sleepy
        val response =
            Communicator.post(
                "http://${binding.host.text.toString()}:${binding.port.text.toString()}/user/login",
                request.serialize()
            )
        if (response.success) {
            //todo same as above
            val theNewlyRegisteredPerson =
                Communicator.get(
                    "http://${binding.host.text.toString()}:${binding.port.text.toString()}/person/${response.personID}",
                    response.authtoken
                )
            println(theNewlyRegisteredPerson.message)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "login succeeded for ${theNewlyRegisteredPerson.firstName} ${theNewlyRegisteredPerson.lastName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "login failed: ${response.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun register() = coroutineScope {
        val request = Request()
        request.run {
            username = binding.username.text.toString()
            password = binding.password.text.toString()
            firstName = binding.firstName.text.toString()
            lastName = binding.lastName.text.toString()
            email = binding.email.text.toString()
            gender = if (binding.maleRadio.isActivated) "m" else "f"
        }
        //todo rename when less sleepy
        val response =
            Communicator.post(
                "http://${binding.host.text.toString()}:${binding.port.text.toString()}/user/register",
                request.serialize()
            )
        if (response.success) {
            //todo same as above
            val theNewlyRegisteredPerson =
                Communicator.get(
                    "http://${binding.host.text.toString()}:${binding.port.text.toString()}/person/${response.personID}",
                    response.authtoken
                )
            println(theNewlyRegisteredPerson.message)
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "registration succeeded for ${theNewlyRegisteredPerson.firstName} ${theNewlyRegisteredPerson.lastName}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            CoroutineScope(Dispatchers.Main).launch {
                Toast.makeText(
                    context,
                    "Registration failed: ${response.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
//        CoroutineScope(Dispatchers.Main).launch {
//            navController.navigate(R.id.action_loginFragment_to_mapsFragment)
//        }
    }
}