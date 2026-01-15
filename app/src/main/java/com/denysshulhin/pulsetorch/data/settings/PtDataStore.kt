package com.denysshulhin.pulsetorch.data.settings

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

val Context.ptDataStore by preferencesDataStore(name = "pulsetorch_prefs")
