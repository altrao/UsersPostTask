package com.userspost.jsonplaceholder

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import kotlin.properties.Delegates

@Configuration
@ConfigurationProperties("external.json-placeholder")
class JsonPlaceholderConfiguration {
    lateinit var baseUrl: String
    lateinit var postsUri: String
    lateinit var usersUri: String
    var expiration by Delegates.notNull<Long>()

    fun getPostUrl(id: Int) = "$baseUrl/$postsUri/$id"
    fun getUserUrl(id: Int) = "$baseUrl/$usersUri/$id"
}