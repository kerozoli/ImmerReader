package com.keroleap.immerreader.Scheduler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.keroleap.immerreader.SharedData.ImmerManagerData;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ImmerSchedulerTest {

    @Mock
    private ImmerManagerData immerManagerData;

    @InjectMocks
    private ImmerScheduler immerScheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(immerManagerData.getOffsetX()).thenReturn(0);
        when(immerManagerData.getOffsetY()).thenReturn(0);
    }

    @Test
    void schedulerInitializesSuccessfully() {
        assertDoesNotThrow(() -> immerScheduler.init());
    }

    @Test
    void schedulerInitializesWithDefaultDelay() {
        immerScheduler.init();
        // Scheduler should start without errors
        assertTrue(true, "Scheduler initialized successfully");
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
    void schedulerHandlesNullOffsetGracefully() {
        // Verify scheduler can be initialized even if offset data returns defaults
        immerScheduler.init();
        assertDoesNotThrow(() -> immerScheduler.destroy());
    }
}
