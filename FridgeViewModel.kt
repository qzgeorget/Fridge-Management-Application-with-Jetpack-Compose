package com.example.coursework

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.getValue
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class FridgeViewModel: ViewModel() {
    private val firebaseDatabase =
        Firebase.database("https://coursework-b6ca9-default-rtdb.europe-west1.firebasedatabase.app/")
    private val fridgeDatabaseReference = firebaseDatabase.getReference("Fridge")
    private val listDatabaseReference = firebaseDatabase.getReference("Grocery List")

    private val _foodItems = MutableStateFlow<List<FoodItemWithDate>>(emptyList())
    val foodItems = _foodItems.asStateFlow()

    private val _tempFoodItems = MutableStateFlow<List<FoodItemWithDate>>(emptyList())
    val tempFoodItems = _tempFoodItems.asStateFlow()

    init {
        fetchFoodItems()
    }

    private fun fetchFoodItems() {
        fridgeDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val items = mutableListOf<FoodItemWithDate>()
                for (snapshot in dataSnapshot.children) {
                    val item = snapshot.getValue(FoodItemWithDate::class.java)
                    if (item != null) {
                        items.add(item)
                    }
                }
                _foodItems.update { items }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FridgeViewModel", "Failed to read value.", error.toException())
            }
        })

        listDatabaseReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val items = mutableListOf<FoodItemWithDate>()
                for (snapshot in dataSnapshot.children) {
                    val item = snapshot.getValue(FoodItemWithDate::class.java)
                    if (item != null) {
                        items.add(item)
                    }
                }
                _tempFoodItems.update { items }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("FridgeViewModel", "Failed to read value.", error.toException())
            }
        })
    }

    fun tempSaveToList(foodItem: FoodItem, expiryDate: String) {
        val item = FoodItemWithDate(foodItem, expiryDate)
        val updatedList = _tempFoodItems.value + item
        _tempFoodItems.value = updatedList
    }

    fun getItemsExpiringNextDay(callback: (List<FoodItemWithDate>) -> Unit) {
        fetchFoodItems()

        val calendar = Calendar.getInstance()
        val today = calendar.time

        // Observe changes to _foodItems
        viewModelScope.launch {
            _foodItems.collect { foodItems ->
                val itemsExpiringNextDay = mutableListOf<FoodItemWithDate>()

                for (item in foodItems) {
                    val expiryDate =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(item.expiryDate)
                    if (expiryDate != null && expiryDate.before(today)) {
                        itemsExpiringNextDay.add(item)
                    }
                }

                // Execute the callback with the list of items expiring next day
                callback(itemsExpiringNextDay)
            }
        }
    }

    fun checkout() {
        val updatedList = _foodItems.value + _tempFoodItems.value
        _foodItems.value = updatedList
    }

    fun removeFridgeSelectedItem(foodItemWithDate: FoodItemWithDate) {
        val updatedList = _foodItems.value - foodItemWithDate
        _foodItems.value = updatedList
        updateFridgeFirebase()
    }

    fun removeListSelectedItem(foodItem: FoodItem) {
        val foodItemWithDateToRemove = _foodItems.value.find { it.foodItem == foodItem }
        if (foodItemWithDateToRemove != null) {
            // Create an updated list excluding the found item
            val updatedList = _foodItems.value - foodItemWithDateToRemove
            // Update the state
            _foodItems.value = updatedList
            // Update Firebase with the new list
            updateListFirebase()
        } else {
            Log.w("removeSelectedItem", "Item not found: $foodItem")
        }
    }

    fun emptyGroceryList(searchViewModel: SearchViewModel){
        _tempFoodItems.value = emptyList()
        searchViewModel.emptyGroceryList()
        updateListFirebase()
    }

    fun updateFridgeFirebase() {
        updateListFirebase()
        val items = _foodItems.value
        fridgeDatabaseReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (item in items) {
                    fridgeDatabaseReference.push().setValue(item).addOnCompleteListener { pushTask ->
                        if (pushTask.isSuccessful) {
                            Log.d("UpdateFirebase", "Item ${item.foodItem.name} added successfully.")
                        } else {
                            Log.e("UpdateFirebase", "Failed to add item ${item.foodItem.name}.", pushTask.exception)
                        }
                    }
                }
            } else {
                Log.e("UpdateFirebase", "Failed to clear database.", task.exception)
            }
        }
    }
    fun updateListFirebase() {
        val items = _tempFoodItems.value
        listDatabaseReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                for (item in items) {
                    listDatabaseReference.push().setValue(item).addOnCompleteListener { pushTask ->
                        if (pushTask.isSuccessful) {
                            Log.d("UpdateFirebase", "Item ${item.foodItem.name} added successfully.")
                        } else {
                            Log.e("UpdateFirebase", "Failed to add item ${item.foodItem.name}.", pushTask.exception)
                        }
                    }
                }
            } else {
                Log.e("UpdateFirebase", "Failed to clear database.", task.exception)
            }
        }
    }

}

data class FoodItemWithDate(
    val foodItem: FoodItem = FoodItem(),
    val expiryDate: String
){
    constructor() : this(FoodItem(), "")
}