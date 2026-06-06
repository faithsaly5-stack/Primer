package com.example.primerpro

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface NCBIApi {
    @GET("entrez/eutils/efetch.fcgi?db=nucleotide&rettype=fasta&retmode=text")
    suspend fun fetchFasta(@Query("id") id: String): ResponseBody
}

object NCBIClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://eutils.ncbi.nlm.nih.gov/")
        .build()

    val api: NCBIApi = retrofit.create(NCBIApi::class.java)
}
