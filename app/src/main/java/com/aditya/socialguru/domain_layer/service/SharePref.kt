package com.aditya.socialguru.domain_layer.service

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aditya.socialguru.data_layer.model.User
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SharePref(val context: Context) {


    companion object{
        private val USER_PREFERENCES_NAME = "user_preferences"

        private val Context.dataStore by preferencesDataStore(
            name = USER_PREFERENCES_NAME
        )
    }




    private object PreferencesKeys {

        val  USER_KEY= stringPreferencesKey("USER_KEY")
    }

    suspend fun setPrefUser(user: User) {
        val json = Gson().toJson(user)
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USER_KEY] = json
        }
    }

    fun getPrefUser(): Flow<User?> {
        return context.dataStore.data.map { preferences ->
            val jsonString = preferences[PreferencesKeys.USER_KEY]
            if (jsonString != null) {
                Gson().fromJson(jsonString, User::class.java)
            } else {
                null
            }
        }
    }



    suspend fun setPrefBoolean(key:Preferences.Key<Boolean> ,value:Boolean) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getPrefBoolean(key:Preferences.Key<Boolean>): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: false
        }
    }

    suspend fun setPrefString(key:Preferences.Key<String> ,value:String) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getPrefString(key:Preferences.Key<String>): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[key]
        }
    }

    suspend fun setPrefInt(key:Preferences.Key<Int> ,value:Int) {
        context.dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    fun getPrefInt(key:Preferences.Key<Int>): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: 0
        }
    }



}