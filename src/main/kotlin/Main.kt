import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.*
import kotlin.system.exitProcess

data class GitHubUser(
    var repos: List<Repository>,
    val login: String,
    val followers: Int,
    val following: Int,
    @SerializedName("created_at") val createdAt: String
) {
    override fun toString(): String {
        val repoNames = repos.joinToString(", ") { it.name }.ifEmpty { "No Repositories" }

        return """
            GitHub User:
            --------------
            * Login      : $login
            * Followers  : $followers
            * Following  : $following
            * Created At : $createdAt
            * Repos      : $repoNames
            =========================
        """.trimIndent()
    }
}

data class Repository(
    val name: String,
    @SerializedName("html_url") val htmlUrl: String,
    @Transient var owner: String
) {
    override fun toString(): String {
        return """
            Repository:
            ---------------
            Name  : $name
            Owner : $owner
            Link  : $htmlUrl
        """.trimIndent()
    }
}

// API Service


class Storage() {
    private val usernameToUser = mutableMapOf<String, GitHubUser>()
    private val usernameToRepos = mutableMapOf<String, List<Repository>>()
    private val reposList = mutableListOf<Repository>()

    fun saveUserData(user: GitHubUser) {
        usernameToUser[user.login] = user
        for (repo in user.repos) {
            repo.owner = user.login
            reposList.add(repo)
        }
        usernameToRepos[user.login] = user.repos
    }

    fun cachedUser(username: String): GitHubUser? {
        return usernameToUser[username]
    }

    fun listAllUsers(): List<GitHubUser> {
        return usernameToUser.values.toList()
    }

    fun searchAmongUsers(searchTerm: String): List<GitHubUser> {
        return this.listAllUsers().filter { it.login.contains(searchTerm, true) }
    }

    fun searchAmongUserRepos(searchTerm: String): List<Repository> {
        return reposList.filter { it.name.contains(searchTerm, true) }
    }
}

// Retrofit Client


class GitHubClient() {
    private val api = this.createGitHubService()

    interface GitHubApi {
        @GET("users/{username}")
        suspend fun getUser(@Path("username") username: String): GitHubUser

        @GET("users/{username}/repos")
        suspend fun getUserRepos(@Path("username") username: String): List<Repository>
    }

    private fun createGitHubService(): GitHubApi {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.github.com/")
            .client(OkHttpClient.Builder().build())
            .addConverterFactory(GsonConverterFactory.create(Gson()))
            .build()

        return retrofit.create(GitHubApi::class.java)
    }

    suspend fun fetchUser(username: String): GitHubUser? {
        return try {
            api.getUser(username)
        } catch (e: HttpException) {
            handleHttpException(e)
            null
        }
    }

    suspend fun fetchRepos(username: String): List<Repository> {
        return try {
            api.getUserRepos(username)
        } catch (e: HttpException) {
            handleHttpException(e)
            emptyList()
        }
    }

    private fun handleHttpException(e: HttpException) {
        when (e.code()) {
            404 -> println("GitHub API 404 Not Found")
            500 -> println("GitHub API Internal Error")
            503 -> println("GitHub API Unavailable")
        }
    }
}

// CLI
suspend fun main() {

    val scanner = Scanner(System.`in`)
    val gitHubClient = GitHubClient()
    val storage = Storage()

    while (true) {
        print("1. Get User by username\n" +
                "2. Show users in memory\n" +
                "3. Search among users in memory\n" +
                "4. Search among repositories in memory\n" +
                "5. Exit\n> ")
        when (val input = scanner.nextInt()) {
            1 -> {
                print("Write the username:\n> ")
                scanner.nextLine()
                val username = scanner.nextLine().trim()

                val user = storage.cachedUser(username) ?: gitHubClient.fetchUser(username)

                if (user != null) {
                    user.repos = gitHubClient.fetchRepos(username)
                    storage.saveUserData(user)
                    println(user)
                }
            }
            2 -> {
                print("listing all users:\n")
                for (user in storage.listAllUsers()) {
                    println("\t" + user.toString())
                }
            }
            3 -> {
                print("write the term you want to search:\n> ")
                scanner.nextLine()
                val searchTerm = scanner.nextLine().trim()
                val users = storage.searchAmongUsers(searchTerm)
                for (user in users) {
                    println("\t" + user.toString())
                }
            }
            4 -> {
                print("write the term you want to search:\n> ")
                scanner.nextLine()
                val searchTerm = scanner.nextLine().trim()
                val repos = storage.searchAmongUserRepos(searchTerm)
                for (repo in repos) {
                    println("\t" + repo.toString())
                }
            }
            5 -> {
                exitProcess(0)
            }
        }
    }
}
