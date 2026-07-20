package com.merchant.smart.pay.hub.sph.controller;

import com.common.smart.pay.hub.sph.dto.response.ApiResponse;
import com.merchant.smart.pay.hub.sph.controller.MerchantController;
import com.merchant.smart.pay.hub.sph.dto.request.MerchantDTO;
import com.merchant.smart.pay.hub.sph.dto.response.MerchantResponseDTO;
import com.merchant.smart.pay.hub.sph.service.MerchantService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantControllerTest {

    @Mock
    private MerchantService merchantService;

    @InjectMocks
    private MerchantController merchantController;

    @Test
    void createMerchant() {
        MerchantDTO req = MerchantDTO.builder().merchantCode("M1").businessName("Biz").businessDescription("D").businessEmail("a@b.com").businessPhone("+1234567890").businessAddress("addr").businessCity("city").businessState("st").businessZipCode("00000").contactPersonName("cp").contactPersonEmail("cp@b.com").contactPersonPhone("+1234567890").build();
        MerchantResponseDTO resp = MerchantResponseDTO.builder().id("1").merchantCode("M1").businessName("Biz").build();
        when(merchantService.createMerchant(req)).thenReturn(resp);

        var response = merchantController.createMerchant(req);
        assertEquals(201, response.getStatusCodeValue());
        assertTrue(response.getBody() instanceof ApiResponse);
    }

    @Test
    void getMerchantById() {
        MerchantResponseDTO resp = MerchantResponseDTO.builder().id("1").merchantCode("M1").businessName("Biz").build();
        when(merchantService.getMerchantById("1")).thenReturn(resp);

        var r = merchantController.getMerchantById("1");
        assertEquals(200, r.getStatusCodeValue());
    }

    @Test
    void getMerchantByCode() {
        MerchantResponseDTO resp = MerchantResponseDTO.builder().id("1").merchantCode("M1").businessName("Biz").build();
        when(merchantService.getMerchantByCode("M1")).thenReturn(resp);

        var r = merchantController.getMerchantByCode("M1");
        assertEquals(200, r.getStatusCodeValue());
    }

    @Test
    void getAllMerchants() {
        MerchantResponseDTO dto = MerchantResponseDTO.builder().id("1").merchantCode("M1").businessName("Biz").build();
        Page<MerchantResponseDTO> page = new PageImpl<>(List.of(dto));
        when(merchantService.getAllMerchants(any())).thenReturn(page);

        var r = merchantController.getAllMerchants(0, 20);
        assertEquals(200, r.getStatusCodeValue());
    }

    @Test
    void updateMerchant() {
        MerchantDTO req = new MerchantDTO();
        MerchantResponseDTO resp = MerchantResponseDTO.builder().id("1").merchantCode("M1").businessName("Biz").build();
        when(merchantService.updateMerchant(eq("1"), any())).thenReturn(resp);

        var r = merchantController.updateMerchant("1", req);
        assertEquals(200, r.getStatusCodeValue());
    }

    @Test
    void deleteMerchant() {
        doNothing().when(merchantService).deleteMerchant("1");
        var r = merchantController.deleteMerchant("1");
        assertEquals(200, r.getStatusCodeValue());
        verify(merchantService).deleteMerchant("1");
    }
}

