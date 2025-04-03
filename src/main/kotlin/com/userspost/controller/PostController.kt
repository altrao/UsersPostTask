package com.userspost.controller

import com.userspost.jsonplaceholder.JsonPlaceholderService
import com.userspost.jsonplaceholder.UserPost
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/posts")
class PostController(
    private val jsonPlaceholderService: JsonPlaceholderService
) {

    @GetMapping("{id}")
    suspend fun getPost(@PathVariable id: Int): ResponseEntity<UserPost?> {
        if (id < 0) {
            return ResponseEntity.badRequest().build()
        }

        val userPost = jsonPlaceholderService.fetch(id)

        return if (userPost != null) {
            ResponseEntity.ok(userPost)
        } else {
            ResponseEntity.notFound().build()
        }
    }
}
