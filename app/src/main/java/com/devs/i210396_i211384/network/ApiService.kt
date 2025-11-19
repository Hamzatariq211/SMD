package com.devs.i210396_i211384.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// Request Data Classes
data class SignupRequest(
    val email: String,
    val password: String,
    val username: String,
    val firstName: String,
    val lastName: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class UpdateProfileRequest(
    val firstName: String,
    val lastName: String,
    val username: String,
    val bio: String,
    val website: String,
    val email: String,
    val phone: String,
    val gender: String,
    val profileImageUrl: String,
    val isPrivate: Boolean
)

data class CreatePostRequest(
    val imageBase64: String,
    val caption: String,
    val location: String = ""
)

data class LikePostRequest(
    val postId: String
)

data class CommentRequest(
    val postId: String,
    val commentText: String
)

data class UploadStoryRequest(
    val storyImageBase64: String
)

data class ViewStoryRequest(
    val storyId: String
)

data class FollowRequest(
    val userId: String
)

data class RespondFollowRequest(
    val requestId: String,
    val action: String // "accept" or "reject"
)

data class SendMessageRequest(
    val receiverId: String,
    val messageText: String,
    val messageType: String = "text",
    val mediaUrl: String = "",
    val postId: String = "",
    val isVanishMode: Boolean = false
)

data class EditMessageRequest(
    val messageId: String,
    val messageText: String
)

data class DeleteMessageRequest(
    val messageId: String
)

data class UpdateStatusRequest(
    val isOnline: Boolean
)

data class UpdateFCMTokenRequest(
    val fcmToken: String
)

data class ReportScreenshotRequest(
    val chatRoomId: String
)

// Response Data Classes
data class AuthResponse(
    val message: String,
    val userId: String,
    val token: String,
    val isProfileSetup: Boolean
)

data class UserProfile(
    val id: String,
    val email: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val bio: String?,
    val website: String?,
    val phone: String?,
    val gender: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    val isProfileSetup: Boolean,
    val isPrivate: Boolean,
    val isOnline: Boolean,
    val lastSeen: Long
)

data class UserProfileWithCounts(
    val id: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val profileImageUrl: String?,
    val coverImageUrl: String?,
    val bio: String?,
    val website: String?,
    val isPrivate: Boolean,
    val isOnline: Boolean,
    val lastSeen: Long,
    val postsCount: Int,
    val followersCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean,
    val hasPendingRequest: Boolean
)

data class UserSearchResult(
    val id: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val profileImageUrl: String?,
    val bio: String?,
    val followingCount: Int,
    val followersCount: Int
)

data class UserListItem(
    val id: String,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val profileImageUrl: String?,
    val bio: String?,
    val isPrivate: Boolean,
    val isOnline: Boolean,
    val lastSeen: Long,
    val followersCount: Int,
    val followingCount: Int,
    val isFollowing: Boolean,
    val hasPendingRequest: Boolean
)

data class PostResponse(
    val postId: String,
    val userId: String,
    val username: String,
    val userProfileImage: String,
    val postImageBase64: String,
    val caption: String,
    val likeCount: Int,
    val commentCount: Int,
    val timestamp: Long,
    val isLiked: Boolean
)

data class CommentResponse(
    val commentId: String,
    val userId: String,
    val username: String,
    val userProfileImage: String,
    val commentText: String,
    val timestamp: Long
)

data class StoryResponse(
    val storyId: String,
    val userId: String,
    val username: String,
    val userProfileImage: String,
    val storyImageBase64: String,
    val timestamp: Long,
    val expiryTime: Long,
    val viewCount: Int
)

data class UserStoriesCollection(
    val userId: String,
    val username: String,
    val userProfileImage: String,
    val stories: List<StoryResponse>
)

data class FollowRequestResponse(
    val requestId: String,
    val fromUserId: String,
    val fromUsername: String,
    val fromProfileImageUrl: String,
    val timestamp: Long
)

data class ChatListItem(
    val chatRoomId: String,
    val otherUserId: String,
    val username: String,
    val profileImageUrl: String,
    val isOnline: Boolean,
    val lastSeen: Long,
    val lastMessage: String,
    val lastMessageTime: Long,
    val unreadCount: Int
)

data class MessageResponse(
    val messageId: String,
    val senderId: String,
    val receiverId: String,
    val messageText: String,
    val messageType: String,
    val imageBase64: String,
    val postId: String,
    val isEdited: Boolean,
    val editedAt: Long,
    val isDeleted: Boolean,
    val isSeen: Boolean,
    val timestamp: Long
)

data class NotificationResponse(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val referenceId: String?,
    val isRead: Boolean,
    val timestamp: Long,
    val fromUsername: String?,
    val fromProfileImage: String?
)

data class ErrorResponse(
    val error: String
)

interface ApiService {
    // ==================== Authentication ====================
    @POST("api/auth/signup.php")
    suspend fun signup(@Body request: SignupRequest): Response<AuthResponse>

    @POST("api/auth/login.php")
    suspend fun login(@Body request: LoginRequest): Response<AuthResponse>

    @POST("api/auth/logout.php")
    suspend fun logout(): Response<Map<String, String>>

    // ==================== Users ====================
    @GET("api/users/me.php")
    suspend fun getCurrentUser(): Response<UserProfile>

    @PUT("api/users/update.php")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<Map<String, String>>

    @GET("api/users/profile.php")
    suspend fun getUserProfile(@Query("userId") userId: String): Response<UserProfileWithCounts>

    @GET("api/users/search.php")
    suspend fun searchUsers(@Query("query") query: String): Response<List<UserSearchResult>>

    @GET("api/users/getAllUsers.php")
    suspend fun getAllUsers(): Response<List<UserListItem>>

    @POST("api/users/updateStatus.php")
    suspend fun updateOnlineStatus(@Body request: UpdateStatusRequest): Response<Map<String, String>>

    // ==================== Posts ====================
    @POST("api/posts/create.php")
    suspend fun createPost(@Body request: CreatePostRequest): Response<Map<String, Any>>

    @GET("api/posts/feed.php")
    suspend fun getPostsFeed(@Query("page") page: Int = 1): Response<List<PostResponse>>

    @GET("api/posts/getPost.php")
    suspend fun getPost(@Query("postId") postId: String): Response<PostResponse>

    @GET("api/posts/userPosts.php")
    suspend fun getUserPosts(@Query("userId") userId: String): Response<List<PostResponse>>

    @POST("api/posts/like.php")
    suspend fun likePost(@Body request: LikePostRequest): Response<Map<String, Any>>

    @POST("api/posts/comment.php")
    suspend fun addComment(@Body request: CommentRequest): Response<Map<String, Any>>

    @GET("api/posts/getComments.php")
    suspend fun getComments(@Query("postId") postId: String): Response<List<CommentResponse>>

    // ==================== Stories ====================
    @POST("api/stories/upload.php")
    suspend fun uploadStory(@Body request: UploadStoryRequest): Response<Map<String, Any>>

    @GET("api/stories/getUserStories.php")
    suspend fun getUserStories(@Query("userId") userId: String): Response<List<StoryResponse>>

    @GET("api/stories/getStories.php")
    suspend fun getStories(): Response<List<UserStoriesCollection>>

    @POST("api/stories/viewStory.php")
    suspend fun viewStory(@Body request: ViewStoryRequest): Response<Map<String, String>>

    // ==================== Follow System ====================
    @POST("api/follow/follow.php")
    suspend fun followUser(@Body request: FollowRequest): Response<Map<String, String>>

    @POST("api/follow/unfollow.php")
    suspend fun unfollowUser(@Body request: FollowRequest): Response<Map<String, String>>

    @GET("api/follow/requests.php")
    suspend fun getFollowRequests(): Response<List<FollowRequestResponse>>

    @POST("api/follow/respondRequest.php")
    suspend fun respondFollowRequest(@Body request: RespondFollowRequest): Response<Map<String, String>>

    @GET("api/follow/getFollowers.php")
    suspend fun getFollowers(@Query("userId") userId: String): Response<List<UserListItem>>

    @GET("api/follow/getFollowing.php")
    suspend fun getFollowing(@Query("userId") userId: String): Response<List<UserListItem>>

    // ==================== Messages ====================
    @POST("api/messages/send.php")
    suspend fun sendMessage(@Body request: SendMessageRequest): Response<Map<String, Any>>

    @GET("api/messages/getChatList.php")
    suspend fun getChatList(): Response<List<ChatListItem>>

    @GET("api/messages/getMessages.php")
    suspend fun getMessages(@Query("userId") userId: String): Response<List<MessageResponse>>

    @PUT("api/messages/editMessage.php")
    suspend fun editMessage(@Body request: EditMessageRequest): Response<Map<String, String>>

    @HTTP(method = "DELETE", path = "api/messages/deleteMessage.php", hasBody = true)
    suspend fun deleteMessage(@Body request: DeleteMessageRequest): Response<Map<String, String>>

    @POST("api/messages/reportScreenshot.php")
    suspend fun reportScreenshot(@Body request: ReportScreenshotRequest): Response<Map<String, String>>

    // ==================== Notifications ====================
    @POST("api/notifications/updateFCMToken.php")
    suspend fun updateFCMToken(@Body request: UpdateFCMTokenRequest): Response<Map<String, String>>

    @GET("api/notifications/getNotifications.php")
    suspend fun getNotifications(): Response<List<NotificationResponse>>

    companion object {
        // For emulator use 10.0.2.2, for real device use your computer's IP
        private const val BASE_URL = "http://10.0.2.2/instagram_api/"

        fun create(): ApiService {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                    SessionManager.getToken()?.let { token ->
                        request.addHeader("Authorization", "Bearer $token")
                    }
                    chain.proceed(request.build())
                }
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(ApiService::class.java)
        }
    }
}
