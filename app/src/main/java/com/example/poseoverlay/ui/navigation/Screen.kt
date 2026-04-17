package com.example.poseoverlay.ui.navigation

sealed class Screen(val route: String) {
    object Gallery : Screen("gallery")
    
    object ImageAdd : Screen("add?uri={uri}") {
        const val argUri = "uri"
        fun createRoute(uri: String) = "add?uri=$uri"
    }
    
    object ImageEdit : Screen("edit?uri={uri}") {
        const val argUri = "uri"
        
        fun createRoute(uri: String) =
            "edit?uri=$uri"
    }


    object ImageDetail : Screen("detail?urlString={urlString}") {
        const val argUrlString = "urlString"
        fun createRoute(argUrlString: String) = "detail?urlString=$argUrlString"
    }

    object Albums : Screen("albums?category={category}") {
        const val argCategory = "category"
        fun createRoute(argCategory: String) = "albums?category=$argCategory"
    }
}
