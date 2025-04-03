package com.userspost.model

import jakarta.persistence.Entity
import jakarta.persistence.Id

@Entity(name = "Users")
data class User(@Id val id: Int, val name: String, val username: String, val email: String)
