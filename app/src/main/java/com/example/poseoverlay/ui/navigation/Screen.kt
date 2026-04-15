package com.example.poseoverlay.ui.navigation

sealed class Screen(val route: String) {
    object Gallery : Screen("gallery")
    
    object ImageAdd : Screen("add?uri={uri}") {
        const val argUri = "uri"
        fun createRoute(uri: String) = "add?uri=$uri"
    }
    
    object ImageEdit : Screen("edit?uri={uri}&category={category}&description={description}") {
        const val argUri = "uri"
        const val argCategory = "category"
        const val argDescription = "description"
        
        fun createRoute(uri: String, category: String, description: String) = 
            "edit?uri=$uri&category=$category&description=$description"
    }

    object ImageDetail : Screen("detail?urlString={urlString}") {
        const val argUrlString = "urlString"
        fun createRoute(argUrlString: String) = "detail?urlString=$argUrlString"
    }
}
