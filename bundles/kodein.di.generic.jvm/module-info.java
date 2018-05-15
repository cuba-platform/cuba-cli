module kodein.di.generic.jvm {
    requires transitive kodein.di.core.jvm;
    requires transitive kotlin.stdlib;

    exports org.kodein.di.generic;
}