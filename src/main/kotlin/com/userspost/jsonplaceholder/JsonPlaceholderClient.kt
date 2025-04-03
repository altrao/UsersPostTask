package com.userspost.jsonplaceholder

import com.userspost.logger
import com.userspost.model.Post
import com.userspost.model.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import reactor.netty.http.client.HttpClient
import java.sql.Timestamp
import java.time.Duration

@Service
class JsonPlaceholderClient(
    private val configuration: JsonPlaceholderConfiguration,
    private val webClient: WebClient,
    private val timeProvider: TimeProvider
) {
    private val logger = logger()

    suspend fun fetch(id: Int): UserPost? {
        return try {
            requestData<Post>(configuration.getPostUrl(id))?.apply {
                expiresAt = Timestamp.from(timeProvider.now().plusSeconds(configuration.expiration))
            }?.let { post ->
                val user = requestData<User>(configuration.getUserUrl(post.userId)) ?: return@let null
                UserPost(user, post)
            }
        } catch (e: Exception) {
            logger.severe(e.stackTraceToString())
            null
        }
    }

    private suspend inline fun <reified T> requestData(uri: String): T? {
        logger.info("Request fresh $uri")

        return withContext(Dispatchers.IO) {
            webClient.get()
                .uri(uri)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus({ it.is4xxClientError || it.is5xxServerError }) {
                    if (!HttpStatus.NOT_FOUND.isSameCodeAs(it.statusCode())) {
                        logger.severe("Http status ${it.statusCode().value()} on requesting ${it.request().uri}")
                    }

                    Mono.empty()
                }
                .bodyToMono(T::class.java)
                .onErrorResume { Mono.empty() }
                .awaitSingleOrNull()
        }
    }
}

@Configuration
private class WebClientConfig(private val configuration: JsonPlaceholderConfiguration) {
    @Bean
    fun webClient(builder: WebClient.Builder): WebClient {
        return builder.baseUrl(configuration.baseUrl)
            .clientConnector(ReactorClientHttpConnector(HttpClient.create().responseTimeout(Duration.ofSeconds(5))))
            .filter { request, next ->
                next.exchange(request).onErrorResume { Mono.empty() }
            }.build()
    }
}