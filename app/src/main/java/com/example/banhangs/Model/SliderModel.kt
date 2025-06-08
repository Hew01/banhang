package com.example.banhangs.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize // Optional: If you ever need to pass SliderModel instances between components (e.g., in Intents)
data class SliderModel(
    val imageUrl: String = "", // The URL of the image to be loaded by Glide
    // You can add other properties here if needed in the future, for example:
    // val id: String = "", // A unique identifier for the slider item
    // val title: String? = null, // An optional title for the image
    // val clickUrl: String? = null // An optional URL to navigate to when the slider item is clicked
) : Parcelable // Optional: Implement Parcelable if you added @Parcelize