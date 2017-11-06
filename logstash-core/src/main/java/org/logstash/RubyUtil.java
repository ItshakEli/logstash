package org.logstash;

import org.jruby.NativeException;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyException;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.builtin.IRubyObject;
import org.logstash.ackedqueue.ext.JrubyAckedBatchExtLibrary;
import org.logstash.ackedqueue.ext.JrubyAckedQueueExtLibrary;
import org.logstash.ackedqueue.ext.JrubyAckedQueueMemoryExtLibrary;
import org.logstash.ext.JrubyEventExtLibrary;
import org.logstash.ext.JrubyTimestampExtLibrary;

/**
 * Utilities around interaction with the {@link Ruby} runtime.
 */
public final class RubyUtil {

    /**
     * Reference to the global {@link Ruby} runtime.
     */
    public static final Ruby RUBY;

    /**
     * Logstash Ruby Module.
     */
    public static final RubyModule LOGSTASH_MODULE;

    public static final RubyClass RUBY_EVENT_CLASS;

    public static final RubyClass RUBY_ACKED_BATCH_CLASS;

    public static final RubyClass RUBY_TIMESTAMP_CLASS;

    public static final RubyClass PARSER_ERROR;

    public static final RubyClass GENERATOR_ERROR;

    public static final RubyClass LOGSTASH_ERROR;

    public static final RubyClass TIMESTAMP_PARSER_ERROR;

    static {
        RUBY = Ruby.getGlobalRuntime();
        LOGSTASH_MODULE = RUBY.getOrCreateModule("LogStash");
        RUBY_TIMESTAMP_CLASS = setupLogstashClass("Timestamp", new ObjectAllocator() {
            @Override
            public JrubyTimestampExtLibrary.RubyTimestamp allocate(final Ruby runtime,
                final RubyClass rubyClass) {
                return new JrubyTimestampExtLibrary.RubyTimestamp(runtime, rubyClass);
            }
        }, JrubyTimestampExtLibrary.RubyTimestamp.class);
        RUBY_EVENT_CLASS = setupLogstashClass(
            "Event", JrubyEventExtLibrary.RubyEvent::new, JrubyEventExtLibrary.RubyEvent.class
        );
        final RubyModule json = LOGSTASH_MODULE.defineOrGetModuleUnder("Json");
        final RubyClass stdErr = RUBY.getStandardError();
        LOGSTASH_ERROR = LOGSTASH_MODULE.defineClassUnder(
            "Error", stdErr, RubyUtil.LogstashRubyError::new
        );
        PARSER_ERROR = json.defineClassUnder(
            "ParserError", LOGSTASH_ERROR, RubyUtil.LogstashRubyParserError::new
        );
        TIMESTAMP_PARSER_ERROR = LOGSTASH_MODULE.defineClassUnder(
            "TimestampParserError", stdErr, RubyUtil.LogstashTimestampParserError::new
        );
        GENERATOR_ERROR = json.defineClassUnder("GeneratorError", LOGSTASH_ERROR,
            RubyUtil.LogstashRubyGeneratorError::new
        );
        RUBY_EVENT_CLASS.setConstant("METADATA", RUBY.newString(Event.METADATA));
        RUBY_EVENT_CLASS.setConstant(
            "METADATA_BRACKETS", RUBY.newString(Event.METADATA_BRACKETS)
        );
        RUBY_EVENT_CLASS.setConstant("TIMESTAMP", RUBY.newString(Event.TIMESTAMP));
        RUBY_EVENT_CLASS.setConstant(
            "TIMESTAMP_FAILURE_TAG", RUBY.newString(Event.TIMESTAMP_FAILURE_TAG)
        );
        RUBY_EVENT_CLASS.setConstant(
            "TIMESTAMP_FAILURE_FIELD", RUBY.newString(Event.TIMESTAMP_FAILURE_FIELD)
        );
        RUBY_EVENT_CLASS.setConstant("VERSION", RUBY.newString(Event.VERSION));
        RUBY_EVENT_CLASS.setConstant("VERSION_ONE", RUBY.newString(Event.VERSION_ONE));
        RUBY_EVENT_CLASS.defineAnnotatedMethods(JrubyEventExtLibrary.RubyEvent.class);
        RUBY_EVENT_CLASS.defineAnnotatedConstants(JrubyEventExtLibrary.RubyEvent.class);
        RUBY_ACKED_BATCH_CLASS = setupLogstashClass("AckedBatch", new ObjectAllocator() {
            @Override
            public IRubyObject allocate(final Ruby runtime, final RubyClass rubyClass) {
                return new JrubyAckedBatchExtLibrary.RubyAckedBatch(runtime, rubyClass);
            }
        }, JrubyAckedBatchExtLibrary.RubyAckedBatch.class);
        setupLogstashClass(
            "AckedQueue", JrubyAckedQueueExtLibrary.RubyAckedQueue::new,
            JrubyAckedQueueExtLibrary.RubyAckedQueue.class
        );
        setupLogstashClass(
            "AckedMemoryQueue", JrubyAckedQueueMemoryExtLibrary.RubyAckedMemoryQueue::new,
            JrubyAckedQueueMemoryExtLibrary.RubyAckedMemoryQueue.class
        );
    }

    private RubyUtil() {
    }

    /**
     * Wraps a Java exception in a JRuby IOError NativeException.
     * This preserves the Java stacktrace and bubble up as a Ruby IOError
     * @param runtime the Ruby runtime context
     * @param e the Java exception to wrap
     * @return RaiseException the wrapped IOError
     */
    public static RaiseException newRubyIOError(Ruby runtime, Throwable e) {
        // will preserve Java stacktrace & bubble up as a Ruby IOError
        return new RaiseException(e, new NativeException(runtime, runtime.getIOError(), e));
    }

    /**
     * Sets up a Java-defined {@link RubyClass} in the Logstash Ruby module.
     * @param name Name of the class
     * @param allocator Allocator of the class
     * @param jclass Underlying Java class that is annotated by {@link JRubyClass}
     * @return RubyClass
     */
    private static RubyClass setupLogstashClass(final String name,
        final ObjectAllocator allocator, final Class<?> jclass) {
        final RubyClass clazz = RUBY.defineClassUnder(
            name, RUBY.getObject(), allocator, LOGSTASH_MODULE
        );
        clazz.defineAnnotatedMethods(jclass);
        return clazz;
    }

    @JRubyClass(name = "Error")
    public static final class LogstashRubyError extends RubyException {

        public LogstashRubyError(final Ruby runtime, final RubyClass metaClass) {
            super(runtime, metaClass);
        }
    }

    @JRubyClass(name = "ParserError")
    public static final class LogstashRubyParserError extends RubyException {

        public LogstashRubyParserError(final Ruby runtime, final RubyClass metaClass) {
            super(runtime, metaClass);
        }
    }

    @JRubyClass(name = "GeneratorError")
    public static final class LogstashRubyGeneratorError extends RubyException {

        public LogstashRubyGeneratorError(final Ruby runtime, final RubyClass metaClass) {
            super(runtime, metaClass);
        }
    }

    @JRubyClass(name = "TimestampParserError")
    public static final class LogstashTimestampParserError extends RubyException {

        public LogstashTimestampParserError(final Ruby runtime, final RubyClass metaClass) {
            super(runtime, metaClass);
        }
    }
}
