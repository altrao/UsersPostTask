package com.userspost.jsonplaceholder

import com.userspost.model.PostRepository
import com.userspost.model.User
import com.userspost.model.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class JsonPlaceholderService(
    private val jsonPlaceholderClient: JsonPlaceholderClient,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    @Value("\${spring.datasource.max-active:10}")
    databasePool: Int
) {
    companion object {
        fun getSemaphorePermits(databaseLimit: Int): Int = (databaseLimit * 0.8).toInt()
    }

    /**
     * Avoiding race conditions for both resources as it happens under high load.
     */
    private val postsDeferred = ConcurrentHashMap<Int, Deferred<UserPost?>>()
    private val usersDeferred = ConcurrentHashMap<Int, Deferred<User?>>()

    private val semaphore = Semaphore(permits = getSemaphorePermits(databasePool))

    suspend fun fetch(id: Int): UserPost? = withContext(Dispatchers.IO) {
        postsDeferred.computeIfAbsent(id) {
            async {
                semaphore.withPermit {
                    val cachedPost = postRepository.findValidPost(id) ?: return@withPermit
                    val cachedUser = userRepository.findById(cachedPost.userId)

                    if (cachedUser.isPresent) {
                        return@async UserPost(cachedUser.get(), cachedPost)
                    }
                }

                val fresh = jsonPlaceholderClient.fetch(id) ?: return@async null

                safeCache(fresh)

                return@async fresh
            }
        }.await().apply {
            postsDeferred.remove(id)
        }
    }

    /**
     * Overcomplicated solution to avoid conflict when inserting users.
     * When many posts of the same user are requested at once it will try cache all those users and
     * there will be more than one transaction inserting that same user which will throw an exception.
     *
     * Both operations are async so the caller doesn't have to wait for the database to finish its process,
     * enabling the response to be returned quicker.
     */
    private suspend fun CoroutineScope.safeCache(userPost: UserPost) {
        async {
            semaphore.withPermit {
                postRepository.save(userPost.post)

                usersDeferred.computeIfAbsent(userPost.user.id) {
                    async {
                        userRepository.save(userPost.user)
                    }
                }
            }
        }.await()

        usersDeferred.remove(userPost.user.id)
    }
}