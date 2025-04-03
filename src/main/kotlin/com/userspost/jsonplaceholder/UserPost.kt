package com.userspost.jsonplaceholder

import com.userspost.model.Post
import com.userspost.model.User

data class UserPost(val user: User, val post: Post)