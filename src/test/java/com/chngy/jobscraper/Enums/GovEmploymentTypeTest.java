package com.chngy.jobscraper.Enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

import java.util.List;

public class GovEmploymentTypeTest {

    @Test
    public void govEmploymentTypeWhenGivenAListOfEligibleStringsReturnsListOfCodes (){
        List<String> testArrayStringsEnumConstant = List.of("fullTime", "contract", "internship");
        List<String> testArrayStringsCode = List.of("Full-time", "Contract", "Internship");

        List<String> res = GovEmploymentType.fromEnumConstantToCode(testArrayStringsEnumConstant);
        assertEquals(testArrayStringsCode, res);
    }

    @Test
    public void govEmploymentTypeWhenGivenAListOfIneligibleStringsReturnsEmptyList(){
        List<String> testArrayStringsEnumConstant = List.of("cannot", "bake", "cake");
        List<String> testArrayStringsCode = List.of("");

        List<String> res = GovEmploymentType.fromEnumConstantToCode(testArrayStringsEnumConstant);
        assertEquals(testArrayStringsCode, res);
    }

    @Test
    public void govEmploymentTypeWhenGivenACodeReturnsAGovEmploymentType () {
        String codeString = "Others";
        GovEmploymentType expectedRes = GovEmploymentType.others;

        assertEquals(expectedRes, GovEmploymentType.fromCode(codeString).orElse(null));
    }

    @Test
    public void govEmploymentTypeWhenGivenAnInvalidCodeReturnsNull () {
        String codeString = "notPresent";

        assertNull(GovEmploymentType.fromCode(codeString).orElse(null));
    }
}
