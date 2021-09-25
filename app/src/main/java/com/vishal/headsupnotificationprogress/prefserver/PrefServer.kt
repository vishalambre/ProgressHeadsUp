package com.vishal.headsupnotificationprogress.prefserver

import android.content.Context
import android.content.SharedPreferences

/** @author Vishal Ambre */

/**
 * A Simple SharedPreference that can be Observed
 */
// Todo: Refactor entire class
// Todo: Handle for all types, make this generic, unsafe cast
//Todo: Use PreferenceX instead
class PrefServer<T> private constructor(
    fileName: String,
    private val key: String,
    context: Context,
    default: Int
) {

    private val prefChangeListenerSet = HashSet<PrefChangeListener<T>>()
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences(fileName, Context.MODE_PRIVATE)
    private var prefValue: T = sharedPreferences.getInt(key, default) as T //Todo unsafe cast

    @Synchronized
    fun get() = prefValue

    @Synchronized
    fun put(value: T) {
        prefValue = value
        putInt(value as Int)
        prefChangeListenerSet.forEach {
            it.onPrefChanged(value)
        }
    }

    private fun putInt(value: Int) {
        sharedPreferences.edit().putInt(key, value).apply()
    }

    fun addPrefChangeListener(prefChangeListener: PrefChangeListener<T>) {
        prefChangeListenerSet.add(prefChangeListener)
    }

    //Todo: Add LifeCycleOwner instead
    fun removePrefChangeListener(prefChangeListener: PrefChangeListener<T>) {
        prefChangeListenerSet.remove(prefChangeListener)
    }

    companion object {
        fun <T> from(fileName: String, key: String, context: Context, default: Int) =
            PrefServer<T>(fileName, key, context, default)
    }

    interface PrefChangeListener<T> {
        fun onPrefChanged(newPrefValue: T)
    }
}