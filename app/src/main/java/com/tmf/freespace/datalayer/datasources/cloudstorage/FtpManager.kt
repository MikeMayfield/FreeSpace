package com.tmf.freespace.datalayer.datasources.cloudstorage

import android.util.Log
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class FtpManager {

    private val ftpClient = FTPClient()
    private var currentServer = ""


    //begin Public API methods

    /**
     * Logs into the remote FTP server.
     *
     * @param server The hostname or IP address of the FTP server.
     * @param port The port number for the FTP connection (default is 21).
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @return True if login was successful, false otherwise.
     * @throws java.io.IOException If an error occurs during connection or login.
     */
    suspend fun login(server: String, port: Int = 21, username: String, password: String): Boolean {
        if (server == currentServer) {
            return true  //Already logged into this server
        }

        if (currentServer.isNotEmpty()) {
            logout()
        }

        try {
            if (isConnected()) {
                return true
            }

            ftpClient.connect(server, port)
            val replyCode = ftpClient.replyCode
            if (!FTPReply.isPositiveCompletion(replyCode)) {
                ftpClient.disconnect()
                System.err.println("FTP server refused connection. Reply code: $replyCode")
                return false
            }

            if (ftpClient.login(username, password)) {
                ftpClient.enterLocalPassiveMode()
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE)
                return true
            } else {
                System.err.println("Login to FTP server failed.")
                if (isConnected()) {
                    try {
                        ftpClient.logout()
                        ftpClient.disconnect()
                    }
                    catch (e: IOException) {
                        System.err.println("Error during cleanup after failed login: ${e.message}")
                    }
                }
                return false
            }
        }
        catch (e: IOException) {
            System.err.println("Error connecting or logging in to FTP server: ${e.message}")
            if (isConnected()) {
                try {
                    ftpClient.disconnect()
                }
                catch (e: IOException) {
                    Log.e("login", "Error during cleanup after failed login: ${e.message}")
                }
            }
            return false
        }
    }

    /**
     * Logs out of the remote FTP server and closes the connection.
     *
     * @return True if logout and disconnection were successful, false otherwise.
     */
    fun logout() {
        try {
            var loggedOut = true
            currentServer = ""
            if (isConnected()) {
                loggedOut = ftpClient.logout()
                ftpClient.disconnect()
            }
        } catch (e: IOException) {
            System.err.println("Error logging out or disconnecting from FTP server: ${e.message}")
        }
    }

    /**
     * Copies a local file to the FTP server, ensuring BINARY_FILE_TYPE,
     * and creating the remote directory path if it doesn't exist.
     *
     * @param localFile The local file to upload.
     * @param remotePath The full remote path including directory and filename (e.g., "freespace/userID/fileID.tmp").
     * @return True if the file was successfully uploaded, false otherwise.
     * @throws IOException If an error occurs during file transfer or directory creation.
     * @throws IllegalStateException If not connected to the FTP server.
     */
    suspend fun uploadFile(localFile: File, remotePath: String): Boolean {
        if (!localFile.exists() || !localFile.isFile) {
            System.err.println("Local file does not exist or is not a regular file: ${localFile.absolutePath}")
            return false
        }

        if (!ftpClient.isConnected) {
            throw IllegalStateException("BUG: Not connected to FTP server")
        }

        try {
            setBinaryFileType()  // Ensure binary file type for this transfer
            if (changeToWorkingDirectory(extractPathFromFullPath(remotePath))) {
                FileInputStream(localFile).use { inputStream ->
                    return ftpClient.storeFile(extractFileNameFromFullPath(remotePath), inputStream)
                }
            }
            else {
                return false
            }
        } catch (e: IOException) {
            System.err.println("Error uploading file to FTP server at '$remotePath': ${e.message}")
            return false
        }
    }

    /**
     * Download a file from the FTP server to a local file.
     * This is a suspend function and should be called from a coroutine.
     *
     * @param remoteFilePath The full path of the file on the FTP server (including leading "/" for root directory)
     * @param localFile The local File object where the downloaded content will be saved.
     *                  The parent directory of this file must exist.
     * @return True if the file was successfully downloaded, false otherwise.
     * @throws IOException If an error occurs during file transfer or local file operations.
     * @throws IllegalStateException If not connected to the FTP server.
     */
    fun downloadFile(remoteFilePath: String, localFile: File): Boolean {
        if (!isConnected()) {
            throw IllegalStateException("Not connected to FTP server. Please login first.")
        }

        try {
            if (changeToWorkingDirectory(extractPathFromFullPath(remoteFilePath))) {
                FileOutputStream(localFile).use { outputStream: OutputStream ->
                    return ftpClient.retrieveFile(extractFileNameFromFullPath(remoteFilePath), outputStream)
                }
            }
            else {
                return false
            }
        }
        catch (e: IOException) {
            Log.e("downloadFile", "Error downloading file '$remoteFilePath' from FTP server: ${e.message}")
            if (localFile.exists()) {
                localFile.delete()
            }
            return false
        }
    }

    /**
     * Renames a file or directory on the FTP server.
     *
     * @param fromPath The current full path of the file/directory on the server (e.g. /freespace/userID/fileID.rmt.tmp)
     * @param newName The new file name on the server (e.g. /freespace/userID/fileID.rmt)
     * @return True if renaming was successful, false otherwise.
     * @throws IOException If an FTP protocol error occurs.
     * @throws IllegalStateException If not connected to the FTP server.
     */
    fun renameRemoteFile(fromPath: String, newName: String): Boolean {
        if (!isConnected()) {
            throw IllegalStateException("Not connected to FTP server. Please login first.")
        }
        if (fromPath.isBlank() || newName.isBlank()) {
            System.err.println("Source or destination path for rename cannot be blank.")
            return false
        }

        try {
            if (changeToWorkingDirectory(extractPathFromFullPath(fromPath))) {
                val fromName = extractFileNameFromFullPath(fromPath)
                return ftpClient.rename(fromName, newName)
            }
            else {
                return false
            }
        } catch (e: IOException) {
            System.err.println("Error renaming remote item from '$fromPath' to '$newName': ${e.message}")
            throw e // Or return false and log
        }
    }

    /**
     * Close FTP client if currently connected
     */
    suspend fun close() {
        if (currentServer.isNotEmpty()) {
            logout()
        }
    }

    //end Public API methods

    //begin Private methods

    /**
     * Change to a specified remote directory path on the FTP server if it doesn't already exist.
     * This method will attempt to create each directory in the path if it doesn't exist.
     *
     * @param dirPath The directory path to create (e.g., "uploads/data/archive").
     * @return True if the working directory was changed or created successfully, false otherwise.
     * @throws IOException If an FTP protocol error occurs.
     * NOTE: On successful return, the specified directory is selected as the current working directory
     */
    private fun changeToWorkingDirectory(dirPath: String): Boolean {
        if (dirPath.isBlank()) {  // No directory to create
            return true
        }

        var originalWorkingDirectory = "/"
        try {
            originalWorkingDirectory = ftpClient.printWorkingDirectory() // Save current directory path from CWD command
        } catch (e: IOException) {
            Log.e("changeToWorkingDirectory", "Error getting current working directory: ${e.message}")
        }

        //If already in desired directory, return true
        if (originalWorkingDirectory == "/$dirPath") {
            return true
        }

        var currentPathSb = StringBuilder()
        try {
            val directories = dirPath.split("/").filter { it.isNotEmpty() }
            var pathChanged = false

            for (dirComponent in directories) {
                currentPathSb.append("/$dirComponent")
                val currentPath = currentPathSb.toString()

                // Check if directory exists by trying to change to it
                if (ftpClient.changeWorkingDirectory(currentPath)) {
                    pathChanged = true
                    continue // Directory exists
                }

                // Directory does not exist or cannot be changed to. Try to create it
                if (ftpClient.makeDirectory(currentPath)) {
                    Log.d("changeToWorkingDirectory", "Created remote directory: $currentPath")
                    //Try to change into it again to verify
                    if (!ftpClient.changeWorkingDirectory(currentPath)) {
                        Log.e("changeToWorkingDirectory", "Failed to change into newly created directory: $currentPath")
                        ftpClient.changeWorkingDirectory(originalWorkingDirectory)
                        return false
                    }
                    pathChanged = true
                } else {
                    // Check if it failed because it already exists (some servers might behave this way)
                    // A more robust check would be to list files and see if it's there as a dir.
                    // For now, assume makeDirectory failure means it couldn't be created.
                    val replyCode = ftpClient.replyCode
                    val replyString = ftpClient.replyString
                    Log.w("changeToWorkingDirectory", "Could not create remote directory component: $currentPath. Reply: $replyCode - $replyString")

                    // Attempt to list the parent to see if currentPath exists (more robust check)
                    val parent = currentPath.substringBeforeLast('/', "")
                    val componentName = currentPath.substringAfterLast('/')
                    val files: Array<FTPFile>? = if (parent.isNotEmpty()) ftpClient.listFiles(parent) else ftpClient.listFiles()
                    val dirExistsAsFile = files?.any { it.isDirectory && it.name == componentName } == true
                    if (!dirExistsAsFile) {
                        if (pathChanged) {
                            ftpClient.changeWorkingDirectory(originalWorkingDirectory)
                        }
                        return false
                    } else {
                        // It seems it already existed, try changing into it again
                        if (!ftpClient.changeWorkingDirectory(currentPath)) {
                            if (pathChanged) {
                                return ftpClient.changeWorkingDirectory(originalWorkingDirectory)
                            }

                        }
                        pathChanged = true
                    }
                }
            }

            // Successfully created/navigated the full path
            return true
        } catch (e: IOException) {
            Log.e("changeToWorkingDirectory", "IOException during changeToWorkingDirectory for '$dirPath': ${e.message}")
        }
        return false
    }


    /**
     * Checks if currently connected to the FTP server.
     */
    private fun isConnected(): Boolean {
        try {
            return ftpClient.isConnected
        } catch (e: IOException) {
            Log.e("isConnected", "Error checking connection status: ${e.message}")
            return false
        }
    }

    private fun setBinaryFileType() {
        try {
            if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
                System.err.println("Could not set file type to binary. Upload may fail or be corrupted.")
                // Decide if you want to proceed or return false
            }
        } catch (e: IOException) {
            Log.e("SetBinaryFileType", "Error setting file type to binary: ${e.message}")
        }
    }

    private fun extractPathFromFullPath(fullPath: String) : String {
        return fullPath.substringBeforeLast('/')
    }

    private fun extractFileNameFromFullPath(fullPath: String) : String {
        return fullPath.substringAfterLast('/')
    }

    //end Private methods
}