package com.keroleap.immerreader.Controller;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ImmerControllerTest {

    private final ImmerController controller = new ImmerController();

    @Test
    public void testGetNumber() {
        assertEquals(7, controller.getNumber(true, true, true, false, false, false, false));
    }

}
