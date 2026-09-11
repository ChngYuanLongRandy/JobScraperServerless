package com.chngy.jobscraper.Enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GovYoe {
    zeroToOne("0+-+1+year"),
    oneToThree("1+-+3+years"),
    fourToSix("4+-+6+years"),
    sevenToNine("7+-+9+years"),
    moreThanTen(">+10+years");

    private final String code;
}
