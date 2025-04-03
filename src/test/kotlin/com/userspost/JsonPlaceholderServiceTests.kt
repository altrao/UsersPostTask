package com.userspost

import com.userspost.jsonplaceholder.JsonPlaceholderClient
import com.userspost.jsonplaceholder.JsonPlaceholderService
import com.userspost.jsonplaceholder.UserPost
import com.userspost.model.Post
import com.userspost.model.PostRepository
import com.userspost.model.User
import com.userspost.model.UserRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.util.*

class JsonPlaceholderServiceTests {
    private val jsonPlaceholderClient: JsonPlaceholderClient = mockk()
    private val postRepository: PostRepository = mockk(relaxed = true)
    private val userRepository: UserRepository = mockk(relaxed = true)
    private val databasePool: Int = 5

    private val service = JsonPlaceholderService(jsonPlaceholderClient, postRepository, userRepository, databasePool)

    @Test
    fun `should return cached data when available`() = runTest {
        val cachedPost = Post(1, 2, "title", "body")
        val cachedUser = User(2, "name", "username", "email")

        every { postRepository.findValidPost(1) } returns cachedPost
        every { userRepository.findById(2) } returns Optional.of(cachedUser)

        val result = service.fetch(1)

        assert(result == UserPost(cachedUser, cachedPost))
        verify(exactly = 1) { postRepository.findValidPost(1) }
        verify(exactly = 1) { userRepository.findById(2) }
        coVerify(exactly = 0) { jsonPlaceholderClient.fetch(any()) }
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { postRepository.save(any()) }
    }

    @Test
    fun `should fetch fresh data when there is no cache`() = runTest {
        every { postRepository.findValidPost(1) } returns null

        val freshUser = User(2, "name", "username", "email")
        val freshPost = Post(1, 2, "title", "body")

        coEvery { jsonPlaceholderClient.fetch(1) } returns UserPost(freshUser, freshPost)

        coEvery { userRepository.save(any()) } returns freshUser
        coEvery { postRepository.save(any()) } returns freshPost

        val result = service.fetch(1)

        assert(result == UserPost(freshUser, freshPost))
        verify(exactly = 1) { postRepository.findValidPost(1) }
        coVerify(exactly = 1) { jsonPlaceholderClient.fetch(1) }
        verify(exactly = 1) { userRepository.save(freshUser) }
        verify(exactly = 1) { postRepository.save(freshPost) }
    }

    @Test
    fun `should fetch fresh data when user is not present`() = runTest {
        val cachedPost = Post(1, 2, "title", "body")
        every { postRepository.findValidPost(1) } returns cachedPost
        every { userRepository.findById(2) } returns Optional.empty()

        val freshUser = User(2, "name", "username", "email")
        val freshPost = Post(1, 2, "title", "body")

        coEvery { jsonPlaceholderClient.fetch(1) } returns UserPost(freshUser, freshPost)

        coEvery { userRepository.save(any()) } returns freshUser
        coEvery { postRepository.save(any()) } returns freshPost

        val result = service.fetch(1)

        assert(result == UserPost(freshUser, freshPost))
        verify(exactly = 1) { postRepository.findValidPost(1) }
        verify(exactly = 1) { userRepository.findById(2) }
        coVerify(exactly = 1) { jsonPlaceholderClient.fetch(1) }
        verify(exactly = 1) { userRepository.save(freshUser) }
        verify(exactly = 1) { postRepository.save(freshPost) }
    }

    @Test
    fun `should return null when fresh data is not available`() = runTest {
        every { postRepository.findValidPost(1) } returns null
        coEvery { jsonPlaceholderClient.fetch(1) } returns null

        val result = service.fetch(1)

        assert(result == null)
        verify(exactly = 1) { postRepository.findValidPost(1) }
        coVerify(exactly = 1) { jsonPlaceholderClient.fetch(1) }
        verify(exactly = 0) { userRepository.save(any()) }
        verify(exactly = 0) { postRepository.save(any()) }
    }
}
