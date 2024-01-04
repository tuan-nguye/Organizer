package tests;


import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * Test suite containing all tests from all classes in the tests.classes directory.
 */
@Suite
@SelectPackages("tests.classes")
public class AllTestsSuite {
}
