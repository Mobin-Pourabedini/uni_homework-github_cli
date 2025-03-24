import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.*

fun getGitHubToken(): String {
    return System.getenv("GITHUB_TOKEN") ?: throw IllegalStateException("GITHUB_TOKEN is not set!")
}

// Data Models
data class GitHubUser(
    val login: String,
    val followers: Int,
    val following: Int,
    @SerializedName("created_at") val createdAt: String
)

data class Repository(
    val name: String
)

// API Service
interface GitHubApi {
    @GET("users/{username}")
    suspend fun getUser(@Path("username") username: String): GitHubUser

    @GET("users/{username}/repos")
    suspend fun getUserRepos(@Path("username") username: String): List<Repository>
}

// Retrofit Client
fun createGitHubService(authToken: String): GitHubApi {
    val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    val authInterceptor = Interceptor { chain ->
        val originalRequest: Request = chain.request()
        val authenticatedRequest = originalRequest.newBuilder()
            .header("Authorization", "Bearer $authToken")  // Add Bearer Token here
            .build()
        chain.proceed(authenticatedRequest)
    }
    val client = OkHttpClient.Builder().addInterceptor(logging).addInterceptor(authInterceptor).build()

    val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .build()

    return retrofit.create(GitHubApi::class.java)
}

// CLI
suspend fun main() {

    val scanner = Scanner(System.`in`)
    val api = createGitHubService(getGitHubToken())
    val retrofit = Retrofit.Builder().baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create(Gson()))
        .build()
    var ret = retrofit.create(GitHubApi::class.java)
    val user = ret.getUser("b4rd14")
    println(user)
}
