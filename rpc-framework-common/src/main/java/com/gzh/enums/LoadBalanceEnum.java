package com.gzh.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum LoadBalanceEnum {
    LOAD_BALANCE("loadBalance");

    private final String name;
}
