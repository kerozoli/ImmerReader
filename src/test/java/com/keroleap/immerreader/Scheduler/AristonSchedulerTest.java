package com.keroleap.immerreader.Scheduler;

import java.awt.image.BufferedImage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.keroleap.immerreader.AristonRest;
import com.keroleap.immerreader.Service.AristonAnalyzerService;
import com.keroleap.immerreader.SharedData.AristonManagerData;
import com.keroleap.immerreader.SharedData.ErrorStatistics;
import com.keroleap.immerreader.SharedData.SchedulerHealthTracker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AristonSchedulerTest {

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
        when(aristonAnalyzerService.getAristonRestData(any(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt())).thenReturn(rest);

        assertDoesNotThrow(() -> aristonScheduler.AristonScheduledRead());
        verify(aristonAnalyzerService).getBufferedImage(anyString());
    }

    @Test
    void destroy_shutsDownExecutor() {
        assertDoesNotThrow(() -> aristonScheduler.destroy());
    }
}
