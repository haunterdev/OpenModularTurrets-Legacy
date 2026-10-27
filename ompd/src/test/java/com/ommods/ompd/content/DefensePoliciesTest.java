package com.ommods.ompd.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefensePoliciesTest {
    @Test
    void fenceSpareListMatchesOneTwelve() {
        assertFalse(DefensePolicies.fenceDamages(true, false, false));
        assertFalse(DefensePolicies.fenceDamages(false, true, true));
        assertTrue(DefensePolicies.fenceDamages(false, true, false));
        assertTrue(DefensePolicies.fenceDamages(false, false, false));
    }

    @Test
    void trapIsSolidOnlyForOwnerPlayer() {
        assertTrue(DefensePolicies.trapIsSolid(true, true));
        assertFalse(DefensePolicies.trapIsSolid(true, false));
        assertFalse(DefensePolicies.trapIsSolid(false, true));
    }
}
