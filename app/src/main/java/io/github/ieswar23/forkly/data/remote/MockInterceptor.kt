package io.github.ieswar23.forkly.data.remote

import com.google.gson.Gson
import com.google.gson.JsonParser
import io.github.ieswar23.forkly.data.remote.dto.PlaceOrderResponseDto
import io.github.ieswar23.forkly.data.remote.dto.RiderDto
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.FileNotFoundException
import java.io.InputStream
import kotlin.random.Random

/**
 * Serves the Forkly "backend" from bundled JSON in `assets/api`, with realistic latency.
 * The rest of the stack (Retrofit, Gson, repositories) is identical to a real network setup,
 * so swapping in a live server only means removing this interceptor.
 */
class MockInterceptor(
    /** Opens a file under `assets/api`, e.g. `restaurants.json`. */
    private val openAsset: (String) -> InputStream,
    private val gson: Gson,
    private val random: Random = Random.Default,
    private val minDelayMs: Long = 300,
    private val maxDelayMs: Long = 700,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Thread.sleep(random.nextLong(minDelayMs, maxDelayMs + 1))

        val segments = request.url.pathSegments.dropWhile { it == "v1" }.filter { it.isNotEmpty() }
        return try {
            val body = route(request.method, segments)
            if (body == null) {
                respond(request, 404, """{"error":"Not found"}""")
            } else {
                respond(request, 200, body)
            }
        } catch (e: FileNotFoundException) {
            respond(request, 404, """{"error":"Not found"}""")
        }
    }

    private fun route(method: String, segments: List<String>): String? = when {
        method == "GET" && segments == listOf("restaurants") -> asset("restaurants.json")
        method == "GET" && segments.size == 3 && segments[0] == "restaurants" && segments[2] == "menu" ->
            asset("menus/${segments[1]}.json")
        method == "GET" && segments == listOf("banners") -> asset("banners.json")
        method == "GET" && segments == listOf("categories") -> asset("categories.json")
        method == "POST" && segments == listOf("orders") -> placeOrder()
        else -> null
    }

    private fun placeOrder(): String {
        val riders = JsonParser.parseString(asset("riders.json")).asJsonArray
            .map { gson.fromJson(it, RiderDto::class.java) }
        val response = PlaceOrderResponseDto(
            orderId = "FK" + random.nextInt(10_000_000, 99_999_999),
            placedAt = System.currentTimeMillis(),
            rider = riders[random.nextInt(riders.size)],
        )
        return gson.toJson(response)
    }

    private fun asset(path: String): String =
        openAsset(path).bufferedReader().use { it.readText() }

    private fun respond(request: Request, code: Int, json: String): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Not Found")
            .body(json.toResponseBody(JSON))
            .addHeader("Content-Type", "application/json")
            .build()

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
