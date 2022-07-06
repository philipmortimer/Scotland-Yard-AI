package uk.ac.bris.cs.scotlandyard.ui.ai;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Includes all tests
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        CachedDijkstraTest.class,
        MrXLocationsTest.class,
})
public class AllTest {}
