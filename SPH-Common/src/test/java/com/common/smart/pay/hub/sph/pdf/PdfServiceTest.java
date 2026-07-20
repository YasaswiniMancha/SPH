package com.common.smart.pay.hub.sph.pdf;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PdfServiceTest {

    @Test
    void generatePdfReturnsBytes() {
        TemplateEngine templateEngine = Mockito.mock(TemplateEngine.class);
        Mockito.when(templateEngine.process(Mockito.eq("template"), Mockito.any(Context.class)))
                .thenReturn("<html><body><p>Hello</p></body></html>");

        PdfService pdfService = new PdfService(templateEngine);
        byte[] pdf = pdfService.generatePdf("template", Map.of("name", "test"));

        assertThat(pdf).isNotNull();
        assertThat(pdf.length).isGreaterThan(0);
    }
    //  this class is used to test the PdfService class, which generates PDF files from HTML templates using Thymeleaf. The test method generatePdfReturnsBytes() mocks the TemplateEngine and verifies that the generatePdf() method returns a non-null byte array with a length greater than 0.
}

