package com.example

import com.example.models.ApiResponse
import com.example.repository.HeroRepository
import com.example.repository.NEXT_PAGE_KEY
import com.example.repository.PREVIOUS_PAGE_KEY
import io.ktor.server.testing.*
import kotlin.test.*
import io.ktor.http.*
import io.ktor.application.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.koin.java.KoinJavaComponent.inject

class ApplicationTest {
    private val heroRepository: HeroRepository by inject(HeroRepository::class.java)

    @Test
    fun `access root endpoint, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                assertEquals(
                    expected = "Welcome to Boruto API",
                    actual = response.content
                )
            }
        }
    }

    @Test
    fun `access all heroes endpoint, assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val expected = ApiResponse(
                    success = true,
                    message = "Ok",
                    prevPage = null,
                    nextPage = 2,
                    heroes = heroRepository.page1,
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                println("Expected, $expected")
                println("Actual, $actual")

                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access all heroes endpoint, query non existing page number, assert error`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=6").apply {
                assertEquals(
                    expected = HttpStatusCode.NotFound,
                    actual = response.status()
                )
                val expected = ApiResponse(
                    success = false,
                    message = "Heroes not found..",
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                println("Expected, $expected")
                println("Actual, $actual")

                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }


    @ExperimentalSerializationApi
    @Test
    fun `access all heroes endpoint, query invalid page number, assert error`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes?page=invalid").apply {
                assertEquals(
                    expected = HttpStatusCode.BadRequest,
                    actual = response.status()
                )
                val expected = ApiResponse(
                    success = false,
                    message = "Only Numbers allowed",
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                println("Expected, $expected")
                println("Actual, $actual")

                assertEquals(
                    expected = expected,
                    actual = actual
                )
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access all heroes endpoint, query all pages assert correct information`() {
        withTestApplication(moduleFunction = Application::module) {
            val pages = 1..5
            val heroes = listOf(heroRepository.page1, heroRepository.page2, heroRepository.page3, heroRepository.page4, heroRepository.page5)
            pages.forEach{ page ->
                println("Current page: $page")
                handleRequest(HttpMethod.Get, "/boruto/heroes?page=$page").apply {
                    assertEquals(
                        expected = HttpStatusCode.OK,
                        actual = response.status()
                    )
                    val expected = ApiResponse(
                        success = true,
                        message = "Ok",
                        prevPage = calculatePage(page=page)["prevPage"],
                        nextPage = calculatePage(page=page)["nextPage"],
                        heroes = heroes[page-1]
                    )
                    val actual = Json.decodeFromString<ApiResponse>(response.content.toString())
                    println("Previous page: ${calculatePage(page=page)["prevPage"]}")
                    println("Next page: ${calculatePage(page=page)["nextPage"]}")
                    println("Heroes: ${heroes[page-1]}")
                    assertEquals(
                        expected = expected,
                        actual = actual
                    )
                }
            }
        }
    }

    private fun calculatePage(page: Int): Map<String, Int?> {
        var prevPage: Int? = page
        var nextPage: Int? = page
        if (page in 1..4) {
            nextPage = nextPage?.plus(1)
        }
        if (page in 2..5) {
            prevPage = prevPage?.minus(1)
        }
        if (page == 1) {
            prevPage = null
        }
        if (nextPage == 5) {
            nextPage = null
        }
        return mapOf(PREVIOUS_PAGE_KEY to prevPage, NEXT_PAGE_KEY to nextPage)
    }

    @ExperimentalSerializationApi
    @Test
    fun `access search heroes endpoint, query hero name, assert single hero result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=sas").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString()).heroes.size
                assertEquals(expected = 1, actual = actual)
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access search heroes endpoint, query hero name, assert multiple hero result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=sa").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString()).heroes.size
                assertEquals(expected = 3, actual = actual)
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access search heroes endpoint, empty query, assert empty list as a result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString()).heroes
                assertEquals(expected = emptyList(), actual = actual)
            }
        }
    }

    @ExperimentalSerializationApi
    @Test
    fun `access search heroes endpoint, query non exisiting hero, assert empty list as a result`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/boruto/heroes/search?name=parita").apply {
                assertEquals(
                    expected = HttpStatusCode.OK,
                    actual = response.status()
                )
                val actual = Json.decodeFromString<ApiResponse>(response.content.toString()).heroes
                assertEquals(expected = emptyList(), actual = actual)
            }
        }
    }


    @ExperimentalSerializationApi
    @Test
    fun `access non existing endpoint assert empty not found`() {
        withTestApplication(moduleFunction = Application::module) {
            handleRequest(HttpMethod.Get, "/unknown").apply {
                assertEquals(
                    expected = HttpStatusCode.NotFound,
                    actual = response.status()
                )
                assertEquals(expected = "Page not found.", actual = response.content)
            }
        }
    }
}
