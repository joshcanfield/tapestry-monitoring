package com.joshcanfield.tapestry5.internal.monitor;

import com.joshcanfield.tapestry5.annotations.Monitor;
import org.apache.tapestry5.plastic.MethodInvocation;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.javasimon.Stopwatch;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.Map;

import static com.joshcanfield.tapestry5.annotations.Monitor.ExceptionFilter;
import static com.joshcanfield.tapestry5.annotations.Monitor.ExceptionFilter.Strategy;
import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Test(
        // Tests share mock objects so don't run them concurrently
        singleThreaded = true
)
public class TestMonitorAdvice {
    final IMocksControl control = EasyMock.createControl();
    final Stopwatch stopwatch = control.createMock(Stopwatch.class);
    final Monitor monitor = control.createMock(Monitor.class);
    @SuppressWarnings("unchecked")
    final Map<String, Stopwatch> exceptionMap = control.createMock(Map.class);
    final MethodInvocation invocation = control.createMock(MethodInvocation.class);

    final MonitorAdvice monitorAdvice = new MonitorAdvice(stopwatch, monitor, exceptionMap);

    @BeforeMethod
    public void resetControl() {
        control.reset();
    }

    @Test
    public void normal_flow() {

        expect(invocation.proceed()).andReturn(invocation);
        expect(invocation.didThrowCheckedException()).andReturn(false);
        expect(stopwatch.addTime(anyLong())).andReturn(stopwatch);

        control.replay();

        monitorAdvice.advise(invocation);

        control.verify();
    }

    @Test
    public void segregate_checked_exception() {
        final ExceptionFilter filter = setupCheckedExceptionTest(Strategy.Segregate);

        final Stopwatch exceptionStopwatch = control.createMock(Stopwatch.class);
        expect(filter.name()).andReturn("error");
        expect(exceptionMap.get(eq("error"))).andReturn(exceptionStopwatch);

        expect(exceptionStopwatch.getName()).andReturn("test.stopwatch.name");
        expect(exceptionStopwatch.addTime(anyLong())).andReturn(exceptionStopwatch);

        control.replay();

        monitorAdvice.advise(invocation);

        control.verify();
    }

    @Test
    public void included_checked_exception() {
        setupCheckedExceptionTest(Strategy.Include);

        // The original stopwatch should be called
        expect(stopwatch.addTime(anyLong())).andReturn(stopwatch);
        expect(stopwatch.getName()).andReturn("test.stopwatch.name");

        control.replay();

        monitorAdvice.advise(invocation);

        control.verify();
    }

    @Test
    public void ignored_checked_exception() {
        setupCheckedExceptionTest(Strategy.Ignore);

        expect(stopwatch.getName()).andReturn("test.stopwatch.name");

        control.replay();

        monitorAdvice.advise(invocation);

        control.verify();
    }

    @Test
    public void ignored_runtime_exception() {
        setupRuntimeExceptionTest(Strategy.Ignore);

        expect(stopwatch.getName()).andReturn("test.stopwatch.name");

        control.replay();

        boolean excepted = false;
        try {
            monitorAdvice.advise(invocation);
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "TEST");
            excepted = true;
        }

        assertTrue(excepted, "expecting exception");
        control.verify();
    }

    @Test
    public void included_runtime_exception() {
        final ExceptionFilter filter = setupRuntimeExceptionTest(Strategy.Include);

        expect(stopwatch.addTime(anyLong())).andReturn(stopwatch);
        expect(stopwatch.getName()).andReturn("test.stopwatch.name");

        control.replay();

        boolean excepted = false;
        try {
            monitorAdvice.advise(invocation);
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "TEST");
            excepted = true;
        }

        assertTrue(excepted, "expecting exception");
        control.verify();
    }

    @Test
    public void segregated_runtime_exception() {
        final ExceptionFilter filter = setupRuntimeExceptionTest(Strategy.Segregate);

        final Stopwatch exceptionStopwatch = control.createMock(Stopwatch.class);
        expect(filter.name()).andReturn("error");
        expect(exceptionStopwatch.addTime(anyLong())).andReturn(exceptionStopwatch);
        expect(exceptionStopwatch.getName()).andReturn("test.stopwatch.name");
        expect(exceptionMap.get(eq("error"))).andReturn(exceptionStopwatch);

        control.replay();

        boolean excepted = false;
        try {
            monitorAdvice.advise(invocation);
        } catch (RuntimeException e) {
            assertEquals(e.getMessage(), "TEST");
            excepted = true;
        }

        assertTrue(excepted, "expecting exception");
        control.verify();
    }

    @Test
    public void assignable_from_filters() {

        // SQLDataException extends SQLNonTransientException extends SQLException
        ExceptionFilter sqlExceptionFilter = control.createMock(ExceptionFilter.class);
        expect(sqlExceptionFilter.name()).andReturn("sql_error");
        expect(sqlExceptionFilter.value()).andReturn(new Class[]{SQLException.class});
        expect(sqlExceptionFilter.strategy()).andReturn(Strategy.Segregate);

        ExceptionFilter exceptionFilter = control.createMock(ExceptionFilter.class);
        ExceptionFilter[] exceptions = new ExceptionFilter[]{sqlExceptionFilter, exceptionFilter};

        expect(monitor.exceptions()).andReturn(exceptions);

        final Stopwatch exceptionStopwatch = control.createMock(Stopwatch.class);
        expect(exceptionStopwatch.addTime(anyLong())).andReturn(exceptionStopwatch);
        expect(exceptionStopwatch.getName()).andReturn("test.stopwatch.name");
        expect(exceptionMap.get(eq("sql_error"))).andReturn(exceptionStopwatch);

        expect(invocation.proceed()).andReturn(invocation);
        expect(invocation.didThrowCheckedException()).andReturn(true);
        expect(invocation.getCheckedException(Exception.class)).andReturn(new SQLDataException("Test Exception"));
        invocation.rethrow();
        expectLastCall();

        control.replay();

        monitorAdvice.advise(invocation);

        control.verify();
    }

    private ExceptionFilter setupCheckedExceptionTest(Strategy strategy) {
        expect(invocation.proceed()).andReturn(invocation);
        expect(invocation.didThrowCheckedException()).andReturn(true);
        expect(invocation.getCheckedException(Exception.class)).andReturn(new Exception("Test Exception"));
        invocation.rethrow();
        expectLastCall();


        ExceptionFilter filter = control.createMock(ExceptionFilter.class);
        ExceptionFilter[] exceptions = new ExceptionFilter[]{filter};
        expect(monitor.exceptions()).andReturn(exceptions);
        expect(filter.value()).andReturn(new Class[]{Exception.class});
        expect(filter.strategy()).andReturn(strategy);
        return filter;
    }

    private ExceptionFilter setupRuntimeExceptionTest(Strategy strategy) {
        expect(invocation.proceed()).andThrow(new RuntimeException("TEST"));
        expect(invocation.didThrowCheckedException()).andReturn(false);

        ExceptionFilter filter = control.createMock(ExceptionFilter.class);
        ExceptionFilter[] exceptions = new ExceptionFilter[]{filter};
        expect(monitor.exceptions()).andReturn(exceptions);
        expect(filter.value()).andReturn(new Class[]{Exception.class});
        expect(filter.strategy()).andReturn(strategy);
        return filter;
    }
}
