module velocity {
    requires transitive commons.collections;
    requires transitive commons.lang;
    requires transitive java.logging;

    requires commons.logging;

    exports org.apache.velocity;
    exports org.apache.velocity.anakia;
    exports org.apache.velocity.app;
    exports org.apache.velocity.context;
    exports org.apache.velocity.convert;
    exports org.apache.velocity.exception;
    exports org.apache.velocity.io;
    exports org.apache.velocity.runtime;
    exports org.apache.velocity.servlet;
    exports org.apache.velocity.texen;
    exports org.apache.velocity.util;
}