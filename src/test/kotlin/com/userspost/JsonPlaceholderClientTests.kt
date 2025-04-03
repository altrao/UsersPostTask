package com.userspost

import com.userspost.jsonplaceholder.JsonPlaceholderClient
import com.userspost.jsonplaceholder.JsonPlaceholderConfiguration
import com.userspost.jsonplaceholder.TimeProvider
import com.userspost.jsonplaceholder.UserPost
import com.userspost.model.Post
import com.userspost.model.User
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.sql.Timestamp
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class JsonPlaceholderClientTests {
    private val exampleUrl = "https://example.com"
    private val postsUrl = "$exampleUrl/posts"
    private val usersUrl = "$exampleUrl/users"
    private val instant = Instant.now()

    private val configuration: JsonPlaceholderConfiguration = mockk()
    private val webClient: WebClient = mockk()
    private val timeProvider: TimeProvider = mockk()
    private val client = JsonPlaceholderClient(configuration, webClient, timeProvider)

    private val uriSpecMock: WebClient.RequestHeadersUriSpec<*> = mockk()
    private val headersSpecMock: WebClient.RequestHeadersSpec<*> = mockk()
    private val retrieveMock: WebClient.ResponseSpec = mockk()

    @BeforeEach
    fun setup() {
        every { webClient.get() } returns uriSpecMock
        every { timeProvider.now() } returns instant
    }

    @Test
    fun `should return UserPost when both user and post exist`() = runTest {
        every { configuration.expiration } returns 10
        every { configuration.getPostUrl(948594) } returns "$postsUrl/948594"
        every { configuration.getUserUrl(4548) } returns "$usersUrl/4548"

        val post = Post(948594, 4548, "Title", "Body")
        mockWebClientResponse("$postsUrl/948594", post)

        val user = User(4548, "John Doe", "johndoe", "john@example.com")
        mockWebClientResponse("$usersUrl/4548", user)

        val result = client.fetch(948594)

        assertEquals(UserPost(user, post), result)
    }


    @Test
    fun `should return null when post does not exist`() = runTest {
        every { configuration.expiration } returns 10
        every { configuration.getPostUrl(1) } returns "$postsUrl/1"

        mockWebClientResponse<Post>("$postsUrl/1", null)

        val result = client.fetch(1)

        assertNull(result)
        verify(exactly = 0) { configuration.getUserUrl(any()) }
    }

    @Test
    fun `should return null when user does not exist`() = runTest {
        every { configuration.expiration } returns 10
        every { configuration.getPostUrl(456) } returns "$postsUrl/456"
        every { configuration.getUserUrl(321) } returns "$usersUrl/321"

        val post = Post(456, 321, "Title", "Body")
        mockWebClientResponse<Post>("$postsUrl/456", post)

        mockWebClientResponse<User>("$usersUrl/321", null)

        val result = client.fetch(456)
        assertNull(result)
    }

    @ParameterizedTest
    @ValueSource(longs = [10, 60, 3600, 7200, 1800, 1, 0])
    fun `should return updated post expiration timestamp`(timeout: Long) = runTest {
        every { configuration.expiration } returns timeout
        every { configuration.getPostUrl(4783) } returns "$postsUrl/4783"
        every { configuration.getUserUrl(9584) } returns "$usersUrl/9584"

        val post = Post(4783, 9584, "Title", "Body")
        mockWebClientResponse("$postsUrl/4783", post)

        val user = User(9584, "John Doe", "johndoe", "john@example.com")
        mockWebClientResponse("$usersUrl/9584", user)

        val result = client.fetch(4783)

        assertNotNull(result)
        assertNotNull(result.post.expiresAt)
        assertEquals((result.post.expiresAt!!.time - Timestamp.from(instant).time) / 1000, timeout)
    }

    @Test
    fun `should return null when an exception is thrown`() = runTest {
        every { configuration.expiration } returns 0
        every { configuration.getPostUrl(1) } returns "$postsUrl/1"
        every { configuration.getPostUrl(2) } returns "$postsUrl/2"

        every { uriSpecMock.uri("$postsUrl/2") } throws RuntimeException()
        every { uriSpecMock.uri("$postsUrl/1") } throws WebClientResponseException(
            HttpStatus.NOT_FOUND.value(),
            "Not Found",
            null,
            null,
            null
        )

        assertNull(client.fetch(1))
        assertNull(client.fetch(2))
    }

    private inline fun <reified T> mockWebClientResponse(url: String, response: T?) {
        every { uriSpecMock.uri(url) } returns headersSpecMock
        every { headersSpecMock.accept(MediaType.APPLICATION_JSON) } returns headersSpecMock
        every { headersSpecMock.retrieve() } returns retrieveMock
        every { retrieveMock.onStatus(any(), any()) } returns retrieveMock
        every { retrieveMock.bodyToMono(T::class.java) } returns Mono.justOrEmpty(response)
//        every {  }
    }

    @AfterEach
    fun clear() {
        clearMocks(configuration, webClient, uriSpecMock, headersSpecMock, retrieveMock, timeProvider)
    }
}