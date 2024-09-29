package com.aditya.socialguru.domain_layer.remote_service.update

import com.aditya.socialguru.data_layer.model.git.GitHubRelease
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface UpdateService {
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun latestRelease(@Path("owner") owner: String, @Path("repo") repository: String): Response<GitHubRelease>
}