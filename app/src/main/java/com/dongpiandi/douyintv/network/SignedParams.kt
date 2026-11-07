package com.dongpiandi.douyintv.network

import java.net.URLEncoder

class SignedParams {
    private val values = mutableListOf<Pair<String, String>>()

    fun add(name: String, value: String?): SignedParams = apply {
        val newValue = name to (value ?: "")
        val index = values.indexOfFirst { it.first == name }
        if (index >= 0) {
            values[index] = newValue
        } else {
            values += newValue
        }
    }

    fun add(name: String, value: Number): SignedParams = add(name, value.toString())

    fun encoded(): String = values.joinToString("&") { (name, value) ->
        "${encode(name)}=${encode(value)}"
    }

    fun sign(signer: AbogusSigner, body: String = ""): String {
        val unsigned = encoded()
        val signature = signer.sign(unsigned, body)
        return "$unsigned&a_bogus=${encode(signature)}"
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name())
}
