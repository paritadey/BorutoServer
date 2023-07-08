package com.example.plugins

import com.example.routes.getAllHeroes
import com.example.routes.root
import io.ktor.application.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*

fun Application.configureRouting() {
    routing {
        root()
        getAllHeroes()
        static("/images"){
            resources("images")
        }
    }
}
