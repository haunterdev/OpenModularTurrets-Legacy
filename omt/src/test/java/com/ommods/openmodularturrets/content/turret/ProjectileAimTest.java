package com.ommods.openmodularturrets.content.turret;

import com.ommods.openmodularturrets.content.turret.ProjectileAim.Solution;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The faithful 1.12 ProjectileTurret.doTargetedShot lead math, pure so it unit tests without the
 * game. From a turret block and a target (with height and velocity) it produces the launch direction
 * and speed: aim at the target's mid height, lead by the flight time, and scale speed by how far the
 * lead stretched the vector. Non-gravity shots fly faster (time = dist/3.0) than arced shots
 * (dist/1.6), and the direction gets the same -0.1 vertical nudge the original applied.
 */
class ProjectileAimTest {

    private static final double EPS = 1.0e-6;

    @Test
    void stationaryTargetAimsStraightAtFullBaseSpeed() {
        // Target 10 blocks east at the turret's height, not moving: no lead, base speed 3.0.
        Solution s = ProjectileAim.solve(0, 0, 0, 10.5, 0.5, 0.5, 0.0, 0.0, 0.0, 0.0, false);
        assertEquals(10.0, s.dirX(), EPS);
        assertEquals(-0.1, s.dirY(), EPS);
        assertEquals(0.0, s.dirZ(), EPS);
        assertEquals(3.0f, s.velocity(), 1.0e-4f);
    }

    @Test
    void movingTargetLeadsInDirectionOfTravelForNonGravityShot() {
        // Same geometry, target sliding +Z at 3 b/t. time = dist/3 = 10/3, so lead = 3 * 10/3 = 10.
        Solution s = ProjectileAim.solve(0, 0, 0, 10.5, 0.5, 0.5, 0.0, 0.0, 0.0, 3.0, false);
        assertEquals(10.0, s.dirX(), EPS);
        assertEquals(10.0, s.dirZ(), EPS);
        // speedFactor = |(10,0,10)| / 10 = sqrt(2); velocity = 3 * sqrt(2).
        assertEquals((float) (3.0 * Math.sqrt(2.0)), s.velocity(), 1.0e-4f);
    }

    @Test
    void gravityShotUsesSlowerFlightTimeSoItLeadsFurther() {
        // Gravity arc: time = dist/1.6 = 6.25, lead = 3 * 6.25 = 18.75.
        Solution s = ProjectileAim.solve(0, 0, 0, 10.5, 0.5, 0.5, 0.0, 0.0, 0.0, 3.0, true);
        assertEquals(18.75, s.dirZ(), EPS);
    }

    @Test
    void aimsAtTargetMidHeightNotFeet() {
        // A 2-block-tall target: aim rises by height*0.5 = 1.0 above the feet.
        // d1 = (ty + h*0.5) - (by + 0.5) = (0.5 + 1.0) - 0.5 = 1.0, then the -0.1 nudge.
        Solution s = ProjectileAim.solve(0, 0, 0, 10.5, 0.5, 0.5, 2.0, 0.0, 0.0, 0.0, false);
        assertEquals(0.9, s.dirY(), EPS);
    }
}
