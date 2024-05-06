package com.lagradost.quicknovel.ui.loginAndRegiester

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.lagradost.quicknovel.util.NetworkResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * ViewModel for the Register screen.
 * This ViewModel is responsible for handling user registration.
 *
 * @property firebaseAuth FirebaseAuth instance for user authentication.
 * @property db FirebaseFirestore instance for database operations.
 */
class RegisterViewModel(
    private val firebaseAuth: FirebaseAuth,
    private val db: FirebaseFirestore
): ViewModel() {

    // MutableStateFlow for holding the registration result.
    private val _register =
        MutableStateFlow<NetworkResult<UserSign>>(NetworkResult.UnSpecified())
    // Publicly exposed Flow for observing the registration result.
    val register: Flow<NetworkResult<UserSign>> = _register

    // Channel for holding the validation result.
    private val _validation = Channel<RegisterFailedState>()
    // Publicly exposed Flow for observing the validation result.
    val validation = _validation.receiveAsFlow()

    /**
     * Creates a new user account with the provided email and password.
     *
     * @param user UserSign object containing user details.
     * @param password User's password.
     */
    fun createAccountWithEmailAndPassword(user: UserSign, password: String) {
        if (checkValidation(user, password)) {
            runBlocking {
                _register.emit(NetworkResult.Loading())
            }
            user.email?.let {
                firebaseAuth.createUserWithEmailAndPassword(it, password)
                    .addOnSuccessListener {firebaseUser ->
                        firebaseUser.user?.let {
                            saveUserInfo(it.uid,user)
                        }
                    }
                    .addOnFailureListener {
                        _register.value = NetworkResult.Error(it.message.toString())
                    }
            }
        }else{
            val registerValidation = user.email?.let { validateEmail(it) }?.let {
                RegisterFailedState(
                    it,
                    validatePassword(password)
                )
            }
            viewModelScope.launch {
                if (registerValidation != null) {
                    _validation.send(registerValidation)
                }
            }
        }
    }

    /**
     * Checks if the provided user details are valid.
     *
     * @param user UserSign object containing user details.
     * @param password User's password.
     * @return Boolean indicating whether the validation was successful.
     */
    private fun checkValidation(user: UserSign, password: String): Boolean {
        val emailValidation = user.email?.let { validateEmail(it) }
        val passwordValidation = validatePassword(password)
        return emailValidation is RegisterValidation.Success && passwordValidation is RegisterValidation.Success
    }

    /**
     * Saves the user's information in the Firestore database.
     *
     * @param userUid User's unique ID.
     * @param user UserSign object containing user details.
     */
    private fun saveUserInfo(userUid: String,user: UserSign) {
        db.collection(USER_COLLECTION)
            .document(userUid)
            .set(user)
            .addOnSuccessListener {
                _register.value = NetworkResult.Success(user)
                sendConfirmationEmail()
            }
            .addOnFailureListener {
                _register.value = NetworkResult.Error(it.message.toString())
            }
    }

    /**
     * Sends a confirmation email to the user.
     */
    private fun sendConfirmationEmail() {
        val user = firebaseAuth.currentUser
        user?.let {
            it.sendEmailVerification()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        viewModelScope.launch {
                            _register.emit(NetworkResult.Success(UserSign()))
                        }
                    } else {
                        viewModelScope.launch {
                            _register.emit(NetworkResult.Error(task.exception?.message.toString()))
                        }
                    }
                }
        }
    }

    companion object {
        // Collection name for storing user information in Firestore.
        const val USER_COLLECTION = "users"
    }
}