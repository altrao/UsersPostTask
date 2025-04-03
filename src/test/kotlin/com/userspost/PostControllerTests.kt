package com.userspost

import com.userspost.jsonplaceholder.JsonPlaceholderService
import com.userspost.jsonplaceholder.UserPost
import com.userspost.model.Post
import com.userspost.model.User
import kotlinx.coroutines.test.runTest
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.RequestBuilder
import org.springframework.test.web.servlet.ResultActions
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.request
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import kotlin.test.Test

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTests(
    @Autowired private val mockMvc: MockMvc
) {
    @MockitoBean
    lateinit var jsonPlaceholderService: JsonPlaceholderService

    @Test
    fun `should return not found when post does not exist`() = runTest {
        `when`(jsonPlaceholderService.fetch(99)).thenReturn(null)

        mockMvc.performAsync(get("/api/posts/99"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return post when cached data exists`() = runTest {
        val post = Post(
            id = 1,
            userId = 143,
            title = "Test Post",
            body = "Test Body"
        )

        val user = User(143, "John Doe", "johndoe", "johndoe@example.com")

        val userPost = UserPost(user, post)
        `when`(jsonPlaceholderService.fetch(1)).thenReturn(userPost)

        mockMvc.performAsync(get("/api/posts/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.user.name").value("John Doe"))
            .andExpect(jsonPath("$.user.email").value("johndoe@example.com"))
            .andExpect(jsonPath("$.user.username").value("johndoe"))
            .andExpect(jsonPath("$.post.id").value(1))
            .andExpect(jsonPath("$.post.userId").value(143))
            .andExpect(jsonPath("$.post.title").value("Test Post"))
            .andExpect(jsonPath("$.post.body").value("Test Body"))
    }

    @Test
    fun `should return bad request when an invalid post id is requested`() {
        mockMvc.perform(get("/api/posts/abcd"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/api/posts/1438a"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/api/posts/_2893"))
            .andExpect(status().isBadRequest)

        mockMvc.perform(get("/api/posts/345890903458"))
            .andExpect(status().isBadRequest)
    }

    private fun MockMvc.performAsync(requestBuilder: RequestBuilder): ResultActions {
        return mockMvc.perform(
            asyncDispatch(
                mockMvc.perform(requestBuilder).andExpect(request().asyncStarted()).andReturn()
            )
        )
    }
}
