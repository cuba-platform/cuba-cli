package ${project.rootPackage}

import com.haulmont.cuba.web.testsupport.TestContainer
import java.util.*

open class ${project.testContainerPrefix}WebTestContainer : TestContainer() {

    init {
        appComponents = ArrayList(Arrays.asList(
                "com.haulmont.cuba" // add CUBA add-ons and custom app components here
        ))
        appPropertiesFiles = Arrays.asList( // List the files defined in your web.xml
                // in appPropertiesConfig context parameter of the web module
                "${project.rootPackageDirectory}/web-app.properties",  // Add this file which is located in CUBA and defines some properties
                // specifically for test environment. You can replace it with your own
                // or add another one in the end.
                "com/haulmont/cuba/web/testsupport/test-web-app.properties"
        )
    }

    class Common private constructor() : ${project.testContainerPrefix}WebTestContainer() {

        @Throws(Throwable::class)
        override fun before() {
            if (!initialized) {
                super.before()
                initialized = true
            }
            setupContext()
        }

        override fun after() {
            cleanupContext()
            // never stops - do not call super
        }

        companion object {

            // A common singleton instance of the test container which is initialized once for all tests
            val INSTANCE = Common()
            @Volatile
            private var initialized = false
        }

    }
}