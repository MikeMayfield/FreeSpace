package com.tmf.freespace.ftpclient

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPFile // Needed for checking directory existence
import org.apache.commons.net.ftp.FTPReply
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream

class FtpManager {

    private val ftpClient = FTPClient()
    @Volatile
    private var isConnected = false


    //begin Public API methods

    /**
     * Logs into the remote FTP server.
     *
     * @param server The hostname or IP address of the FTP server.
     * @param port The port number for the FTP connection (default is 21).
     * @param username The username for authentication.
     * @param password The password for authentication.
     * @return True if login was successful, false otherwise.
     * @throws IOException If an error occurs during connection or login.
     */
    suspend fun login(server: String, port: Int = 21, username: String, password: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                if (isConnected && ftpClient.isConnected) {
                    return@withContext true
                }

                ftpClient.connect(server, port)
                val replyCode = ftpClient.replyCode
                if (!FTPReply.isPositiveCompletion(replyCode)) {
                    ftpClient.disconnect()
                    System.err.println("FTP server refused connection. Reply code: $replyCode")
                    return@withContext false
                }

                if (ftpClient.login(username, password)) {
                    ftpClient.enterLocalPassiveMode()
                    ftpClient.setFileType(FTP.BINARY_FILE_TYPE) // Set here
                    isConnected = true
                    true
                } else {
                    System.err.println("Login to FTP server failed.")
                    if (ftpClient.isConnected) {
                        try {
                            ftpClient.logout()
                            ftpClient.disconnect()
                        } catch (e: IOException) {
                            System.err.println("Error during cleanup after failed login: ${e.message}")
                        }
                    }
                    isConnected = false
                    false
                }
            } catch (e: IOException) {
                System.err.println("Error connecting or logging in to FTP server: ${e.message}")
                if (ftpClient.isConnected) {
                    try {
                        ftpClient.disconnect()
                    } catch (ioe: IOException) {
                        // Log this or ignore
                    }
                }
                isConnected = false
                throw e
            }
        }
    }

    /**
     * Logs out of the remote FTP server and closes the connection.
     *
     * @return True if logout and disconnection were successful, false otherwise.
     */
    suspend fun logout(): Boolean {
        // ... implementation from previous response ...
        return withContext(Dispatchers.IO) {
            if (!isConnected && !ftpClient.isConnected) {
                return@withContext true
            }
            try {
                var loggedOut = true
                if (ftpClient.isConnected) {
                    loggedOut = ftpClient.logout()
                    ftpClient.disconnect()
                }
                isConnected = false
                loggedOut
            } catch (e: IOException) {
                System.err.println("Error logging out or disconnecting from FTP server: ${e.message}")
                isConnected = false
                false
            }
        }
    }

    /**
     * Copies a local file to the FTP server, ensuring BINARY_FILE_TYPE,
     * and creating the remote directory path if it doesn't exist.
     *
     * @param localFile The local file to upload.
     * @param remotePath The full remote path including directory and filename (e.g., "uploads/data/myfile.dat").
     * @return True if the file was successfully uploaded, false otherwise.
     * @throws IOException If an error occurs during file transfer or directory creation.
     * @throws IllegalStateException If not connected to the FTP server.
     */
    suspend fun uploadFile(localFile: File, remotePath: String): Boolean {
        if (!isConnected()) {
            throw IllegalStateException("Not connected to FTP server. Please login first.")
        }
        if (!localFile.exists() || !localFile.isFile) {
            System.err.println("Local file does not exist or is not a regular file: ${localFile.absolutePath}")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                // Ensure binary file type for this transfer
                if (!ftpClient.setFileType(FTP.BINARY_FILE_TYPE)) {
                    System.err.println("Could not set file type to binary. Upload may fail or be corrupted.")
                    // Decide if you want to proceed or return false
                }

                val remoteDir: String
                val remoteFileName: String

                if (remotePath.contains("/")) {
                    remoteDir = remotePath.substringBeforeLast("/", missingDelimiterValue = "")
                    remoteFileName = remotePath.substringAfterLast("/")
                    if (remoteDir.isNotEmpty()) {
                        if (!makeRemoteDirectory(remoteDir)) {
                            System.err.println("Could not create or verify remote directory: $remoteDir")
                            return@withContext false
                        }
                    }
                } else {
                    // No directory path, uploading to current working directory
                    remoteDir = "" // Or ftpClient.printWorkingDirectory() if you need to be explicit
                    remoteFileName = remotePath
                }

                // After ensuring directory exists (if any), attempt to store the file.
                // The path for storeFile should be relative to the current working directory
                // or an absolute path if the server supports it and makeRemoteDirectory
                // correctly navigated/created it.
                // If makeRemoteDirectory changed the CWD, remoteFileName is fine.
                // If makeRemoteDirectory used absolute paths and didn't change CWD,
                // then remotePath itself should be used.
                // For simplicity, let's assume makeRemoteDirectory will set CWD or handle absolute.
                // A more robust makeRemoteDirectory would return the final path or ensure CWD.

                val finalRemotePath = if (remoteDir.isNotEmpty()) "$remoteDir/$remoteFileName" else remoteFileName

                FileInputStream(localFile).use { inputStream ->
                    ftpClient.storeFile(finalRemotePath, inputStream)
                }
            } catch (e: IOException) {
                System.err.println("Error uploading file to FTP server at '$remotePath': ${e.message}")
                throw e
            }
        }
    }

    /**
     * Downloads a file from the FTP server to a local file.
     * This is a suspend function and should be called from a coroutine.
     *
     * @param remoteFileName The name of the file on the FTP server (can include path).
     * @param localFile The local File object where the downloaded content will be saved.
     *                  The parent directory of this file must exist.
     * @return True if the file was successfully downloaded, false otherwise.
     * @throws IOException If an error occurs during file transfer or local file operations.
     * @throws IllegalStateException If not connected to the FTP server.
     */
    suspend fun downloadFile(remoteFileName: String, localFile: File): Boolean {
        // ... implementation from previous response ...
        if (!isConnected()) {
            throw IllegalStateException("Not connected to FTP server. Please login first.")
        }
        localFile.parentFile?.mkdirs()
        return withContext(Dispatchers.IO) {
            try {
                FileOutputStream(localFile).use { outputStream: OutputStream ->
                    ftpClient.retrieveFile(remoteFileName, outputStream)
                }
            } catch (e: IOException) {
                System.err.println("Error downloading file '$remoteFileName' from FTP server: ${e.message}")
                if (localFile.exists()) {
                    localFile.delete()
                }
                throw e
            }
        }
    }

    /**
     * Renames a file or directory on the FTP server.
     *
     * @param fromPath The current full path of the file/directory on the server.
     * @param toPath The new full path for the file/directory on the server.
     * @return True if renaming was successful, false otherwise.
     * @throws IOException If an FTP protocol error occurs.
     * @throws IllegalStateException If not connected to the FTP server.
     */
    suspend fun renameRemoteFile(fromPath: String, toPath: String): Boolean {
        if (!isConnected()) {
            throw IllegalStateException("Not connected to FTP server. Please login first.")
        }
        if (fromPath.isBlank() || toPath.isBlank()) {
            System.err.println("Source or destination path for rename cannot be blank.")
            return false
        }

        return withContext(Dispatchers.IO) {
            try {
                ftpClient.rename(fromPath, toPath)
            } catch (e: IOException) {
                System.err.println("Error renaming remote item from '$fromPath' to '$toPath': ${e.message}")
                throw e // Or return false and log
            }
        }
    }

    //end Public API methods

    //begin Private methods

    /**
     * Creates a remote directory path on the FTP server if it doesn't already exist.
     * This method will attempt to create each directory in the path.
     *
     * @param dirPath The directory path to create (e.g., "uploads/data/archive").
     * @return True if the directory exists or was successfully created, false otherwise.
     * @throws IOException If an FTP protocol error occurs.
     */
    private suspend fun makeRemoteDirectory(dirPath: String): Boolean {
        return withContext(Dispatchers.IO) {
            if (dirPath.isBlank()) return@withContext true // No directory to create

            val originalWorkingDirectory = ftpClient.printWorkingDirectory() // Save current CWD
            var currentPath = ""

            try {
                val directories = dirPath.split("/").filter { it.isNotEmpty() }
                var pathChanged = false

                for (dirComponent in directories) {
                    currentPath = if (currentPath.isEmpty()) dirComponent else "$currentPath/$dirComponent"
                    // Check if directory exists by trying to change to it
                    if (ftpClient.changeWorkingDirectory(currentPath)) {
                        pathChanged = true
                        continue // Directory exists
                    }

                    // Directory does not exist or cannot be changed to, try to create it
                    if (ftpClient.makeDirectory(currentPath)) {
                        println("Created remote directory: $currentPath")
                        // Optionally, try to change into it again to verify
                        if (!ftpClient.changeWorkingDirectory(currentPath)) {
                            System.err.println("Failed to change into newly created directory: $currentPath")
                            // Revert CWD before returning false
                            if (originalWorkingDirectory != null) ftpClient.changeWorkingDirectory(originalWorkingDirectory)
                            return@withContext false
                        }
                        pathChanged = true
                    } else {
                        // Check if it failed because it already exists (some servers might behave this way)
                        // A more robust check would be to list files and see if it's there as a dir.
                        // For now, assume makeDirectory failure means it couldn't be created.
                        val replyCode = ftpClient.replyCode
                        val replyString = ftpClient.replyString
                        System.err.println("Could not create remote directory component: $currentPath. Reply: $replyCode - $replyString")

                        // Attempt to list the parent to see if currentPath exists (more robust check)
                        val parent = currentPath.substringBeforeLast('/', "")
                        val componentName = currentPath.substringAfterLast('/')
                        val files: Array<FTPFile>? = if (parent.isNotEmpty()) ftpClient.listFiles(parent) else ftpClient.listFiles()
                        val dirExistsAsFile = files?.any { it.isDirectory && it.name == componentName } ?: false

                        if (!dirExistsAsFile) {
                            if (originalWorkingDirectory != null && pathChanged) ftpClient.changeWorkingDirectory(originalWorkingDirectory)
                            return@withContext false
                        } else {
                            // It seems it already existed, try changing into it again
                            if (!ftpClient.changeWorkingDirectory(currentPath)) {
                                if (originalWorkingDirectory != null && pathChanged) ftpClient.changeWorkingDirectory(originalWorkingDirectory)
                                return@withContext false
                            }
                            pathChanged = true
                        }
                    }
                }
                // Successfully created/navigated the full path
                true
            } catch (e: IOException) {
                System.err.println("IOException during makeRemoteDirectory for '$dirPath': ${e.message}")
                false
            } finally {
                // Restore original working directory if it was changed and we didn't intend to stay there
                // This makes makeRemoteDirectory a "safer" utility that doesn't permanently change CWD
                // unless the caller wants to. For upload, we might want to stay in the target dir.
                // For simplicity here, we restore. The uploadFile method will then use the full path.
                if (originalWorkingDirectory != null) {
                    ftpClient.changeWorkingDirectory(originalWorkingDirectory)
                }
            }
        }
    }


    /**
     * Checks if currently connected to the FTP server.
     */
    private suspend fun isConnected(): Boolean {
        return withContext(Dispatchers.IO) {
            isConnected && ftpClient.isConnected
        }
    }

    //end Private methods
}