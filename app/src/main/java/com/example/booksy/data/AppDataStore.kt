package com.example.booksy.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

// ÚNICA definición del DataStore de la app
val Context.appDataStore by preferencesDataStore("booksy_prefs")
