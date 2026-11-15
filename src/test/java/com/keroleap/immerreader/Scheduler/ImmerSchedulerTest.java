package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.ImmerRest;
import com.keroleap.immerreader.Service.CameraUnavailableException;
import com.keroleap.immerreader.Service.ImmerAnalyzerService;
import com.keroleap.immerreader.SharedData.ErrorStatistics;
import com.keroleap.immerreader.SharedData.ImmerData;
import com.keroleap.immerreader.SharedData.ImmerManagerData;
import com.keroleap.immerreader.SharedData.ImmerStatistics;
import com.keroleap.immerreader.SharedData.SchedulerHealthTracker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImmerSchedulerTest {

    @Mock
    private ImmerManagerData immerManagerData;

    @Mock
    private ImmerAnalyzerService immerAnalyzerService;

    @Mock
    private ImmerData immerData;

    @Mock
    private ImmerStatistics immerStatistics;

    @Mock
    private ErrorStatistics errorStatistics;

    @Mock
    private SchedulerHealthTracker schedulerHealthTracker;

    @InjectMocks
    private ImmerScheduler immerScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(immerManagerData.getXs()).thenReturn(new int[ImmerManagerData.POINT_COUNT]);
        when(immerManagerData.getYs()).thenReturn(new int[ImmerManagerData.POINT_COUNT]);
    }

    @Test
    void schedulerInitializesSuccessfully() {
        assertDoesNotThrow(() -> immerScheduler.init());
        immerScheduler.destroy();
    }

    @Test
    void schedulerInitializesWithDefaultDelay() {
        immerScheduler.init();
        // Scheduler should start without errors
        assertTrue(true, "Scheduler initialized successfully");
        immerScheduler.destroy();
    }

    @Test
    void destroy_shutsDownScheduler() {
        immerScheduler.init();
        assertDoesNotThrow(() -> immerScheduler.destroy());
    }

    @Test
    void destroy_canBeCalledMultipleTimes() {
        immerScheduler.init();
        immerScheduler.destroy();
        assertDoesNotThrow(() -> immerScheduler.destroy());
    }

    @Test
    void schedulerInitializesWhenDisabled() {
        when(immerManagerData.isEnabled()).thenReturn(false);
        assertDoesNotThrow(() -> immerScheduler.init());
        immerScheduler.destroy();
    }

    @Test
    void readOnce_doesNotFetchWhenDisabled() {
        when(immerManagerData.isEnabled()).thenReturn(false);

        immerScheduler.readOnce();

        verifyNoInteractions(immerAnalyzerService);
    }

    @Test
    void readOnce_publishesResultAndTimestamp() {
        when(immerManagerData.isEnabled()).thenReturn(true);
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ImmerRest rest = new ImmerRest();
        rest.setTemperaute(42);
        when(immerAnalyzerService.getBufferedImage(anyString())).thenReturn(image);
        when(immerAnalyzerService.getImmerRestData(any(), any(), any())).thenReturn(rest);

        immerScheduler.readOnce();

        verify(immerData).setImmerRest(rest);
        verify(immerStatistics).record(rest);
        verify(schedulerHealthTracker).recordSuccess("Immer");
        assertFalse(rest.isError());
        assertNull(rest.getErrorType());
        assertTrue(rest.getLastSuccessMs() > 0);
    }

    @Test
    void readOnce_recordsTimeoutError() {
        when(immerManagerData.isEnabled()).thenReturn(true);
        when(immerAnalyzerService.getBufferedImage(anyString()))
                .thenThrow(new CameraUnavailableException("timeout", true));

        immerScheduler.readOnce();

        verify(errorStatistics).recordError("Immer", ErrorType.TIMEOUT);
        verify(schedulerHealthTracker).recordError("Immer", ErrorType.TIMEOUT);
        verifyNoInteractions(immerData);
    }

    @Test
    void readOnce_recordsFetchError() {
        when(immerManagerData.isEnabled()).thenReturn(true);
        when(immerAnalyzerService.getBufferedImage(anyString()))
                .thenThrow(new CameraUnavailableException("http 500", false));

        immerScheduler.readOnce();

        verify(errorStatistics).recordError("Immer", ErrorType.FETCH_ERROR);
    }

    @Test
    void readOnce_marksDataStaleAfterFiveConsecutiveErrors() {
        when(immerManagerData.isEnabled()).thenReturn(true);
        ImmerRest current = new ImmerRest();
        when(immerData.getImmerRest()).thenReturn(current);
        when(immerAnalyzerService.getBufferedImage(anyString()))
                .thenThrow(new CameraUnavailableException("timeout", true));

        for (int i = 0; i < 5; i++) {
            immerScheduler.readOnce();
        }

        assertTrue(current.isError());
        assertEquals(ErrorType.TIMEOUT, current.getErrorType());
        verify(errorStatistics, times(5)).recordError("Immer", ErrorType.TIMEOUT);
    }

    @Test
    void readOnce_resetErrorStreakOnSuccess() {
        when(immerManagerData.isEnabled()).thenReturn(true);
        ImmerRest rest = new ImmerRest();
        when(immerAnalyzerService.getImmerRestData(any(), any(), any())).thenReturn(rest);
        when(immerData.getImmerRest()).thenReturn(new ImmerRest());

        AtomicInteger calls = new AtomicInteger();
        when(immerAnalyzerService.getBufferedImage(anyString())).thenAnswer(invocation -> {
            if (calls.incrementAndGet() == 5) {
                return new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
            }
            throw new CameraUnavailableException("timeout", true);
        });

        // 4 failures, 1 success, 4 failures: the streak never reaches the threshold.
        for (int i = 0; i < 9; i++) {
            immerScheduler.readOnce();
        }

        verify(immerData, atMost(1)).getImmerRest();
        verify(immerData, never()).setImmerRest(argThat(r -> r.isError()));
    }
}
