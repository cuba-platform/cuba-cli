package ${project.rootPackage}.core

import ${project.rootPackage}.${jproject.testContainerPrefix}TestContainer
import com.haulmont.cuba.core.Persistence
import com.haulmont.cuba.core.global.AppBeans
import com.haulmont.cuba.core.global.DataManager
import com.haulmont.cuba.core.global.Metadata
import com.haulmont.cuba.security.entity.User
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class SampleIntegrationTest {

    @Test
    fun testLoadUser() {
        persistence.createTransaction().use { tx ->
            val em = persistence.entityManager
            val query = em.createQuery(
                    "select u from sec\$User u where u.login = :userLogin", User::class.java)
            query.setParameter("userLogin", "admin")
            val users = query.resultList
            tx.commit()
            Assertions.assertEquals(1, users.size)
        }
    }

    companion object {

        private lateinit var metadata: Metadata
        private lateinit var persistence: Persistence
        private lateinit var dataManager: DataManager

        @JvmField
        @RegisterExtension
        val cont: ${jproject.testContainerPrefix}TestContainer = ${jproject.testContainerPrefix}TestContainer.Common.INSTANCE

        @BeforeAll
        @JvmStatic
        @Throws(Exception::class)
        fun beforeAll() {
            metadata = cont.metadata()
            persistence = cont.persistence()
            dataManager = AppBeans.get(DataManager::class.java)
        }

        @AfterAll
        @JvmStatic
        @Throws(Exception::class)
        fun afterAll() {
        }
    }
}