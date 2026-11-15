package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.ErrorType;
import com.keroleap.immerreader.Service.AristonAnalyzerService;
import com.keroleap.immerreader.Service.CameraUnavailableException;
import com.keroleap.immerreader.SharedData.AristonData;
import com.keroleap.immerreader.SharedData.AristonManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;
import com.keroleap.immerreader.SharedData.SchedulerHealthTracker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AristonSchedulerTest {

    @Mock
    private AristonData aristonData;

    @Mock
    private AristonManagerData aristonManagerData;

    @Mock
    private AristonAnalyzerService aristonAnalyzerService;

    @Mock
    private ErrorStatistics errorStatistics;

    @Mock
    private SchedulerHealthTracker schedulerHealthTracker;

    @InjectMocks
    private AristonScheduler aristonScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(aristonManagerData.getStartX()).thenReturn(0);
        when(aristonManagerData.getStartY()).thenReturn(0);
        when(aristonManagerData.getControlX()).thenReturn(50);
        when(aristonManagerData.getControlY()).thenReturn(50);
        when(aristonManagerData.getEndX()).thenReturn(100);
        when(aristonManagerData.getEndY()).thenReturn(0);
    }

    @Test
    void schedulerDoesNotFetchWhenDisabled() {
        when(aristonManagerData.isEnabled()).thenReturn(false);
        assertDoesNotThrow(() -> aristonScheduler.AristonScheduledRead());
        verifyNoInteractions(aristonAnalyzerService);
    }

    @Test
    void schedulerHandlesEnabledState() throws Exception {
        when(aristonManagerData.isEnabled()).thenReturn(true);
        BufferedImage mockImage = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        when(aristonAnalyzerService.getBufferedImage(anyString())).thenReturn(mockImage);
        AristonRest rest = new AristonRest();
        rest.setPercentage(50);
        when(aristonAnalyzerService.getAristonRestData(any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(rest);

        assertDoesNotThrow(() -> aristonScheduler.AristonScheduledRead());
        verify(aristonAnalyzerService).getBufferedImage(anyString());
    }

    @Test
    void scheduledRead_publishesResultAndTimestamp() throws Exception {
        when(aristonManagerData.isEnabled()).thenReturn(true);
        when(aristonAnalyzerService.getBufferedImage(anyString()))
                .thenReturn(new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
        AristonRest rest = new AristonRest();
        rest.setPercentage(40);
        when(aristonAnalyzerService.getAristonRestData(any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt()))
                .thenReturn(rest);

        aristonScheduler.AristonScheduledRead();

        verify(aristonData).setAristonRest(rest);
        verify(schedulerHealthTracker).recordSuccess("Ariston");
        assertFalse(rest.isError());
        assertTrue(rest.getLastSuccessMs() > 0);
    }

    @Test
    void scheduledRead_recordsTimeoutError() {
        when(aristonManagerData.isEnabled()).thenReturn(true);
        when(aristonAnalyzerService.getBufferedImage(anyString()))
                .thenThrow(new CameraUnavailableException("timeout", true));

        aristonScheduler.AristonScheduledRead();

        verify(errorStatistics).recordError("Ariston", ErrorType.TIMEOUT);
        verify(schedulerHealthTracker).recordError("Ariston", ErrorType.TIMEOUT);
        verify(aristonData, never()).setAristonRest(any());
    }

    @Test
    void scheduledRead_recordsFetchError() {
        when(aristonManagerData.isEnabled()).thenReturn(true);
        when(aristonAnalyzerService.getBufferedImage(anyString()))
                .thenThrow(new CameraUnavailableException("http 500", false));

        aristonScheduler.AristonScheduledRead();

        verify(errorStatistics).recordError("Ariston", ErrorType.FETCH_ERROR);
    }

    @Test
    void scheduledRead_marksDataStaleAfterFiveConsecutiveErrors() {
        when(aristonManagerData.isEnabled()).thenReturn(true);
        AristonRest current = new AristonRest();
        when(aristonData.getAristonRest()).thenReturn(current);
        when(aristonAnalyzerService.getBufferedImage(anyString()))
                .thenThrow(new CameraUnavailableException("timeout", true));

        for (int i = 0; i < 5; i++) {
            aristonScheduler.AristonScheduledRead();
        }

        assertTrue(current.isError());
        assertEquals(ErrorType.TIMEOUT, current.getErrorType());
    }

    @Test
    void destroy_shutsDownExecutor() {
        assertDoesNotThrow(() -> aristonScheduler.destroy());
    }
}
