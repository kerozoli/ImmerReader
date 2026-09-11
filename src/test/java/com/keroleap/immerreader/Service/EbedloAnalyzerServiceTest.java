package com.keroleap.immerreader.Service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.junit.jupiter.api.Test;

import com.keroleap.immerreader.PlateRest;
import com.keroleap.immerreader.SharedData.EbedloManagerData;
import com.keroleap.immerreader.PlateRest.PlateCenter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EbedloAnalyzerServiceTest {

    private final EbedloAnalyzerService service = new EbedloAnalyzerService();

    @Test
    void detectPlates_findsSingleDarkPlateOnBrightBackground() {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.LIGHT_GRAY);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.DARK_GRAY);
        g.fillOval(70, 70, 60, 60);
        g.dispose();

        EbedloManagerData data = createPlateManagerData(
                new int[] { 0, 200, 200, 0 },
                new int[] { 0, 0, 200, 200 },
                30, 10, 80);

        PlateRest result = service.detectPlates(image, data);

        assertNotNull(result);
        assertEquals(1, result.getDetectedPlateCount());
        PlateCenter plate = result.getPlateCenters().get(0);
        assertTrue(plate.getX() >= 90 && plate.getX() <= 110, "Plate center X should be near 100, was " + plate.getX());
        assertTrue(plate.getY() >= 90 && plate.getY() <= 110, "Plate center Y should be near 100, was " + plate.getY());
        assertTrue(plate.getRadius() >= 20 && plate.getRadius() <= 40, "Plate radius should be near 30, was " + plate.getRadius());
    }

    @Test
    void detectPlates_returnsEmptyWhenPlateAreaNotConfigured() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        EbedloManagerData data = createPlateManagerData(
                new int[] { 0, 0, 0, 0 },
                new int[] { 0, 0, 0, 0 },
                15, 10, 50);

        PlateRest result = service.detectPlates(image, data);

        assertNotNull(result);
        assertEquals(0, result.getDetectedPlateCount());
        assertTrue(result.getPlateCenters().isEmpty());
    }

    @Test
    void detectPlates_returnsEmptyWhenImageIsNull() {
        EbedloManagerData data = createPlateManagerData(
                new int[] { 0, 100, 100, 0 },
                new int[] { 0, 0, 100, 100 },
                15, 10, 50);

        PlateRest result = service.detectPlates(null, data);

        assertNotNull(result);
        assertEquals(0, result.getDetectedPlateCount());
    }

    private EbedloManagerData createPlateManagerData(int[] plateXs, int[] plateYs,
            int threshold, int minRadius, int maxRadius) {
        EbedloManagerData data = new EbedloManagerData();
        data.setPlatePoints(plateXs, plateYs);
        data.setPlateThreshold(threshold);
        data.setPlateMinRadius(minRadius);
        data.setPlateMaxRadius(maxRadius);
        return data;
    }
}
