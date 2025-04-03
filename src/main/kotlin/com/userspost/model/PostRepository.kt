package com.userspost.model

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.NativeQuery
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<Post, Int> {
    @NativeQuery("SELECT user_id, id, title, body, expires_at FROM Posts WHERE id = ?1 AND expires_at > NOW()")
    fun findValidPost(id: Int): Post?
}