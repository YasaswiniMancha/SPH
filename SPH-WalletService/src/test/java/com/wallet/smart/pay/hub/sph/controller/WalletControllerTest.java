package com.wallet.smart.pay.hub.sph.controller;

import com.wallet.smart.pay.hub.sph.controller.WalletController;
import com.wallet.smart.pay.hub.sph.dto.*;
import com.wallet.smart.pay.hub.sph.service.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletControllerTest {

    @Mock
    private WalletService walletService;

    @InjectMocks
    private WalletController walletController;

    @Test
    void createWalletReturnsCreated() {
        CreateWalletRequest req = new CreateWalletRequest("cust-1", "USD", new BigDecimal("100.00"));
        WalletResponse respObj = new WalletResponse(UUID.randomUUID(), "cust-1", "USD", new BigDecimal("100.00"), "ACTIVE");
        when(walletService.createWallet(req)).thenReturn(respObj);

        var resp = walletController.createWallet(req);
        assertEquals(201, resp.getStatusCodeValue());
        assertEquals(respObj, resp.getBody());
    }

    @Test
    void getBalanceReturnsWallet() {
        UUID id = UUID.randomUUID();
        WalletResponse respObj = new WalletResponse(id, "cust-1", "USD", new BigDecimal("50.00"), "ACTIVE");
        when(walletService.getBalance(id)).thenReturn(respObj);

        var result = walletController.getBalance(id);
        assertEquals(respObj, result);
    }

    @Test
    void creditWithIdempotency() {
        UUID id = UUID.randomUUID();
        AmountRequest req = new AmountRequest(new BigDecimal("10.00"), "USD", "topup");
        TransactionResponse txn = new TransactionResponse(UUID.randomUUID(), id, "CREDIT", new BigDecimal("10.00"), "USD", "ref-1", "desc");
        when(walletService.credit(eq(id), eq(req), eq("key-1"))).thenReturn(txn);

        var result = walletController.credit(id, req, "key-1");
        assertEquals(txn, result);
    }

    @Test
    void debit() {
        UUID id = UUID.randomUUID();
        AmountRequest req = new AmountRequest(new BigDecimal("5.00"), "USD", "pay");
        TransactionResponse txn = new TransactionResponse(UUID.randomUUID(), id, "DEBIT", new BigDecimal("5.00"), "USD", "ref-2", "desc");
        when(walletService.debit(eq(id), eq(req), isNull())).thenReturn(txn);

        var result = walletController.debit(id, req, null);
        assertEquals(txn, result);
    }

    @Test
    void transfer() {
        TransferRequest req = new TransferRequest(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1.00"), "xfer");
        TransactionResponse txn = new TransactionResponse(UUID.randomUUID(), req.toWalletId(), "TRANSFER_IN", new BigDecimal("1.00"), "USD", "ref-3", "desc");
        when(walletService.transfer(eq(req), eq("idemp-1"))).thenReturn(txn);

        var result = walletController.transfer(req, "idemp-1");
        assertEquals(txn, result);
    }

    @Test
    void creditNullIdempotency() {
        UUID id = UUID.randomUUID();
        AmountRequest req = new AmountRequest(new BigDecimal("2.00"), "USD", "note");
        TransactionResponse txn = new TransactionResponse(UUID.randomUUID(), id, "CREDIT", new BigDecimal("2.00"), "USD", "ref-4", "desc");
        when(walletService.credit(eq(id), eq(req), isNull())).thenReturn(txn);

        var result = walletController.credit(id, req, null);
        assertEquals(txn, result);
    }
}

