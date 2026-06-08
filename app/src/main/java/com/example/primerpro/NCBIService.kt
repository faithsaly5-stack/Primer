package com.example.primerpro

import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query

interface NCBIApi {
    @GET("entrez/eutils/efetch.fcgi?db=nucleotide&rettype=fasta&retmode=text")
    suspend fun fetchFasta(@Query("id") id: String): ResponseBody

    @GET("entrez/eutils/esearch.fcgi?db=nucleotide&term=biomol+mrna[prop]+AND+homo+sapiens[organism]&retmax=1&retmode=json")
    suspend fun searchRandomHumanMRNA(@Query("retstart") retstart: Int): ResponseBody

    @GET("entrez/eutils/esearch.fcgi?db=nucleotide&term=biomol+genomic[prop]+AND+homo+sapiens[organism]&retmax=1&retmode=json")
    suspend fun searchRandomHumanGenomic(@Query("retstart") retstart: Int): ResponseBody
}

object NCBIClient {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://eutils.ncbi.nlm.nih.gov/")
        .build()

    val api: NCBIApi = retrofit.create(NCBIApi::class.java)
}
