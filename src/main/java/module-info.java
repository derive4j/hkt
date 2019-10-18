
module org.derive4j.hkt {
    requires transitive java.compiler;
    requires jdk.compiler;
    requires static derive4j.annotation;
    requires static auto.service.annotations;

    exports org.derive4j.hkt;

    provides javax.annotation.processing.Processor
        with org.derive4j.hkt.processor.HktProcessor;
}
