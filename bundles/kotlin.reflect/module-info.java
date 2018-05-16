open module kotlin.reflect {
    exports kotlin.reflect.full;
    exports kotlin.reflect.jvm;

    uses kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader;

    requires transitive kotlin.stdlib;
}