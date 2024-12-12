package com.example.taxista.viewModel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.taxista.model.User
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private var googleSignInClient: GoogleSignInClient? = null
    
    private val _authState = MutableStateFlow<AuthState>(AuthState.Initial)
    val authState: StateFlow<AuthState> = _authState
    
    private val _currentUser = MutableStateFlow<FirebaseUser?>(null)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val _currentUsername = MutableStateFlow<String>("")
    val currentUsername: StateFlow<String> = _currentUsername
    
    init {
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
            if (firebaseAuth.currentUser != null) {
                loadUsername(firebaseAuth.currentUser?.uid)
            }
        }
    }

    private fun loadUsername(uid: String?) {
        uid?.let { userId ->
            viewModelScope.launch {
                try {
                    val userDoc = firestore.collection("users").document(userId).get().await()
                    _currentUsername.value = userDoc.getString("username") ?: ""
                } catch (e: Exception) {
                    Log.e("AuthViewModel", "Error loading username", e)
                }
            }
        }
    }

    fun initGoogleSignIn(context: Context, webClientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        
        googleSignInClient = GoogleSignIn.getClient(context, gso)
    }

    fun getGoogleSignInClient(): GoogleSignInClient? = googleSignInClient
    
    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                Log.d("AuthViewModel", "Attempting Google sign in")
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                val authResult = auth.signInWithCredential(credential).await()
                
                // Create or update user document
                authResult.user?.let { user ->
                    val username = user.displayName ?: user.email?.substringBefore("@") ?: ""
                    createUserInFirestore(user.uid, user.email ?: "", username)
                }
                
                _currentUser.value = auth.currentUser
                _authState.value = AuthState.Success
                Log.d("AuthViewModel", "Google sign in successful")
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Google sign in failed"
                Log.e("AuthViewModel", "Google sign in error: $errorMessage", e)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }
    
    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                Log.d("AuthViewModel", "Attempting sign in for email: $email")
                val result = auth.signInWithEmailAndPassword(email, password).await()
                _currentUser.value = result.user
                loadUsername(result.user?.uid)
                _authState.value = AuthState.Success
                Log.d("AuthViewModel", "Sign in successful")
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Sign in failed"
                Log.e("AuthViewModel", "Sign in error: $errorMessage", e)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }
    
    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            try {
                _authState.value = AuthState.Loading
                Log.d("AuthViewModel", "Attempting sign up for email: $email")
                
                val authResult = auth.createUserWithEmailAndPassword(email, password).await()
                createUserInFirestore(authResult.user?.uid ?: "", email, username)
                _currentUser.value = authResult.user
                _currentUsername.value = username
                _authState.value = AuthState.Success
                Log.d("AuthViewModel", "Sign up successful")
            } catch (e: Exception) {
                val errorMessage = e.message ?: "Sign up failed"
                Log.e("AuthViewModel", "Sign up error: $errorMessage", e)
                _authState.value = AuthState.Error(errorMessage)
            }
        }
    }

    private suspend fun createUserInFirestore(uid: String, email: String, username: String) {
        try {
            val user = User(uid, email, username)
            firestore.collection("users").document(uid).set(user).await()
            _currentUsername.value = username
        } catch (e: Exception) {
            Log.e("AuthViewModel", "Error creating user document", e)
        }
    }
    
    fun signOut() {
        viewModelScope.launch {
            try {
                googleSignInClient?.signOut()?.await()
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Error signing out of Google", e)
            }
            auth.signOut()
            _currentUser.value = null
            _currentUsername.value = ""
            _authState.value = AuthState.Initial
            Log.d("AuthViewModel", "User signed out")
        }
    }
}

sealed class AuthState {
    object Initial : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}
