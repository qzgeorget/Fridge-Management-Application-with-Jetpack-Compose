package com.example.coursework

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coursework.data.DataStoreManager
import com.example.coursework.ui.SearchDatePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SearchViewModel(context: Context): ViewModel() {
    private val apiKey = "dc64c7d00667293923fff57e09fe44e1"
    private val apiId = "f281517c"

    private val dataStoreManager = DataStoreManager(context)

    private val _searchText = MutableStateFlow("")
    val searchText = _searchText.asStateFlow()

    private val _selectedItems = MutableStateFlow<List<FoodItem>>(
        runBlocking { dataStoreManager.getFromDataStore() } ?: emptyList()
    )
    val selectedItems = _selectedItems.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching = _isSearching.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> get() = _showDialog

    private val _selectedFoodItem = MutableStateFlow<FoodItem?>(null)
    val selectedFoodItem: StateFlow<FoodItem?> get() = _selectedFoodItem

    private val _foodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val foodItems: StateFlow<List<FoodItem>> = _searchText
        .combine(_foodItems) { text, foodItems ->
            if (text.isBlank()) {
                foodItems
            } else {
                foodItems.filter { it.doesMatchSearchQuery(text) }
            }
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            _foodItems.value
        )

    fun onSearchTextChange(text: String) {
        _searchText.value = text
        fetchFoodItems(text) // Fetch food items whenever the search text changes
    }

    fun clearSearch() {
        _searchText.value = ""
    }

    private fun fetchFoodItems(query: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _isSearching.value = true
            try {
                val url = "https://api.edamam.com/api/food-database/v2/parser?app_id=$apiId&app_key=$apiKey&ingr=$query"
                val result = getJSONFromApi(url) // Your suspend function to fetch data

                val fetchedItems = getDataFromJson(result) // Parse the JSON string into the Array of FoodItems
                _foodItems.value = fetchedItems.toList()
            } catch (e: Exception) {
                // Handle exceptions
                e.printStackTrace()
            } finally {
                _isSearching.value = false
            }
        }
    }

    // Send a GET request to the URL, fetch data, and return the response in JSON string
    private fun getJSONFromApi(url: String): String {
        var result = ""
        var conn: HttpsURLConnection? = null
        try {
            val request = URL(url)
            conn = request.openConnection() as HttpsURLConnection
            conn.connect()
            val inputStream: InputStream = conn.inputStream
            result = convertInputStreamToString(inputStream)
        } catch (e: Exception) {
            result = "Network Error! Please check network connection."
            e.printStackTrace()
        } finally {
            conn?.disconnect()
        }
        return result
    }

    // The helper function that converts the input stream to String
    @Throws(IOException::class)
    private fun convertInputStreamToString(inputStream: InputStream): String {
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        val result = StringBuilder()
        var line: String?

        // Read out the input stream buffer line by line until it's empty
        while (bufferedReader.readLine().also { line = it } != null) {
            result.append(line)
        }
        inputStream.close()
        return result.toString()
    }

    // Take the raw JSON string and pull out the data we need, and construct a list of FoodItems with them
    private fun getDataFromJson(jsonStr: String): Array<FoodItem> {
        val resultStrs: MutableList<FoodItem> = mutableListOf()
        try {
            // These are the names of the JSON objects that need to be extracted
            val exactFoodList = "parsed"
            val guessFoodList = "hints"

            val foodTitle = "food"
            val foodName = "label"
            val foodCategory = "category"
            val foodNutrients = "nutrients"
            val foodEnergy = "ENERC_KCAL"
            val foodProtein = "PROCNT"

            val foodJson = JSONObject(jsonStr)
            var foodArray = foodJson.getJSONArray(exactFoodList)

            if (foodArray.length() == 0) {
                foodArray = foodJson.getJSONArray(guessFoodList)
            }

            for (i in 0 until foodArray.length()) {
                val eachFoodOuter = foodArray.getJSONObject(i)
                val eachFoodInner = eachFoodOuter.getJSONObject(foodTitle)

                val name = eachFoodInner.getString(foodName)
                val category = eachFoodInner.getString(foodCategory)

                val eachFoodNutrients = eachFoodInner.getJSONObject(foodNutrients)
                val energy = eachFoodNutrients.getInt(foodEnergy)
                val protein = eachFoodNutrients.getInt(foodProtein)

                val eachFood = FoodItem(
                    name = name,
                    type = category,
                    energy = energy,
                    protein = protein
                )
                resultStrs.add(eachFood)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return resultStrs.toTypedArray()
    }

    fun addSelectedItem(foodItem: FoodItem) {
        val updatedList = _selectedItems.value + foodItem
        _selectedItems.value = updatedList
        viewModelScope.launch {
            dataStoreManager.saveToDataStore(updatedList)
        }
    }

    fun removeSelectedItem(foodItem: FoodItem) {
        val updatedList = _selectedItems.value - foodItem
        _selectedItems.value = updatedList
        viewModelScope.launch {
            dataStoreManager.saveToDataStore(updatedList)
        }
    }

    fun onSelectedItemClicked(foodItem: FoodItem) {
        _selectedFoodItem.value = foodItem
        _showDialog.value = true
    }

    fun emptyGroceryList(){
        _selectedItems.value = emptyList()
    }

    fun onDialogDismiss() {
        _showDialog.value = false
    }
}

data class FoodItem(
    val name: String,
    val type: String,
    val energy: Int,
    val protein: Int
) {
    constructor() : this("", "", 0, 0)

    fun doesMatchSearchQuery(query: String): Boolean {
        return name.contains(query, ignoreCase = true)
    }
}
