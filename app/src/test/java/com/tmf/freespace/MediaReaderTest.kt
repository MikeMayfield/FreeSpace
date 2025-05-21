package com.tmf.freespace

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.database.MatrixCursor
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import com.tmf.freespace.models.MediaFile
import com.tmf.freespace.models.MediaType
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(MockitoJUnitRunner::class)
class MediaReaderTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    private lateinit var mediaReader: MediaReader
    private lateinit var mockContextCompat: MockedStatic<ContextCompat>

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mediaReader = MediaReader(mockContext)
        Mockito.`when`(mockContext.contentResolver).thenReturn(mockContentResolver)

        // Mock ContextCompat.checkSelfPermission
        // Needs to be mocked statically as it's a static method
        mockContextCompat = Mockito.mockStatic(ContextCompat::class.java)
    }

    @After
    fun tearDown() {
        mockContextCompat.close()
    }

    private fun mockPermission(granted: Boolean) {
        val permissionResult = if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        mockContextCompat.`when`<Int> { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_EXTERNAL_STORAGE) }.thenReturn(permissionResult)
        mockContextCompat.`when`<Int> { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_MEDIA_IMAGES) }.thenReturn(permissionResult)
        mockContextCompat.`when`<Int> { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_MEDIA_VIDEO) }.thenReturn(permissionResult)
        mockContextCompat.`when`<Int> { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_MEDIA_AUDIO) }.thenReturn(permissionResult)
    }

    private fun createCursor(rows: List<Array<out Any?>>): MatrixCursor {
        val columns = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA, // fullPath
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Files.FileColumns.SIZE
        )
        val cursor = MatrixCursor(columns)
        rows.forEach { cursor.addRow(it) }
        return cursor
    }

    @Test
    fun `forNewMediaFiles when permission denied on SDK 32 and below should not query`() {
        // Given
        val originalSdkInt = Build.VERSION.SDK_INT
        setFinalStatic(Build.VERSION::class.java.getField("SDK_INT"), 30) // Simulate SDK 30
        mockPermission(granted = false)
        var callbackCalled = false

        // When
        mediaReader.forNewMediaFiles { callbackCalled = true }

        // Then
        verify(mockContentResolver, times(0)).query(any(), any(), any(), any(), any())
        assert(!callbackCalled)

        // Reset SDK_INT
        setFinalStatic(Build.VERSION::class.java.getField("SDK_INT"), originalSdkInt)
    }

    @Test
    fun `forNewMediaFiles_whenPartialPermissionsGranted_sdk33AndAbove_queriesForGrantedTypes`() {
        // Given
        val originalSdkInt = Build.VERSION.SDK_INT
        setFinalStatic(Build.VERSION::class.java.getField("SDK_INT"), 33) // Simulate SDK 33

        // Grant only image permission
        mockContextCompat.`when`<Int> { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_MEDIA_IMAGES) }.thenReturn(PackageManager.PERMISSION_GRANTED)
        mockContextCompat.`when`<Int> { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_MEDIA_VIDEO) }.thenReturn(PackageManager.PERMISSION_DENIED)
        mockContextCompat.`when`<Int> { ContextCompat.checkSelfPermission(mockContext, Manifest.permission.READ_MEDIA_AUDIO) }.thenReturn(PackageManager.PERMISSION_DENIED)


        val cursorRows = listOf(
            arrayOf(1L, "/path/to/image.jpg", "image/jpeg", System.currentTimeMillis() / 1000, 1024L),
            arrayOf(2L, "/path/to/video.mp4", "video/mp4", System.currentTimeMillis() / 1000, 2048L), // Should be filtered out
            arrayOf(3L, "/path/to/audio.mp3", "audio/mpeg", System.currentTimeMillis() / 1000, 512L)  // Should be filtered out
        )
        val mockCursor = createCursor(cursorRows)
        Mockito.`when`(mockContentResolver.query(any(), any(), any(), any(), any())).thenReturn(mockCursor)
        val processedFiles = mutableListOf<MediaFile>()

        // When
        mediaReader.forNewMediaFiles { processedFiles.add(it) }

        // Then
        verify(mockContentResolver, times(1)).query(any(), any(), any(), any(), any()) // Query should still happen
        assert(processedFiles.size == 1)
        assert(processedFiles[0].mediaType == MediaType.IMAGE)
        assert(processedFiles[0].fullPath == "/path/to/image.jpg")

        // Reset SDK_INT
        setFinalStatic(Build.VERSION::class.java.getField("SDK_INT"), originalSdkInt)
    }
    
    @Test
    fun `forNewMediaFiles when permission granted should query and process media files`() {
        // Given
        mockPermission(granted = true)
        val cursorRows = listOf(
            arrayOf(1L, "/path/to/image.jpg", "image/jpeg", System.currentTimeMillis() / 1000, 1024L),
            arrayOf(2L, "/path/to/video.mp4", "video/mp4", System.currentTimeMillis() / 1000, 2048L),
            arrayOf(3L, "/path/to/audio.mp3", "audio/mpeg", System.currentTimeMillis() / 1000, 512L)
        )
        val mockCursor = createCursor(cursorRows)
        Mockito.`when`(mockContentResolver.query(any(), any(), any(), any(), any())).thenReturn(mockCursor)
        val processedFiles = mutableListOf<MediaFile>()

        // When
        mediaReader.forNewMediaFiles { processedFiles.add(it) }

        // Then
        verify(mockContentResolver, times(1)).query(
            eq(MediaStore.Files.getContentUri("external")),
            any(),
            any(),
            any(),
            eq("${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC")
        )
        assert(processedFiles.size == 3)
        assert(processedFiles[0].mediaType == MediaType.IMAGE)
        assert(processedFiles[1].mediaType == MediaType.VIDEO)
        assert(processedFiles[2].mediaType == MediaType.AUDIO)
    }

    @Test
    fun `forNewMediaFiles should handle null fullPath`() {
        // Given
        mockPermission(granted = true)
        val cursorRows = listOf(
            arrayOf(1L, null, "image/jpeg", System.currentTimeMillis() / 1000, 1024L), // null fullPath
            arrayOf(2L, "/path/to/video.mp4", "video/mp4", System.currentTimeMillis() / 1000, 2048L)
        )
        val mockCursor = createCursor(cursorRows)
        Mockito.`when`(mockContentResolver.query(any(), any(), any(), any(), any())).thenReturn(mockCursor)
        val processedFiles = mutableListOf<MediaFile>()

        // When
        mediaReader.forNewMediaFiles { processedFiles.add(it) }

        // Then
        assert(processedFiles.size == 1) // Only the valid file should be processed
        assert(processedFiles[0].fullPath == "/path/to/video.mp4")
    }

    @Test
    fun `forNewMediaFiles should handle null mimeType`() {
        // Given
        mockPermission(granted = true)
        val cursorRows = listOf(
            arrayOf(1L, "/path/to/image.jpg", null, System.currentTimeMillis() / 1000, 1024L), // null mimeType
            arrayOf(2L, "/path/to/video.mp4", "video/mp4", System.currentTimeMillis() / 1000, 2048L)
        )
        val mockCursor = createCursor(cursorRows)
        Mockito.`when`(mockContentResolver.query(any(), any(), any(), any(), any())).thenReturn(mockCursor)
        val processedFiles = mutableListOf<MediaFile>()

        // When
        mediaReader.forNewMediaFiles { processedFiles.add(it) }

        // Then
        assert(processedFiles.size == 1) // Only the valid file should be processed
//        assert(processedFiles[0].mimeType == "video/mp4")
    }

    @Test
    fun `forNewMediaFiles when query returns null cursor should not invoke callback`() {
        // Given
        mockPermission(granted = true)
        Mockito.`when`(mockContentResolver.query(any(), any(), any(), any(), any())).thenReturn(null)
        var callbackCalled = false

        // When
        mediaReader.forNewMediaFiles { callbackCalled = true }

        // Then
        verify(mockContentResolver, times(1)).query(any(), any(), any(), any(), any())
        assert(!callbackCalled)
    }
    
    @Test
    fun `forNewMediaFiles should handle unsupported mimeType`() {
        // Given
        mockPermission(granted = true)
        val cursorRows = listOf(
            arrayOf(1L, "/path/to/document.pdf", "application/pdf", System.currentTimeMillis() / 1000, 1024L), // unsupported mimeType
            arrayOf(2L, "/path/to/video.mp4", "video/mp4", System.currentTimeMillis() / 1000, 2048L)
        )
        val mockCursor = createCursor(cursorRows)
        Mockito.`when`(mockContentResolver.query(any(), any(), any(), any(), any())).thenReturn(mockCursor)
        val processedFiles = mutableListOf<MediaFile>()

        // When
        mediaReader.forNewMediaFiles { processedFiles.add(it) }

        // Then
        assert(processedFiles.size == 1) // Only the video file should be processed
        assert(processedFiles[0].mediaType == MediaType.VIDEO)
    }

    @Test
    fun `forNewMediaFiles should continue processing if one row causes an error`() {
        // Given
        mockPermission(granted = true)
        val cursorRows = listOf(
            arrayOf(1L, "/path/to/image.jpg", "image/jpeg", System.currentTimeMillis() / 1000, 1024L),
            arrayOf(2L, "/path/to/video.mp4", "video/mp4", "not_a_long", 2048L), // Invalid data (dateModified not a Long)
            arrayOf(3L, "/path/to/audio.mp3", "audio/mpeg", System.currentTimeMillis() / 1000, 512L)
        )
        val mockCursor = createCursor(cursorRows)

        // Simulate an error when trying to get Long for DATE_MODIFIED on the second row
        Mockito.`when`(mockContentResolver.query(any(), any(), any(), any(), any())).thenReturn(mockCursor)
        
        val processedFiles = mutableListOf<MediaFile>()

        // When
        mediaReader.forNewMediaFiles { processedFiles.add(it) }

        // Then
        // Verify that 2 files were processed (the first and third) despite the error in the second
        assert(processedFiles.size == 2)
        assert(processedFiles[0].fullPath == "/path/to/image.jpg")
        assert(processedFiles[1].fullPath == "/path/to/audio.mp3")
    }


    // Helper to set final static fields (like Build.VERSION.SDK_INT)
    private fun setFinalStatic(field: java.lang.reflect.Field, newValue: Any?) {
        field.isAccessible = true
        val modifiersField = java.lang.reflect.Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and java.lang.reflect.Modifier.FINAL.inv())
        field.set(null, newValue)
    }
}
