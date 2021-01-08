package com.igorgmoraes.sincronizacaoreceita.model;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AccountInfo {
    private String agencia;
    private String conta;
    private String saldo;
    private String status;
    private boolean resultado;

    /*
    / TODO: Use Enum instead of String in status. Handle error when the given Status isn't A, I, B, or P.
    private enum Status{
        A,
        I,
        B,
        P
    }*/
}
