open module kotlin.stdlib.jdk8 {
    exports kotlin.collections.jdk8;
    exports kotlin.internal.jdk8;
    exports kotlin.streams.jdk8;
    exports kotlin.text.jdk8;

    requires transitive kotlin.stdlib;
    requires transitive kotlin.stdlib.jdk7;
}