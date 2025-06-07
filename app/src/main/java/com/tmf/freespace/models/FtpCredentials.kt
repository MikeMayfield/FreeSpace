package com.tmf.freespace.models

import java.nio.charset.StandardCharsets
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.text.isBlank

/**
 * Decode and provide access to FTP credentials from a given FTP token.
 *
 * @param ftpToken The FTP token in the format "1:serverID:dotted.Ip.Address:login:password".
 */
class FtpCredentials(
    var ftpToken: String  //Format: "1:serverID:dotted.Ip.Address:login:password"
) {
    var serverID = -1  //Unique identifier for the FTP server
    lateinit var ipAddress: String  //IP address of FTP server in dotted notation
    lateinit var username: String  //FTP username
    lateinit var password: String  //FTP password

    init {
        try {
            if (ftpToken.isEmpty()) {
                ftpToken = simulateFtpToken()
            }
            decodeToken(ftpToken)
        } catch (e: Exception) {
            // Handle decoding errors appropriately
            // For example, log the error and set fields to invalid states
            // or rethrow a custom exception.
            System.err.println("Error decoding FTP token: ${e.message}")
            // Set fields to a default/error state if initialization fails

            ipAddress = "ERROR_DECODING_TOKEN"
            username = "ERROR_DECODING_TOKEN"
            password = "ERROR_DECODING_TOKEN"
            // Consider throwing a custom exception if the credentials are unusable
            // throw IllegalArgumentException("Invalid FTP token format", e)
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeToken(token: String) {
        val decodedString = token  //TODO Decode string when sent from server
//        // 1. Convert from UrlBase64/Standard Base64 to byte array
//        //    Assuming standard Base64 here. Use Base64.URL_SAFE if applicable.
//        val decodedBytes: ByteArray
//        try {
//            decodedBytes = Base64.decode(source = token)
//        } catch (e: IllegalArgumentException) {
//            System.err.println("FTP Token is not valid Base64: ${e.message}")
//            throw IllegalArgumentException("Token is not valid Base64", e)
//        }
//
//        // 2. Decode byte array by XORing with -1 and converting to string (assuming UTF-8)
//        decodedBytes.forEachIndexed { index, byte ->
//            decodedBytes[index] = (byte.toInt() xor -1).toByte()
//        }
//        val decodedString = String(decodedBytes, StandardCharsets.UTF_8)

        // 3. Split tokens
        //    Assuming the format is "VERSION:IP_ADDRESS:USERNAME:PASSWORD"
        //    Adjust the delimiter and order as needed.
        val parts = decodedString.split(":", limit = 5) // limit = 5 to ensure password can contain colons
        if (parts.size < 5) { // Or whatever minimum number of parts you expect
            throw IllegalArgumentException("Invalid token structure after decoding. Expected at least 4 parts, got ${parts.size}.")
        }

        // 4. Verify version number (optional but recommended)
        val versionStr = parts[0]
        try {
            val version = versionStr.toInt()
            if (version != 1) { // Assuming current version is 1
                throw IllegalArgumentException("Unsupported token version: $version")
            }
        } catch (e: NumberFormatException) {
            throw IllegalArgumentException("Invalid version number in token: $versionStr", e)
        }

        // 5. Update fields. Assign parts based on your defined order
        this.serverID = parts[1].toInt()
        this.ipAddress = parts[2]
        this.username = parts[3]
        this.password = parts[4]

        // Basic validation (optional, but good practice)
        if (this.serverID == -1
            || this.ipAddress.isBlank()
            || this.username.isBlank()
            || this.password.isBlank()) {
                throw IllegalArgumentException("Decoded IP address, username, or password is blank.")
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun simulateFtpToken(): String {
        val combinedParts = "1:1:1.2.3.4:user:password"
//        val decodedBytes: ByteArray = combinedParts.toByteArray()
//        val encodedBytes = ByteArray(decodedBytes.size)
//        decodedBytes.forEachIndexed { index, decodedByte ->
//            encodedBytes[index] = (decodedByte.toInt() xor -1).toByte()
//        }
//        val base64Encoded = Base64(true, true, Base64.PaddingOption.ABSENT).encode(source = decodedBytes)
//        return base64Encoded
        return combinedParts  //TODO Encode string to match encoding used with server
    }

    override fun toString(): String {
        return "FtpCredentials(ipAddress='$ipAddress', username='$username', serverID=$serverID)"
        // Avoid logging or toString-ing the password directly for security.
    }}
