package dev.katsute.jcore;

import org.junit.jupiter.api.*;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class JunitTests {

    private static final PrintStream SysOUT = System.out;
    private final ByteArrayOutputStream OUT = new ByteArrayOutputStream();

    @SuppressWarnings("SpellCheckingInspection")
    @BeforeAll
    public static void beforeAll(){
        Assumptions.assumeTrue(System.getenv("sample_only") == null);
        //System.out.println("::start-group::ENV" + '\n' + System.getenv() + '\n' + "::endgroup::");
        System.out.println("::stop-commands::stop-key");
    }

    @AfterAll
    public static void afterAll(){
        System.out.println("::stop-key::");
    }

    @BeforeEach
    public void beforeEach(){
        System.setOut(new PrintStream(new MultiOutputStream(SysOUT, OUT)));
    }

    @AfterEach
    public void afterEach(){
        System.setOut(SysOUT);
    }

    @SuppressWarnings("SimplifiableAssertion")
    @Test
    public void testAssertion(){
        try{
            Assertions.assertTrue(false, Workflow.errorSupplier("expected expression to be true"));
        }catch(final AssertionFailedError ignored){ }

        if(!"true".equals(System.getenv("CI"))) return;

        final String first = OUT.toString().trim().split("%0A")[0];
        Assertions.assertTrue(first.startsWith("::error "));
        Assertions.assertTrue(first.endsWith(": expected expression to be true"));
    }

    @Test
    public void testAssumption(){
        try{
            Assumptions.assumeTrue(false, Workflow.warningSupplier("expected expression to be true"));
        }catch(final TestAbortedException ignored){ }

        if(!"true".equals(System.getenv("CI"))) return;

        final String first = OUT.toString().trim().split("%0A")[0];
        Assertions.assertTrue(first.startsWith("::warning "));
        Assertions.assertTrue(first.endsWith(": expected expression to be true"));
    }

}