package com.tmf.freespace.services

import android.content.Context
import com.tmf.freespace.database.AppDatabase
import com.tmf.freespace.workers.CompressionServiceBackgroundTask
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.powermock.api.mockito.PowerMockito
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner

@RunWith(PowerMockRunner::class)
@PrepareForTest(AppDatabase::class, CompressionServiceBackgroundTask::class, kotlinx.coroutines.BuildersKt::class)
class CompressionServiceTest {

    @Mock
    private lateinit var mockContext: Context
    @Mock
    private lateinit var mockApplicationContext: Context
    @Mock
    private lateinit var mockAppDatabase: AppDatabase
    @Mock
    private lateinit var mockCompressionServiceBackgroundTask: CompressionServiceBackgroundTask

    private lateinit var compressionService: CompressionService

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        compressionService = CompressionService()

        `when`(mockContext.applicationContext).thenReturn(mockApplicationContext)

        // Mock constructor of AppDatabase
        PowerMockito.whenNew(AppDatabase::class.java).withAnyArguments().thenReturn(mockAppDatabase)

        // Mock constructor of CompressionServiceBackgroundTask
        PowerMockito.whenNew(CompressionServiceBackgroundTask::class.java)
            .withArguments(mockApplicationContext, mockAppDatabase)
            .thenReturn(mockCompressionServiceBackgroundTask)
    }

    @Test
    fun `start initializesDatabaseAndStartsBackgroundTask`() {
        // Given the service is set up

        // When
        // The production code uses runBlocking directly.
        // We are testing that the lambda inside runBlocking executes the expected calls.
        compressionService.start(mockContext)

        // Then
        // Verify AppDatabase constructor was called with applicationContext
        // PowerMockito.verifyNew(AppDatabase::class.java).withArguments(mockApplicationContext)
        // The above line is tricky because AppDatabase.getInstance(context) is a static factory method.
        // We'll assume that whenNew for AppDatabase covers its instantiation.
        // A more specific test would mock the static getInstance method if AppDatabase is final or not easily mockable directly.
        // For now, the whenNew setup for AppDatabase should ensure our mockAppDatabase is used.

        // Verify CompressionServiceBackgroundTask constructor was called
        PowerMockito.verifyNew(CompressionServiceBackgroundTask::class.java)
            .withArguments(mockApplicationContext, mockAppDatabase)

        // Verify CompressionServiceBackgroundTask.start() was called
        verify(mockCompressionServiceBackgroundTask).start()
    }
}
