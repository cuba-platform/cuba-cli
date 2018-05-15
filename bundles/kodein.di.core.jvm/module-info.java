module kodein.di.core.jvm {
    requires transitive kotlin.stdlib;

    exports org.kodein.di;
    exports org.kodein.di.bindings;
    exports org.kodein.di.internal;
}