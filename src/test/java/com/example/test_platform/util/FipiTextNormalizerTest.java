package com.example.test_platform.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FipiTextNormalizerTest {

    @Test
    void testNormalizeVectorsAndFractions() {
        String input1 = "Даны векторы →𝑎 (25; 0) и →𝑏 (1; −5). Найдите длину вектора →𝑎 −4 →𝑏 .";
        String normalized1 = FipiTextNormalizer.normalize(input1);

        assertTrue(normalized1.contains("$\\vec{a}$"));
        assertTrue(normalized1.contains("$\\vec{b}$"));

        String input2 = "Найдите корень уравнения (1 7)x+4 = 49.";
        String normalized2 = FipiTextNormalizer.normalize(input2);

        assertTrue(normalized2.contains("\\left(\\frac{1}{7}\\right)^{x+4}"));
    }

    @Test
    void testFormatForPdf() {
        String input = "Даны векторы →𝑎 и →𝑏 .";
        String pdfFormatted = FipiTextNormalizer.formatForPdf(input);

        assertFalse(pdfFormatted.contains("→"));
        assertTrue(pdfFormatted.contains("вектор a"));
    }
}
