package com.userspost.model

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.sql.Timestamp

@Entity(name = "Posts")
data class Post(
    @Id val id: Int,
    val userId: Int,
    val title: String,
    val body: String,
    @JsonIgnore
    var expiresAt: Timestamp? = null
)
