import kotlin.reflect.jvm.internal.impl.builtins.BuiltInsLoader;
import kotlin.reflect.jvm.internal.impl.serialization.deserialization.builtins.BuiltInsLoaderImpl;
import kotlin.reflect.jvm.internal.impl.util.ModuleVisibilityHelper;
import kotlin.reflect.jvm.internal.impl.resolve.ExternalOverridabilityCondition;

open module kotlin.reflect {
    exports kotlin.reflect.full;
    exports kotlin.reflect.jvm;

    uses BuiltInsLoader;
    provides BuiltInsLoader with BuiltInsLoaderImpl;

    uses ModuleVisibilityHelper;
    uses ExternalOverridabilityCondition;

    requires transitive kotlin.stdlib;
}